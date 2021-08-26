package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder.CASE_APPLICATION_CREATED;
import static uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder.CASE_MARKED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder.CASE_STAT_DECS;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.commands.CreateCaseApplication;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRejected;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateCaseApplicationHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseApplicationRecorded.class, CaseStatDecRecorded.class, CaseApplicationRejected.class,
            CaseMarkedReadyForDecision.class, CaseStatusChanged.class, ApplicationStatusChanged.class);

    @InjectMocks
    private CreateCaseApplicationHandler createCaseApplicationHandler;

    private final CaseAggregate caseAggregate = new CaseAggregate();

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID APP_ID = UUID.randomUUID();
    private static final UUID TYPE_ID = UUID.randomUUID();
    private static final UUID APPLICANT_ID = UUID.randomUUID();
    private static final String PRIORITY = "HIGH";


    final Metadata metadata = Envelope
            .metadataBuilder()
            .withName("sjp.command.create-case-application")
            .withId(UUID.randomUUID())
            .build();

    @Before
    public void setup() {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        caseAggregate.getState().markCaseCompleted();
        caseAggregate.getState().setCaseId(UUID.randomUUID());
        caseAggregate.getState().setManagedByAtcm(true);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateCaseApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("createCaseApplication")
                        .thatHandles("sjp.command.handler.create-case-application")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication(false)));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }


    public CreateCaseApplication createCaseApplication(final boolean isApplicationExist) {

        return CreateCaseApplication.createCaseApplication()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(APP_ID)
                        .withApplicationReceivedDate("2020-09-03")
                        .withType(CourtApplicationType.courtApplicationType()
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(CourtApplicationParty.courtApplicationParty()
                                .withId(APPLICANT_ID)
                                .withSummonsRequired(false)
                                .withNotificationRequired(false)
                                .build()).build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(isApplicationExist)
                .build();
    }
}
