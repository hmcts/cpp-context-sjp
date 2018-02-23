package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.SerializationUtils.serialize;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.domain.command.CompleteCase;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

public class CompleteCaseTest extends CaseAggregateBaseTest {

    @Test
    public void shouldMarkCaseAsCompleted() {
        CompleteCase caseCompleted = new CompleteCase(aCase.getId().toString());

        Stream<Object> eventStream = caseAggregate.completeCase(caseCompleted);
        List<Object> events = asList(eventStream.toArray());

        assertThat("Has CaseCompleted event", events, hasItem(isA(CaseCompleted.class)));
        assertThat("Case marked as completed", caseAggregate.isCaseCompleted(), is(true));
    }

    @Test
    public void shouldThrowExceptionIfCaseIsAlreadyCompleted() {
        CompleteCase caseCompleted = new CompleteCase(aCase.getId().toString());

        caseAggregate.completeCase(caseCompleted);

        try {
            Stream<Object> eventStream = caseAggregate.completeCase(caseCompleted);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("CaseAggregate has already been completed " + caseCompleted.getCaseId()));
        }
    }

    @Test
    public void shouldSerilazeAggregateObjectGraph(){
        byte[] result = serialize(caseAggregate);
        assertTrue(result != null);
    }
}
