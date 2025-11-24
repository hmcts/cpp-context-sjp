package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;

import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class CaseCreationFailedBecauseCaseAlreadyExistedTest extends CaseAggregateBaseTest {

    @Test
    public void throwsCaseCreationFailedBecauseCaseAlreadyExisted() {
        //when
        final Stream<Object> eventStream = caseAggregate.receiveCase(aCase, clock.now());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat("Throws CaseCreationFailedBecauseCaseAlreadyExisted", events,
                        hasItem(Matchers.allOf(
                                isA(CaseCreationFailedBecauseCaseAlreadyExisted.class),
                                hasProperty("caseId", is(aCase.getId())),
                                hasProperty("urn", is(aCase.getUrn()))
                        ))
        );
    }
}
