package uk.gov.moj.sjp.it.util;


import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.helper.AbstractCaseHelper.GET_CASE_BY_ID_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.AbstractCaseHelper.GET_CASE_BY_URN_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.sjp.it.helper.CaseCourtReferralHelper.CASES_REFERRED_TO_COURT_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CaseDocumentHelper.GET_CASE_DOCUMENTS_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CaseSearchResultHelper.CASE_SEARCH_RESULTS_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CitizenHelper.GET_CASE_BY_URN_AND_POSTCODE_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.test.AwaitingCasesIT.AWAITING_CASES_MEDIA_TYPE;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

public class DefaultRequests {

    public static RequestParamsBuilder getCaseById(final String caseId) {
        return getCaseById(caseId, USER_ID);
    }

    public static RequestParamsBuilder getCaseById(final String caseId, String userId) {
        return requestParams(getReadUrl("/cases/" + caseId), GET_CASE_BY_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getCaseByUrn(final String caseUrn) {
        return requestParams(getReadUrl("/cases?urn=" + caseUrn), GET_CASE_BY_URN_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCaseDocumentsByCaseId(final String caseId) {
        return getCaseDocumentsByCaseId(caseId, USER_ID);
    }

    public static RequestParamsBuilder getCaseDocumentsByCaseId(final String caseId, final String userId) {
        return requestParams(getReadUrl("/cases/" + caseId + "/documents"), GET_CASE_DOCUMENTS_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder searchCases(final String query) {
        return requestParams(getReadUrl("/search?q=" + query), CASE_SEARCH_RESULTS_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder findAwaitingCases() {
        return requestParams(getReadUrl("/cases/awaiting"), AWAITING_CASES_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCasesReferredToCourt() {
        return requestParams(getReadUrl("/cases/referred-to-court"), CASES_REFERRED_TO_COURT_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCaseByUrnAndPostcode(final String caseUrn, final String postcode) {
        return requestParams(getReadUrl("/cases-for-citizen?urn=" + caseUrn + "&postcode=" + postcode), GET_CASE_BY_URN_AND_POSTCODE_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }
}
