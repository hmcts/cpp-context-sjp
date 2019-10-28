package uk.gov.moj.cpp.sjp.domain.aggregate;

import static io.netty.util.internal.StringUtil.EMPTY_STRING;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

public abstract class CaseAggregateBaseTest {

    protected final Clock clock = new StoppedClock(new UtcClock().now());
    protected CaseAggregate caseAggregate;
    protected Case aCase;
    protected CaseReceived caseReceivedEvent;

    protected UUID caseId;
    protected UUID defendantId;
    protected UUID offenceId;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
        aCase = buildCaseReceived();
        caseReceivedEvent = collectSingleEvent(caseAggregate.receiveCase(buildCaseReceived(), clock.now()), CaseReceived.class);

        caseId = caseReceivedEvent.getCaseId();
        defendantId = caseReceivedEvent.getDefendant().getId();
        offenceId = caseReceivedEvent.getDefendant().getOffences().stream().findFirst().map(Offence::getId).orElse(null);
    }

    /**
     * Override to customise the received case
     */
    Case buildCaseReceived() {
        return CaseBuilder.aDefaultSjpCase().build();
    }

    final CaseReceived buildCaseReceived(final Case aCase) {
        return new CaseReceived(
                aCase.getId(),
                aCase.getUrn(),
                aCase.getEnterpriseId(),
                aCase.getProsecutingAuthority(),
                aCase.getCosts(),
                aCase.getPostingDate(),
                aCase.getDefendant(),
                aCase.getPostingDate().plusDays(NUMBER_DAYS_WAITING_FOR_PLEA),
                clock.now());
    }

    void resetAggregate() {
        caseAggregate = new CaseAggregate();
        caseId = defendantId = offenceId = null;
        aCase = null;
        caseReceivedEvent = null;
    }

    <T> T collectSingleEvent(final Stream<Object> events, final Class<T> eventType) {
        return collectSingleEvent(events.collect(toList()), eventType);
    }

    <T> T collectSingleEvent(final List<Object> events, final Class<T> eventType) {
        if (events.size() != 1) {
            fail("Collection has more than 1 item: " + events.stream().map(o -> o.getClass().getSimpleName()).collect(toList()));
        }

        final Object firstEvent = events.get(0);
        if (!eventType.isInstance(firstEvent)) {
            fail(format(
                    "Expected a single instance of %s, but found %s.",
                    eventType.getSimpleName(),
                    firstEvent.getClass().getSimpleName()));
        }

        return eventType.cast(firstEvent);
    }

    <T> T collectFirstEvent(final Stream<Object> events, final Class<T> eventType) {
        return collectFirstEvent(events.collect(toList()), eventType);
    }

    <T> T collectFirstEvent(final List<Object> events, final Class<T> eventType) {
        final Object firstEvent = events.get(0);
        if (!eventType.isInstance(firstEvent)) {
            fail(format(
                    "Expected a single instance of %s, but found %s.",
                    eventType.getSimpleName(),
                    firstEvent.getClass().getSimpleName()));
        }

        return eventType.cast(firstEvent);
    }

    /**
     * Fluid high coverage aggregate tester
     */
    static class AggregateTester {

        private final List<Object> events;
        private String description;

        private AggregateTester(final List<Object> events) {
            this.events = events;
        }

        /**
         * Includes the call to the aggregate
         */
        static AggregateTester when(final Stream<Object> aggregateAction) {
            return new AggregateTester(aggregateAction.collect(toList()));
        }

        /**
         * Explains what you are trying to test
         *
         * @param description shown in case of error
         */
        AggregateTester reason(final String description) {
            this.description = description;
            return this;
        }

        /**
         * List all the events after the aggregate execution
         */
        //TODO FIX-ME ATCM-4334
        void thenExpect(final Object... items) {

            if (!new ReflectionEquals(events.toArray()).matches(items)) {
                fail(buildErrorMessage());
            }
        }

        private String buildErrorMessage() {
            return ofNullable(description).orElse(EMPTY_STRING) + lineSeparator() +
                    "Emitted Events: " + Optional.of(events)
                    .map(e -> e.stream().map(o -> o.getClass().getSimpleName()).collect(toList()))
                    .orElse(null);
        }

    }

}