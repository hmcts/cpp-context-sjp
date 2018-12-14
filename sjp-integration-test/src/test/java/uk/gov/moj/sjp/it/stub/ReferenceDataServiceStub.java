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
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

public class ReferenceDataServiceStub {

    public static void stubQueryOffences(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedataoffences-service");

        final String urlPath = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("cjsoffencecode", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));

        waitForStubToBeReady(urlPath + "?cjsoffencecode=ANY&date=2018-08-27", "application/vnd.referencedataoffences.offences-list+json");
    }

    public static void stubProsecutorQuery(final String prosecutingAuthorityCode, final UUID prosecutorId) {
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
                                                .add("shortName", prosecutingAuthorityCode)))
                                .build()
                                .toString())));

        waitForStubToBeReady(
                urlPath + "?prosecutorCode=" + prosecutingAuthorityCode,
                "application/vnd.referencedata.query.prosecutors+json");
    }

    public static void stubHearingTypesQuery(final String hearingTypeId, final String hearingDescription) {
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
                                                .add("hearingDescription", hearingDescription)))
                                .build()
                                .toString())));

        waitForStubToBeReady(urlPath, "application/vnd.reference-data.hearing-types+json");
    }

    public static void stubReferralReasonsQuery(final String referralReasonId, final String referralReason) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/referral-reasons";
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(createObjectBuilder()
                                .add("referralReasons", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", referralReasonId)
                                                .add("reason", referralReason)))
                                .build()
                                .toString())));

        waitForStubToBeReady(urlPath, "application/vnd.reference-data.referral-reasons+json");
    }

    public static void stubReferralDocumentMetadataQuery(final String id, final String documentType) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/documents-metadata/.*";

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/vnd.referencedata.get-all-document-metadata+json")
                        .withBody(createObjectBuilder()
                                .add("documentsMetadata", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("documentType", documentType)
                                                .add("id", id)))
                                .build()
                                .toString())));

        waitForStubToBeReady("/referencedata-service/query/api/rest/referencedata/documents-metadata/2019-01-01", "application/vnd.reference-data.get-all-document-metadata+json");
    }

    public static void stubCourtByCourtHouseOUCodeQuery(final String courtHouseOUCode, final String localJusticeAreaNationalCourtCode, final String courtHouseName) {
        stubCourt(courtHouseOUCode, getOrganisationUnit(localJusticeAreaNationalCourtCode, courtHouseName));
    }

    public static void stubCourtByCourtHouseOUCodeQuery(final String courtHouseOUCode, final String localJusticeAreaNationalCourtCode) {
        stubCourtByCourtHouseOUCodeQuery(courtHouseOUCode, localJusticeAreaNationalCourtCode, "court house name");
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

        waitForStubToBeReady(urlPath + "?oucode=" + courtHouseOUCode, "application/vnd.referencedata.query.organisationunits+json");
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
            waitForStubToBeReady(urlPath + "?postCode=" + encodedPostcode, "application/vnd.reference-data.country-by-postcode+json");

        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(format("Not able to URL encode postcode: %s", postcode), e);
        }
    }

    public static void stubCountryNationalities(String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-nationality";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));
        waitForStubToBeReady(urlPath, "application/vnd.reference-data.country-nationality+json");
    }

    public static void stubEthnicities(String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String query = "application/vnd.reference-data.ethnicities+json";
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/ethnicities";

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(query))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resourceName))));
        waitForStubToBeReady(urlPath, query);
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

}
