package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.JudiciaryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferringJudicialDecisionView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.junit.Test;

public class SjpReferralViewHelperTest {

    private static final String LEGAL_ADVISER_FIRST_NAME = "Barack";
    private static final String LEGAL_ADVISER_LAST_NAME = "Jameson";
    private static final ZonedDateTime CASE_DECISION_CREATION_DATE = ZonedDateTime.now();
    private static final String COURT_HOUSE_NAME = "Court house name";
    private static final String COURT_HOUSE_CODE = "Court house code";
    private static final LocalDate CASE_POSTING_DATE = LocalDate.of(2018, 11, 20);
    private static final String MAGISTRATE_NAME = "magistrate name";
    private static final JsonObject LEGAL_ADVISER_DETAILS = createLegalAdviserDetail();
    private static final String JUDICIAL_NAME = format("%s %s", LEGAL_ADVISER_FIRST_NAME, LEGAL_ADVISER_LAST_NAME);
    private static final String LEGAL_ADVISER = "LEGAL_ADVISER";
    private static final JudiciaryView JUDICIARY_VIEW = createJudiciaryView();
    private static final CaseDetails CASE_DETAILS = createCaseDetails();


    private SjpReferralViewHelper sjpReferralViewHelper = new SjpReferralViewHelper();

    @Test
    public void shouldCreateSjpReferralViewWithMagistrateAndAdviserJudiciaries() {

        final JsonObject sessionDetails = createObjectBuilder()
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("courtHouseCode", COURT_HOUSE_CODE)
                .add("magistrate", MAGISTRATE_NAME)
                .build();

        final SjpReferralView sjpReferralView = sjpReferralViewHelper.createSjpReferralView(
                CASE_DETAILS, sessionDetails,
                LEGAL_ADVISER_DETAILS,
                CASE_DECISION_CREATION_DATE);

        assertThat(sjpReferralView.getNoticeDate(), is(CASE_POSTING_DATE));
        assertThat(sjpReferralView.getReferralDate(), is(CASE_DECISION_CREATION_DATE.toLocalDate()));
        assertThat(sjpReferralView.getReferringJudicialDecision(), is(
                new ReferringJudicialDecisionView(
                        COURT_HOUSE_NAME,
                        COURT_HOUSE_CODE,
                        newArrayList(
                                JUDICIARY_VIEW,
                        new JudiciaryView(MAGISTRATE_NAME, "MAGISTRATE")))));
    }

    @Test
    public void shouldCreateSjpReferralViewWithLegalAdviserJudiciaryOnlyWhenNoMagistrate() {

        final JsonObject sessionDetails = createObjectBuilder()
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("courtHouseCode", COURT_HOUSE_CODE)
                .build();

        final SjpReferralView sjpReferralView = sjpReferralViewHelper.createSjpReferralView(
                CASE_DETAILS,
                sessionDetails,
                LEGAL_ADVISER_DETAILS,
                CASE_DECISION_CREATION_DATE);

        assertThat(sjpReferralView.getNoticeDate(), is(CASE_POSTING_DATE));
        assertThat(sjpReferralView.getReferralDate(), is(CASE_DECISION_CREATION_DATE.toLocalDate()));
        assertThat(sjpReferralView.getReferringJudicialDecision(), is(
                new ReferringJudicialDecisionView(
                        COURT_HOUSE_NAME,
                        COURT_HOUSE_CODE,
                        singletonList(JUDICIARY_VIEW))));
    }

    private static JsonObject createLegalAdviserDetail() {
        return createObjectBuilder()
                .add("firstName", LEGAL_ADVISER_FIRST_NAME)
                .add("lastName", LEGAL_ADVISER_LAST_NAME)
                .build();
    }

    private static JudiciaryView createJudiciaryView() {
        return new JudiciaryView(JUDICIAL_NAME, LEGAL_ADVISER);
    }

    private static CaseDetails createCaseDetails() {
        return CaseDetails.caseDetails()
                .withPostingDate(CASE_POSTING_DATE)
                .build();
    }

    private static CaseDecision createCaseDecision() {
        return CaseDecision.caseDecision()
                .withSession(Session.session()
                        .withCourtHouseName(COURT_HOUSE_NAME)
                        .withCourtHouseCode(COURT_HOUSE_CODE)
                        .withMagistrate(MAGISTRATE_NAME)
                        .build())
                .build();
    }

}
