package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import java.util.Arrays;
import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Installments;
import uk.gov.moj.cpp.sjp.persistence.entity.LumpSum;
import uk.gov.moj.cpp.sjp.persistence.entity.NoSeparatePenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.Payment;
import uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms;
import uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.helper.PleaInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.NO_SEPARATE_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod.WEEKLY;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.ATTACH_TO_EARNINGS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits.COMPENSATION_ORDERED;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.ONLINE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;
import static uk.gov.moj.cpp.sjp.query.view.helper.PleaInfo.plea;

public class CaseCourtExtractViewTest {

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private final UUID caseId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final UUID applicationId = randomUUID();
    private static final String TFL = "TFL";
    private static final String DVL = "DVL";

    private final ZonedDateTime savedAt = now();

    @Test
    public void shouldVerifyCaseDetail() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, null, true, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        assertThat(courtExtractView.getGenerationDate(), notNullValue());
        assertThat(courtExtractView.getGenerationTime(), notNullValue());
        assertThat(courtExtractView.getCaseDetails().getReference(), equalTo(aCase.getUrn()));
        assertThat(courtExtractView.getDefendant().getFirstName(), equalTo("Theresa"));
        assertThat(courtExtractView.getDefendant().getLastName(), equalTo("May"));
    }

    @Test
    public void shouldVerifyReverseOrderOfSavedDate() {
        final ZonedDateTime adjournedOffenceDecisionSavedAt = now();
        final ZonedDateTime withdrawnOffenceDecisionSavedAt = adjournedOffenceDecisionSavedAt.plusDays(1);

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(buildOffenceDetailEntity(1, offence1Id)));

        final CaseDecision adjournedCaseDecision = buildCaseDecisionEntity(caseId, false, adjournedOffenceDecisionSavedAt);
        adjournedCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision.getId(), offence1Id, ADJOURN, null, null)));
        final CaseDecision withdrawnCaseDecision = buildCaseDecisionEntity(caseId, false, withdrawnOffenceDecisionSavedAt);
        withdrawnCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(withdrawnCaseDecision.getId(), offence1Id, WITHDRAW, null, null)));

        final CaseDetail aCase = aCase()
                .withCaseId(caseId)
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(adjournedCaseDecision)
                .withCaseDecision(withdrawnCaseDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView withdrawOffenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        final DecisionView adjournOffenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(1);

        final String actualAdjourn = adjournOffenceDecisionView.getLines()
                .stream()
                .filter(offenceDecisionLineView -> "Decision made".equals(offenceDecisionLineView.getLabel()))
                .findFirst()
                .get().getValue();
        assertThat(actualAdjourn, equalTo(DATE_FORMAT.format(adjournedOffenceDecisionSavedAt)));

        String actualWithdraw = withdrawOffenceDecisionView.getLines()
                .stream()
                .filter(offenceDecisionLineView -> "Decision made".equals(offenceDecisionLineView.getLabel()))
                .findFirst()
                .get().getValue();
        assertThat(actualWithdraw, equalTo(withdrawnOffenceDecisionSavedAt.toLocalDate().format(DATE_FORMAT)));
    }

    @Test
    public void shouldVerifyDismissCaseWithMagistrateSession() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, null, true, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyDismissWithPressRestrictionCaseWithMagistrateSession() {
        final PressRestriction pressRestriction = PressRestriction.requested("child1");
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, null, true, pressRestriction);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Reporting restriction", "Direction made under Section 45 of the Youth Justice and Criminal Evidence Act 1999  in respect of " + pressRestriction.getName())));
    }

    @Test
    public void shouldVerifyDismissWithNoPressRestrictionCaseWithMagistrateSession() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, null, true, PressRestriction.revoked());

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Reporting restriction", "Direction restricting publicity revoked")));
    }


    @Test
    public void shouldVerifyWithdrawCaseWithDelegatedPowers() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, WITHDRAW, savedAt, null, false, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyAdjournCaseWithMagistrateSession() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, ADJOURN, savedAt, null, true, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) aCase.getCaseDecisions().get(0).getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyReferForHearingCaseWithDelegatedPowers() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, REFER_FOR_COURT_HEARING, savedAt, null, false, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Referred for court hearing."),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyReopenedInLibraCaseWithDelegatedPowers() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, WITHDRAW, savedAt, null, false, null);
        aCase.setCaseStatus(CaseStatus.REOPENED_IN_LIBRA);
        aCase.setLibraCaseNumber("12345");
        aCase.setReopenedDate(now().toLocalDate());

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision (set aside)"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Decision set aside",
                        format("Case reopened %s%nLibra account no. %s", DATE_FORMAT.format(aCase.getReopenedDate()), aCase.getLibraCaseNumber()))));
    }

    @Test
    public void shouldVerifySingleCaseDecisionWithMultipleOffenceDecisionsWithOrdering() {
        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(
                buildOffenceDetailEntity(1, offence1Id),
                buildOffenceDetailEntity(2, offence2Id)));

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseId, false, savedAt);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null, null)));

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(caseDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(2));

        final DecisionDetailsView dismissOffenceDetailsView = offenceDetailsViews.get(0);
        final DecisionDetailsView withdrawOffenceDetailsView = offenceDetailsViews.get(1);
        final List<DecisionView> dismissOffenceDecisionViews = dismissOffenceDetailsView.getOffenceDecisions();
        final List<DecisionView> withdrawOffenceDecisionViews = withdrawOffenceDetailsView.getOffenceDecisions();
        final DecisionView dismissOffenceDecisionView = dismissOffenceDecisionViews.get(0);
        final DecisionView withdrawOffenceDecisionView = withdrawOffenceDecisionViews.get(0);

        assertThat(dismissOffenceDetailsView.getSequenceNumber(), equalTo(1));
        assertThat(withdrawOffenceDetailsView.getSequenceNumber(), equalTo(2));

        assertThat(dismissOffenceDecisionViews, hasSize(1));
        assertThat(withdrawOffenceDecisionViews, hasSize(1));

        assertThat(dismissOffenceDetailsView.getWording(), equalTo("offence wording"));
        assertThat(withdrawOffenceDetailsView.getWording(), equalTo("offence wording"));
        assertThat(dismissOffenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(dismissOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));

        assertThat(withdrawOffenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(withdrawOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyMultipleCaseDecisionsWithMultipleOffenceDecisions() {
        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(
                buildOffenceDetailEntity(1, offence1Id),
                buildOffenceDetailEntity(2, offence2Id)));

        final CaseDecision firstDecision = buildCaseDecisionEntity(caseId, false, savedAt);
        firstDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(firstDecision.getId(), offence1Id, ADJOURN, null, null),
                buildOffenceDecisionEntity(firstDecision.getId(), offence2Id, DISMISS, null, null)));

        final CaseDecision secondDecision = buildCaseDecisionEntity(caseId, false, savedAt);
        secondDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(secondDecision.getId(), offence1Id, DISMISS, null, null)));


        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(firstDecision)
                .withCaseDecision(secondDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(2));

        final DecisionDetailsView offenceDetailsView1 = offenceDetailsViews.get(0);
        final DecisionDetailsView offenceDetailsView2 = offenceDetailsViews.get(1);

        assertThat(offenceDetailsView1.getSequenceNumber(), equalTo(1));
        assertThat(offenceDetailsView2.getSequenceNumber(), equalTo(2));

        assertThat(offenceDetailsView1.getWording(), equalTo("offence wording"));
        assertThat(offenceDetailsView2.getWording(), equalTo("offence wording"));

        final List<DecisionView> dismissAndAdjournOffenceDecisionViews = offenceDetailsView1.getOffenceDecisions();
        final List<DecisionView> dismissOffenceDecisionViews = offenceDetailsView2.getOffenceDecisions();

        assertThat(dismissAndAdjournOffenceDecisionViews, hasSize(2));
        assertThat(dismissOffenceDecisionViews, hasSize(1));

        final DecisionView dismissDecisionView = dismissAndAdjournOffenceDecisionViews.get(0);
        final DecisionView adjournOffenceDecisionView = dismissAndAdjournOffenceDecisionViews.get(1);
        final DecisionView dismissDecisionView2 = dismissOffenceDecisionViews.get(0);

        assertThat(dismissDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(adjournOffenceDecisionView.getHeading(), equalTo("Previous court decision"));
        assertThat(dismissDecisionView2.getHeading(), equalTo("Court decision"));
        assertThat(dismissDecisionView2.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(firstDecision.getSavedAt())),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(firstDecision.getSavedAt()))));
    }

    @Test
    public void shouldVerifyMultipleCaseDecisionsWithSingleOffenceDecisionWithOrdering() {

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(buildOffenceDetailEntity(1, offence1Id)));

        final CaseDecision adjournedCaseDecision = buildCaseDecisionEntity(caseId, false, now());
        adjournedCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision.getId(), offence1Id, ADJOURN, null, null)));

        final CaseDecision withdrawnCaseDecision = buildCaseDecisionEntity(caseId, false, now().plusDays(1));
        withdrawnCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(withdrawnCaseDecision.getId(), offence1Id, WITHDRAW, null, null)));

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(adjournedCaseDecision)
                .withCaseDecision(withdrawnCaseDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(1));

        final DecisionDetailsView offenceDetailsView = offenceDetailsViews.get(0);
        final List<DecisionView> offenceDecisionViews = offenceDetailsView.getOffenceDecisions();
        final DecisionView withdrawOffenceDecisionView = offenceDecisionViews.get(0);
        final DecisionView adjournOffenceDecisionView = offenceDecisionViews.get(1);

        assertThat(offenceDetailsView.getSequenceNumber(), equalTo(1));
        assertThat(offenceDetailsView.getWording(), equalTo("offence wording"));

        assertThat(offenceDecisionViews, hasSize(2));

        assertThat(adjournOffenceDecisionView.getHeading(), equalTo("Previous court decision"));
        assertThat(adjournOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) adjournedCaseDecision.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(adjournedCaseDecision.getSavedAt()))));

        assertThat(withdrawOffenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(withdrawOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(withdrawnCaseDecision.getSavedAt()))));
    }

    @Test
    public void shouldVerifyMultipleAdjournDecisionsWithSingleOffenceDecisionWithOrdering() {
        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(buildOffenceDetailEntity(1, offence1Id)));

        final CaseDecision adjournedCaseDecision1 = buildCaseDecisionEntity(caseId, false, now());
        adjournedCaseDecision1.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision1.getId(), offence1Id, ADJOURN, null, null)));

        final CaseDecision adjournedCaseDecision2 = buildCaseDecisionEntity(caseId, false, now().plusDays(1));
        adjournedCaseDecision2.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision2.getId(), offence1Id, ADJOURN, null, null)));

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(adjournedCaseDecision1)
                .withCaseDecision(adjournedCaseDecision2)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(1));

        final DecisionDetailsView offenceDetailsView = offenceDetailsViews.get(0);

        assertThat(offenceDetailsView.getSequenceNumber(), equalTo(1));
        assertThat(offenceDetailsView.getWording(), equalTo("offence wording"));

        final List<DecisionView> offenceDecisionViews = offenceDetailsView.getOffenceDecisions();

        assertThat(offenceDecisionViews, hasSize(2));

        final DecisionView adjournOffenceDecisionView1 = offenceDecisionViews.get(0);
        final DecisionView adjournOffenceDecisionView2 = offenceDecisionViews.get(1);

        assertThat(adjournOffenceDecisionView2.getHeading(), equalTo("Previous court decision"));
        assertThat(adjournOffenceDecisionView2.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) adjournedCaseDecision1.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(adjournedCaseDecision1.getSavedAt()))));

        assertThat(adjournOffenceDecisionView1.getHeading(), equalTo("Court decision"));
        assertThat(adjournOffenceDecisionView1.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) adjournedCaseDecision2.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(adjournedCaseDecision2.getSavedAt()))));
    }

    @Test
    public void shouldNotContainLinesIfFineCompensationOrGuiltyPleaTakenAccountIsNull() {
        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(
                buildOffenceDetailEntity(1, offence1Id)));

        final CaseDecision firstDecision = buildCaseDecisionEntity(caseId, false, savedAt);
        firstDecision.setOffenceDecisions(Collections.singletonList(
                buildFinancialPenaltyOffenceDecisionEntity(offence1Id, null, null, null, "No compensation reason", null)));

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(firstDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);
        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();
        final DecisionDetailsView offenceDetailsView = offenceDetailsViews.get(0);
        final List<DecisionView> offenceDecisionViews = offenceDetailsView.getOffenceDecisions();
        final List<OffenceDecisionLineView> lines = offenceDecisionViews.get(0).getLines();
        assertTrue(lines
                .stream()
                .map(OffenceDecisionLineView::getLabel)
                .noneMatch(label -> ("To pay compensation of".equalsIgnoreCase(label)) ||
                        "To pay a fine of".equalsIgnoreCase(label) ||
                        "Defendant's guilty plea".equalsIgnoreCase(label)));

    }

    @Test
    public void shouldVerifyCaseWithEndorsmentCaseWithFINANCIAL_PENALTY() {
        final ZonedDateTime pleaDate = now().minusDays(2);
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, FINANCIAL_PENALTY, savedAt, plea(GUILTY, pleaDate), true, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("To pay a fine of", "£30"),
                new OffenceDecisionLineView("To pay compensation of", "£20"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Driver record endorsed", ""),
                new OffenceDecisionLineView("Penalty points", "3"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyCaseWithPressRestrictionCaseWithFINANCIAL_PENALTY() {
        final PressRestriction pressRestriction = PressRestriction.requested("child1");
        final ZonedDateTime pleaDate = now().minusDays(2);
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, FINANCIAL_PENALTY, savedAt, plea(GUILTY, pleaDate), true, pressRestriction);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("To pay a fine of", "£30"),
                new OffenceDecisionLineView("To pay compensation of", "£20"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Driver record endorsed", ""),
                new OffenceDecisionLineView("Penalty points", "3"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Reporting restriction", "Direction made under Section 45 of the Youth Justice and Criminal Evidence Act 1999  in respect of " + pressRestriction.getName())));

    }

    @Test
    public void shouldVerifyCaseWithNoPressRestrictionCaseWithFINANCIAL_PENALTY() {
        final PressRestriction pressRestriction = PressRestriction.revoked();
        final ZonedDateTime pleaDate = now().minusDays(2);
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, FINANCIAL_PENALTY, savedAt, plea(GUILTY, pleaDate), true, pressRestriction);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("To pay a fine of", "£30"),
                new OffenceDecisionLineView("To pay compensation of", "£20"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Driver record endorsed", ""),
                new OffenceDecisionLineView("Penalty points", "3"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Reporting restriction", "Direction restricting publicity revoked")));

    }

    @Test
    public void shouldVerifyCaseWithEndorsementCaseWithDischarge() {
        final ZonedDateTime pleaDate = now().minusDays(2);
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISCHARGE, savedAt, plea(GUILTY, pleaDate), true, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Discharged conditionally"),
                new OffenceDecisionLineView("Period", "12 days"),
                new OffenceDecisionLineView("Driver record endorsed", "Section 34(2) Road Traffic Offenders Act 1988"),
                new OffenceDecisionLineView("Disqualified for", "3 days"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyCaseWithPressRestrictionCaseWithDischarge() {
        final ZonedDateTime pleaDate = now().minusDays(2);
        final PressRestriction pressRestriction = PressRestriction.requested("child1");
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISCHARGE, savedAt, plea(GUILTY, pleaDate), true, pressRestriction);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Discharged conditionally"),
                new OffenceDecisionLineView("Period", "12 days"),
                new OffenceDecisionLineView("Driver record endorsed", "Section 34(2) Road Traffic Offenders Act 1988"),
                new OffenceDecisionLineView("Disqualified for", "3 days"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Reporting restriction", "Direction made under Section 45 of the Youth Justice and Criminal Evidence Act 1999  in respect of " + pressRestriction.getName())));
    }

    @Test
    public void shouldVerifyCaseWithEndorsmentCaseWithNoSeperatePenalty() {
        final ZonedDateTime pleaDate = now().minusDays(2);
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, NO_SEPARATE_PENALTY, savedAt, plea(GUILTY, pleaDate), true, null);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final DecisionView offenceDecisionView = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions().get(0).getOffenceDecisions().get(0);
        assertThat(offenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "No seperate penalty"),
                new OffenceDecisionLineView("Driver record endorsed", ""),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifySingleCaseDecisionWithMultipleOffenceDecisionsWithApplication() {
        final ZonedDateTime savedABeforeApplication = now().minusDays(1);

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(
                buildOffenceDetailEntity(1, offence1Id),
                buildOffenceDetailEntity(2, offence2Id)));

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseId, false, savedABeforeApplication);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null, null)));

        final CaseApplicationDecision caseApplicationDecision = buildCaseApplicationDecisionEntity(randomUUID(), true, true, "out of time reason", now());

        final CaseApplication caseApplication = buildCaseApplication(caseApplicationDecision, applicationId, savedAt.toLocalDate(), "Ref123", ApplicationType.STAT_DEC);
        caseApplicationDecision.setCaseApplication(caseApplication);

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(caseDecision)
                .withCaseApplication(caseApplication)
                .build();


        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(1).getOffencesApplicationsDecisions();
        final List<DecisionDetailsView> offenceDetailsAppViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(2));

        final DecisionDetailsView statDecApplicationDetailsView = offenceDetailsAppViews.get(0);
        final DecisionDetailsView dismissOffenceDetailsView = offenceDetailsViews.get(0);
        final DecisionDetailsView withdrawOffenceDetailsView = offenceDetailsViews.get(1);
        final DecisionView StatDecDecisionViews = statDecApplicationDetailsView.getApplicationDecision();
        final List<DecisionView> dismissOffenceDecisionViews = dismissOffenceDetailsView.getOffenceDecisions();
        final List<DecisionView> withdrawOffenceDecisionViews = withdrawOffenceDetailsView.getOffenceDecisions();
        final DecisionView dismissOffenceDecisionView = dismissOffenceDecisionViews.get(0);
        final DecisionView withdrawOffenceDecisionView = withdrawOffenceDecisionViews.get(0);

        assertThat(dismissOffenceDetailsView.getSequenceNumber(), equalTo(1));
        assertThat(withdrawOffenceDetailsView.getSequenceNumber(), equalTo(2));
        assertThat(statDecApplicationDetailsView.getSequenceNumber(), equalTo(0));

        assertThat(dismissOffenceDecisionViews, hasSize(1));
        assertThat(withdrawOffenceDecisionViews, hasSize(1));

        assertThat(statDecApplicationDetailsView.getWording(), equalTo("Appearance to make statutory declaration (SJP case)"));
        assertThat(dismissOffenceDetailsView.getWording(), equalTo("offence wording"));
        assertThat(withdrawOffenceDetailsView.getWording(), equalTo("offence wording"));

        assertThat(StatDecDecisionViews.getHeading(), equalTo("Court decision"));
        assertThat(StatDecDecisionViews.getLines(), contains(
                new OffenceDecisionLineView("Result", "Statutory declaration made under section 16E of the Magistrates' Courts Act 1980. Conviction and Sentence imposed on " + DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Accepted outside 21 days", "out of time reason"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));


        assertThat(dismissOffenceDecisionView.getHeading(), equalTo("Previous court decision"));
        assertThat(dismissOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedABeforeApplication)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedABeforeApplication))));

        assertThat(withdrawOffenceDecisionView.getHeading(), equalTo("Previous court decision"));
        assertThat(withdrawOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedABeforeApplication))));
    }

    @Test
    public void shouldVerifySingleCaseDecisionWithMultipleOffenceDecisionsWithDecisionPostApplication() {

        final ZonedDateTime savedABeforeApplication = now().minusDays(2);
        final ZonedDateTime savedAtForApplication = now().minusDays(1);

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(
                buildOffenceDetailEntity(1, offence1Id),
                buildOffenceDetailEntity(2, offence2Id)));

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseId, false, savedABeforeApplication);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null, null)));

        final CaseDecision caseDecisionPostApplication = buildCaseDecisionEntity(caseId, false, now());
        caseDecisionPostApplication.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null, null)));

        final CaseApplicationDecision caseApplicationDecision = buildCaseApplicationDecisionEntity(randomUUID(), true, false, "", savedAtForApplication);


        final CaseApplication caseApplication = buildCaseApplication(caseApplicationDecision, applicationId, savedAt.toLocalDate(), "Ref123", ApplicationType.REOPENING);
        caseApplicationDecision.setCaseApplication(caseApplication);

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(caseDecision)
                .withCaseDecision(caseDecisionPostApplication)
                .withCaseApplication(caseApplication)
                .build();


        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();
        final List<DecisionDetailsView> offenceDetailsApplicationViews = courtExtractView.getDecisionCourtExtractView().get(1).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(2));

        final DecisionDetailsView statDecApplicationDetailsView = offenceDetailsApplicationViews.get(0);
        final DecisionDetailsView dismissOffenceDetailsView = offenceDetailsViews.get(0);
        final DecisionDetailsView withdrawOffenceDetailsView = offenceDetailsViews.get(1);
        final DecisionView StatDecDecisionViews = statDecApplicationDetailsView.getApplicationDecision();
        final List<DecisionView> dismissOffenceDecisionViews = dismissOffenceDetailsView.getOffenceDecisions();
        final List<DecisionView> withdrawOffenceDecisionViews = withdrawOffenceDetailsView.getOffenceDecisions();
        final DecisionView dismissOffenceDecisionView = dismissOffenceDecisionViews.get(0);
        final DecisionView withdrawOffenceDecisionView = withdrawOffenceDecisionViews.get(0);

        assertThat(dismissOffenceDetailsView.getSequenceNumber(), equalTo(1));
        assertThat(withdrawOffenceDetailsView.getSequenceNumber(), equalTo(2));

        assertThat(dismissOffenceDecisionViews, hasSize(1));
        assertThat(withdrawOffenceDecisionViews, hasSize(1));


        assertThat(statDecApplicationDetailsView.getWording(), equalTo("Application to reopen case"));
        assertThat(dismissOffenceDetailsView.getWording(), equalTo("offence wording"));
        assertThat(withdrawOffenceDetailsView.getWording(), equalTo("offence wording"));

        assertThat(StatDecDecisionViews.getHeading(), equalTo("Previous court decision"));
        assertThat(StatDecDecisionViews.getLines(), contains(
                new OffenceDecisionLineView("Result", "Case reopened under section 142 of the Magistrates' Courts Act 1980. Conviction and Sentence imposed on " + DATE_FORMAT.format(savedAtForApplication) + " set aside."),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAtForApplication))));


        assertThat(dismissOffenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(dismissOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));

        assertThat(withdrawOffenceDecisionView.getHeading(), equalTo("Court decision"));
        assertThat(withdrawOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }


    @Test
    public void shouldVerifySingleCaseDecisionWithMultipleOffenceDecisionsWithApplicationRefused() {

        final ZonedDateTime savedABeforeApplication = now().minusDays(2);

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(
                buildOffenceDetailEntity(1, offence1Id),
                buildOffenceDetailEntity(2, offence2Id)));

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseId, false, savedABeforeApplication);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null, null)));

        final CaseApplicationDecision caseApplicationDecision = buildCaseApplicationRefusedDecisionEntity(randomUUID(), false, false, "rejection Reason", now());

        final CaseApplication caseApplication = buildCaseApplication(caseApplicationDecision, applicationId, savedAt.toLocalDate(), "Ref123", ApplicationType.REOPENING);
        caseApplicationDecision.setCaseApplication(caseApplication);

        final CaseDetail aCase = aCase()
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(caseDecision)
                .withCaseApplication(caseApplication)
                .build();


        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<DecisionDetailsView> offenceDetailsViews = courtExtractView.getDecisionCourtExtractView().get(1).getOffencesApplicationsDecisions();
        final List<DecisionDetailsView> offenceDetailsApplicationViews = courtExtractView.getDecisionCourtExtractView().get(0).getOffencesApplicationsDecisions();

        assertThat(offenceDetailsViews, hasSize(2));

        final DecisionDetailsView statDecApplicationDetailsView = offenceDetailsApplicationViews.get(0);
        final DecisionDetailsView dismissOffenceDetailsView = offenceDetailsViews.get(0);
        final DecisionView StatDecDecisionViews = statDecApplicationDetailsView.getApplicationDecision();
        final List<DecisionView> dismissOffenceDecisionViews = dismissOffenceDetailsView.getOffenceDecisions();
        final DecisionView dismissOffenceDecisionView = dismissOffenceDecisionViews.get(0);

        assertThat(dismissOffenceDetailsView.getSequenceNumber(), equalTo(1));

        assertThat(dismissOffenceDecisionViews, hasSize(1));


        assertThat(statDecApplicationDetailsView.getWording(), equalTo("Application to reopen case"));
        assertThat(dismissOffenceDetailsView.getWording(), equalTo("offence wording"));

        assertThat(StatDecDecisionViews.getHeading(), equalTo("Court decision"));
        assertThat(StatDecDecisionViews.getLines(), contains(
                new OffenceDecisionLineView("Result", "Application refused"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(now()))));


        assertThat(dismissOffenceDecisionView.getHeading(), equalTo("Previous court decision"));
        assertThat(dismissOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedABeforeApplication)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedABeforeApplication))));

    }

    private final CaseApplication buildCaseApplication(final CaseApplicationDecision caseApplicationDecision, final UUID applicationId, final LocalDate dateReceived, final String applicationReference, final ApplicationType applicationType) {

        final CaseApplication caseApplication = new CaseApplication();
        caseApplication.setApplicationId(applicationId);
        caseApplication.setApplicationDecision(caseApplicationDecision);
        caseApplication.setTypeId(randomUUID());
        caseApplication.setDateReceived(dateReceived);
        caseApplication.setTypeCode(randomUUID().toString());
        caseApplication.setApplicationReference(applicationReference);
        caseApplication.setApplicationType(applicationType);
        caseApplication.setOutOfTime(caseApplicationDecision.getOutOfTime());
        caseApplication.setOutOfTimeReason(caseApplicationDecision.getOutOfTimeReason());
        return caseApplication;
    }

    private static CaseDetail buildSingleOffenceCaseWithSingleDecision(final UUID caseId,
                                                                       final UUID offenceId,
                                                                       final DecisionType decisionType,
                                                                       final ZonedDateTime savedAt,
                                                                       final PleaInfo pleaAtDecisionTime,
                                                                       boolean magistrate, PressRestriction pressRestriction) {
        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(buildOffenceDetailEntity(1, offenceId)));

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseId, magistrate, savedAt);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offenceId, decisionType, pleaAtDecisionTime, pressRestriction)));
        caseDecision.setFinancialImposition(buildFinancialImposition(ATTACH_TO_EARNINGS, new LumpSum(BigDecimal.valueOf(30.34), 0, LocalDate.now())));

        return aCase()
                .withCaseId(caseId)
                .withDefendantDetail(defendantDetail)
                .withCaseDecision(caseDecision)
                .build();
    }

    private static FinancialImposition buildFinancialImposition(PaymentType paymentType, LumpSum lumpSum) {
        FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(BigDecimal.valueOf(200.23), null,
                        BigDecimal.valueOf(300), null,
                        true, null),
                new Payment(BigDecimal.valueOf(300), paymentType,
                        "some reason", COMPENSATION_ORDERED,
                        new PaymentTerms(false,
                                lumpSum,
                                new Installments(BigDecimal.valueOf(40), WEEKLY, LocalDate.of(2019, 8, 1))), null));
        return financialImposition;
    }

    private static CaseDecision buildCaseDecisionEntity(final UUID caseId, final boolean magistrate, final ZonedDateTime savedAt) {
        final Session session = new Session(randomUUID(), randomUUID(), "ASDF", "Lavender Hill",
                "YUIO", magistrate ? "Magistrate name" : null, now(), Arrays.asList(TFL, DVL));

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setCaseId(caseId);
        caseDecision.setId(randomUUID());
        caseDecision.setSavedAt(savedAt);
        caseDecision.setSession(session);

        return caseDecision;
    }

    private static CaseApplicationDecision buildCaseApplicationDecisionEntity(final UUID decisionId, final boolean granted, final boolean outOfTime, final String outOfTimeReason, final ZonedDateTime savedAt) {
        final Session session = new Session(randomUUID(), randomUUID(), "ASDF", "Lavender Hill",
                "YUIO", "Magistrate name", now(), Arrays.asList(TFL, DVL));

        final CaseApplicationDecision caseApplicationDecision = new CaseApplicationDecision();
        caseApplicationDecision.setDecisionId(decisionId);
        caseApplicationDecision.setGranted(granted);
        caseApplicationDecision.setOutOfTime(outOfTime);
        caseApplicationDecision.setOutOfTimeReason(outOfTimeReason);
        caseApplicationDecision.setSavedAt(savedAt);
        caseApplicationDecision.setSession(session);

        return caseApplicationDecision;
    }

    private static CaseApplicationDecision buildCaseApplicationRefusedDecisionEntity(final UUID decisionId, final boolean granted, final boolean outOfTime, final String rejectionReason, final ZonedDateTime savedAt) {
        final Session session = new Session(randomUUID(), randomUUID(), "ASDF", "Lavender Hill",
                "YUIO", "Magistrate name", now(), Arrays.asList(TFL, DVL));

        final CaseApplicationDecision caseApplicationDecision = new CaseApplicationDecision();
        caseApplicationDecision.setDecisionId(decisionId);
        caseApplicationDecision.setGranted(granted);
        caseApplicationDecision.setOutOfTime(outOfTime);
        caseApplicationDecision.setRejectionReason(rejectionReason);
        caseApplicationDecision.setSavedAt(savedAt);
        caseApplicationDecision.setSession(session);

        return caseApplicationDecision;
    }

    private static OffenceDecision buildFinancialPenaltyOffenceDecisionEntity(
            final UUID offenceId,
            final PleaInfo pleaAtDecisionTime,
            final Boolean guiltyPleaTakenIntoAccount,
            final BigDecimal compensation,
            final String noCompensationReason,
            final BigDecimal fine) {
        final OffenceDecision offenceDecision = new FinancialPenaltyOffenceDecision(offenceId,
                randomUUID(),
                PROVED_SJP,
                guiltyPleaTakenIntoAccount,
                compensation,
                noCompensationReason,
                fine,
                null,
                null,
                null, null);
        if (nonNull(pleaAtDecisionTime)) {
            offenceDecision.setPleaAtDecisionTime(pleaAtDecisionTime.pleaType);
            offenceDecision.setPleaDate(pleaAtDecisionTime.pleaDate);
        }
        return offenceDecision;
    }

    private static OffenceDecision buildOffenceDecisionEntity(final UUID decisionId,
                                                              final UUID offenceId,
                                                              final DecisionType type,
                                                              final PleaInfo pleaAtDecisionTime, PressRestriction pressRestriction) {
        OffenceDecision offenceDecision = null;
        switch (type) {
            case DISMISS:
                offenceDecision = new DismissOffenceDecision(offenceId,
                        decisionId,
                        VerdictType.FOUND_NOT_GUILTY, pressRestriction);
                break;
            case ADJOURN:
                offenceDecision = new AdjournOffenceDecision(offenceId,
                        decisionId,
                        "",
                        now().plusDays(7).toLocalDate(),
                        VerdictType.NO_VERDICT,
                        null, pressRestriction);
                break;
            case REFER_FOR_COURT_HEARING:
                offenceDecision = new ReferForCourtHearingDecision(offenceId,
                        decisionId,
                        UUID.fromString("7e2f843e-d639-40b3-8611-8015f3a18957"),
                        10,
                        "",
                        VerdictType.NO_VERDICT,
                        null, pressRestriction);
                break;
            case WITHDRAW:
                offenceDecision = new WithdrawOffenceDecision(offenceId,
                        decisionId,
                        randomUUID(),
                        VerdictType.NO_VERDICT, pressRestriction);
                break;

            case FINANCIAL_PENALTY:
                FinancialPenaltyOffenceDecision financialPenaltyOffenceDecision = new FinancialPenaltyOffenceDecision(offenceId, randomUUID(), VerdictType.FOUND_NOT_GUILTY, true, BigDecimal.valueOf(20), null, BigDecimal.valueOf(30), null, null, null, pressRestriction);
                financialPenaltyOffenceDecision.setLicenceEndorsement(true);
                financialPenaltyOffenceDecision.setPenaltyPointsImposed(3);
                offenceDecision = financialPenaltyOffenceDecision;
                break;
            case DISCHARGE:
                DischargeOffenceDecision dischargeOffenceDecision = new DischargeOffenceDecision(offenceId, randomUUID(), VerdictType.FOUND_NOT_GUILTY, new DischargePeriod(PeriodUnit.DAY, 12), null, null, null, DischargeType.CONDITIONAL, null, null, null, null, null, null, null, null, null, null, null, pressRestriction);

                dischargeOffenceDecision.setLicenceEndorsement(true);
                dischargeOffenceDecision.setDisqualification(true);
                dischargeOffenceDecision.setDisqualificationPeriodUnit(DisqualificationPeriodTimeUnit.DAY);
                dischargeOffenceDecision.setDisqualificationPeriodValue(3);
                dischargeOffenceDecision.setDisqualificationType(DisqualificationType.DISCRETIONARY);
                offenceDecision = dischargeOffenceDecision;
                break;
            case NO_SEPARATE_PENALTY:

                offenceDecision = new NoSeparatePenaltyOffenceDecision(offenceId, randomUUID(), VerdictType.FOUND_NOT_GUILTY, null, null, true, pressRestriction);


        }

        if (nonNull(pleaAtDecisionTime)) {
            offenceDecision.setPleaAtDecisionTime(pleaAtDecisionTime.pleaType);
            offenceDecision.setPleaDate(pleaAtDecisionTime.pleaDate);
        }
        return offenceDecision;
    }

    private static OffenceDetail buildOffenceDetailEntity(int sequenceNumber, final UUID offenceId) {
        final OffenceDetail.OffenceDetailBuilder offenceDetailBuilder = OffenceDetail.builder().
                setId(offenceId).
                setCode("CA03013").
                setWording("offence wording").
                setPleaMethod(ONLINE).
                setSequenceNumber(sequenceNumber);

        return offenceDetailBuilder.build();
    }
}
