package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.json.schemas.domains.sjp.PleaType.GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.justice.json.schemas.domains.sjp.PleaType.NOT_GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Offence.offence;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class NotifiedPleaViewHelperTest {

    private static ZonedDateTime REFERRAL_DATE = ZonedDateTime.now();
    private static ZonedDateTime YESTERDAY = ZonedDateTime.now().minusDays(1);
    private static final UUID OFFENCE_ID = randomUUID();

    private NotifiedPleaViewHelper notifiedPleaViewHelper = new NotifiedPleaViewHelper();

    @Parameterized.Parameter(0)
    public PleaType plea;

    @Parameterized.Parameter(1)
    public ZonedDateTime pleaDate;

    @Parameterized.Parameter(2)
    public String notifiedPlea;

    @Parameterized.Parameter(3)
    public LocalDate notifiedPleaDate;

    @Parameterized.Parameters(name = "plea={0}, plea date={1}, expected notified plea={2}, expected notified plea date={3}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {GUILTY, YESTERDAY, "NOTIFIED_GUILTY", YESTERDAY.toLocalDate()},
                {GUILTY_REQUEST_HEARING, YESTERDAY, "NOTIFIED_GUILTY", YESTERDAY.toLocalDate()},
                {NOT_GUILTY, YESTERDAY, "NOTIFIED_NOT_GUILTY", YESTERDAY.toLocalDate()},
                {null, null, "NO_NOTIFICATION", REFERRAL_DATE.toLocalDate()},
        });
    }

    @Test
    public void shouldCreateNotifiedPleaView() {
        final NotifiedPleaView notifiedPleaView = notifiedPleaViewHelper.createNotifiedPleaView(
                caseReferredForCourtHearing().withReferredAt(REFERRAL_DATE).build(),
                singletonList(offence()
                        .withId(OFFENCE_ID)
                        .withPlea(plea)
                        .withPleaDate(pleaDate)
                        .build()));

        assertThat(notifiedPleaView.getOffenceId(), is(OFFENCE_ID));
        assertThat(notifiedPleaView.getNotifiedPleaDate(), is(notifiedPleaDate));
        assertThat(notifiedPleaView.getNotifiedPleaValue(), is(notifiedPlea));
    }

}
