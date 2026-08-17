package uk.gov.moj.sjp.it.helper;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.ImmutableMap;


public class CaseApplicationHelper {

    public static void createCaseApplication(final UUID userId, final UUID caseId, final UUID appId, final UUID typeId, final String code, final String type, final LocalDate applicationReceivedDate, final String appStatus, String filePath) {

        final String url = format("/cases/%s/applications", caseId.toString());

        final JsonObject createCaseApplicationPayload = getFileContentAsJson(filePath,
                ImmutableMap.<String, Object>builder()
                        .put("APP_ID", appId.toString())
                        .put("TYPE_ID", typeId.toString())
                        .put("APPLICATION_CODE", code)
                        .put("APPLICATION_TYPE", type)
                        .put("APPLICATION_RECEIVED_DATE", applicationReceivedDate)
                        .put("APP_STATUS", appStatus)
                        .build());

        makePostCall(userId, url,
                "application/vnd.sjp.create-case-application+json",
                createCaseApplicationPayload.toString(),
                ACCEPTED);

    }

    public static void saveApplicationDecision(final UUID userId, final UUID caseId, final UUID appId,
                                               final UUID sessionId, final boolean granted, final Boolean outOfTime,
                                               final Boolean outOfTimeReason, final String rejectionReason) {

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("granted", granted)
                .add("sessionId", sessionId.toString());

        ofNullable(outOfTime).ifPresent(oot -> payloadBuilder.add("outOfTime", oot));
        ofNullable(outOfTimeReason).ifPresent(ootReason -> payloadBuilder.add("outOfTimeReason", ootReason));
        ofNullable(rejectionReason).ifPresent(rejReason -> payloadBuilder.add("rejectionReason", rejReason));

        final String url = format("/cases/%s/applications/%s/decision", caseId.toString(), appId.toString());
        makePostCall(userId, url, "application/vnd.sjp.save-application-decision+json",
                payloadBuilder.build().toString(), ACCEPTED);

    }

}
