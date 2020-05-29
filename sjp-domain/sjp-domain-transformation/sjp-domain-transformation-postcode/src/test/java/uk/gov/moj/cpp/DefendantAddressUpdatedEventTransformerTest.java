package uk.gov.moj.cpp;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.DefendantAddressUpdatedEventTransformer;

public class DefendantAddressUpdatedEventTransformerTest {

    private DefendantAddressUpdatedEventTransformer defendantAddressUpdatedEventTransformer = new DefendantAddressUpdatedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(defendantAddressUpdatedEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("defendant-address-updated.json",
                "defendant-address-updated-expected.json");
    }

}