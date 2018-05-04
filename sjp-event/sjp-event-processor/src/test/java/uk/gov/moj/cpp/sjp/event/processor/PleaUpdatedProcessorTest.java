package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

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

        final JsonEnvelope privateEvent = createEnvelope(PleaUpdated.EVENT_NAME,
                Json.createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(OFFENCE_ID, offenceId.toString())
                        .add(PLEA, pleaType.name())
                        .build());

        pleaUpdatedProcessor.handlePleaUpdated(privateEvent);

        verify(caseStateService).pleaUpdated(caseId, offenceId, pleaType, privateEvent.metadata());
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
