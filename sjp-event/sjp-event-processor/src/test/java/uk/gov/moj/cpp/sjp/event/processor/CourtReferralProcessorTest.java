package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved.decisionToReferCaseForCourtHearingSaved;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DocumentCategoryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingTypeView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferringJudicialDecisionView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;
import uk.gov.moj.cpp.sjp.event.processor.service.ResultingService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.CourtDocumentsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.HearingRequestsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.ProsecutionCasesDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.SjpReferralDataSourcingService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtReferralProcessorTest {

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private ProsecutionCasesDataSourcingService prosecutionCasesDataSourcingService;

    @Mock
    private SjpReferralDataSourcingService sjpReferralDataSourcingService;

    @Mock
    private HearingRequestsDataSourcingService hearingRequestsDataSourcingService;

    @Mock
    private SjpService sjpService;

    @Mock
    private CourtDocumentsDataSourcingService courtDocumentsDataSourcingService;

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @InjectMocks
    private CourtReferralProcessor courtReferralProcessor;

    @Test
    public void shouldSentReferCaseForCourtHearingCommandWhenDecisionToReferCaseForCourtHearingSaved() {

        final DecisionToReferCaseForCourtHearingSaved decisionToReferCaseForCourtHearingSaved = decisionToReferCaseForCourtHearingSaved()
                .withCaseId(randomUUID())
                .withSessionId(randomUUID())
                .withReferralReasonId(randomUUID())
                .withHearingTypeId(randomUUID())
                .withEstimatedHearingDuration(nextInt(0, 999))
                .withListingNotes(randomAlphanumeric(100))
                .withDecisionSavedAt(now(UTC))
                .build();

        final Envelope<DecisionToReferCaseForCourtHearingSaved> event = envelopeFrom(metadataWithRandomUUID("public.resulting.decision-to-refer-case-for-court-hearing-saved"), decisionToReferCaseForCourtHearingSaved);

        courtReferralProcessor.decisionToReferCaseForCourtHearingSaved(event);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().envelopedWith(event.metadata()).withName("sjp.command.refer-case-for-court-hearing"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(decisionToReferCaseForCourtHearingSaved.getCaseId().toString())),
                                withJsonPath("$.sessionId", equalTo(decisionToReferCaseForCourtHearingSaved.getSessionId().toString())),
                                withJsonPath("$.referralReasonId", equalTo(decisionToReferCaseForCourtHearingSaved.getReferralReasonId().toString())),
                                withJsonPath("$.hearingTypeId", equalTo(decisionToReferCaseForCourtHearingSaved.getHearingTypeId().toString())),
                                withJsonPath("$.estimatedHearingDuration", equalTo(decisionToReferCaseForCourtHearingSaved.getEstimatedHearingDuration())),
                                withJsonPath("$.listingNotes", equalTo(decisionToReferCaseForCourtHearingSaved.getListingNotes())),
                                withJsonPath("$.requestedAt", equalTo(decisionToReferCaseForCourtHearingSaved.getDecisionSavedAt().toString()))
                        )))));
    }

    @Test
    public void shouldIgnoreCourtReferralRejectionForNonSjpCase() {

        final JsonObject payload = createObjectBuilder()
                .add("prosecutorCourtReferral", createObjectBuilder().build())
                .add("rejectedReason", "Rejection reason")
                .build();

        final JsonEnvelope event = createEnvelope("public.progression.refer-prosecution-cases-to-court-rejected", payload);

        courtReferralProcessor.referToCourtHearingRejected(event);

        verify(sender, never()).send(any());
    }

    @Test
    public void shouldSendCommandWhenProgressionContextReferralRejectionEventReceived() {
        final String rejectionDescription = "Business rule validations";
        final UUID caseId = randomUUID();

        final JsonObject payload = createObjectBuilder()
                .add("sjpCourtReferral", createObjectBuilder()
                        .add("sjpReferral", createObjectBuilder().build())
                        .add("prosecutionCases", createArrayBuilder()
                                .add(createObjectBuilder().add("id", caseId.toString()))))
                .add("rejectedReason", rejectionDescription)
                .build();

        final JsonEnvelope event = createEnvelope("public.progression.refer-prosecution-cases-to-court-rejected", payload);

        courtReferralProcessor.referToCourtHearingRejected(event);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().envelopedWith(event.metadata()).withName("sjp.command.record-case-referral-for-court-hearing-rejection"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.rejectionReason", equalTo(rejectionDescription)),
                                withJsonPath("$.rejectedAt", equalTo(clock.now().toString()))
                        )))));
    }

    @Test
    public void shouldSendCommandToProgressionWhenCaseReferredForCourtHearing() {
        final UUID caseId = randomUUID();

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(caseId)
                .build();

        final Metadata caseReferredForCourtHearingMetadata = metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing").build();
        final Envelope<CaseReferredForCourtHearing> caseReferredForCourtHearingEnvelope = envelopeFrom(
                caseReferredForCourtHearingMetadata,
                caseReferredForCourtHearing);

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(caseId.toString())
                .withOnlinePleaReceived(true)
                .build();
        when(sjpService.getCaseDetails(any(), any(JsonEnvelope.class))).thenReturn(caseDetails);

        final ProsecutionCaseView prosecutionCaseView = createDummyProsecutionCaseView(caseId);
        when(prosecutionCasesDataSourcingService.createProsecutionCaseViews(any(), any(), any(), any())).thenReturn(singletonList(prosecutionCaseView));

        final SjpReferralView sjpReferralView = createDummySjpReferralView();
        when(sjpReferralDataSourcingService.createSjpReferralView(any(), any(), any())).thenReturn(sjpReferralView);

        final HearingRequestView listHearingRequestView = createDummyHearingRequestView();
        when(hearingRequestsDataSourcingService.createHearingRequestViews(any(), any(), any(), any())).thenReturn(singletonList(listHearingRequestView));

        final CourtDocumentView courtDocumentView = createDummyCourtDocumentView();
        when(courtDocumentsDataSourcingService.createCourtDocumentViews(any(), any(), any())).thenReturn(singletonList(courtDocumentView));

        courtReferralProcessor.caseReferredForCourtHearing(caseReferredForCourtHearingEnvelope);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().envelopedWith(caseReferredForCourtHearingMetadata).withName("progression.refer-cases-to-court"),
                        payloadIsJson(allOf(
                                withJsonPath("$.courtReferral.listHearingRequests.length()", equalTo(1)),
                                withJsonPath("$.courtReferral.listHearingRequests[0].jurisdictionType", equalTo(listHearingRequestView.getJurisdictionType())),
                                withJsonPath("$.courtReferral.listHearingRequests[0].estimateMinutes", equalTo(listHearingRequestView.getEstimateMinutes())),
                                withJsonPath("$.courtReferral.listHearingRequests[0].prosecutorDatesToAvoid", equalTo(listHearingRequestView.getProsecutorDatesToAvoid())),
                                withJsonPath("$.courtReferral.listHearingRequests[0].listingDirections", equalTo(listHearingRequestView.getListingDirections())),
                                withJsonPath("$.courtReferral.listHearingRequests[0].hearingType.id", equalTo(listHearingRequestView.getHearingType().getId().toString())),
                                withJsonPath("$.courtReferral.listHearingRequests[0].listDefendantRequests.length()", equalTo(listHearingRequestView.getListDefendantRequests().size())),
                                withJsonPath("$.courtReferral.prosecutionCases.length()", equalTo(1)),
                                withJsonPath("$.courtReferral.prosecutionCases[0].id", equalTo(caseId.toString())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].initiationCode", equalTo(prosecutionCaseView.getInitiationCode())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].statementOfFacts", equalTo(prosecutionCaseView.getStatementOfFacts())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].prosecutionCaseIdentifier.prosecutionAuthorityId", equalTo(prosecutionCaseView.getProsecutionCaseIdentifier().getProsecutionAuthorityId().toString())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", equalTo(prosecutionCaseView.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].prosecutionCaseIdentifier.prosecutionAuthorityReference", equalTo(prosecutionCaseView.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].defendants.length()", equalTo(prosecutionCaseView.getDefendants().size())),
                                withJsonPath("$.courtReferral.sjpReferral.noticeDate", equalTo(sjpReferralView.getNoticeDate().format(DateTimeFormatter.ISO_DATE))),
                                withJsonPath("$.courtReferral.sjpReferral.referralDate", equalTo(sjpReferralView.getReferralDate().format(DateTimeFormatter.ISO_DATE))),
                                withJsonPath("$.courtReferral.sjpReferral.referringJudicialDecision.location", equalTo(sjpReferralView.getReferringJudicialDecision().getLocation())),
                                withJsonPath("$.courtReferral.sjpReferral.referringJudicialDecision.judiciary.length()", equalTo(sjpReferralView.getReferringJudicialDecision().getJudiciary().size())),
                                withJsonPath("$.courtReferral.courtDocuments.length()", equalTo(1)),
                                withJsonPath("$.courtReferral.courtDocuments[0].courtDocumentId", equalTo(courtDocumentView.getCourtDocumentId().toString())),
                                withJsonPath("$.courtReferral.courtDocuments[0].name", equalTo(courtDocumentView.getName())),
                                withJsonPath("$.courtReferral.courtDocuments[0].documentTypeId", equalTo(courtDocumentView.getDocumentTypeId().toString())),
                                withJsonPath("$.courtReferral.courtDocuments[0].mimeType", equalTo(courtDocumentView.getMimeType())),
                                withJsonPath("$.courtReferral.courtDocuments[0].documentCategory.defendantDocument.prosecutionCaseId", equalTo(courtDocumentView.getDocumentCategory().getDefendantDocument().getProsecutionCaseId().toString())),
                                withJsonPath("$.courtReferral.courtDocuments[0].materials[0].id", equalTo(courtDocumentView.getMaterials().get(0).getId().toString())),
                                withJsonPath("$.courtReferral.courtDocuments[0].materials[0].name", equalTo(courtDocumentView.getMaterials().get(0).getName()))
                        )))));
    }

    private CourtDocumentView createDummyCourtDocumentView() {
        return new CourtDocumentView(
                randomUUID(),
                new DocumentCategoryView(
                        new DefendantDocumentView(
                                randomUUID(),
                                emptyList()
                        )
                ),
                "Bank Statement",
                randomUUID(),
                "pdf",
                Collections.singletonList(new MaterialView(randomUUID(),
                        "BankStatment.pdf",
                        ZonedDateTime.now(),
                        "pdf"))
        );
    }

    private HearingRequestView createDummyHearingRequestView() {
        return new HearingRequestView(
                "magistrates",
                20,
                "wednesdays",
                "tricky defendant",
                new HearingTypeView(randomUUID()),
                emptyList());
    }

    private SjpReferralView createDummySjpReferralView() {
        return new SjpReferralView(
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                new ReferringJudicialDecisionView(
                        "Lavender Hill Magistrates' Court",
                        emptyList()));
    }

    private ProsecutionCaseView createDummyProsecutionCaseView(UUID caseId) {
        return new ProsecutionCaseView(
                caseId,
                "J",
                "You did it",
                new ProsecutionCaseIdentifierView(
                        randomUUID(),
                        "TFL",
                        "TFL12345"),
                emptyList());
    }

    @Test
    public void shouldHandleCourtReferralRelatedEvents() {
        assertThat(CourtReferralProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(allOf(
                        method("decisionToReferCaseForCourtHearingSaved").thatHandles("public.resulting.decision-to-refer-case-for-court-hearing-saved"),
                        method("caseReferredForCourtHearing").thatHandles("sjp.events.case-referred-for-court-hearing"),
                        method("referToCourtHearingRejected").thatHandles("public.progression.refer-prosecution-cases-to-court-rejected")
                )));
    }

}