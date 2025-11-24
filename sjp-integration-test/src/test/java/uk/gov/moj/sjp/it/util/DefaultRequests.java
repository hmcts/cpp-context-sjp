package uk.gov.moj.sjp.it.util;


import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.helper.CaseDocumentHelper.GET_CASE_DOCUMENTS_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CaseSearchResultHelper.CASE_SEARCH_RESULTS_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CitizenHelper.GET_CASE_BY_URN_AND_POSTCODE_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import java.net.URLEncoder;
import java.util.UUID;

public class DefaultRequests {
    private static final String GET_CASE_BY_ID_MEDIA_TYPE = "application/vnd.sjp.query.case+json";
    private static final String GET_PROSECUTION_CASE_BY_ID_MEDIA_TYPE = "application/vnd.sjp.query.prosecution-case+json";
    private static final String GET_POTENTIAL_CASES_BY_DEFENDANT_ID_MEDIA_TYPE = "application/vnd.sjp.query.defendant-potential-cases+json";
    private static final String GET_CASE_BY_ID_WITH_DOCUMENT_METADATA_MEDIA_TYPE = "application/vnd.sjp.query.case-with-document-metadata+json";

    public static RequestParamsBuilder getCaseById(final UUID caseId) {
        return getCaseById(caseId, USER_ID);
    }

    public static RequestParamsBuilder getProsecutionCaseById(final UUID caseId) {
        return getProsecutionCaseById(caseId, USER_ID);
    }

    public static RequestParamsBuilder getCaseById(final UUID caseId, final UUID userId) {
        return requestParams(getReadUrl("/cases/") + caseId, GET_CASE_BY_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getProsecutionCaseById(final UUID caseId, final UUID userId) {
        return requestParams(getReadUrl("/cases/") + caseId, GET_PROSECUTION_CASE_BY_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getCaseByIdWithDocumentMetadata(final UUID caseId, final UUID userId) {
        return requestParams(getReadUrl("/cases/") + caseId, GET_CASE_BY_ID_WITH_DOCUMENT_METADATA_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getCaseDocumentsByCaseId(final UUID caseId, final UUID userId) {
        return requestParams(getReadUrl("/cases/" + caseId + "/documents"), GET_CASE_DOCUMENTS_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder searchCases(final String query, final UUID userId) {
        final RequestParamsBuilder requestParamsBuilder = requestParams(getReadUrl("/search?q=" + query), CASE_SEARCH_RESULTS_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
        return requestParamsBuilder;
    }

    public static RequestParamsBuilder getCaseByUrnAndPostcode(final String caseUrn, final String postcode) {
        return requestParams(getReadUrl("/cases-for-citizen?urn=" + caseUrn + "&postcode=" + URLEncoder.encode(postcode)), GET_CASE_BY_URN_AND_POSTCODE_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getPotentialCasesByDefendantId(final UUID defendantId) {
        return requestParams(getReadUrl("/defendant/" + defendantId + "/potential-cases"),
                                        GET_POTENTIAL_CASES_BY_DEFENDANT_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }
}
