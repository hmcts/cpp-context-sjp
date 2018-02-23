package uk.gov.moj.cpp.sjp.event.processor;

import static org.junit.Assert.assertEquals;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.listener.CaseUpdatedListener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EventProcessorAnnotationTest {

    private static final String EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA = "sjp.events.case-reopened-in-libra";
    private static final String EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UPDATED = "sjp.events.case-reopened-in-libra-updated";
    private static final String EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UNDONE = "sjp.events.case-reopened-in-libra-undone";
    private static final String METHOD_CASE_REOPENED_IN_LIBRA = "handleCaseReopenedInLibra";
    private static final String METHOD_CASE_REOPENED_IN_LIBRA_UPDATED = "handleCaseReopenedInLibraUpdated";
    private static final String METHOD_CASE_REOPENED_IN_LIBRA_UNDONE = "handleCaseReopenedInLibraUndone";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {CaseUpdatedListener.class, EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA, METHOD_CASE_REOPENED_IN_LIBRA},
                {CaseUpdatedListener.class, EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UPDATED, METHOD_CASE_REOPENED_IN_LIBRA_UPDATED},
                {CaseUpdatedListener.class, EVENT_PRIVATE_CASE_REOPENED_IN_LIBRA_UNDONE, METHOD_CASE_REOPENED_IN_LIBRA_UNDONE},
                {AllOffencesWithdrawalRequestedProcessor.class, "sjp.events.all-offences-withdrawal-requested", "publishAllOffencesWithdrawalEvent"},
                {CaseUpdateRejectedProcessor.class, "sjp.events.case-update-rejected", "caseUpdateRejected"}
        });
    }

    private final Class<Object> listener;

    private final String eventName;

    private final String methodName;

    public EventProcessorAnnotationTest(Class<Object> listener, String eventName, String methodName) {
        this.listener = listener;
        this.eventName = eventName;
        this.methodName = methodName;
    }

    @Test
    public void shouldHandleCaseReopenedInLibraEventMessage() throws Exception {
        final Method handleMethod = listener.getMethod(methodName, JsonEnvelope.class);
        final Handles handleMethodAnnotation = handleMethod.getDeclaredAnnotation(Handles.class);
        assertEquals(eventName, handleMethodAnnotation.value());
    }
}