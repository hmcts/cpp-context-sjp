package uk.gov.moj.cpp;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.sjp.OnlinePleaReceivedEventTransformer;

public class OnlinePleaReceivedEventTransformerTest  {

    private OnlinePleaReceivedEventTransformer onlinePleaReceivedEventTransformer = new OnlinePleaReceivedEventTransformer();
    private EventTransformerTestHelper eventTransformerTestHelper = new EventTransformerTestHelper(onlinePleaReceivedEventTransformer);

    @Test
    public void transformEventAndAssertPayload() {
        eventTransformerTestHelper.transformEventAndAssertPayload("online-plea-received.json",
                "online-plea-received-expected.json");
    }

    @Test
    public void transformEventAndAssertPayloadWithoutEmployer() {
        eventTransformerTestHelper.transformEventAndAssertPayload("online-plea-received-without-employer.json",
                "online-plea-received-without-employer-expected.json");
    }
}

