package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.UPDATED_DATE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaUpdatedProcessorTest {

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private PleaUpdatedProcessor pleaUpdatedProcessor;

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

        verify(caseStateService).pleaUpdated(caseId, offenceId, pleaType, metadataCreatedAt, privateEvent.metadata());
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

        pleaUpdatedProcessor.handlePleaUpdated(privateEvent);

        verify(caseStateService).pleaUpdated(eq(caseId), eq(offenceId), eq(pleaType), notNull(ZonedDateTime.class), eq(privateEvent.metadata()));
    }

    @Test
    public void shouldUpdateCaseStateWhenPleaCancelled() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(PleaCancelled.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .build());

        pleaUpdatedProcessor.handlePleaCancelled(privateEvent);

        verify(caseStateService).pleaCancelled(caseId, offenceId, privateEvent.metadata());
    }

    @Test
    public void shouldHandlePleaEvent() {
        assertThat(PleaUpdatedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(allOf(
                        method("handlePleaUpdated").thatHandles(PleaUpdated.EVENT_NAME),
                        method("handlePleaCancelled").thatHandles(PleaCancelled.EVENT_NAME)
                )));
    }

}
