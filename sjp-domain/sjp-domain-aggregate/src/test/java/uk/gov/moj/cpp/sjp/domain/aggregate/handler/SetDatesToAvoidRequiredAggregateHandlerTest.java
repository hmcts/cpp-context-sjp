package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.SetDatesToAvoidRequiredAggregateHandler.INSTANCE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class SetDatesToAvoidRequiredAggregateHandlerTest {

    private final CaseAggregateState caseAggregateState = new CaseAggregateState();

    private UUID caseId;

    private LocalDate pleaDate;

    @Before
    public void setUp() {
        // given
        final UUID offenceId = UUID.randomUUID();
        final Set<UUID> offenceIds = new HashSet<>();
        offenceIds.add(offenceId);
        final UUID defendantId = UUID.randomUUID();

        caseId = UUID.randomUUID();
        pleaDate = LocalDate.now();
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.setCaseId(caseId);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, offenceIds);
        caseAggregateState.getOffences().add(offenceId);
        caseAggregateState.putOffencePleaDate(offenceId, pleaDate);
        caseAggregateState.setPleas(asList(new Plea(defendantId, offenceId, NOT_GUILTY)));
    }

    @Test
    public void shouldCreateDateToAvoidRequiredEventWhenThePleaIsNotGuiltyAndDatesToAvoidIsNotSet() {
        // when
        final Stream<Object> eventStream = INSTANCE.handleSetDatesToAvoidRequired(caseAggregateState);

        // then
        assertThat(eventStream.collect(toList()),
                containsInAnyOrder(new DatesToAvoidRequired(caseId, pleaDate.plusDays(10))));

    }

    @Test
    public void shouldCreateDateToAvoidRequiredEventWhenThePleaIsNotGuiltyAndDatesToAvoidIsSet() {
        caseAggregateState.setDatesToAvoidPreviouslyRequested();
        // when
        final Stream<Object> eventStream = INSTANCE.handleSetDatesToAvoidRequired(caseAggregateState);

        // then
        assertThat(eventStream.count(), is(0L));
    }

}