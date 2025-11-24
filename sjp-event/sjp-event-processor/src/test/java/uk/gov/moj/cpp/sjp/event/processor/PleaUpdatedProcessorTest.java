package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEAD_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.UPDATED_DATE;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaUpdatedProcessorTest {

    @Mock
    private CaseStateService caseStateService;

    @Mock
    private Clock clock;

    @InjectMocks
    private PleaUpdatedProcessor pleaUpdatedProcessor;

    @Mock
    private Sender sender;

    private final Clock stoppedClock = new StoppedClock(new UtcClock().now());

    @Test
    public void shouldUpdateCaseStateWhenPleaUpdated() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final PleaType pleaType = GUILTY;
        final ZonedDateTime pleaUpdatedDate = ZonedDateTime.now(UTC).minusDays(2);

        final JsonEnvelope privateEvent = createEnvelope(PleaUpdated.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEA, pleaType.name())
                        .add(UPDATED_DATE, pleaUpdatedDate.toString())
                        .build());

        pleaUpdatedProcessor.handlePleaUpdated(privateEvent);
        verify(caseStateService).pleaUpdated(caseId, offenceId, pleaType, pleaUpdatedDate, privateEvent.metadata());
    }

    @Test
    public void shouldUpdateCaseStateWhenPleaUpdatedWithoutDateWithMetadataCreationDate() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final PleaType pleaType = NOT_GUILTY;
        final ZonedDateTime metadataCreatedAt = ZonedDateTime.now(UTC).minusMonths(3);

        final JsonEnvelope privateEvent = envelopeFrom(
                metadataWithRandomUUID(PleaUpdated.EVENT_NAME)
                        .createdAt(metadataCreatedAt)
                        .build(),
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEA, pleaType.name())
                        .build());

        pleaUpdatedProcessor.handlePleaUpdated(privateEvent);

        final ArgumentCaptor<ZonedDateTime> createdAtCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);

        verify(caseStateService).pleaUpdated(eq(caseId), eq(offenceId), eq(pleaType), createdAtCaptor.capture(), eq(privateEvent.metadata()));

        final ZonedDateTime actualCreatedAt = createdAtCaptor.getValue();
        assertThat(actualCreatedAt.getYear(), is(metadataCreatedAt.getYear()));
        assertThat(actualCreatedAt.getMonth(), is(metadataCreatedAt.getMonth()));
        assertThat(actualCreatedAt.getHour(), is(metadataCreatedAt.getHour()));
        assertThat(actualCreatedAt.getMinute(), is(metadataCreatedAt.getMinute()));
        assertThat(actualCreatedAt.getSecond(), is(metadataCreatedAt.getSecond()));
    }

    @Test
    public void shouldUpdateCaseStateWhenPleaUpdatedWithoutDateWithoutMetadataCreationDate() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final PleaType pleaType = NOT_GUILTY;

        final JsonEnvelope privateEvent = createEnvelope(PleaUpdated.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEA, pleaType.name())
                        .build());

        assertThat(privateEvent.payloadAsJsonObject().containsKey(UPDATED_DATE), is(false));
        assertThat(privateEvent.metadata().createdAt().isPresent(), is(false));

        when(clock.now()).thenReturn(stoppedClock.now());

        pleaUpdatedProcessor.handlePleaUpdated(privateEvent);

        verify(caseStateService).pleaUpdated(eq(caseId), eq(offenceId), eq(pleaType), notNull(ZonedDateTime.class), eq(privateEvent.metadata()));
    }

    @Test
    public void shouldUpdateCaseStateWhenPleadedGuilty() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(PleadedGuilty.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEAD_DATE, stoppedClock.now().toString())
                        .build());

        pleaUpdatedProcessor.handlePleadedGuilty(privateEvent);
        verify(caseStateService).pleaUpdated(eq(caseId), eq(offenceId), eq(GUILTY),
                notNull(ZonedDateTime.class), eq(privateEvent.metadata()));
    }

    @Test
    public void shouldUpdateCaseStateWhenPleadedGuiltyCourtHearingRequested() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(PleadedGuiltyCourtHearingRequested.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEAD_DATE, stoppedClock.now().toString())
                        .build());

        pleaUpdatedProcessor.handlePleadedGuiltyCourtHearingRequested(privateEvent);
        verify(caseStateService).pleaUpdated(eq(caseId), eq(offenceId), eq(GUILTY_REQUEST_HEARING),
                notNull(ZonedDateTime.class), eq(privateEvent.metadata()));
    }

    @Test
    public void shouldUpdateCaseStateWhenPleadedNotGuilty() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(PleadedNotGuilty.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEAD_DATE, stoppedClock.now().toString())
                        .build());

        pleaUpdatedProcessor.handlePleadedNotGuilty(privateEvent);
        verify(caseStateService).pleaUpdated(eq(caseId), eq(offenceId), eq(NOT_GUILTY),
                notNull(ZonedDateTime.class), eq(privateEvent.metadata()));
    }

    @Test
    public void shouldHandlePleaEvent() {
        assertThat(PleaUpdatedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(allOf(
                        method("handlePleaUpdated").thatHandles(PleaUpdated.EVENT_NAME),
                        method("handlePleaCancelled").thatHandles(PleaCancelled.EVENT_NAME),
                        method("handlePleadedGuilty").thatHandles(PleadedGuilty.EVENT_NAME),
                        method("handlePleadedGuiltyCourtHearingRequested").thatHandles(PleadedGuiltyCourtHearingRequested.EVENT_NAME),
                        method("handlePleadedNotGuilty").thatHandles(PleadedNotGuilty.EVENT_NAME)
                )));
    }

}
