package uk.gov.moj.cpp.sjp;

import org.junit.Test;

public class EmployerUpdatedEventTransformerTest {

    private EmployerUpdatedEventTransformer employerUpdatedEventTransformer = new EmployerUpdatedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(employerUpdatedEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("employer-updated.json",
                "employer-updated-expected.json");
    }
}

