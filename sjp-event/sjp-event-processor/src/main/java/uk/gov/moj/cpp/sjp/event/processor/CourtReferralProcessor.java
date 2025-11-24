package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection.recordCaseReferralForCourtHearingRejection;

import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferCaseForCourtHearingCommand;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.UsersGroupsService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.CourtDocumentsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.HearingRequestsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.ProsecutionCasesDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.SjpReferralDataSourcingService;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtReferralProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtReferralProcessor.class);

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
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Inject
    private SjpReferralDataSourcingService sjpReferralDataSourcingService;

    @Inject
    private HearingRequestsDataSourcingService hearingRequestsDataSourcingService;

    @Inject
    private CourtDocumentsDataSourcingService courtDocumentsDataSourcingService;

    @Inject
    private UsersGroupsService usersGroupsService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("public.progression.refer-prosecution-cases-to-court-rejected")
    public void referToCourtHearingRejected(final JsonEnvelope event) {
        final JsonObject rejectionEvent = event.payloadAsJsonObject();

        if (rejectionEvent.containsKey("sjpCourtReferral")) {
            final JsonObject caseDetails = rejectionEvent.getJsonObject("sjpCourtReferral").getJsonArray("prosecutionCases").getJsonObject(0);
            final String rejectionReason = rejectionEvent.getString("rejectedReason");

            final JsonEnvelope command = enveloper.withMetadataFrom(
                    envelopeFrom(metadataFrom(event.metadata()), NULL),
                    "sjp.command.record-case-referral-for-court-hearing-rejection")
                    .apply(recordCaseReferralForCourtHearingRejection()
                            .withCaseId(UUID.fromString(caseDetails.getString("id")))
                            .withRejectionReason(rejectionReason)
                            .withRejectedAt(clock.now())
                            .build());

            sender.send(command);
        } else {
            LOGGER.debug("Event public.progression.refer-prosecution-cases-to-court-rejected ignored as not related to SJP court referral request");
        }
    }

    @Handles("sjp.events.case-referred-for-court-hearing")
    public void caseReferredForCourtHearing(final Envelope<CaseReferredForCourtHearing> event) {
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(event.metadata()), NULL);
        final CaseReferredForCourtHearing caseReferredForCourtHearing = event.payload();
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseReferredForCourtHearing.getCaseId(), emptyEnvelope);

        final CaseDecision caseDecision = getReferralDecisionFromCaseDecisions(
                caseReferredForCourtHearing.getDecisionId(),
                caseDetails)
                .orElseThrow(() -> new IllegalArgumentException(
                        format("Referral decision not found for case %s",
                                caseDetails.getId()))
                );

        final DefendantsOnlinePlea defendantOnlinePleaDetails = Optional.of(caseDetails.getOnlinePleaReceived())
                .filter(Boolean::booleanValue)
                .map(pleaReceived -> sjpService.getDefendantPleaDetails(caseDetails.getId(), caseDetails.getDefendant().getId(), emptyEnvelope))
                .orElse(null);

        final Optional<JsonObject> prosecutionCaseFileOptional = prosecutionCaseFileService.getCaseFileDetails(caseDetails.getId(), emptyEnvelope);
        final JsonObject caseFileDefendantDetails = prosecutionCaseFileOptional.map(this::getCaseFileDefendant).orElse(null);

        final List<HearingRequestView> listHearingRequestViews = hearingRequestsDataSourcingService.createHearingRequestViews(
                caseReferredForCourtHearing.getCaseId(),
                caseReferredForCourtHearing.getReferralReasonId(),
                caseReferredForCourtHearing.getReferredOffences(),
                caseReferredForCourtHearing.getDefendantCourtOptions(),
                caseReferredForCourtHearing.getEstimatedHearingDuration(),
                caseReferredForCourtHearing.getListingNotes(),
                caseDetails,
                defendantOnlinePleaDetails,
                emptyEnvelope);
        final SjpReferralView sjpReferral = sjpReferralDataSourcingService.createSjpReferralView(
                caseReferredForCourtHearing.getReferredAt(),
                caseDetails,
                caseDecision,
                emptyEnvelope);
        final List<ProsecutionCaseView> prosecutionCasesView = prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                caseDetails,
                caseDecision,
                caseReferredForCourtHearing.getReferredOffences(),
                caseReferredForCourtHearing.getReferredAt(),
                caseReferredForCourtHearing.getDefendantCourtOptions(),
                caseReferredForCourtHearing.getConvictionDate(),
                caseReferredForCourtHearing.getConvictingCourt(),
                defendantOnlinePleaDetails,
                prosecutionCaseFileOptional.orElse(null),
                caseFileDefendantDetails,
                emptyEnvelope);

        final List<CourtDocumentView> courtDocumentViews = courtDocumentsDataSourcingService.createCourtDocumentViews(
                caseReferredForCourtHearing.getReferredAt(),
                caseDetails,
                emptyEnvelope);

        final JsonEnvelope command = enveloper.withMetadataFrom(
                emptyEnvelope,
                "progression.refer-cases-to-court")
                .apply(new ReferCaseForCourtHearingCommand(
                        sjpReferral,
                        prosecutionCasesView,
                        listHearingRequestViews,
                        courtDocumentViews));

        sender.send(command);
    }

    @Handles("sjp.events.case-referred-for-court-hearing-v2")
    public void caseReferredForCourtHearingV2(final Envelope<CaseReferredForCourtHearingV2> event) {
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(event.metadata()), NULL);
        final CaseReferredForCourtHearingV2 caseReferredForCourtHearing = event.payload();
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseReferredForCourtHearing.getCaseId(), emptyEnvelope);

        final CaseDecision caseDecision = getReferralDecisionFromCaseDecisions(
                caseReferredForCourtHearing.getDecisionId(),
                caseDetails)
                .orElseThrow(() -> new IllegalArgumentException(
                        format("Referral decision not found for case %s",
                                caseDetails.getId()))
                );

        final DefendantsOnlinePlea defendantOnlinePleaDetails = Optional.of(caseDetails.getOnlinePleaReceived())
                .filter(Boolean::booleanValue)
                .map(pleaReceived -> sjpService.getDefendantPleaDetails(caseDetails.getId(), caseDetails.getDefendant().getId(), emptyEnvelope))
                .orElse(null);

        final Optional<JsonObject> prosecutionCaseFileOptional = prosecutionCaseFileService.getCaseFileDetails(caseDetails.getId(), emptyEnvelope);
        final JsonObject caseFileDefendantDetails = prosecutionCaseFileOptional.map(this::getCaseFileDefendant).orElse(null);

        final NextHearing nextHearing = caseReferredForCourtHearing.getNextHearing();
        final List<HearingRequestView> listHearingRequestViews = hearingRequestsDataSourcingService.createHearingRequestViews(
                caseReferredForCourtHearing.getCaseId(),
                caseReferredForCourtHearing.getReferralReasonId(),
                caseReferredForCourtHearing.getReferredOffences(),
                caseReferredForCourtHearing.getDefendantCourtOptions(),
                caseReferredForCourtHearing.getEstimatedHearingDuration(),
                caseReferredForCourtHearing.getListingNotes(),
                caseDetails,
                defendantOnlinePleaDetails,
                emptyEnvelope);
        final SjpReferralView sjpReferral = sjpReferralDataSourcingService.createSjpReferralView(
                caseReferredForCourtHearing.getReferredAt(),
                caseDetails,
                caseDecision,
                emptyEnvelope);
        final List<ProsecutionCaseView> prosecutionCasesView = prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                caseDetails, caseDecision,
                caseReferredForCourtHearing.getReferredOffences(),
                caseReferredForCourtHearing.getReferredAt(),
                caseReferredForCourtHearing.getDefendantCourtOptions(),
                caseReferredForCourtHearing.getConvictionDate(),
                caseReferredForCourtHearing.getConvictingCourt(),
                defendantOnlinePleaDetails,
                prosecutionCaseFileOptional.orElse(null),
                caseFileDefendantDetails, emptyEnvelope);

        final List<CourtDocumentView> courtDocumentViews = courtDocumentsDataSourcingService.createCourtDocumentViews(
                caseReferredForCourtHearing.getReferredAt(),
                caseDetails,
                emptyEnvelope);

        final ReferCaseForCourtHearingCommand referCaseForCourtHearingCommand = new ReferCaseForCourtHearingCommand(
                sjpReferral,
                prosecutionCasesView,
                listHearingRequestViews,
                courtDocumentViews,
                nextHearing);

        final JsonEnvelope command = envelopeFrom(
                metadataFrom(emptyEnvelope.metadata()).withName("progression.refer-cases-to-court"),
                objectToJsonObjectConverter.convert(referCaseForCourtHearingCommand));

        sender.send(command);
    }

    private JsonObject getCaseFileDefendant(final JsonObject caseFile) {
        return caseFile.getJsonArray("defendants").getJsonObject(0);
    }

    private Optional<CaseDecision> getReferralDecisionFromCaseDecisions(
            final UUID decisionId,
            final CaseDetails caseDetails) {

        return caseDetails.getCaseDecisions()
                .stream()
                .filter(caseDecision -> caseDecision.getId().equals(decisionId))
                .findFirst();
    }

}
