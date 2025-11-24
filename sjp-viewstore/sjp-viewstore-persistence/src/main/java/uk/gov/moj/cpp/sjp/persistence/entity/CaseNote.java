package uk.gov.moj.cpp.sjp.persistence.entity;


import uk.gov.justice.json.schemas.domains.sjp.NoteType;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "case_note")
public class CaseNote {

    @Id
    @Column(name = "note_id")
    private UUID noteId;

    @JsonIgnore
    @Column(name = "case_id")
    private UUID caseId;

    @JsonIgnore
    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "author_first_name")
    private String authorFirstName;

    @Column(name = "author_last_name")
    private String authorLastName;

    @Column(name = "note_text")
    private String noteText;

    @Column(name = "note_type")
    @Enumerated(EnumType.STRING)
    private NoteType noteType;

    @Column(name = "added_at")
    private ZonedDateTime addedAt;

    @Column(name = "decision_id")
    private UUID decisionId;

    public UUID getNoteId() {
        return noteId;
    }

    public void setNoteId(UUID noteId) {
        this.noteId = noteId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(UUID authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getAuthorFirstName() {
        return authorFirstName;
    }

    public void setAuthorFirstName(String authorFirstName) {
        this.authorFirstName = authorFirstName;
    }

    public String getAuthorLastName() {
        return authorLastName;
    }

    public void setAuthorLastName(String authorLastName) {
        this.authorLastName = authorLastName;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
    }

    public ZonedDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(ZonedDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public Optional<UUID> getDecisionId() {
        return Optional.ofNullable(decisionId);
    }

    public void setDecisionId(UUID decisionId) {
        this.decisionId = decisionId;
    }
}
