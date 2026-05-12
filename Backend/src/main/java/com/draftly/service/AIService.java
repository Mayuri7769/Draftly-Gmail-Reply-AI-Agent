package com.draftly.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    @Value("${ai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Requirement: Generate contextually relevant reply drafts[cite: 15].
     * Supports various tones (formal, concise, friendly).
     */
    public String generateResponse(String emailContent, String tone, List<String> pastEmails) {
        // Step 3: Analyze previously sent emails to infer style
        String userStyle = inferUserStyle(pastEmails);

        String prompt = constructPrompt(emailContent, tone, userStyle);

        try {
            return callAIProvider(prompt);
        } catch (Exception e) {
            // Fallback keeps the product usable when external AI provider fails.
            return buildFallbackDraft(emailContent, tone);
        }
    }

    private String constructPrompt(String content, String tone, String styleSnippet) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant helping a professional draft email replies. ");
        sb.append("The tone of the reply should be: ").append(tone).append(". "); //

        // Incorporating the inferred style
        sb.append("Apply the following style preference: ").append(styleSnippet).append(". ");

        sb.append("\n\nOriginal Email Content (Context):\n").append(content); //
        sb.append("\n\nInstructions: Draft a helpful, polite reply. Provide body text only.");

        return sb.toString();
    }

    /**
     * Requirement 3: Analyze previously sent emails to infer specific style.
     */
    public String inferUserStyle(List<String> pastSentEmails) {
        if (pastSentEmails == null || pastSentEmails.isEmpty()) {
            return "Professional and polite.";
        }
        // Logic to analyze style patterns goes here
        return "The user typically uses concise sentences and professional sign-offs.";
    }

    private String callAIProvider(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API key is missing.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
        Map<String, Object> data = response.getBody();
        if (data == null) {
            throw new IllegalStateException("Empty AI response.");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("AI response has no candidates.");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = content == null ? null : (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty() || parts.get(0).get("text") == null) {
            throw new IllegalStateException("AI response text is missing.");
        }

        return parts.get(0).get("text").toString().trim();
    }

    private String buildFallbackDraft(String emailContent, String tone) {
        String opening;
        String middle = "Thank you for your email. I have reviewed the details and will get back to you shortly with the next steps.";
        String closing;

        String normalizedTone = tone == null ? "" : tone.trim().toLowerCase();
        switch (normalizedTone) {
            case "friendly":
                opening = "Hi,";
                closing = "Thanks for your patience!";
                break;
            case "concise":
                opening = "Hello,";
                middle = "Thanks for the email. I reviewed it and will respond with next steps soon.";
                closing = "Regards.";
                break;
            case "formal":
            default:
                opening = "Dear Sender,";
                closing = "Best regards.";
                break;
        }

        if (emailContent != null && emailContent.toLowerCase().contains("interview")) {
            middle = "Thank you for sharing the interview-related update. I have reviewed it and will respond shortly.";
        }

        return opening + "\n\n" + middle + "\n\n" + closing;
    }
}