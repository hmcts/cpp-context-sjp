package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;


import java.time.ZonedDateTime;
import java.util.UUID;
import javax.inject.Inject;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.*;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseReservedProcessor {

    @Inject
    private Sender sender;

    @Inject
    private TimerService timerService;

    @Handles(CaseReserved.EVENT_NAME)
    public void handleCaseReserved(final JsonEnvelope caseReservedEnvelope){

        final UUID caseId = UUID.fromString(caseReservedEnvelope.payloadAsJsonObject().getString("caseId"));
        final ZonedDateTime undoReserveCaseExpiryDate = ZonedDateTime.now().plusDays(1);

        sender.send(envelop(caseReservedEnvelope.payloadAsJsonObject())
                .withName("public.sjp.case-reserved")
                .withMetadataFrom(caseReservedEnvelope));

        timerService.startTimerForUndoReserveCase(caseId, undoReserveCaseExpiryDate, caseReservedEnvelope.metadata() );
    }

    @Handles(CaseAlreadyReserved.EVENT_NAME)
    public void handleCaseAlreadyReserved(final JsonEnvelope caseReservedEnvelope){

        sender.send(envelop(caseReservedEnvelope.payloadAsJsonObject())
                .withName("public.sjp.case-already-reserved")
                .withMetadataFrom(caseReservedEnvelope));
    }

    @Handles(CaseUnReserved.EVENT_NAME)
    public void handleCaseUnReserved(final JsonEnvelope caseReservedEnvelope){

        sender.send(envelop(caseReservedEnvelope.payloadAsJsonObject())
                .withName("public.sjp.case-unreserved")
                .withMetadataFrom(caseReservedEnvelope));
    }

    @Handles(CaseAlreadyUnReserved.EVENT_NAME)
    public void handleCaseAlreadyUnReserved(final JsonEnvelope caseReservedEnvelope){

        sender.send(envelop(caseReservedEnvelope.payloadAsJsonObject())
                .withName("public.sjp.case-already-unreserved")
                .withMetadataFrom(caseReservedEnvelope));
    }

    @Handles(CaseReserveFailedAsAlreadyCompleted.EVENT_NAME)
    public void handleCaseReservedFailedAsAlreadyCompleted(final JsonEnvelope caseReservedFailedEnvelope){
        sender.send(envelop(caseReservedFailedEnvelope.payloadAsJsonObject())
                .withName("public.sjp.case-reserve-failed-as-already-completed")
                .withMetadataFrom(caseReservedFailedEnvelope));
    }
}
