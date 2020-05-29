package uk.gov.moj.cpp;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.DefendantDetailsMovedFromPeopleEventTransformer;

public class DefendantDetailsMovedFromPeopleEventTransformerTest {

    private DefendantDetailsMovedFromPeopleEventTransformer defendantDetailsMovedFromPeopleEventTransformer = new DefendantDetailsMovedFromPeopleEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(defendantDetailsMovedFromPeopleEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("defendant-details-moved-from-people.json",
                "defendant-details-moved-from-people-expected.json");
    }
}



