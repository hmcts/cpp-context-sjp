package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DeleteDocsStartedProcessor {

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles(FinancialMeansDeleteDocsStarted.EVENT_NAME)
    public void handleDeleteDocsStarted(final JsonEnvelope deleteDocsStartedEnvelope) {
        final JsonObject deleteDocsStartedPayload = deleteDocsStartedEnvelope.payloadAsJsonObject();
        final FinancialMeansDeleteDocsStarted deleteDocsStarted = jsonObjectToObjectConverter.convert(deleteDocsStartedPayload, FinancialMeansDeleteDocsStarted.class);
        sender.send(envelop(
                createObjectBuilder()
                        .add(CASE_ID, deleteDocsStarted.getCaseId().toString())
                        .add(DEFENDANT_ID, deleteDocsStarted.getDefendantId().toString())
                        .build())
                .withName("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn")
                .withMetadataFrom(deleteDocsStartedEnvelope));
    }

}
