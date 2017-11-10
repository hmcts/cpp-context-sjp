package uk.gov.moj.sjp.it.util;


import static java.lang.String.format;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.helper.AbstractCaseHelper.GET_CASE_BY_ID_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.AbstractCaseHelper.GET_CASE_BY_URN_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.sjp.it.helper.CaseCourtReferralHelper.CASES_REFERRED_TO_COURT_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CaseDocumentHelper.GET_CASE_DOCUMENTS_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CaseSearchResultHelper.CASE_SEARCH_RESULTS_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.CaseSjpHelper.GET_SJP_CASE_BY_URN_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.SearchCaseByMaterialIdHelper.CASES_SEARCH_BY_MATERIAL_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.SearchCasesByPersonIdHelper.CASES_SEARCH_MEDIA_TYPE;
import static uk.gov.moj.sjp.it.helper.UpdateOffencesForDefendantHelper.GET_CASE_DEFENDANTS;
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

    public static RequestParamsBuilder getSjpCaseByUrn(final String caseUrn) {
        return requestParams(getReadUrl("/cases?urn=" + caseUrn), GET_SJP_CASE_BY_URN_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getDefendantsByCaseId(final String caseId) {
        return requestParams(getReadUrl(format("/cases/%s/defendants", caseId)), GET_CASE_DEFENDANTS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCaseDocumentsByCaseId(final String caseId) {
        return getCaseDocumentsByCaseId(caseId, USER_ID);
    }

    public static RequestParamsBuilder getCaseDocumentsByCaseId(final String caseId, final String userId) {
        return requestParams(getReadUrl("/cases/" + caseId + "/documents"), GET_CASE_DOCUMENTS_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder searchCasesByMaterialId(final String materialId) {
        return requestParams(getReadUrl("/search?q=" + materialId), CASES_SEARCH_BY_MATERIAL_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder searchCasesByPersonId(final String personId) {
        return requestParams(getReadUrl("/search?q=" + personId), CASES_SEARCH_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
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

}
