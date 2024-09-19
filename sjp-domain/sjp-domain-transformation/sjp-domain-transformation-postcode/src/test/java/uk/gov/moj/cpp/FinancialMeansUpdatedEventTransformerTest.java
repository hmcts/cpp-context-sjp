package uk.gov.moj.cpp;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.sjp.CaseReceivedEventTransformer;
import uk.gov.moj.cpp.sjp.FinancialMeansUpdatedEventTransformer;

public class FinancialMeansUpdatedEventTransformerTest {

    private FinancialMeansUpdatedEventTransformer financialMeansUpdatedEventTransformer = new FinancialMeansUpdatedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(financialMeansUpdatedEventTransformer);
    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("all-financial-means-updated.json",
                "all-financial-means-updated-expected.json");
    }

}

