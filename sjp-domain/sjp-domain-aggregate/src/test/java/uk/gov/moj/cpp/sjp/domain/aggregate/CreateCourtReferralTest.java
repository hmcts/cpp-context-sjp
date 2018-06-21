package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CourtReferralCreated;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class CreateCourtReferralTest {

    private CaseAggregate caseAggregate;

    private final Clock clock = new UtcClock();

    @Before
    public void setup() {
        this.caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateCourtReferral() {

        final Case caseDetail = CaseBuilder.aDefaultSjpCase().build();
        caseAggregate.receiveCase(caseDetail, clock.now());

        final LocalDate hearingDate = LocalDate.now().plusWeeks(1);

        final List<Object> events = caseAggregate.createCourtReferral(caseDetail.getId(), hearingDate).collect(Collectors.toList());

        assertThat(events.size(), is(1));

        final CourtReferralCreated event = (CourtReferralCreated) events.get(0);
        assertEquals(caseDetail.getId(), event.getCaseId());
        assertEquals(hearingDate, event.getHearingDate());
    }

    @Test
    public void shouldNotCreateCourtReferral() {

        final List<Object> events = caseAggregate.createCourtReferral(randomUUID(),
                LocalDate.now(UTC).plusWeeks(1)).collect(Collectors.toList());

        assertThat(events.size(), is(1));

        assertThat(events.get(0), instanceOf(CaseNotFound.class));
    }

}

