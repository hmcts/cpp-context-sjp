package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CitizenHelper;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.BeforeClass;
import org.junit.Test;

public class GetCaseByUrnAndPostcodeIT extends BaseIntegrationTest {

    private static final String POSTCODE = "W1T 1JY";
    private static final String OFFENCE_CODE = "CA03010";
    private static String urn;
    private CitizenHelper citizenHelper = new CitizenHelper();
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeClass
    public static void init() {
        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withOffenceCode(OFFENCE_CODE);
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        urn = createCasePayloadBuilder.getUrn();

        stubQueryOffencesByCode(OFFENCE_CODE);
    }

    @Test
    public void shouldFindCaseByUrnAndPostcode() {
        final JsonObject expected = Json.createReader(getClass().getResourceAsStream("/GetCaseByUrnAndPostcodeIT/expected.json")).readObject();
        citizenHelper.verifyCaseByPersonUrnAndPostcode(expected, urn, POSTCODE);
    }

    @Test
    public void shouldNotFindCaseByUrnAndInvalidPostcode() {
        citizenHelper.verifyNoCaseByPersonUrnAndPostcode(urn, "INVALID");
    }

    @Test
    public void shouldNotFindCaseByInvalidUrnAndPostcode() {
        citizenHelper.verifyNoCaseByPersonUrnAndPostcode("INVALID", POSTCODE);
    }

    @Test
    public void shouldFindCaseByUrnWithoutPrefixAndPostcode() {
        final String urnWithoutPrefix = urn.replaceAll("(\\p{Alpha})", "");
        citizenHelper.verifyCaseByPersonUrnWithoutPrefixAndPostcode(urnWithoutPrefix, urn, POSTCODE);
    }
}
