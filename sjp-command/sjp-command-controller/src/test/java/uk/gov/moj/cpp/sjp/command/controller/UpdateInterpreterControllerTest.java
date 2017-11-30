package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper.RejectReason.CASE_ASSIGNED;
import static uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper.RejectReason.CASE_COMPLETED;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateInterpreterControllerTest {

    @Mock
    private Sender sender;

    @Mock
    private CaseUpdateHelper caseUpdateHelper;

    @InjectMocks
    private UpdateInterpreterController updateInterpreterController;

    @Test
    public void shouldHandleCommandsAndPassThrough() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope updateInterpreterCommand = getUpdateInterpreterCommand(caseId);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(updateInterpreterCommand)).thenReturn(Optional.empty());

        updateInterpreterController.updateInterpreter(updateInterpreterCommand);

        verify(sender).send(updateInterpreterCommand);
    }

    @Test
    public void shouldRejectInterpreterUpdateWhenCaseAssigned() {
        shouldRejectCaseUpdate(CASE_ASSIGNED);
    }

    @Test
    public void shouldRejectInterpreterUpdateWhenCaseCompleted() {
        shouldRejectCaseUpdate(CASE_COMPLETED);
    }

    private void shouldRejectCaseUpdate(final CaseUpdateHelper.RejectReason rejectReason) {

        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope updateInterpreterCommand = getUpdateInterpreterCommand(caseId);
        final JsonEnvelope rejectCaseUpdateCommand = getRejectCaseUpdateCommand(caseId, rejectReason);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(updateInterpreterCommand)).thenReturn(Optional.of(rejectCaseUpdateCommand));

        updateInterpreterController.updateInterpreter(updateInterpreterCommand);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue(), jsonEnvelope(
                metadata().withName("sjp.command.case-update-rejected"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.reason", equalTo(rejectReason.name()))
                ))));
    }

    private JsonEnvelope getUpdateInterpreterCommand(final UUID caseId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", UUID.randomUUID().toString())
                .add("language", "Welsh")
                .build();
        return createEnvelope("sjp.command.update-interpreter", payload);
    }

    private JsonEnvelope getRejectCaseUpdateCommand(final UUID caseId, final CaseUpdateHelper.RejectReason rejectReason) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("reason", rejectReason.name())
                .build();
        return createEnvelope("sjp.command.case-update-rejected", payload);
    }
}
