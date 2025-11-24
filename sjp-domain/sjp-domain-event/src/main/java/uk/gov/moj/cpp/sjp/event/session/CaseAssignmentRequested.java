package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Session;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

@Event(CaseAssignmentRequested.EVENT_NAME)
public class CaseAssignmentRequested {

    public static final String EVENT_NAME = "sjp.events.case-assignment-requested";

    private final Session session;

    public CaseAssignmentRequested(@JsonProperty("session") final Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseAssignmentRequested that = (CaseAssignmentRequested) o;
        return Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }
}
