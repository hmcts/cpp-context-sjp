package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.controller.UpdatePleaController;
import uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePleaControllerTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();

    private static final String STRUCTURE_COMMAND_PLEA_UPDATE = "sjp.command.update-plea";
    private static final String STRUCTURE_CPS_PLEA_UPDATE = "sjp.cps.update-plea";
    private static final String PLEA = "GUILTY";

    private static final String STRUCTURE_COMMAND_CASE_UPDATE_REJECTED = "sjp.command.case-update-rejected";
    private static final String REASON_CASE_ASSIGNED = CaseUpdateHelper.RejectReason.CASE_ASSIGNED.name();
    private static final String REASON_CASE_COMPLETED = CaseUpdateHelper.RejectReason.CASE_COMPLETED.name();

    @Mock
    private Sender sender;
    @InjectMocks
    private UpdatePleaController controller;
    @Mock
    private CaseUpdateHelper caseUpdateHelper;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void shouldUpdatePlea() {

        final JsonEnvelope envelope = getCommandEnvelope(false);
        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(envelope)).thenReturn(Optional.empty());
        controller.updatePlea(envelope);
        verify(sender).send(envelope);

    }

    @Test
    public void shouldCancelPlea() {

        final JsonEnvelope envelope = getCommandEnvelope(true);
        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(envelope)).thenReturn(Optional.empty());
        controller.cancelPlea(envelope);
        verify(sender).send(envelope);

    }

    @Test
    public void shouldUpdateCpsPlea() {

        final JsonEnvelope envelope = getCpsUpdatePleaCommandEnvelope();
        controller.updateCpsPlea(envelope);
        verify(sender).send(envelope);

    }

    @Test
    public void shouldRejectCaseUpdateWhenCaseAssigned() {
        //Given
        final JsonEnvelope command = getCommandEnvelope(false);

        final JsonObject caseUpdateRejectedPayload = Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("reason", REASON_CASE_ASSIGNED)
                .build();
        final JsonEnvelope envelope = createEnvelope(STRUCTURE_COMMAND_CASE_UPDATE_REJECTED, caseUpdateRejectedPayload);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(command))
                .thenReturn(Optional.of(envelope));

        controller.updatePlea(command);
        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue(), jsonEnvelope(
                metadata().withName(STRUCTURE_COMMAND_CASE_UPDATE_REJECTED),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                        withJsonPath("$.reason", equalTo(REASON_CASE_ASSIGNED))
                        ))));
    }

    @Test
    public void shouldRejectCaseUpdateWhenCaseCompleted() {
        //Given
        final JsonEnvelope command = getCommandEnvelope(false);

        final JsonObject caseUpdateRejectedPayload = Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("reason", REASON_CASE_COMPLETED)
                .build();
        final JsonEnvelope envelope = createEnvelope(STRUCTURE_COMMAND_CASE_UPDATE_REJECTED, caseUpdateRejectedPayload);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(command))
                .thenReturn(Optional.of(envelope));

        controller.updatePlea(command);
        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(captor.capture());

        assertThat(captor.getValue(), jsonEnvelope(
                metadata().withName(STRUCTURE_COMMAND_CASE_UPDATE_REJECTED),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                        withJsonPath("$.reason", equalTo(REASON_CASE_COMPLETED))
                ))));
    }

    @Test
    public void shouldHaveAnnotationWithProperActionName() throws NoSuchMethodException {
        Method updatePlea = UpdatePleaController.class.getMethod("updatePlea", JsonEnvelope.class);

        Handles annotation = updatePlea.getAnnotation(Handles.class);
        assertEquals(STRUCTURE_COMMAND_PLEA_UPDATE, annotation.value());
    }

    private JsonEnvelope getCommandEnvelope(boolean cancel) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("offenceId", OFFENCE_ID.toString());
        if (!cancel) {
            builder.add("plea", PLEA);
        }
        return createEnvelope(STRUCTURE_COMMAND_PLEA_UPDATE, builder.build());
    }

    private JsonEnvelope getCpsUpdatePleaCommandEnvelope() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .build();
        return createEnvelope(STRUCTURE_CPS_PLEA_UPDATE, payload);
    }
}