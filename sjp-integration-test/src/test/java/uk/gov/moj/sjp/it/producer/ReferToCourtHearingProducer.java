package uk.gov.moj.sjp.it.producer;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;

import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;

public final class ReferToCourtHearingProducer {

    private final UUID caseId;
    private final UUID hearingTypeId;
    private final UUID referralReasonId;
    private final String rejectionReason;

    public ReferToCourtHearingProducer(
            final UUID caseId,
            final UUID referralReasonId,
            final UUID hearingTypeId,
            final String rejectionReason) {
        this.caseId = caseId;
        this.hearingTypeId = hearingTypeId;
        this.referralReasonId = referralReasonId;
        this.rejectionReason = rejectionReason;
    }

    public void rejectCaseReferral() {
        final JsonObject payload = getFileContentAsJson("CourtReferralIT/public.progression.refer-prosecution-cases-to-court-rejected.json", ImmutableMap.<String, Object>builder()
                .put("caseId", caseId)
                .put("referralReasonId", referralReasonId)
                .put("hearingTypeId", hearingTypeId)
                .put("rejectionReason", rejectionReason)
                .build());

        JmsMessageProducerClient messageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        messageProducerClient.sendMessage("public.progression.refer-prosecution-cases-to-court-rejected", payload);

    }

}
