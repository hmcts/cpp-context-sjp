package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.JudiciaryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferringJudicialDecisionView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import javax.json.JsonObject;

import org.junit.Test;

public class SjpReferralViewHelperTest {

    private static final String LEGAL_ADVISER_FIRST_NAME = "Barack";
    private static final String LEGAL_ADVISER_LAST_NAME = "Jameson";
    private static final ZonedDateTime CASE_DECISION_CREATION_DATE = ZonedDateTime.now();
    private static final String COURT_HOUSE_NAME = "Court house name";
    private static final String CASE_POSTING_DATE = "2018-11-20";
    private static final String MAGISTRATE_NAME = "magistrate name";

    private SjpReferralViewHelper sjpReferralViewHelper = new SjpReferralViewHelper();

    @Test
    public void shouldCreateSjpReferralViewWithMagistrateAndAdviserJudiciaries() {

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withPostingDate(CASE_POSTING_DATE)
                .build();
        final JsonObject sessionDetails = createObjectBuilder()
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("magistrate", MAGISTRATE_NAME)
                .build();

        final JsonObject legalAdviserDetails = createLegalAdviserDetail();

        final SjpReferralView sjpReferralView = sjpReferralViewHelper.createSjpReferralView(
                caseDetails,
                sessionDetails,
                legalAdviserDetails,
                CASE_DECISION_CREATION_DATE);

        assertReferralViewMatchesExpected(
                sjpReferralView,
                newArrayList(
                        new JudiciaryView(
                                format("%s %s", LEGAL_ADVISER_FIRST_NAME, LEGAL_ADVISER_LAST_NAME),
                                "LEGAL_ADVISER"),
                        new JudiciaryView(MAGISTRATE_NAME, "MAGISTRATE")));
    }

    private JsonObject createLegalAdviserDetail() {
        return createObjectBuilder()
                .add("firstName", LEGAL_ADVISER_FIRST_NAME)
                .add("lastName", LEGAL_ADVISER_LAST_NAME)
                .build();
    }

    @Test
    public void shouldCreateSjpReferralViewWithLegalAdviserJudiciaryOnlyWhenNoMagistrate() {

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withPostingDate("2018-11-20")
                .build();
        final JsonObject legalAdviserDetails = createLegalAdviserDetail();

        final JsonObject sessionDetails = createObjectBuilder()
                .add("courtHouseName", COURT_HOUSE_NAME)
                .build();

        final SjpReferralView sjpReferralView = sjpReferralViewHelper.createSjpReferralView(
                caseDetails,
                sessionDetails,
                legalAdviserDetails,
                CASE_DECISION_CREATION_DATE);

        assertReferralViewMatchesExpected(
                sjpReferralView,
                singletonList(new JudiciaryView(
                        format("%s %s", LEGAL_ADVISER_FIRST_NAME, LEGAL_ADVISER_LAST_NAME),
                        "LEGAL_ADVISER")));
    }

    private void assertReferralViewMatchesExpected(final SjpReferralView sjpReferralView, List<JudiciaryView> judiciaryViews) {
        assertThat(sjpReferralView.getNoticeDate(), is(LocalDate.parse(CASE_POSTING_DATE)));
        assertThat(sjpReferralView.getReferralDate(), is(CASE_DECISION_CREATION_DATE.toLocalDate()));
        assertThat(sjpReferralView.getReferringJudicialDecision(), is(
                new ReferringJudicialDecisionView(
                        COURT_HOUSE_NAME,
                        judiciaryViews)));
    }
}
