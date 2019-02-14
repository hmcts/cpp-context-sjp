package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.json.schemas.domains.sjp.ListingDetails.listingDetails;
import static uk.gov.justice.json.schemas.domains.sjp.Note.note;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.LISTING;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection.recordCaseReferralForCourtHearingRejection;

import uk.gov.justice.json.schemas.domains.sjp.Note;
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
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;
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

    @Handles("public.resulting.decision-to-refer-case-for-court-hearing-saved")
    public void decisionToReferCaseForCourtHearingSaved(final Envelope<DecisionToReferCaseForCourtHearingSaved> event) {
        final DecisionToReferCaseForCourtHearingSaved decisionToReferCaseForCourtHearingSaved = event.payload();

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(event.metadata()), NULL);

        final JsonObject sessionDetails = sjpService.getSessionDetails(
                decisionToReferCaseForCourtHearingSaved.getSessionId(),
                emptyEnvelope);

        final JsonObject legalAdviserDetails = usersGroupsService.getUserDetails(
                UUID.fromString(sessionDetails.getString("userId")),
                emptyEnvelope);

        final Optional<Note> listingNote = ofNullable(decisionToReferCaseForCourtHearingSaved.getListingNotes()).map(note -> note()
                .withId(randomUUID())
                .withText(note)
                .withType(LISTING)
                .withAddedAt(decisionToReferCaseForCourtHearingSaved.getDecisionSavedAt())
                .build());

        final ReferCaseForCourtHearing commandPayload = ReferCaseForCourtHearing.referCaseForCourtHearing()
                .withCaseId(decisionToReferCaseForCourtHearingSaved.getCaseId())
                .withDecisionId(decisionToReferCaseForCourtHearingSaved.getDecisionId())
                .withSessionId(decisionToReferCaseForCourtHearingSaved.getSessionId())
                .withLegalAdviser(user()
                        .withUserId(UUID.fromString(legalAdviserDetails.getString("userId")))
                        .withFirstName(legalAdviserDetails.getString("firstName"))
                        .withLastName(legalAdviserDetails.getString("lastName"))
                        .build())
                .withListingDetails(listingDetails()
                        .withReferralReasonId(decisionToReferCaseForCourtHearingSaved.getReferralReasonId())
                        .withHearingTypeId(decisionToReferCaseForCourtHearingSaved.getHearingTypeId())
                        .withEstimatedHearingDuration(decisionToReferCaseForCourtHearingSaved.getEstimatedHearingDuration())
                        .withRequestedAt(decisionToReferCaseForCourtHearingSaved.getDecisionSavedAt())
                        .withListingNotes(listingNote.orElse(null))
                        .build())
                .build();

        final JsonEnvelope command = enveloper.withMetadataFrom(envelopeFrom(metadataFrom(event.metadata()), NULL), "sjp.command.refer-case-for-court-hearing")
                .apply(commandPayload);

        sender.send(command);
    }

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

        final DefendantsOnlinePlea defendantOnlinePleaDetails = Optional.of(caseDetails.getOnlinePleaReceived())
                .filter(Boolean::booleanValue)
                .map(pleaReceived -> sjpService.getDefendantPleaDetails(caseDetails.getId(), emptyEnvelope))
                .orElse(null);

        final JsonObject caseFileDefendantDetails = prosecutionCaseFileService.getCaseFileDefendantDetails(caseDetails.getId(), emptyEnvelope).orElse(null);

        final List<HearingRequestView> listHearingRequestViews = hearingRequestsDataSourcingService.createHearingRequestViews(
                caseReferredForCourtHearing,
                caseDetails,
                defendantOnlinePleaDetails,
                caseFileDefendantDetails,
                emptyEnvelope);
        final SjpReferralView sjpReferral = sjpReferralDataSourcingService.createSjpReferralView(
                caseReferredForCourtHearing,
                caseDetails,
                emptyEnvelope);
        final List<ProsecutionCaseView> prosecutionCasesView = prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                caseDetails,
                caseReferredForCourtHearing,
                defendantOnlinePleaDetails,
                caseFileDefendantDetails,
                emptyEnvelope);
        final List<CourtDocumentView> courtDocumentViews = courtDocumentsDataSourcingService.createCourtDocumentViews(
                caseReferredForCourtHearing,
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

}
