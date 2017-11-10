package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Test;

public class CaseCreationFailedBecauseCaseAlreadyExistedTest {

    private final CaseAggregate caseAggregate = new CaseAggregate();
    private final Case DEFAULT_CASE = CaseBuilder.aDefaultSjpCase().build();
    private final Clock CLOCK = new StoppedClock(ZonedDateTime.now());

    @Test
    public void throwsCaseCreationFailedBecauseCaseAlreadyExisted() {
        //given
        caseAggregate.createCase(DEFAULT_CASE, CLOCK.now());

        //when
        final Stream<Object> eventStream = caseAggregate.createCase(DEFAULT_CASE, CLOCK.now());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat("Throws CaseCreationFailedBecauseCaseAlreadyExisted", events,
                        hasItem(Matchers.allOf(
                                        isA(CaseCreationFailedBecauseCaseAlreadyExisted.class),
                                        hasProperty("caseId", is(DEFAULT_CASE.getId())),
                                        hasProperty("urn", is(DEFAULT_CASE.getUrn()))
                        ))
        );
    }
}
