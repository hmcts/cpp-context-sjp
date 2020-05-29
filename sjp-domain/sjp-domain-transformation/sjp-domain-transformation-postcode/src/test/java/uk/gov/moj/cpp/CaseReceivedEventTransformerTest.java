package uk.gov.moj.cpp;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.CaseReceivedEventTransformer;

public class CaseReceivedEventTransformerTest {

    private CaseReceivedEventTransformer caseReceivedEventTransformer = new CaseReceivedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(caseReceivedEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("case-received.json",
                "case-received-expected.json");
    }

    @Test
    public void containsPostcodeToTransform() {
        eventTransformerTestHelper.transformEventAndAssertPayload("case-received.json",
                "case-received-expected.json");
    }
}

