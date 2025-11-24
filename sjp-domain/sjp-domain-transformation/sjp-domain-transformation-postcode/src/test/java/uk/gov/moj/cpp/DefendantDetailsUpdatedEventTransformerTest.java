package uk.gov.moj.cpp;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.sjp.DefendantDetailsUpdatedEventTransformer;

public class DefendantDetailsUpdatedEventTransformerTest {

    private DefendantDetailsUpdatedEventTransformer defendantDetailsUpdatedEventTransformer = new DefendantDetailsUpdatedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(defendantDetailsUpdatedEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("defendant-details-updated.json",
                "defendant-details-updated-expected.json");
    }
}

