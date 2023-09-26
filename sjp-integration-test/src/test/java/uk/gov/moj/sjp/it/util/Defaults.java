package uk.gov.moj.sjp.it.util;

import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.json.schemas.domains.sjp.User;

import java.util.UUID;

public class Defaults {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("58ea6e5f-193d-49cc-af43-edfed4f5e5fc");
    public static final User DEFAULT_USER = new User("John", "Smith", DEFAULT_USER_ID);
    public static final DelegatedPowers DEFAULT_LEGAL_ADVISER = DelegatedPowers.delegatedPowers().withFirstName("Erica").withLastName("Wilson").withUserId(UUID.fromString("58ea6e5f-193d-49cc-af43-edfed4f5e5fc")).build();


    public static final String DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    public static final String DEFAULT_NON_LONDON_LJA_NATIONAL_COURT_CODE = "2905";
    public static final String DEFAULT_LONDON_COURT_HOUSE_OU_CODE = "B01OK";
    public static final String DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE = "B20EB00";
    public static final String DEFAULT_AOCP_COURT_HOUSE_OU_CODE = "B52CM00";
    public static final String DEFAULT_AOCP_LJA_NATIONAL_COURT_CODE = "1450";
}
