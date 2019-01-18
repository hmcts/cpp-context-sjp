package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseAdjournmentProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private CaseStateService caseStateService;

    @Handles("public.resulting.case-adjourned-to-later-sjp-hearing")
    public void caseAdjournedToLaterSjpHearing(final JsonEnvelope event) {
        final JsonObject adjournmentDetails = event.payloadAsJsonObject();

        final JsonEnvelope recordCaseAdjournedCommand = enveloper.withMetadataFrom(
                event,
                "sjp.command.record-case-adjourned-to-later-sjp-hearing")
                .apply(adjournmentDetails);

        sender.send(recordCaseAdjournedCommand);
    }

    @Handles("sjp.events.case-adjourned-for-later-sjp-hearing-recorded")
    public void caseAdjournedForLaterSjpHearingRecorded(final JsonEnvelope event) {
        final JsonObject adjournmentDetails = event.payloadAsJsonObject();

        final UUID caseId = fromString(adjournmentDetails.getString("caseId"));
        final LocalDate adjournedTo = parse(adjournmentDetails.getString("adjournedTo"));

        caseStateService.caseAdjournedForLaterHearing(caseId, adjournedTo.atStartOfDay(), event.metadata());
    }


}
