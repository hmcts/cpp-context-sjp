package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

import javax.json.Json;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReopenedApiTest {

    private static final String COMMAND_MARK = "sjp.mark-case-reopened-in-libra";
    private static final String CONTROLLER_COMMAND_MARK = "sjp.command.mark-case-reopened-in-libra";
    private static final String COMMAND_UPDATE = "sjp.update-case-reopened-in-libra";
    private static final String CONTROLLER_COMMAND_UPDATE = "sjp.command.update-case-reopened-in-libra";
    private static final String COMMAND_UNDO = "sjp.undo-case-reopened-in-libra";
    private static final String CONTROLLER_COMMAND_UNDO = "sjp.command.undo-case-reopened-in-libra";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private CaseReopenedApi caseReopenedApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private final String caseId = UUID.randomUUID().toString();

    private final String libraCaseId = UUID.randomUUID().toString();

    @Test
    public void shouldHandleCommand() {
        shouldPropagateCommandWhenValidDates(
                caseReopenedApi::markCaseReopenedInLibra,
                COMMAND_MARK,
                CONTROLLER_COMMAND_MARK
        );
    }

    @Test
    public void shouldUpdateCaseReopenedWhenValidDates() {
        shouldPropagateCommandWhenValidDates(
                caseReopenedApi::updateCaseReopenedInLibra,
                COMMAND_UPDATE,
                CONTROLLER_COMMAND_UPDATE
        );
    }

    @Test
    public void shouldUndoCaseReopened() {
        final JsonEnvelope command = EnvelopeFactory.createEnvelope(COMMAND_UNDO, Json.createObjectBuilder().build());

        caseReopenedApi.undoCaseReopenedInLibra(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(CONTROLLER_COMMAND_UNDO));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldNotMarkAndThrowExceptionForInvalidReOpenedDate() {
        shouldThrowExceptionForInvalidReOpenedDate(
                caseReopenedApi::markCaseReopenedInLibra,
                COMMAND_MARK
        );
    }

    @Test
    public void shouldNotUpdateAndThrowExceptionForInvalidReOpenedDate() {
        shouldThrowExceptionForInvalidReOpenedDate(
                caseReopenedApi::updateCaseReopenedInLibra,
                COMMAND_UPDATE
        );
    }

    private void shouldThrowExceptionForInvalidReOpenedDate(final Consumer<JsonEnvelope> f, final String command) {
        final JsonEnvelope envelope = getEnvelope(LocalDate.now().plusDays(2).toString(), command);
        exception.expect(BadRequestException.class);
        exception.expectMessage("invalid_reopened_date");

        f.accept(envelope);
    }

    private void shouldPropagateCommandWhenValidDates(final Consumer<JsonEnvelope> f, final String commandName, final String newCommandName) {
        final JsonEnvelope command = getEnvelope(LocalDate.now().minusDays(1).toString(), commandName);

        f.accept(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(newCommandName));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    private JsonEnvelope getEnvelope(final String reopenedDate, final String command) {
        return EnvelopeFactory.createEnvelope(command,
                Json.createObjectBuilder()
                        .add("caseId", caseId)
                        .add("reopenedDate", reopenedDate)
                        .add("libraCaseNumber", libraCaseId)
                        .build());
    }
}