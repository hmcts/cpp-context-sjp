package uk.gov.moj.cpp.sjp.query.view.converter;

import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;

public class ProsecutingAuthorityAccessFilterConverter {

    public String convertToProsecutingAuthorityAccessFilter(final ProsecutingAuthorityAccess prosecutingAuthorityAccess) {

        if (prosecutingAuthorityAccess.equals(ProsecutingAuthorityAccess.NONE)) {
            return null;
        } else if (ProsecutingAuthorityAccess.ALL.equals(prosecutingAuthorityAccess)) {
            return "%";
        } else {
            return prosecutingAuthorityAccess.getProsecutingAuthority();
        }
    }
}
