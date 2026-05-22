package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.DONE;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCasesManagementStatusProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private UpdateCasesManagementStatusProcessor updateCasesManagementStatusProcessor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> jsonEnvelopeCaptor;

    @Test
    public void shouldUpdateCaseManagementStatus() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope("sjp.events.update-cases-management-status",
                createObjectBuilder().add("cases", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", case1Id.toString())
                                .add("caseManagementStatus", CaseManagementStatus.IN_PROGRESS.toString()))
                        .add(createObjectBuilder()
                                .add("caseId", case2Id.toString())
                                .add("caseManagementStatus", DONE.toString()))
                ).build());

        updateCasesManagementStatusProcessor.updateCasesManagementStatus(privateEvent);

        verify(sender, times(3)).send(jsonEnvelopeCaptor.capture());

        final List<DefaultEnvelope> envelopes = jsonEnvelopeCaptor.getAllValues();

        assertEquals("sjp.command.change-case-management-status", envelopes.get(0).metadata().name());
        assertEquals("sjp.command.change-case-management-status", envelopes.get(1).metadata().name());
        assertEquals("public.sjp.cases-management-status-updated", envelopes.get(2).metadata().name());
    }
}
