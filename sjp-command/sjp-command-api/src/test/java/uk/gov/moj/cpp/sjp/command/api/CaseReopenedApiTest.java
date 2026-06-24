package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
        final JsonEnvelope command = EnvelopeFactory.createEnvelope(COMMAND_UNDO, createObjectBuilder().build());

        caseReopenedApi.undoCaseReopenedInLibra(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(CONTROLLER_COMMAND_UNDO));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldNotMarkAndThrowExceptionForInvalidReOpenedDate() {
        final JsonEnvelope envelope = getEnvelope(LocalDate.now().plusDays(2).toString(), COMMAND_MARK);

        var e = assertThrows(BadRequestException.class, () -> ((Consumer<JsonEnvelope>) caseReopenedApi::markCaseReopenedInLibra).accept(envelope));
        assertThat(e.getMessage(), is("invalid_reopened_date"));
    }

    @Test
    public void shouldNotUpdateAndThrowExceptionForInvalidReOpenedDate() {
        final JsonEnvelope envelope = getEnvelope(LocalDate.now().plusDays(2).toString(), COMMAND_UPDATE);

        var e = assertThrows(BadRequestException.class, () -> ((Consumer<JsonEnvelope>) caseReopenedApi::updateCaseReopenedInLibra).accept(envelope));
        assertThat(e.getMessage(), is("invalid_reopened_date"));
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
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("reopenedDate", reopenedDate)
                        .add("libraCaseNumber", libraCaseId)
                        .build());
    }
}