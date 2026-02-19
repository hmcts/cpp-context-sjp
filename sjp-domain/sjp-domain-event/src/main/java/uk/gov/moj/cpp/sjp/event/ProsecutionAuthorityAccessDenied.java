package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.List;

@Event(ProsecutionAuthorityAccessDenied.EVENT_NAME)
public class ProsecutionAuthorityAccessDenied {

    private final String userAuthority;
    private final String caseAuthority;
    private final List<String> agentProsecutorAuthorityAccess;

    public static final String EVENT_NAME = "sjp.events.prosecution-authority-access-denied";


    public ProsecutionAuthorityAccessDenied(final String userAuthority, final String caseAuthority,
                                            final List<String> agentProsecutorAuthorityAccess) {
        this.userAuthority = userAuthority;
        this.caseAuthority = caseAuthority;
        this.agentProsecutorAuthorityAccess = agentProsecutorAuthorityAccess;
    }

    public String getUserAuthority() {
        return userAuthority;
    }

    public String getCaseAuthority() {
        return caseAuthority;
    }

    public List<String> getAgentProsecutorAuthorityAccess() {
        return agentProsecutorAuthorityAccess;
    }
}
