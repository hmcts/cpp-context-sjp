package uk.gov.moj.cpp.sjp;

import org.junit.Test;

public class DefendantAddressUpdatedEventTransformerTest {

    private DefendantAddressUpdatedEventTransformer defendantAddressUpdatedEventTransformer = new DefendantAddressUpdatedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(defendantAddressUpdatedEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("defendant-address-updated.json",
                "defendant-address-updated-expected.json");
    }

}