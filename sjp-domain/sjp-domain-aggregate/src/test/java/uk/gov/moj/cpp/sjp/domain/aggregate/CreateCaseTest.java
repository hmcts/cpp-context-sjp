package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.COSTS;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.DATE_OF_HEARING;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.INITIATION_CODE;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.LIBRA_HEARING_LOCATION;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.LIBRA_ORIGINATING_ORG;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.NUM_PREVIOUS_CONVICTIONS;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.OFFENCES;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.PERSONID;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.POSTING_DATE;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.PTI_URN;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.SUMMONS_CODE;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.TIME_OF_HEARING;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.URN;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseStarted;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class CreateCaseTest {

    private CaseAggregate caseAggregate;
    private Case aCase;

    private Clock clock = new StoppedClock(ZonedDateTime.now());

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void testApply_whenSjpCaseCreatedEvent() throws Exception {
        SjpCaseCreated sjpCaseCreated = new SjpCaseCreated(
                DefaultTestData.CASE_ID.toString(),
                URN,
                PTI_URN,
                INITIATION_CODE,
                SUMMONS_CODE,
                ProsecutingAuthority.TFL,
                LIBRA_ORIGINATING_ORG,
                LIBRA_HEARING_LOCATION,
                DATE_OF_HEARING,
                TIME_OF_HEARING,
                PERSONID,
                UUID.randomUUID(),
                NUM_PREVIOUS_CONVICTIONS,
                COSTS,
                POSTING_DATE,
                OFFENCES,
                clock.now());

        caseAggregate.apply(sjpCaseCreated);

        assertThat("Sets case id", caseAggregate.getCaseId(), equalTo(DefaultTestData.CASE_ID));
        assertThat("Sets urn", caseAggregate.getUrn(), equalTo(URN));
    }

    @Test
    public void testCreateCase_whenValidSjpCase_shouldTriggerExpectedCaseCreatedAndStartedEvents() throws Exception {
        aCase = CaseBuilder.aDefaultSjpCase().build();
        CaseStarted expectedCaseStarted = new CaseStarted(DefaultTestData.CASE_ID);
        caseAggregate = new CaseAggregate();

        SjpCaseCreated expectedSjpCaseCreated = new SjpCaseCreated(DefaultTestData.CASE_ID.toString(),
                URN,
                PTI_URN,
                INITIATION_CODE,
                SUMMONS_CODE,
                ProsecutingAuthority.TFL,
                LIBRA_ORIGINATING_ORG,
                LIBRA_HEARING_LOCATION,
                DATE_OF_HEARING,
                TIME_OF_HEARING,
                PERSONID,
                UUID.randomUUID(),
                NUM_PREVIOUS_CONVICTIONS,
                COSTS,
                POSTING_DATE,
                OFFENCES,
                clock.now());

        Stream<Object> eventsStream = caseAggregate.createCase(aCase, clock.now());

        List<Object> events = asList(eventsStream.toArray());
        assertThat("Must trigger 1 event", events, hasSize(1));
        assertThat(events, hasItem(isA(SjpCaseCreated.class)));
        assertThat("Must trigger a SjpCaseCreated event", events, hasItem(isA(SjpCaseCreated.class)));
        assertThat("Must trigger the right SjpCaseCreated event", events, hasItem(equalTo(expectedSjpCaseCreated)));

        assertThat(caseAggregate.getCaseId(), is(not(nullValue())));

        assertThat(caseAggregate.getOffenceIdsByDefendantId().size(), is(1));
        assertThat(caseAggregate.getOffenceIdsByDefendantId().values().stream().count(), is(1L));
    }
}
