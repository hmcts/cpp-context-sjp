package uk.gov.moj.sjp.it.test;

import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.CitizenHelper;
import uk.gov.moj.sjp.it.stub.PeopleStub;

import org.junit.BeforeClass;
import org.junit.Test;

public class GetCaseByUrnAndPostcodeIT {

    private static final String POSTCODE = "CR0 1XG";
    private static String urn;
    private CitizenHelper citizenHelper = new CitizenHelper();

    @BeforeClass
    public static void init() {
        // Stub feature switching (Authorisation service)
        stubEnableAllCapabilities();

        try (final CaseSjpHelper caseSjpHelper = new CaseSjpHelper()) {
            caseSjpHelper.createCase();
            caseSjpHelper.verifyCaseCreatedUsingId();

            PeopleStub.stubPerson(caseSjpHelper.getDefendantPersonId(), POSTCODE);

            urn = caseSjpHelper.getCaseUrn();
        }
    }

    @Test
    public void shouldFindCaseByUrnAndPostcode() {
        citizenHelper.verifyCaseByPersonUrnAndPostcode(urn, POSTCODE);
    }

    @Test
    public void shouldNotFindCaseByUrnAndInvalidPostcode() {
        citizenHelper.verifyNoCaseByPersonUrnAndPostcode(urn, "INVALID");
    }

    @Test
    public void shouldNotFindCaseByInvalidUrnAndPostcode() {
        citizenHelper.verifyNoCaseByPersonUrnAndPostcode("INVALID", POSTCODE);
    }
}
