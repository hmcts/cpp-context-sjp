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
import uk.gov.moj.cpp.sjp.command.controller.UpdateFinancialMeansController;
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
public class UpdateFinancialMeansControllerTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseUpdateHelper caseUpdateHelper;

    @InjectMocks
    private UpdateFinancialMeansController updateFinancialMeansController;

    @Test
    public void shouldHandleCommandsAndPassThrough() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope updateFinancialMeansCommand = getUpdateFinancialMeansCommand(caseId);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(updateFinancialMeansCommand)).thenReturn(Optional.empty());

        updateFinancialMeansController.updateFinancialMeans(updateFinancialMeansCommand);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(updateFinancialMeansCommand);
    }

    @Test
    public void shouldRejectCaseUpdateWhenCaseAssigned() {
        shouldRejectCaseUpdate(CASE_ASSIGNED);
    }

    @Test
    public void shouldRejectCaseUpdateWhenCaseCompleted() {
        shouldRejectCaseUpdate(CASE_COMPLETED);
    }

    private void shouldRejectCaseUpdate(final CaseUpdateHelper.RejectReason rejectReason) {

        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope updateFinancialMeansCommand = getUpdateFinancialMeansCommand(caseId);
        final JsonEnvelope rejectCaseUpdateCommand = getRejectCaseUpdateCommand(caseId, rejectReason);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(updateFinancialMeansCommand)).thenReturn(Optional.of(rejectCaseUpdateCommand));

        updateFinancialMeansController.updateFinancialMeans(updateFinancialMeansCommand);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue(), jsonEnvelope(
                metadata().withName("sjp.command.case-update-rejected"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.reason", equalTo(rejectReason.name()))
                ))));
    }

    private JsonEnvelope getUpdateFinancialMeansCommand(final UUID caseId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", UUID.randomUUID().toString())
                .add("income", Json.createObjectBuilder())
                .add("benefits", Json.createObjectBuilder())
                .build();
        return createEnvelope("sjp.command.update-financial-means", payload);
    }

    private JsonEnvelope getRejectCaseUpdateCommand(final UUID caseId, final CaseUpdateHelper.RejectReason rejectReason) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("reason", rejectReason.name())
                .build();
        return createEnvelope("sjp.command.case-update-rejected", payload);
    }
}