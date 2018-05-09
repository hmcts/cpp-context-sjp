package uk.gov.moj.cpp.sjp.command.controller;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatesToAvoidControllerTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private DatesToAvoidController datesToAvoidController;

    @Test
    public void shouldSendThroughAddDatesToAvoidCommand() {
        final UUID caseId = randomUUID();
        final JsonEnvelope addDatesToAvoidCommand = getAddDatesToAvoidCommand(caseId);

        datesToAvoidController.addDatesToAvoid(addDatesToAvoidCommand);

        verify(sender).send(addDatesToAvoidCommand);
    }

    private JsonEnvelope getAddDatesToAvoidCommand(final UUID caseId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", randomUUID().toString())
                .add("datesToAvoid", "No Thursdays")
                .build();
        return createEnvelope("sjp.command.add-dates-to-avoid", payload);
    }
}
