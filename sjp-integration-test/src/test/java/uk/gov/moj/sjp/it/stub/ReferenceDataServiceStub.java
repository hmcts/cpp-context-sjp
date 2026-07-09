package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_AOCP_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_AOCP_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;

public class ReferenceDataServiceStub {

    private static final String QUERY_API_PATH = "/referencedata-service/query/api/rest/referencedata";
    private static final String QUERY_PROSECUTORS_MIME_TYPE = "application/vnd.referencedata.query.prosecutors+json";
    private static final String QUERY_VICTIM_SURCHARGE_TYPE = "application/vnd.referencedata.query.victim-surcharges+json";
    private static final String QUERY_VERDICT_TYPES_MIME_TYPE = "application/vnd.reference-data.verdict-types+json";
    private static final String QUERY_VERDICT_TYPES_BY_JURISDICTION__MIME_TYPE = "application/vnd.referencedata.query.verdict-types-jurisdiction+json";
    private static final String QUERY_BAIL_STATUSES_MIME_TYPE = "application/vnd.referencedata.bail-statuses+json";
    private static final String QUERY_ALL_RESULT_DEFINITIONS_MIME_TYPE = "application/vnd.referencedata.get-all-result-definitions+json";
    private static final String QUERY_RESULT_DEFINITIONS_MIME_TYPE = "application/vnd.referencedata.query-result-definitions+json";
    private static final String QUERY_ALL_FIXED_LIST_MIME_TYPE = "application/vnd.referencedata.get-all-fixed-list+json";


    public static void stubAllReferenceData() {
        stubFixedLists();
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        stubQueryVictimSurcharge();
    }

    public static void stubAllResultDefinitions() {
        final String urlPath = QUERY_API_PATH + "/result-definitions";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_ALL_RESULT_DEFINITIONS_MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.all-result-definitions.json"))));
    }

    public static void stubResultDefinitionByResultCode(final String resultCode) {
        final String urlPath = QUERY_API_PATH + "/result-definitions";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_RESULT_DEFINITIONS_MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.result-definition-by-resultcode.json")
                                .replaceAll("RESULT_CODE", resultCode))));
    }

    public static void stubFixedLists() {
        final String urlPath = QUERY_API_PATH + "/fixed-list";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_ALL_FIXED_LIST_MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.query.get-all-fixed-list.json"))));
    }

    public static void stubBailStatuses() {
        final String urlPath = QUERY_API_PATH + "/bail-statuses";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_BAIL_STATUSES_MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.query.bailstatuses.json"))));
    }

    public static void stubQueryForVerdictTypes() {
        final String urlPath = QUERY_API_PATH + "/verdict-types";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_VERDICT_TYPES_MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(
                                getPayload("stub-data/referencedata.verdict-types.json")
                        )));
    }

    public static void stubQueryForVerdictTypesByJurisdiction() {
        final String urlPath = QUERY_API_PATH + "/verdict-types-jurisdiction";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_VERDICT_TYPES_BY_JURISDICTION__MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(
                                getPayload("stub-data/referencedata.verdict-types-by-MAGISTRATE-jurisdiction.json")
                        )));
    }

    public static JsonObject stubQueryOffencesByCode(final String code) {
        return stubQueryOffencesByCode(code, equalTo(code));
    }

    public static JsonObject stubAnyQueryOffences() {
        return stubQueryOffencesByCode(DEFAULT_OFFENCE_CODE, matching(".*"));
    }

    public static void stubAllIndividualProsecutorsQueries() {
        Arrays.stream(ProsecutingAuthority.values()).forEach(
                prosecutingAuthority ->
                        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID())
        );
    }

    public static void stubQueryForAllProsecutors() {
        final String urlPath = QUERY_API_PATH + "/prosecutors";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(QUERY_PROSECUTORS_MIME_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, QUERY_PROSECUTORS_MIME_TYPE)
                        .withBody(
                                getPayload("stub-data/referencedata.query.prosecutors.json")
                        )));
    }

    public static void stubProsecutorQuery(final String prosecutingAuthorityCode, final String prosecutingAuthorityFullName, final UUID prosecutorId) {
        stubProsecutorQuery(prosecutingAuthorityCode, prosecutingAuthorityFullName, prosecutorId, false);
    }

    public static void stubProsecutorQuery(final String prosecutingAuthorityCode, final String prosecutingAuthorityFullName, final UUID prosecutorId, final boolean policeFlag) {
        final String urlPath = QUERY_API_PATH + "/prosecutors";

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("prosecutorCode", matching(prosecutingAuthorityCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(createObjectBuilder()
                                .add("prosecutors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", prosecutorId.toString())
                                                .add("shortName", prosecutingAuthorityCode)
                                                .add("fullName", prosecutingAuthorityFullName)
                                                .add("policeFlag", policeFlag)
                                                .add("majorCreditorCode", "Transport for London")
                                                .add("aocpApproved", true)
                                                .add("contactEmailAddress", "tfl@tfl.com")
                                                .add("address", createObjectBuilder()
                                                        .add("address1", "6th Floor Windsor House")
                                                        .add("address2", "42-50 Victoria Street")
                                                        .add("address3", "London"))))
                                .build()
                                .toString())));
    }

    public static void stubHearingTypesQuery(final String hearingTypeId, final String hearingCode, final String hearingDescription) {
        final String urlPath = QUERY_API_PATH + "/hearing-types";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(createObjectBuilder()
                                .add("hearingTypes", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", hearingTypeId)
                                                .add("hearingCode", hearingCode)
                                                .add("hearingDescription", hearingDescription)))
                                .build()
                                .toString())));
    }

    public static void stubReferralDocumentMetadataQuery(final String id, final String documentType) {
        final String urlPath = QUERY_API_PATH + "/documents-type-access/.*";

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/vnd.referencedata.get-all-document-type-access+json")
                        .withBody(createObjectBuilder()
                                .add("documentsTypeAccess", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("section", documentType)
                                                .add("id", id)))
                                .build()
                                .toString())));
    }

    public static void stubCourtByCourtHouseOUCodeQuery(final String courtHouseOUCode, final String localJusticeAreaNationalCourtCode, final String courtHouseName) {
        stubCourt(courtHouseOUCode, getOrganisationUnit(localJusticeAreaNationalCourtCode, courtHouseName));
    }

    public static void stubCourtByCourtHouseOUCodeQuery(final String courtHouseOUCode, final String localJusticeAreaNationalCourtCode) {
        stubCourtByCourtHouseOUCodeQuery(courtHouseOUCode, localJusticeAreaNationalCourtCode, "court house name");
    }

    public static void stubDefaultCourtByCourtHouseOUCodeQuery() {
        stubCourtByCourtHouseOUCodeQuery(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE, "court house name");
        stubCourtByCourtHouseOUCodeQuery(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, DEFAULT_NON_LONDON_LJA_NATIONAL_COURT_CODE, "court house name");
        stubCourtByCourtHouseOUCodeQuery(DEFAULT_AOCP_COURT_HOUSE_OU_CODE, DEFAULT_AOCP_LJA_NATIONAL_COURT_CODE, "court house name");
    }

    public static void stubReferralReasonsQuery(final UUID referralReasonId, final String hearingCode, final String referralReason) {
        final String urlPath = QUERY_API_PATH + "/referral-reasons";
        final String mediaType = "application/vnd.reference-data.referral-reasons+json";

        final JsonObject response = createObjectBuilder().add("referralReasons", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", referralReasonId.toString())
                                .add("hearingCode", hearingCode)
                                .add("reason", referralReason)))
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(ACCEPT, mediaType)
                        .withBody(response.toString())));
    }

    public static void stubOffenceFineLevelsQuery(final int fineLevel, final BigDecimal maxValue) {
        final String urlPath = QUERY_API_PATH + "/offence-fine-levels";
        final String mediaType = "application/vnd.reference-data.offence-fine-levels+json";

        final JsonObject response = createObjectBuilder().add("fineLevels", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("fineLevel", fineLevel)
                                .add("maxValue", maxValue)))
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(ACCEPT, mediaType)
                        .withBody(response.toString())));
    }

    public static void stubWithdrawalReasonsQuery(final UUID withdrawalReasonId, final String withdrawalReason) {
        stubWithdrawalReasonsQuery(ImmutableMap.of(withdrawalReasonId, withdrawalReason));
    }

    public static void stubWithdrawalReasonsQuery(final Map<UUID, String> withdrawalReasons) {
        final JsonArray withdrawalReasonsArray = withdrawalReasons.entrySet().stream()
                .map(e -> createObjectBuilder().add("id", e.getKey().toString()).add("reasonCodeDescription", e.getValue()))
                .reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add).build();

        final JsonObject response = createObjectBuilder().add("offenceWithdrawRequestReasons", withdrawalReasonsArray).build();

        final String urlPath = QUERY_API_PATH + "/offence-withdraw-request-reason";
        final String mediaType = "application/vnd.reference-data.offence-withdrawal-request-reason+json";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(ACCEPT, mediaType)
                        .withBody(response.toString())));
    }

    private static void stubCourt(final String courtHouseOUCode, final String responseBody) {
        final String urlPath = QUERY_API_PATH + "/organisationunits";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("oucode", equalTo(courtHouseOUCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responseBody)));
    }

    public static void stubEnforcementAreaByPostcode(final String postCode, final String nationalCourtCode, final String nationalCourtName) {
        final String urlPath = QUERY_API_PATH + "/enforcement-area";

        final JsonObject enforcementAreaJson = getFileContentAsJson("stub-data/referencedata.query.enforcement-area.json");
        final JsonObjectBuilder localJusticeArea = createObjectBuilder(enforcementAreaJson.getJsonObject("localJusticeArea"));
        localJusticeArea.add("nationalCourtCode", nationalCourtCode);
        localJusticeArea.add("name", nationalCourtName);
        final JsonObject stubbedResponse = createObjectBuilder(enforcementAreaJson)
                .add("localJusticeArea", localJusticeArea.build())
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("postcode", equalTo(postCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(stubbedResponse.toString())));
    }

    public static void stubEnforcementAreaByLocalLJACode(final String nationalCourtCode, final String nationalCourtName) {
        final String urlPath = QUERY_API_PATH + "/enforcement-area";

        final JsonObject enforcementAreaJson = getFileContentAsJson("stub-data/referencedata.query.enforcement-area.json");
        final JsonObjectBuilder localJusticeArea = createObjectBuilder(enforcementAreaJson.getJsonObject("localJusticeArea"));
        localJusticeArea.add("nationalCourtCode", nationalCourtCode);
        localJusticeArea.add("name", nationalCourtName);
        final JsonObject stubbedResponse = createObjectBuilder(enforcementAreaJson)
                .add("localJusticeArea", localJusticeArea.build())
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("localJusticeAreaNationalCourtCode", equalTo(nationalCourtCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(stubbedResponse.toString())));
    }

    public static void stubEnforcementAreaByLjaCode() {
        final String urlPath = pathFor("/enforcement-area");
        final JsonObject enforcementAreaJson = getFileContentAsJson("stub-data/referencedata.query.enforcement-area.json");

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("localJusticeAreaNationalCourtCode", equalTo(DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(enforcementAreaJson.toString())));
    }

    public static void stubRegionByPostcode(final String nationalCourtCode, final String region) {
        final String urlPath = QUERY_API_PATH + "/local-justice-areas";

        final JsonObject localJusticeArea = createObjectBuilder()
                .add("nationalCourtCode", nationalCourtCode)
                .add("region", region)
                .build();

        final JsonArray localJusticeAreaArray = createArrayBuilder()
                .add(localJusticeArea)
                .build();

        final JsonObject stubbedResponse = createObjectBuilder()
                .add("localJusticeAreas", localJusticeAreaArray)
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("nationalCourtCode", equalTo(nationalCourtCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(stubbedResponse.toString())));
    }

    public static void stubCountryByPostcodeQuery(final String postcode, final String country) {
        final String urlPath = QUERY_API_PATH + "/country-by-postcode";

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("postCode", equalTo(postcode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(createObjectBuilder().add("country", country).build().toString())));
    }

    public static void stubCountryNationalities(final String resourceName) {
        final String urlPath = QUERY_API_PATH + "/country-nationality";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));
    }

    public static void stubEthnicities(final String resourceName) {
        final String query = "application/vnd.reference-data.ethnicities+json";
        final String urlPath = QUERY_API_PATH + "/ethnicities";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(query))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));
    }

    private static JsonObject stubQueryOffencesByCode(final String code, final StringValuePattern offenceCodeMatcher) {
        final String urlPath = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences";
        final JsonObject offenceDefinition = getFileContentAsJson(format("stub-data/offences/%s.json", code));
        final JsonObject stubbedResponse = createObjectBuilder()
                .add("offences", createArrayBuilder().add(offenceDefinition))
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("cjsoffencecode", offenceCodeMatcher)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(stubbedResponse.toString())));
        return offenceDefinition;
    }

    private static String getOrganisationUnit(final String localJusticeAreaNationalCourtCode, final String courtHouseName) {
        try {
            return IOUtils.toString(ReferenceDataServiceStub.class.getResourceAsStream("/stub-data/referencedata.query.organisationunits.json"))
                    .replace("$(courtHouseName)", courtHouseName)
                    .replace("$(localJusticeAreaNationalCourtCode)", localJusticeAreaNationalCourtCode);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void stubResultIds() {
        final String urlPath = QUERY_API_PATH + "/results";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.query.results.json"))));
    }

    public static void stubRegionalOrganisations() {
        final String urlPath = pathFor("/regional-organisations");
        final String mediaType = "application/vnd.referencedata.query.regional-organisations-except-region-name-police+json";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, mediaType)
                        .withBody(getPayload("stub-data/referencedata.regional-organisations.json"))));
    }

    public static void stubReferralReason(final String id, final String payload) {
        final String urlPath = QUERY_API_PATH + "/referral-reasons/" + id;

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/vnd.reference-data.query.get-referral-reason+json")
                        .withBody(getPayload(payload)))
        );
    }

    public static void stubQueryVictimSurcharge() {
        stubFor(get(urlPathEqualTo(QUERY_API_PATH + "/victim-surcharges"))
                .withHeader(ACCEPT, equalTo(QUERY_VICTIM_SURCHARGE_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, QUERY_VICTIM_SURCHARGE_TYPE)
                        .withBody(getPayload("stub-data/referencedata.victim-surcharges.json")))
        );
    }

    public static String stubDvlaPenaltyPointNotificationEmailAddress() {
        final String urlPath = pathFor("/organisation");
        final String dvlaEmailAddress = "rehab@dvla.gov.uk";

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("orgName", equalTo("DVLA Penalty Point Notification"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(
                                createObjectBuilder()
                                        .add("id", randomUUID().toString())
                                        .add("seqNum", 10010)
                                        .add("orgName", "DVLA Penalty Point Notification")
                                        .add("orgType", "DVLA")
                                        .add("startDate", "2020-05-01")
                                        .add("emailAddress", dvlaEmailAddress)
                                        .build()
                                        .toString()
                        )));
        return dvlaEmailAddress;
    }

    private static final String WITHDRAWAL_REASONS_ENDPOINT = "/referencedata-service/query/api/rest/referencedata/offence-withdraw-request-reasons";
    private static final String WITHDRAWAL_REASONS_CONTENT_TYPE = "application/vnd.referencedata.offence-withdraw-request-reasons+json";

    public static void stubWithdrawalReasons() {
        stubFor(get(urlPathEqualTo(WITHDRAWAL_REASONS_ENDPOINT))
                .withHeader(ACCEPT, equalTo(WITHDRAWAL_REASONS_CONTENT_TYPE))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, WITHDRAWAL_REASONS_CONTENT_TYPE)
                        .withBody(getPayload("stub-data/referencedata-withdrawal-reasons.json")))
        );
    }

    public static void stubVariableWithdrawalReasons(final String reasonId, final String reasonCode) {
        final String payload = getPayload("stub-data/referencedata-variable-withdrawal-reasons.json")
                .replaceAll("REASON_ID", reasonId)
                .replaceAll("REASON_CODE", reasonCode);

        stubFor(get(urlPathEqualTo(WITHDRAWAL_REASONS_ENDPOINT))
                .withHeader(ACCEPT, equalTo(WITHDRAWAL_REASONS_CONTENT_TYPE))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, WITHDRAWAL_REASONS_CONTENT_TYPE)
                        .withBody(payload))
        );
    }

    private static String pathFor(final String endpoint) {
        return QUERY_API_PATH + endpoint;
    }
}
