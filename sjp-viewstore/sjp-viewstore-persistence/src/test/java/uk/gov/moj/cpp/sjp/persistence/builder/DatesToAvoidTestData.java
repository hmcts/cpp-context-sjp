package uk.gov.moj.cpp.sjp.persistence.builder;

import java.time.ZonedDateTime;
import java.util.UUID;

public class DatesToAvoidTestData {

    private final UUID caseId;
    private final String prosecutingAuthority;
    private final String previouslySubmittedDatesToAvoid;
    private final boolean inSession;
    private final boolean completed;
    private final ZonedDateTime pleaDate;

    public DatesToAvoidTestData(final String prosecutingAuthority, final String previouslySubmittedDatesToAvoid,
                                final boolean inSession, final boolean completed, final ZonedDateTime pleaDate) {
        this.caseId = UUID.randomUUID();
        this.prosecutingAuthority = prosecutingAuthority;
        this.previouslySubmittedDatesToAvoid = previouslySubmittedDatesToAvoid;
        this.inSession = inSession;
        this.completed = completed;
        this.pleaDate = pleaDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getPreviouslySubmittedDatesToAvoid() {
        return previouslySubmittedDatesToAvoid;
    }

    public boolean isInSession() {
        return inSession;
    }

    public boolean isCompleted() {
        return completed;
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }
}
