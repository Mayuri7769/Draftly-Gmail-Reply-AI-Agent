package com.draftly.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_drafts")
public class EmailDraft {

    @Id
    private String id; // The Gmail Message ID (Unique identifier)

    private String threadId; // Requirement: Maintain thread integrity
    private String sender;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body; // Requirement: Store AI-generated draft content

    private String status; // PENDING, DRAFT_GENERATED, APPROVED, SENT, FAILED
    private String tone;   // formal, concise, friendly

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // Requirement: Track status changes for Step 6

    // Default Constructor
    public EmailDraft() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}