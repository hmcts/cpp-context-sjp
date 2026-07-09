package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision.caseDecision;
import static uk.gov.justice.json.schemas.domains.sjp.queries.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.justice.json.schemas.domains.sjp.queries.QueryOffenceDecision.queryOffenceDecision;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2.caseReferredForCourtHearingV2;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DocumentCategoryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingTypeView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferringJudicialDecisionView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.CourtDocumentsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.HearingRequestsDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.ProsecutionCasesDataSourcingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.SjpReferralDataSourcingService;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Mock
    private HearingRequestsDataSourcingService hearingRequestsDataSourcingService;

    @Mock
    private SjpService sjpService;

    @Mock
    private CourtDocumentsDataSourcingService courtDocumentsDataSourcingService;

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @InjectMocks
    private CourtReferralProcessor courtReferralProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> commandCaptor;

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
                                withJsonPath("$.rejectedAt")
                        )))));
    }

    @Test
    public void shouldSendCommandToProgressionWhenCaseReferredForCourtHearing() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(caseId)
                .withDecisionId(randomUUID())
                .build();

        final Metadata caseReferredForCourtHearingMetadata = metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing").build();
        final Envelope<CaseReferredForCourtHearing> caseReferredForCourtHearingEnvelope = envelopeFrom(
                caseReferredForCourtHearingMetadata,
                caseReferredForCourtHearing);

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(caseId)
                .withOnlinePleaReceived(true)
                .withDefendant(Defendant.defendant().withId(defendantId).build())
                .withCaseDecisions(asList(caseDecision()
                        .withId(caseReferredForCourtHearing.getDecisionId())
                        .withOffenceDecisions(asList(queryOffenceDecision()
                                .withDecisionType(REFER_FOR_COURT_HEARING)
                                .withReferralReasonId(randomUUID())
                                .build()
                        )).build()
                ))
                .build();
        when(sjpService.getCaseDetails(any(), any(JsonEnvelope.class))).thenReturn(caseDetails);

        final ProsecutionCaseView prosecutionCaseView = createDummyProsecutionCaseView(caseId);
        when(prosecutionCasesDataSourcingService.createProsecutionCaseViews(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(singletonList(prosecutionCaseView));

        final SjpReferralView sjpReferralView = createDummySjpReferralView();
        when(sjpReferralDataSourcingService.createSjpReferralView(any(), any(), any(), any())).thenReturn(sjpReferralView);

        final Optional<JsonObject> caseFileDefendantDetails = Optional.of(createObjectBuilder()
                .add("defendants", createArrayBuilder().add(createObjectBuilder()))
                .build());
        when(prosecutionCaseFileService.getCaseFileDetails(eq(caseId), any())).thenReturn(caseFileDefendantDetails);

        final HearingRequestView listHearingRequestView = createDummyHearingRequestView();
        when(hearingRequestsDataSourcingService.createHearingRequestViews(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(singletonList(listHearingRequestView));

        final CourtDocumentView courtDocumentView = createDummyCourtDocumentView();
        when(courtDocumentsDataSourcingService.createCourtDocumentViews((ZonedDateTime) any(), any(), any())).thenReturn(singletonList(courtDocumentView));

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
                                withJsonPath("$.courtReferral.courtDocuments[0].containsFinancialMeans", equalTo(courtDocumentView.getContainsFinancialMeans())),
                                withJsonPath("$.courtReferral.courtDocuments[0].materials[0].id", equalTo(courtDocumentView.getMaterials().get(0).getId().toString())),
                                withJsonPath("$.courtReferral.courtDocuments[0].materials[0].name", equalTo(courtDocumentView.getMaterials().get(0).getName()))
                        )))));
    }

    @Test
    public void shouldSendCommandToProgressionWhenCaseReferredForCourtHearingV2() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final CaseReferredForCourtHearingV2 caseReferredForCourtHearing = caseReferredForCourtHearingV2()
                .withCaseId(caseId)
                .withDecisionId(randomUUID())
                .build();

        final Metadata caseReferredForCourtHearingMetadata = metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing").build();
        final Envelope<CaseReferredForCourtHearingV2> caseReferredForCourtHearingEnvelope = envelopeFrom(
                caseReferredForCourtHearingMetadata,
                caseReferredForCourtHearing);

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(caseId)
                .withOnlinePleaReceived(true)
                .withDefendant(Defendant.defendant().withId(defendantId).build())
                .withCaseDecisions(asList(caseDecision()
                        .withId(caseReferredForCourtHearing.getDecisionId())
                        .withOffenceDecisions(asList(queryOffenceDecision()
                                .withDecisionType(REFER_FOR_COURT_HEARING)
                                .withReferralReasonId(randomUUID())
                                .build()
                        )).build()
                ))
                .build();
        when(sjpService.getCaseDetails(any(), any(JsonEnvelope.class))).thenReturn(caseDetails);

        final ProsecutionCaseView prosecutionCaseView = createDummyProsecutionCaseView(caseId);
        when(prosecutionCasesDataSourcingService.createProsecutionCaseViews(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(singletonList(prosecutionCaseView));

        final SjpReferralView sjpReferralView = createDummySjpReferralView();
        when(sjpReferralDataSourcingService.createSjpReferralView(any(), any(), any(), any())).thenReturn(sjpReferralView);

        final Optional<JsonObject> caseFileDefendantDetails = Optional.of(createObjectBuilder()
                .add("defendants", createArrayBuilder().add(createObjectBuilder()))
                .build());
        when(prosecutionCaseFileService.getCaseFileDetails(eq(caseId), any())).thenReturn(caseFileDefendantDetails);

        final HearingRequestView listHearingRequestView = createDummyHearingRequestView();
        when(hearingRequestsDataSourcingService.createHearingRequestViews(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(singletonList(listHearingRequestView));

        final CourtDocumentView courtDocumentView = createDummyCourtDocumentView();
        when(courtDocumentsDataSourcingService.createCourtDocumentViews((ZonedDateTime) any(), any(), any())).thenReturn(singletonList(courtDocumentView));

        courtReferralProcessor.caseReferredForCourtHearingV2(caseReferredForCourtHearingEnvelope);

        verify(sender).send(commandCaptor.capture());

        final JsonEnvelope actualCommand = commandCaptor.getValue();
        assertThat(actualCommand.metadata().name(), is("progression.refer-cases-to-court"));
        final HearingRequestView actualListHearingRequestView = jsonObjectToObjectConverter.convert(actualCommand.payloadAsJsonObject().getJsonObject("courtReferral").getJsonArray("listHearingRequests").getJsonObject(0), HearingRequestView.class);

        assertThat(actualListHearingRequestView.getJurisdictionType(), is(listHearingRequestView.getJurisdictionType()));
        assertThat(actualListHearingRequestView.getEstimateMinutes(), is(listHearingRequestView.getEstimateMinutes()));
        assertThat(actualListHearingRequestView.getProsecutorDatesToAvoid(), is(listHearingRequestView.getProsecutorDatesToAvoid()));
        assertThat(actualListHearingRequestView.getListingDirections(), is(listHearingRequestView.getListingDirections()));
        assertThat(actualListHearingRequestView.getHearingType(), is(listHearingRequestView.getHearingType()));
        assertThat(actualListHearingRequestView.getListDefendantRequests().size(), is(listHearingRequestView.getListDefendantRequests().size()));

        final ProsecutionCaseView actualProsecutionCaseView = jsonObjectToObjectConverter.convert(actualCommand.payloadAsJsonObject().getJsonObject("courtReferral").getJsonArray("prosecutionCases").getJsonObject(0), ProsecutionCaseView.class);
        assertThat(actualProsecutionCaseView, is(prosecutionCaseView));

        final SjpReferralView actualSjpReferralView = jsonObjectToObjectConverter.convert(actualCommand.payloadAsJsonObject().getJsonObject("courtReferral").getJsonObject("sjpReferral"), SjpReferralView.class);
        assertThat(actualSjpReferralView, is(sjpReferralView));

        final CourtDocumentView actualCourtDocumentView = jsonObjectToObjectConverter.convert(actualCommand.payloadAsJsonObject().getJsonObject("courtReferral").getJsonArray("courtDocuments").getJsonObject(0), CourtDocumentView.class);
        assertThat(actualCourtDocumentView.getCourtDocumentId(), is(courtDocumentView.getCourtDocumentId()));
        assertThat(actualCourtDocumentView.getName(), is(courtDocumentView.getName()));
        assertThat(actualCourtDocumentView.getDocumentTypeId(), is(courtDocumentView.getDocumentTypeId()));
        assertThat(actualCourtDocumentView.getMimeType(), is(courtDocumentView.getMimeType()));
        assertThat(actualCourtDocumentView.getDocumentCategory().getDefendantDocument().getProsecutionCaseId(), is(courtDocumentView.getDocumentCategory().getDefendantDocument().getProsecutionCaseId()));
        assertThat(actualCourtDocumentView.getMaterials().get(0).getId(), is(courtDocumentView.getMaterials().get(0).getId()));
        assertThat(actualCourtDocumentView.getMaterials().get(0).getName(), is(courtDocumentView.getMaterials().get(0).getName()));
    }

    @Test
    public void shouldHandleCourtReferralRelatedEvents() {
        assertThat(CourtReferralProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(allOf(
                        method("caseReferredForCourtHearing").thatHandles("sjp.events.case-referred-for-court-hearing"),
                        method("referToCourtHearingRejected").thatHandles("public.progression.refer-prosecution-cases-to-court-rejected")
                )));
    }

    private CourtDocumentView createDummyCourtDocumentView() {
        return new CourtDocumentView(
                randomUUID(),
                new DocumentCategoryView(
                        new DefendantDocumentView(
                                randomUUID(),
                                null
                        ), null
                ),
                "Bank Statement",
                randomUUID(),
                "pdf",
                true,
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
                        "",
                        emptyList()));
    }

    private ProsecutionCaseView createDummyProsecutionCaseView(UUID caseId) {
        return new ProsecutionCaseView(
                caseId,
                "J",
                "You did it",
                "Some facts in welsh",
                new ProsecutionCaseIdentifierView(
                        randomUUID(),
                        "TFL",
                        "TFL12345", "TFL12345"),
                emptyList(),
                "TFL");
    }

}
