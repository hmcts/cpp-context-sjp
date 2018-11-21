package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;

public class NotifiedPleaViewHelperTest {

    private NotifiedPleaViewHelper notifiedPleaViewHelper = new NotifiedPleaViewHelper();

    private static final UUID OFFENCE_ID = randomUUID();
    private static final ZonedDateTime PLEA_DATE = ZonedDateTime.now();

    @Test
    public void shouldCreateNotifiedPleaViewUsingPleaDetailsWhenPresent() {
        final String plea = "NOT_GUILTY";

        final NotifiedPleaView notifiedPleaView = notifiedPleaViewHelper.createNotifiedPleaView(
                createCaseDetails(),
                CaseReferredForCourtHearing.caseReferredForCourtHearing().build(),
                DefendantsOnlinePlea.defendantsOnlinePlea()
                        .withPleaDetails(PleaDetails.pleaDetails()
                                .withPlea(plea)
                                .build())
                        .build(),
                singletonList(Offence.offence()
                        .withId(OFFENCE_ID.toString())
                        .build()));

        assertThat(notifiedPleaView.getOffenceId(), is(OFFENCE_ID));
        assertThat(notifiedPleaView.getNotifiedPleaDate(), is(PLEA_DATE.toLocalDate()));
        assertThat(notifiedPleaView.getNotifiedPleaValue(), is("NOTIFIED_" + plea));
    }

    private CaseDetails createCaseDetails() {
        return CaseDetails.caseDetails()
                    .withDefendant(Defendant.defendant()
                            .withOffences(
                                    singletonList(
                                            Offence.offence()
                                                    .withPleaDate(PLEA_DATE)
                                                    .build()))
                            .build())
                    .build();
    }

    @Test
    public void shouldCreateNotifiedPleaViewUsingReferralDateWhenPleaNotPresent() {
        final ZonedDateTime referralDate = ZonedDateTime.now();

        final NotifiedPleaView notifiedPleaView = notifiedPleaViewHelper.createNotifiedPleaView(
                createCaseDetails(),
                CaseReferredForCourtHearing.caseReferredForCourtHearing()
                        .withReferredAt(referralDate)
                        .build(),
                null,
                singletonList(Offence.offence()
                        .withId(OFFENCE_ID.toString())
                        .build()));

        assertThat(notifiedPleaView.getOffenceId(), is(OFFENCE_ID));
        assertThat(notifiedPleaView.getNotifiedPleaDate(), is(referralDate.toLocalDate()));
        assertThat(notifiedPleaView.getNotifiedPleaValue(), is("NO_NOTIFICATION"));
    }
}
