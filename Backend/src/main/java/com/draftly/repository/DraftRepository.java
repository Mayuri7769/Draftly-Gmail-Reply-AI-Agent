package com.draftly.repository;

import com.draftly.model.EmailDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DraftRepository extends JpaRepository<EmailDraft, String> {

    /**
     * Requirement: Logging and History.
     * Fetches all drafts with a specific status (e.g., "SENT", "PENDING").
     */
    List<EmailDraft> findByStatus(String status);

    /**
     * Requirement: Learn from user patterns.
     * Fetches the most recent drafts to infer tone and style.
     */
    List<EmailDraft> findTop5ByOrderByCreatedAtDesc();

    /**
     * Finds a specific draft by its thread ID to maintain conversation integrity.
     */
    EmailDraft findByThreadId(String threadId);
}