package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.argThat;
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

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmployerControllerTest {

    @Mock
    private Sender sender;

    @Mock
    private CaseUpdateHelper caseUpdateHelper;

    @InjectMocks
    private EmployerController updateEmployerController;

    @Test
    public void shouldSendThroughUpdateEmployerCommand() {
        final UUID caseId = randomUUID();
        final JsonEnvelope updateEmployerCommand = getUpdateEmployerCommand(caseId);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(updateEmployerCommand)).thenReturn(Optional.empty());

        updateEmployerController.updateEmployer(updateEmployerCommand);

        verify(sender).send(updateEmployerCommand);
    }

    @Test
    public void shouldSendThroughDeleteEmployerCommand() {
        final UUID caseId = randomUUID();
        final JsonEnvelope deleteEmployerCommand = getDeleteEmployerCommand(caseId);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(deleteEmployerCommand)).thenReturn(Optional.empty());

        updateEmployerController.deleteEmployer(deleteEmployerCommand);

        verify(sender).send(deleteEmployerCommand);
    }

    @Test
    public void shouldRejectUpdateEmployerCommandWhenCaseAssigned() {
        shouldRejectEmployerUpdate(CASE_ASSIGNED);
    }

    @Test
    public void shouldRejectUpdateEmployerCommandWhenCaseCompleted() {
        shouldRejectEmployerUpdate(CASE_COMPLETED);
    }

    @Test
    public void shouldRejectDeleteEmployerCommandWhenCaseAssigned() {
        shouldRejectEmployerDeletion(CASE_ASSIGNED);
    }

    @Test
    public void shouldRejectDeleteEmployerCommandWhenCaseCompleted() {
        shouldRejectEmployerDeletion(CASE_COMPLETED);
    }

    private void shouldRejectEmployerUpdate(final CaseUpdateHelper.RejectReason rejectReason) {

        final UUID caseId = randomUUID();
        final JsonEnvelope updateEmployerCommand = getUpdateEmployerCommand(caseId);
        final JsonEnvelope rejectCaseUpdateCommand = getRejectCaseUpdateCommand(caseId, rejectReason);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(updateEmployerCommand)).thenReturn(Optional.of(rejectCaseUpdateCommand));

        updateEmployerController.updateEmployer(updateEmployerCommand);

        verify(sender).send(argThat(getCaseUpdateRejectedMatcher(caseId, rejectReason)));
    }

    private void shouldRejectEmployerDeletion(final CaseUpdateHelper.RejectReason rejectReason) {
        final UUID caseId = randomUUID();
        final JsonEnvelope deleteEmployerCommand = getDeleteEmployerCommand(caseId);
        final JsonEnvelope rejectCaseUpdateCommand = getRejectCaseUpdateCommand(caseId, rejectReason);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(deleteEmployerCommand)).thenReturn(Optional.of(rejectCaseUpdateCommand));

        updateEmployerController.deleteEmployer(deleteEmployerCommand);

        verify(sender).send(argThat(getCaseUpdateRejectedMatcher(caseId, rejectReason)));
    }

    private JsonEnvelope getUpdateEmployerCommand(final UUID caseId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", randomUUID().toString())
                .add("income", Json.createObjectBuilder())
                .add("benefits", Json.createObjectBuilder())
                .build();
        return createEnvelope("sjp.command.handler.update-employer", payload);
    }

    private JsonEnvelope getDeleteEmployerCommand(final UUID caseId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", randomUUID().toString())
                .build();
        return createEnvelope("sjp.command.delete-employer", payload);
    }

    private JsonEnvelope getRejectCaseUpdateCommand(final UUID caseId, final CaseUpdateHelper.RejectReason rejectReason) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("reason", rejectReason.name())
                .build();
        return createEnvelope("sjp.command.case-update-rejected", payload);
    }

    private Matcher<JsonEnvelope> getCaseUpdateRejectedMatcher(final UUID caseId, CaseUpdateHelper.RejectReason rejectReason) {
        return jsonEnvelope(
                metadata().withName("sjp.command.case-update-rejected"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.reason", equalTo(rejectReason.name()))
                )));
    }
}