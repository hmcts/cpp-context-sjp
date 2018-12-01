package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection.recordCaseReferralForCourtHearingRejection;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;
import uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferCaseForCourtHearingCommand;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.HearingRequestsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.ProsecutionCasesDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.SjpReferralDataSourcingService;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtReferralProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private Clock clock;

    @Inject
    private SjpService sjpService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProsecutionCasesDataSourcingService prosecutionCasesDataSourcingService;

    @Inject
    private SjpReferralDataSourcingService sjpReferralDataSourcingService;

    @Inject
    private HearingRequestsDataSourcingService hearingRequestsDataSourcingService;

    @Handles("public.resulting.decision-to-refer-case-for-court-hearing-saved")
    public void decisionToReferCaseForCourtHearingSaved(final Envelope<DecisionToReferCaseForCourtHearingSaved> event) {
        final DecisionToReferCaseForCourtHearingSaved decisionToReferCaseForCourtHearingSaved = event.payload();

        final ReferCaseForCourtHearing commandPayload = ReferCaseForCourtHearing.referCaseForCourtHearing()
                .withCaseId(decisionToReferCaseForCourtHearingSaved.getCaseId())
                .withSessionId(decisionToReferCaseForCourtHearingSaved.getSessionId())
                .withReferralReasonId(decisionToReferCaseForCourtHearingSaved.getReferralReasonId())
                .withHearingTypeId(decisionToReferCaseForCourtHearingSaved.getHearingTypeId())
                .withEstimatedHearingDuration(decisionToReferCaseForCourtHearingSaved.getEstimatedHearingDuration())
                .withListingNotes(decisionToReferCaseForCourtHearingSaved.getListingNotes())
                .withRequestedAt(decisionToReferCaseForCourtHearingSaved.getDecisionSavedAt())
                .build();

        final JsonEnvelope command = enveloper.withMetadataFrom(envelopeFrom(metadataFrom(event.metadata()), NULL), "sjp.command.refer-case-for-court-hearing")
                .apply(commandPayload);

        sender.send(command);
    }

    @Handles("public.progression.refer-prosecution-cases-to-court-rejected")
    public void referToCourtHearingRejected(final JsonEnvelope event) {
        final JsonObject rejectionEvent = event.payloadAsJsonObject();
        final JsonObject rejectedCourtReferral = rejectionEvent.getJsonObject("courtReferral");

        if (rejectedCourtReferral.containsKey("sjpReferral")) {

            final JsonObject caseDetails = rejectedCourtReferral.getJsonArray("prosecutionCases").getJsonObject(0);
            final String rejectionReason = rejectionEvent.getString("rejectedReason");

            final JsonEnvelope command = enveloper.withMetadataFrom(
                    envelopeFrom(metadataFrom(event.metadata()), NULL),
                    "sjp.command.record-case-referral-for-court-hearing-rejection")
                    .apply(recordCaseReferralForCourtHearingRejection()
                            .withCaseId(fromString(caseDetails.getString("id")))
                            .withRejectionReason(rejectionReason)
                            .withRejectedAt(clock.now())
                            .build());

            sender.send(command);
        }
    }

    @Handles("sjp.events.case-referred-for-court-hearing")
    public void caseReferredForCourtHearing(final Envelope<CaseReferredForCourtHearing> event) {
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(event.metadata()), NULL);
        final CaseReferredForCourtHearing caseReferredForCourtHearing = event.payload();
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseReferredForCourtHearing.getCaseId(), emptyEnvelope);

        final DefendantsOnlinePlea defendantOnlinePleaDetails = Optional.of(caseDetails.getOnlinePleaReceived())
                .filter(Boolean::booleanValue)
                .map(pleaReceived -> sjpService.getDefendantPleaDetails(fromString(caseDetails.getId()), emptyEnvelope))
                .orElse(null);

        final List<HearingRequestView> listHearingRequestViews = hearingRequestsDataSourcingService.createHearingRequestViews(
                caseReferredForCourtHearing,
                caseDetails,
                defendantOnlinePleaDetails,
                emptyEnvelope);
        final SjpReferralView sjpReferral = sjpReferralDataSourcingService.createSjpReferralView(
                caseReferredForCourtHearing,
                caseDetails,
                emptyEnvelope);
        final List<ProsecutionCaseView> prosecutionCasesView = prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                caseDetails,
                caseReferredForCourtHearing,
                defendantOnlinePleaDetails,
                emptyEnvelope);

        final JsonEnvelope command = enveloper.withMetadataFrom(
                emptyEnvelope,
                "progression.refer-cases-to-court")
                .apply(new ReferCaseForCourtHearingCommand(
                        sjpReferral,
                        prosecutionCasesView,
                        listHearingRequestViews));

        sender.send(command);
    }

}
