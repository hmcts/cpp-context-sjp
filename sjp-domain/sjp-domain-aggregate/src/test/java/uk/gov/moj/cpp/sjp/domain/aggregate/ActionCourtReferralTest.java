package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;

import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.CourtReferralNotFound;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.event.CourtReferralActioned;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class ActionCourtReferralTest {

    private CaseAggregate caseAggregate;

    @Before
    public void setup() {
        this.caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldActionCourtReferral() {

        final Case caseDetail = CaseBuilder.aDefaultSjpCase().build();
        caseAggregate.createCase(caseDetail, ZonedDateTime.now());
        caseAggregate.createCourtReferral(caseDetail.getId().toString(), LocalDate.now().plusWeeks(1));

        final List<Object> events = caseAggregate.actionCourtReferral(caseDetail.getId().toString()).collect(Collectors.toList());

        assertThat(events.size(), is(1));

        final CourtReferralActioned event = (CourtReferralActioned) events.get(0);
        assertEquals(caseDetail.getId(), event.getCaseId());
        // Within one second of now()
        assertThat(Duration.between(event.getActioned(), ZonedDateTime.now(UTC)).getSeconds(), lessThan(1L));
    }

    @Test
    public void shouldNotActionCourtReferral() {

        final List<Object> events = caseAggregate.actionCourtReferral(randomUUID().toString()).collect(Collectors.toList());

        assertThat(events.size(), is(1));

        assertThat(events.get(0), instanceOf(CourtReferralNotFound.class));
    }

}

