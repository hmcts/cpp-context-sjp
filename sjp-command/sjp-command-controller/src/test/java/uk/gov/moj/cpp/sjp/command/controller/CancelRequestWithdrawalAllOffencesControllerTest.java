package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.controller.CancelRequestWithdrawalAllOffencesController;
import uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CancelRequestWithdrawalAllOffencesControllerTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String STRUCTURE_COMMAND_CASE_UPDATE_REJECTED = "sjp.command.case-update-rejected";
    private static final String STRUCTURE_COMMAND_CANCEL_REQUEST_WITHDRAWAL_ALL_OFFENCES = "sjp.command.cancel-request-withdrawal-all-offences";

    @Mock
    private Sender sender;
    @InjectMocks
    private CancelRequestWithdrawalAllOffencesController controller;
    @Mock
    private CaseUpdateHelper caseUpdateHelper;

    @Test
    public void shouldCancelRequestWithdrawalAllOffences() {
        final JsonEnvelope envelope = getCancelWithdrawCommandEnvelope();
        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(envelope))
                .thenReturn(Optional.empty());

        controller.cancelRequestWithdrawalAllOffences(envelope);
        verify(sender).send(envelope);
    }

    @Test
    public void shouldRejectCaseUpdate() {
        //Given
        final JsonEnvelope command = getCancelWithdrawCommandEnvelope();

        final JsonObject caseUpdateRejectedPayload = Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("reason", CaseUpdateHelper.RejectReason.CASE_COMPLETED.name())
                .build();
        final JsonEnvelope envelope = createEnvelope(STRUCTURE_COMMAND_CASE_UPDATE_REJECTED, caseUpdateRejectedPayload);

        when(caseUpdateHelper.checkForCaseUpdateRejectReasons(command))
                .thenReturn(Optional.of(envelope));

        controller.cancelRequestWithdrawalAllOffences(command);

        verify(sender).send(envelope);
    }

    @Test
    public void shouldHaveAnnotationWithProperActionName() throws NoSuchMethodException {
        Method cancelRequestWithdrawalMethod = CancelRequestWithdrawalAllOffencesController.class.getMethod("cancelRequestWithdrawalAllOffences", JsonEnvelope.class);

        Handles annotation = cancelRequestWithdrawalMethod.getAnnotation(Handles.class);
        assertEquals(STRUCTURE_COMMAND_CANCEL_REQUEST_WITHDRAWAL_ALL_OFFENCES, annotation.value());
    }

    private JsonEnvelope getCancelWithdrawCommandEnvelope() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .build();
        return createEnvelope(STRUCTURE_COMMAND_CANCEL_REQUEST_WITHDRAWAL_ALL_OFFENCES, payload);
    }
}
