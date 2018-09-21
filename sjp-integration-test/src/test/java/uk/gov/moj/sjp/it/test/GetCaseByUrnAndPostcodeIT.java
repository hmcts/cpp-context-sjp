package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CitizenHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.BeforeClass;
import org.junit.Test;

public class GetCaseByUrnAndPostcodeIT extends BaseIntegrationTest {

    private static final String POSTCODE = "W1T 1JY";
    private static String urn;
    private CitizenHelper citizenHelper = new CitizenHelper();

    @BeforeClass
    public static void init() {
        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        urn = createCasePayloadBuilder.getUrn();

        ReferenceDataStub.stubQueryOffences("/GetCaseByUrnAndPostcodeIT/referencedataoffences.offences-list.json");
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
