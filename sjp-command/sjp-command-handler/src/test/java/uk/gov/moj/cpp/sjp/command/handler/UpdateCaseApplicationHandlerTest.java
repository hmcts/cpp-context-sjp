package uk.gov.moj.cpp.sjp.command.handler;

import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusCreated;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import javax.json.JsonObjectBuilder;
import java.util.UUID;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.APPEALED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.RELISTED;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;

@ExtendWith(MockitoExtension.class)
public class UpdateCaseApplicationHandlerTest {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String APPLICATION_ID_PROPERTY = "applicationId";
    private static final String APPLICATION_STATUS = "applicationStatus";

    private final CaseAggregate caseAggregate = new CaseAggregate();

    @InjectMocks
    private UpdateCaseApplicationHandler updateCaseApplicationHandler;

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;


    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CCApplicationStatusCreated.class, CCApplicationStatusUpdated.class, CaseStatusChanged.class);

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Spy
    private Clock clock = new UtcClock();

    @BeforeEach
    public void setUp() {
        when(eventSource.getStreamById(eq(CASE_ID))).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), eq(CaseAggregate.class))).thenReturn(caseAggregate);
        caseAggregate.getState().markCaseCompleted();
        caseAggregate.getState().setCaseId(CASE_ID);
    }

    @Test
    public void shouldRaiseApplicationStatusCreatedEventWhenApplicationStatusIsStatutoryDeclarationPending() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.STATUTORY_DECLARATION_PENDING);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-created"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.STATUTORY_DECLARATION_PENDING.name()))

                                )))
                ));

    }

    @Test
    public void shouldRaiseApplicationStatusCreatedEventWhenApplicationStatusIsReopeningPending() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.REOPENING_PENDING);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-created"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.REOPENING_PENDING.name()))

                                )))
                ));

    }

    @Test
    public void shouldRaiseApplicationStatusCreatedEventWhenApplicationStatusIsAppealPending() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.APPEAL_PENDING);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-created"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.APPEAL_PENDING.name()))

                                )))
                ));

    }



    @Test
    public void shouldRaiseApplicationStatusUpdatedAndCaseChangedEventWhenApplicationStatusIsStatutoryDeclarationGranted() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.STATUTORY_DECLARATION_GRANTED.name()))

                                ))),
                        jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                                payloadIsJson(AllOf.allOf(
                                        withJsonPath("$.caseId", Matchers.equalTo(CASE_ID.toString())),
                                        withJsonPath("$.caseStatus", Matchers.equalTo(RELISTED.toString()))
                                ))
                        )
                ));

    }

    @Test
    public void shouldRaiseApplicationStatusUpdatedAndCaseChangedEventWhenApplicationStatusIsReopeningGranted() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.REOPENING_GRANTED);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.REOPENING_GRANTED.name()))

                                ))),
                        jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                                payloadIsJson(AllOf.allOf(
                                        withJsonPath("$.caseId", Matchers.equalTo(CASE_ID.toString())),
                                        withJsonPath("$.caseStatus", Matchers.equalTo(RELISTED.toString()))
                                ))
                        )
                ));

    }

    @Test
    public void shouldRaiseApplicationStatusUpdatedAndCaseChangedEventWhenApplicationStatusIsAppealAllowed() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.APPEAL_ALLOWED);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.APPEAL_ALLOWED.name()))

                                ))),
                        jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                                payloadIsJson(AllOf.allOf(
                                        withJsonPath("$.caseId", Matchers.equalTo(CASE_ID.toString())),
                                        withJsonPath("$.caseStatus", Matchers.equalTo(APPEALED.toString()))
                                ))
                        )
                ));

    }

    @Test
    public void shouldRaiseApplicationStatusUpdatedEventWhenApplicationStatusIsStatutoryDeclarationWithDrawn() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN.name()))

                                )))
                ));

    }

    @Test
    public void shouldRaiseApplicationStatusUpdatedEventWhenApplicationStatusIsNotKnown() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonEnvelope command = createUpdateCaseApplicationCommand(CASE_ID, applicationId, ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        updateCaseApplicationHandler.updateCaseApplication(command);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(),
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.cc-application-status-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                        withJsonPath("$.status", equalTo(ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN.name()))

                                )))
                ));

    }





    private JsonEnvelope createUpdateCaseApplicationCommand(final UUID caseId, final UUID applicationId, final ApplicationStatus applicationStatus) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add(CASE_ID_PROPERTY, caseId.toString())
                .add(APPLICATION_ID_PROPERTY, applicationId.toString())
                .add(APPLICATION_STATUS, applicationStatus.name());

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-cc--case-application-status"),
                payload.build());
    }
}
