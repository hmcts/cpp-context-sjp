package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.List;

@Event(ProsecutionAuthorityAccessDenied.EVENT_NAME)
public class ProsecutionAuthorityAccessDenied  implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String caseAuthority;
    private final List<String> prosecutorAuthorityAccess;

    public static final String EVENT_NAME = "sjp.events.prosecution-authority-access-denied";


    public ProsecutionAuthorityAccessDenied(final String caseAuthority,
                                            final List<String> prosecutorAuthorityAccess) {
        this.caseAuthority = caseAuthority;
        this.prosecutorAuthorityAccess = prosecutorAuthorityAccess;
    }

    public String getCaseAuthority() {
        return caseAuthority;
    }

    public List<String> getProsecutorAuthorityAccess() {
        return prosecutorAuthorityAccess;
    }
}
