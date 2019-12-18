package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetDatesToAvoidRequiredAggregateHandler {

    public static final SetDatesToAvoidRequiredAggregateHandler INSTANCE = new SetDatesToAvoidRequiredAggregateHandler();

    private SetDatesToAvoidRequiredAggregateHandler() {
    }

    public Stream<Object> handleSetDatesToAvoidRequired(CaseAggregateState caseAggregateState) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        raiseDatesToAvoidRequiredIfNecessary(caseAggregateState, streamBuilder);
        return streamBuilder.build();
    }

    private void raiseDatesToAvoidRequiredIfNecessary(final CaseAggregateState caseAggregateState,
                                                      final Stream.Builder<Object> streamBuilder) {
        final UUID offenceId = caseAggregateState.getOffences().iterator().next();
        if (datesToAvoidNotSetAndDatesToAvoidTimerNotStarted(caseAggregateState)
                && notGuiltyProvided(caseAggregateState)) {
            final LocalDate expectedDateReady = caseAggregateState.getOffencePleaDates().get(offenceId).plusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID);
            streamBuilder.add(new DatesToAvoidRequired(caseAggregateState.getCaseId(), expectedDateReady));
        }
    }

    private boolean notGuiltyProvided(final CaseAggregateState caseAggregateState) {
        return nonNull(caseAggregateState.getPleas())
                && caseAggregateState.getPleas().stream().anyMatch(e -> NOT_GUILTY.equals(e.getPleaType()));
    }

    private static boolean datesToAvoidNotSetAndDatesToAvoidTimerNotStarted(final CaseAggregateState state) {
        return !state.isDatesToAvoidTimerPreviouslyStarted() && isEmpty(state.getDatesToAvoid());
    }

}
