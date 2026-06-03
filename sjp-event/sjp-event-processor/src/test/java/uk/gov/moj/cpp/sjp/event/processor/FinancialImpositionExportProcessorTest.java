package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementNotificationService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FinancialImpositionExportProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private EnforcementNotificationService enforcementNotificationService;

    private final static UUID CASE_ID = randomUUID();
    private final static UUID DEFENDANT_ID = randomUUID();
    private final static String ACCOUNT_NUMBER = "123456780";

    @InjectMocks
    private FinancialImpositionExportProcessor financialImpositionExportProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldPublishPublicEventForAccountNumberAdded() {
        final JsonEnvelope privateEvent = createFinancialImpositionAccountNumberAddedEvent();
        financialImpositionExportProcessor.financialImpositionAccountNumberAdded(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), is("public.sjp.financial-imposition-account-number-added"));
        assertThat(publicEvent.payloadAsJsonObject(), equalTo(privateEvent.payloadAsJsonObject()));
    }

    private JsonEnvelope createFinancialImpositionAccountNumberAddedEvent() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("accountNumber", ACCOUNT_NUMBER)
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.events.financial-imposition-account-number-added"),
                payload);
    }

}
