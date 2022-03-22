package uk.gov.moj.sjp.it.command;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.getPostCallResponse;

import java.util.UUID;

import javax.ws.rs.core.Response;

public class CreateCaseDecision {

    private static final String CASE_DECISION_MEDIA_TYPE = "application/vnd.sjp.save-decision+json";
    private static final String CREATE_CASE_DECISION_URL = "/cases/%s/decision";

    public static String createCaseDecision(UUID caseId, String payload, Response.Status status) {
        final String urlPath = String.format(CREATE_CASE_DECISION_URL, caseId);
        return getPostCallResponse(urlPath, CASE_DECISION_MEDIA_TYPE, payload, status);
    }
}
