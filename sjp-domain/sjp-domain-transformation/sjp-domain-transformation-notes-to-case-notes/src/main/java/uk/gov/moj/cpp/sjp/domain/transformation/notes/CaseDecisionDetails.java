package uk.gov.moj.cpp.sjp.domain.transformation.notes;

import java.util.UUID;

public class CaseDecisionDetails {

    private UUID decisionId;
    private String decisionNotes;
    private final String userId;
    private final String createdAt;
    private final String noteType;

    public CaseDecisionDetails(final UUID decisionId, final String decisionNotes, final String userId, final String createdAt, final String noteType) {
        this.decisionId = decisionId;
        this.decisionNotes = decisionNotes;
        this.userId = userId;
        this.createdAt = createdAt;
        this.noteType = noteType;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public String getUserId() {
        return userId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getNoteType() {
        return noteType;
    }
}
