package com.draftly.service;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import com.draftly.model.EmailDraft;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.security.GeneralSecurityException;

@Service
public class GmailService {

    /**
     * Step 2: Fetch unread emails and extract essential metadata.
     */
    public List<Message> fetchUnreadEmails(Gmail service) throws IOException {
        ListMessagesResponse response = service.users().messages().list("me")
                .setQ("is:unread")
                .execute();

        List<Message> fullMessages = new ArrayList<>();
        if (response.getMessages() != null) {
            for (Message message : response.getMessages()) {
                Message fullMsg = service.users().messages().get("me", message.getId())
                        .setFormat("full")
                        .execute();
                fullMessages.add(fullMsg);
            }
        }
        return fullMessages;
    }

    /**
     * Step 5 & 6: Send approved drafts with automated retry logic.
     */
    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendReply(Gmail service, EmailDraft draft) throws Exception {
        String to = extractEmailAddress(draft.getSender());
        String subject = draft.getSubject() == null ? "" : draft.getSubject();
        if (!subject.toLowerCase().startsWith("re:")) {
            subject = "Re: " + subject;
        }
        String body = draft.getBody() == null || draft.getBody().isBlank()
                ? "Thanks for your email."
                : draft.getBody();

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        mimeMessage.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to));
        mimeMessage.setSubject(subject, StandardCharsets.UTF_8.name());
        mimeMessage.setText(body, StandardCharsets.UTF_8.name());

        // Keeps the message in the same conversation thread on Gmail.
        mimeMessage.setHeader("In-Reply-To", draft.getId());
        mimeMessage.setHeader("References", draft.getId());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());

        Message gmailMessage = new Message();
        gmailMessage.setRaw(raw);
        gmailMessage.setThreadId(draft.getThreadId());
        service.users().messages().send("me", gmailMessage).execute();
        System.out.println("Email sent successfully to thread: " + draft.getThreadId());
    }

    /**
     * Step 2: Map Gmail Message object to internal Database model.
     */
    public EmailDraft mapMessageToDraft(Message message) {
        EmailDraft draft = new EmailDraft();
        draft.setId(message.getId());
        draft.setThreadId(message.getThreadId());
        draft.setSender("Unknown Sender");
        draft.setSubject("(No Subject)");

        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            List<MessagePartHeader> headers = message.getPayload().getHeaders();
            for (MessagePartHeader header : headers) {
                if ("From".equalsIgnoreCase(header.getName())) draft.setSender(header.getValue());
                if ("Subject".equalsIgnoreCase(header.getName())) draft.setSubject(header.getValue());
            }
        }
        draft.setBody(message.getSnippet());
        if (message.getInternalDate() != null) {
            LocalDateTime receivedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(message.getInternalDate()),
                    ZoneId.systemDefault()
            );
            draft.setCreatedAt(receivedAt);
            draft.setUpdatedAt(receivedAt);
        }
        return draft;
    }

    /**
     * Step 6: Log extracted metadata for monitoring/debugging.
     */
    public void logEmailMetadata(Message message) {
        String threadId = message.getThreadId();
        String snippet = message.getSnippet();
        System.out.println("LOG: Processing Thread ID: " + threadId + " | Snippet: " + snippet);
    }


    public Gmail getGmailClient(OAuth2AuthorizedClient authorizedClient) throws GeneralSecurityException, IOException {
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Google OAuth session is missing. Please log in again.");
        }

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> {
                    String token = authorizedClient.getAccessToken().getTokenValue();
                    request.getHeaders().setAuthorization("Bearer " + token);
                }
        )
                .setApplicationName("Draftly")
                .build();
    }

    private String extractEmailAddress(String sender) {
        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException("Sender email is missing.");
        }
        int lt = sender.indexOf('<');
        int gt = sender.indexOf('>');
        if (lt >= 0 && gt > lt) {
            return sender.substring(lt + 1, gt).trim();
        }
        return sender.trim();
    }
}