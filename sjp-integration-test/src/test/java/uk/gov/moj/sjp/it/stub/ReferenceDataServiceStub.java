package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.io.Charsets.UTF_8;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_LJA_NATIONAL_COURT_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;

public class ReferenceDataServiceStub {

    public static JsonObject stubQueryOffencesByCode(final String code) {
        return stubQueryOffencesByCode(code, equalTo(code));
    }

    public static JsonObject stubAnyQueryOffences() {
        return stubQueryOffencesByCode(DEFAULT_OFFENCE_CODE, matching(".*"));
    }

    public static void stubQueryOffenceById(final UUID offenceId) {
        InternalEndpointMockUtils.stubPingFor("referencedataoffences-service");
        final JsonObject offence = createObjectBuilder()
                .add("offenceId", offenceId.toString())
                .add("cjsOffenceCode", "1")
                .add("modeOfTrial", "SIMP").build();
        final String urlPath = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences";
        stubFor(get(urlPathEqualTo(format(urlPath, offenceId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(offence.toString())));
    }

    public static void stubAllProsecutorsQuery() {
        Arrays.stream(ProsecutingAuthority.values()).forEach(
                prosecutingAuthority -> stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID()));
    }

    public static void stubProsecutorQuery(final String prosecutingAuthorityCode, final String prosecutingAuthorityFullName, final UUID prosecutorId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors";
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
                                                .add("address", createObjectBuilder()
                                                        .add("address1", "6th Floor Windsor House")
                                                        .add("address2", "42-50 Victoria Street")
                                                        .add("address3", "London"))))
                                .build()
                                .toString())));
    }

    public static void stubHearingTypesQuery(final String hearingTypeId, final String hearingCode, final String hearingDescription) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/hearing-types";
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
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/documents-type-access/.*";

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
    }

    public static void stubReferralReasonsQuery(final UUID referralReasonId, final String hearingCode, final String referralReason) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final JsonObject response = createObjectBuilder().add("referralReasons", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", referralReasonId.toString())
                        .add("hearingCode", hearingCode)
                        .add("reason", referralReason)))
                .build();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/referral-reasons";
        final String mediaType = "application/vnd.reference-data.referral-reasons+json";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(ACCEPT, mediaType)
                        .withBody(response.toString())));
    }

    public static void stubOffenceFineLevelsQuery(final int fineLevel, final BigDecimal maxValue) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final JsonObject response = createObjectBuilder().add("fineLevels", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("fineLevel", fineLevel)
                        .add("maxValue", maxValue)))
                .build();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/offence-fine-levels";
        final String mediaType = "application/vnd.reference-data.offence-fine-levels+json";

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
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final JsonArray withdrawalReasonsArray = withdrawalReasons.entrySet().stream()
                .map(e -> createObjectBuilder().add("id", e.getKey().toString()).add("reasonCodeDescription", e.getValue()))
                .reduce(Json.createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add).build();

        final JsonObject response = createObjectBuilder().add("offenceWithdrawRequestReasons", withdrawalReasonsArray).build();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/offence-withdraw-request-reason";
        final String mediaType = "application/vnd.reference-data.offence-withdrawal-request-reason+json";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(ACCEPT, mediaType)
                        .withBody(response.toString())));
    }

    private static void stubCourt(final String courtHouseOUCode, final String responseBody) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisationunits";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("oucode", equalTo(courtHouseOUCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responseBody)));
    }

    public static void stubEnforcementAreaByPostcode(final String postCode, final String nationalCourtCode, final String nationalCourtName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/enforcement-area";

        final JsonObject enforcementAreaJson = getFileContentAsJson("stub-data/referencedata.query.enforcement-area.json");
        final JsonObjectBuilder localJusticeArea = createObjectBuilder(enforcementAreaJson.getJsonObject("localJusticeArea"));
        localJusticeArea.add("nationalCourtCode", nationalCourtCode);
        localJusticeArea.add("name", nationalCourtName);
        final JsonObject stubbedResponse = createObjectBuilder(enforcementAreaJson)
                .add("localJusticeArea", localJusticeArea.build())
                .build();

        try {
            stubFor(get(urlPathEqualTo(urlPath))
                    .withQueryParam("postcode", equalTo(encode(postCode, UTF_8.name())))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader(ID, randomUUID().toString())
                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .withBody(stubbedResponse.toString())));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(format("Not able to URL encode postcode: %s", postCode), e);
        }
    }

    public static void stubRegionByPostcode(final String nationalCourtCode, final String region) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/local-justice-areas";

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

        try {
            stubFor(get(urlPathEqualTo(urlPath))
                    .withQueryParam("nationalCourtCode", equalTo(encode(nationalCourtCode, UTF_8.name())))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader(ID, randomUUID().toString())
                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .withBody(stubbedResponse.toString())));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(format("Not able to URL encode nationalCourtCode: %s", nationalCourtCode), e);
        }
    }

    public static void stubCountryByPostcodeQuery(final String postcode, final String country) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-by-postcode";

        final String encodedPostcode;
        try {
            encodedPostcode = encode(postcode, UTF_8.name());

            stubFor(get(urlPathEqualTo(urlPath))
                    .withQueryParam("postCode", equalTo(encodedPostcode))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader(ID, randomUUID().toString())
                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .withBody(createObjectBuilder().add("country", country).build().toString())));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(format("Not able to URL encode postcode: %s", postcode), e);
        }
    }

    public static void stubCountryNationalities(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-nationality";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));
    }

    public static void stubEthnicities(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String query = "application/vnd.reference-data.ethnicities+json";
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/ethnicities";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(query))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));
    }

    private static JsonObject stubQueryOffencesByCode(final String code, final ValueMatchingStrategy offenceCodeMatcher) {
        InternalEndpointMockUtils.stubPingFor("referencedataoffences-service");

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

    public static void stubResultDefinitions() {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String query = "application/vnd.referencedata.get-all-result-definitions+json";
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/result-definitions";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.result-definitions.json"))));
    }

    public static void stubResultIds() {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String query = "application/vnd.referencedata.query.results+json";
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/results";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/referencedata.query.results.json"))));
    }

}
