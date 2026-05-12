package com.draftly.controller;

import com.draftly.model.EmailDraft;
import com.draftly.repository.DraftRepository;
import com.draftly.service.GmailService;
import com.draftly.service.AIService;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class DraftController {

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private GmailService gmailService;

    @Autowired
    private AIService aiService;



    /**
     * Step 2: Fetch and Log unread emails.
     */
    @GetMapping("/emails/unread")
    public ResponseEntity<?> getUnreadEmails(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) throws Exception {
        try {
            // 1. Get the Gmail client dynamically for THIS specific logged-in user
            Gmail gmailClient = gmailService.getGmailClient(authorizedClient);

            // 2. Fetch raw messages
            List<Message> messages = gmailService.fetchUnreadEmails(gmailClient);

            // 3. Map and Save
            List<EmailDraft> drafts = messages.stream()
                    .map(message -> gmailService.mapMessageToDraft(message))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(draftRepository.saveAll(drafts));
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 403) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                        Map.of(
                                "error", "gmail_api_unavailable",
                                "message", "Gmail API is disabled or not configured for your Google Cloud project. Enable Gmail API in Google Cloud Console and retry in a few minutes."
                        )
                );
            }
            throw e;
        }
    }

    /**
     * Step 3 & 4: Generate draft and store for user review.
     */
    @PostMapping("/ai/generate")
    public ResponseEntity<EmailDraft> generateAndSaveDraft(@RequestParam String id, @RequestParam String tone) {
        EmailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        List<String> pastBodies = draftRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(EmailDraft::getBody)
                .filter(body -> body != null && !body.isBlank())
                .collect(Collectors.toList());
        String context = (draft.getSubject() == null ? "" : draft.getSubject()) +
                "\n\n" + (draft.getBody() == null ? "" : draft.getBody());
        String aiResponse = aiService.generateResponse(context, tone, pastBodies);

        draft.setBody(aiResponse);
        draft.setTone(tone);
        draft.setStatus("DRAFT_GENERATED");
        draft.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(draftRepository.save(draft));
    }

    /**
     * Step 4 & 5: Review, Approve, and Securely Send.
     */
    @PostMapping("/drafts/approve-and-send")
    public ResponseEntity<String> approveAndSend(@RequestParam String id,
                                                 @RequestParam(required = false) String editedBody,
                                                 @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) throws Exception {

        // You MUST get the client here too!
        Gmail gmailClient = gmailService.getGmailClient(authorizedClient);

        EmailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        try {
            if (editedBody != null && !editedBody.isBlank()) {
                draft.setBody(editedBody);
                draft.setStatus("EDITED");
            }
            // Step 4: Enforce the rule that only approved drafts can be sent
            draft.setStatus("APPROVED");

            // Step 5 & 6: Send via Gmail with Retry logic enabled in the service
            gmailService.sendReply(gmailClient, draft);

            draft.setStatus("SENT");
            draft.setUpdatedAt(LocalDateTime.now());
            draftRepository.save(draft);

            return ResponseEntity.ok("Email approved and sent successfully!");
        } catch (Exception e) {
            // Step 6: Log failure
            draft.setStatus("FAILED");
            draftRepository.save(draft);
            return ResponseEntity.internalServerError().body("Failed to send: " + e.getMessage());
        }
    }

    @PutMapping("/drafts/{id}")
    public ResponseEntity<EmailDraft> editDraft(@PathVariable String id, @RequestBody Map<String, String> payload) {
        EmailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        String updatedBody = payload.get("body");
        if (updatedBody == null || updatedBody.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        draft.setBody(updatedBody);
        draft.setStatus("EDITED");
        draft.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(draftRepository.save(draft));
    }

    @PostMapping("/drafts/reject")
    public ResponseEntity<String> rejectDraft(@RequestParam String id) {
        EmailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));
        draft.setStatus("REJECTED");
        draft.setUpdatedAt(LocalDateTime.now());
        draftRepository.save(draft);
        return ResponseEntity.ok("Draft rejected");
    }

    /**
     * Step 6: History View for Frontend monitoring.
     */
    @GetMapping("/history")
    public List<EmailDraft> getHistory(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return draftRepository.findAll();
        }
        return draftRepository.findByStatus(status.toUpperCase());
    }
}