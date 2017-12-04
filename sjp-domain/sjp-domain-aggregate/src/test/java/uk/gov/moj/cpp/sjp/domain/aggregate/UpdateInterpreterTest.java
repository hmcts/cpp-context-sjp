package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdateInterpreterTest {

    private CaseAggregate caseAggregate;

    @Before
    public void init() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateInterpreterUpdatedForDefendantEvent() {
        final SjpCaseCreated sjpCaseCreated = createSjpCase();
        final UUID caseId = UUID.fromString(sjpCaseCreated.getId());
        final UUID defendantId = sjpCaseCreated.getDefendantId();
        final String language = "French";

        final Stream<Object> eventStream = caseAggregate.updateInterpreter(defendantId, language);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final InterpreterUpdatedForDefendant interpreterUpdated = (InterpreterUpdatedForDefendant) events.get(0);

        assertThat(interpreterUpdated.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(interpreterUpdated.getInterpreter().getNeeded(), equalTo(true));
        assertThat(interpreterUpdated.getInterpreter().getLanguage(), equalTo(language));
    }

    @Test
    public void shouldNotCreateInterpreterUpdatedForDefendantEventIfInterpreterLanguageAlreadyExist() {
        final SjpCaseCreated sjpCaseCreated = createSjpCase();
        final UUID defendantId = sjpCaseCreated.getDefendantId();
        final String language = "French";
        caseAggregate.updateInterpreter(defendantId, language);

        final Stream<Object> eventStream = caseAggregate.updateInterpreter(defendantId, language);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events.isEmpty(), equalTo(true));
    }

    @Test
    public void shouldCreateInterpreterCancelledForDefendantEvent() {
        final SjpCaseCreated sjpCaseCreated = createSjpCase();
        final UUID caseId = UUID.fromString(sjpCaseCreated.getId());
        final UUID defendantId = sjpCaseCreated.getDefendantId();
        final String language = "French";
        caseAggregate.updateInterpreter(defendantId, language);

        final Stream<Object> eventStream = caseAggregate.updateInterpreter(defendantId, null);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final InterpreterCancelledForDefendant interpreterUpdated = (InterpreterCancelledForDefendant) events.get(0);

        assertThat(interpreterUpdated.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdated.getDefendantId(), equalTo(defendantId));
    }

    @Test
    public void shouldNotCreateInterpreterCancelledForDefendantEventIfInterpreterDoestNotExist() {
        final SjpCaseCreated sjpCaseCreated = createSjpCase();
        final UUID defendantId = sjpCaseCreated.getDefendantId();

        final Stream<Object> eventStream = caseAggregate.updateInterpreter(defendantId, null);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events.isEmpty(), equalTo(true));
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = randomUUID();
        final String language = "French";

        final Stream<Object> eventStream = caseAggregate.updateInterpreter(defendantId, language);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);

        assertThat(defendantNotFound.getDefendantId(), equalTo(defendantId.toString()));
        assertThat(defendantNotFound.getDescription(), equalTo("Update interpreter"));
    }

    private SjpCaseCreated createSjpCase() {
        final Case sjpCase = CaseBuilder.aDefaultSjpCase().build();
        final Stream<Object> caseCreatedEvents = caseAggregate.createCase(sjpCase, ZonedDateTime.now());
        return caseCreatedEvents.filter(SjpCaseCreated.class::isInstance).map(SjpCaseCreated.class::cast).findFirst().get();
    }
}
