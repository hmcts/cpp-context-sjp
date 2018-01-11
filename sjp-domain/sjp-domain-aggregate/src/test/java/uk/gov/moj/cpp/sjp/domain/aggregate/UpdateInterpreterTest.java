package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class UpdateInterpreterTest {

    private static final String LANGUAGE = "French";

    private CaseAggregate caseAggregate;
    private UUID caseId;
    private UUID defendantId;

    @Before
    public void initialiseCase() {
        caseAggregate = new CaseAggregate();

        CaseReceived caseReceived = receiveCase();
        caseId = caseReceived.getCaseId();
        defendantId = caseReceived.getDefendant().getId();
    }

    @Test
    public void shouldCreateInterpreterUpdatedForDefendantEvent() {
        List<Object> events = caseAggregate.updateInterpreter(defendantId, LANGUAGE).collect(toList());

        assertThat(events, hasSize(1));

        InterpreterUpdatedForDefendant interpreterUpdated = (InterpreterUpdatedForDefendant) events.get(0);

        assertThat(interpreterUpdated.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(interpreterUpdated.getInterpreter().getNeeded(), is(true));
        assertThat(interpreterUpdated.getInterpreter().getLanguage(), equalTo(LANGUAGE));
    }

    @Test
    public void shouldNotCreateInterpreterUpdatedForDefendantEventIfInterpreterLanguageAlreadyExist() {
        caseAggregate.updateInterpreter(defendantId, LANGUAGE);

        assertThat(caseAggregate.updateInterpreter(defendantId, LANGUAGE).count(), is(0L));
    }

    @Test
    public void shouldCreateInterpreterCancelledForDefendantEvent() {
        caseAggregate.updateInterpreter(defendantId, LANGUAGE);

        List<Object> events = caseAggregate.updateInterpreter(defendantId, null).collect(toList());

        assertThat(events, hasSize(1));

        InterpreterCancelledForDefendant interpreterUpdated = (InterpreterCancelledForDefendant) events.get(0);

        assertThat(interpreterUpdated.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdated.getDefendantId(), equalTo(defendantId));
    }

    @Test
    public void shouldNotCreateInterpreterCancelledForDefendantEventIfInterpreterDoestNotExist() {
        long countedEvents = caseAggregate.updateInterpreter(defendantId, null).count();

        assertThat(countedEvents, is(0L));
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = randomUUID();
        List<Object> events = caseAggregate.updateInterpreter(defendantId, LANGUAGE).collect(toList());

        assertThat(events, hasSize(1));
        assertThat(reflectionEquals(
                events.get(0),
                new DefendantNotFound(defendantId.toString(), "Update interpreter")),
                is(true));
    }

    private CaseReceived receiveCase() {
        Case sjpCase = CaseBuilder.aDefaultSjpCase().build();

        return caseAggregate.receiveCase(sjpCase, ZonedDateTime.now())
                .filter(CaseReceived.class::isInstance)
                .map(CaseReceived.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(format("No event of type %s found.", CaseReceived.class.getSimpleName())));
    }
}
