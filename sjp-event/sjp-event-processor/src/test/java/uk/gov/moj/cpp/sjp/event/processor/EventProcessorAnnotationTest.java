package uk.gov.moj.cpp.sjp.event.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import java.lang.reflect.Method;

public class EventProcessorAnnotationTest {

    private static final String EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA = "sjp.events.case-reopened-in-libra";
    private static final String EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UPDATED = "sjp.events.case-reopened-in-libra-updated";
    private static final String EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UNDONE = "sjp.events.case-reopened-in-libra-undone";
    private static final String METHOD_CASE_REOPENED_IN_LIBRA = "handleCaseReopenedInLibra";
    private static final String METHOD_CASE_REOPENED_IN_LIBRA_UPDATED = "handleCaseReopenedInLibraUpdated";
    private static final String METHOD_CASE_REOPENED_IN_LIBRA_UNDONE = "handleCaseReopenedInLibraUndone";

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(CaseReopenedProcessor.class, EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA, METHOD_CASE_REOPENED_IN_LIBRA),
                Arguments.of(CaseReopenedProcessor.class, EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UPDATED, METHOD_CASE_REOPENED_IN_LIBRA_UPDATED),
                Arguments.of(CaseReopenedProcessor.class, EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UNDONE, METHOD_CASE_REOPENED_IN_LIBRA_UNDONE),
                Arguments.of(AllOffencesWithdrawalRequestedProcessor.class, AllOffencesWithdrawalRequested.EVENT_NAME, "handleAllOffencesWithdrawalEvent"),
                Arguments.of(CaseUpdateRejectedProcessor.class, CaseUpdateRejected.EVENT_NAME, "caseUpdateRejected")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldHandleCaseReopenedInLibraEventMessage(Class<Object> listener, String eventName, String methodName) throws Exception {
        final Method handleMethod = listener.getMethod(methodName, JsonEnvelope.class);
        final Handles handleMethodAnnotation = handleMethod.getDeclaredAnnotation(Handles.class);
        assertEquals(eventName, handleMethodAnnotation.value());
    }
}