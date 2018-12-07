package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Session;

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

}
