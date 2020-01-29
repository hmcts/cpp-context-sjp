package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.ONLINE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;
import static uk.gov.moj.cpp.sjp.query.view.helper.PleaInfo.plea;

import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.helper.PleaInfo;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class CaseCourtExtractViewTest {

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private final UUID caseId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final ZonedDateTime savedAt = now();

    @Test
    public void shouldVerifyCaseDetail() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, null, true);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        assertThat(courtExtractView.getGenerationDate(), notNullValue());
        assertThat(courtExtractView.getGenerationTime(), notNullValue());

        assertEquals(aCase.getUrn(), courtExtractView.getCaseDetails().getReference());
        assertEquals("Theresa", courtExtractView.getDefendant().getFirstName());
        assertEquals("May", courtExtractView.getDefendant().getLastName());
    }

    @Test
    public void shouldVerifyReverseOrderOfSavedDate() {
        final ZonedDateTime adjournedOffenceDecisionSavedAt = now();
        final ZonedDateTime withdrawnOffenceDecisionSavedAt = adjournedOffenceDecisionSavedAt.plusDays(1);

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(buildOffenceDetailEntity(1, offence1Id)));

        final CaseDecision adjournedCaseDecision = buildCaseDecisionEntity(caseId, false, adjournedOffenceDecisionSavedAt);
        adjournedCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision.getId(), offence1Id, ADJOURN, null)));
        final CaseDecision withdrawnCaseDecision = buildCaseDecisionEntity(caseId, false, withdrawnOffenceDecisionSavedAt);
        withdrawnCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(withdrawnCaseDecision.getId(), offence1Id, WITHDRAW, null)));

        final CaseDetail aCase = aCase()
                .withCaseId(caseId)
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(adjournedCaseDecision)
                .addCaseDecision(withdrawnCaseDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView withdrawOffenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        final OffenceDecisionView adjournOffenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(1);

        assertEquals(DATE_FORMAT.format(adjournedOffenceDecisionSavedAt), adjournOffenceDecisionView.getLines()
                .stream()
                .filter(offenceDecisionLineView -> "Decision made".equals(offenceDecisionLineView.getLabel()))
                .findFirst()
                .get().getValue());

        assertEquals(withdrawnOffenceDecisionSavedAt.toLocalDate().format(DATE_FORMAT), withdrawOffenceDecisionView.getLines()
                .stream()
                .filter(offenceDecisionLineView -> "Decision made".equals(offenceDecisionLineView.getLabel()))
                .findFirst()
                .get().getValue());
    }

    @Test
    public void shouldVerifyDismissCaseWithMagistrateSession() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, null, true);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView offenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyDismissGuiltyCaseWithMagistrateSession() {
        final ZonedDateTime pleaDate = now().minusDays(2);
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, DISMISS, savedAt, plea(GUILTY, pleaDate), true);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView offenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyWithdrawCaseWithDelegatedPowers() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, WITHDRAW, savedAt, null, false);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView offenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyAdjournCaseWithMagistrateSession() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, ADJOURN, savedAt, null, true);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView offenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) aCase.getCaseDecisions().get(0).getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyReferForHearingCaseWithDelegatedPowers() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, REFER_FOR_COURT_HEARING, savedAt, null, false);

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView offenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Referred for court hearing."),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));
    }

    @Test
    public void shouldVerifyReopenedInLibraCaseWithDelegatedPowers() {
        final CaseDetail aCase = buildSingleOffenceCaseWithSingleDecision(caseId, offence1Id, WITHDRAW, savedAt, null, false);
        aCase.setCaseStatus(CaseStatus.REOPENED_IN_LIBRA);
        aCase.setLibraCaseNumber("12345");
        aCase.setReopenedDate(now().toLocalDate());

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final OffenceDecisionView offenceDecisionView = courtExtractView.getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision (set aside)", offenceDecisionView.getHeading());
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
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null)));

        final CaseDetail aCase = aCase()
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(caseDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<OffenceDetailsView> offenceDetailsViews = courtExtractView.getOffences();

        assertEquals(2, offenceDetailsViews.size());

        final OffenceDetailsView dismissOffenceDetailsView = offenceDetailsViews.get(0);
        final OffenceDetailsView withdrawOffenceDetailsView = offenceDetailsViews.get(1);
        final List<OffenceDecisionView> dismissOffenceDecisionViews = dismissOffenceDetailsView.getOffenceDecisions();
        final List<OffenceDecisionView> withdrawOffenceDecisionViews = withdrawOffenceDetailsView.getOffenceDecisions();
        final OffenceDecisionView dismissOffenceDecisionView = dismissOffenceDecisionViews.get(0);
        final OffenceDecisionView withdrawOffenceDecisionView = withdrawOffenceDecisionViews.get(0);

        assertEquals(1, dismissOffenceDetailsView.getSequenceNumber());
        assertEquals(2, withdrawOffenceDetailsView.getSequenceNumber());

        assertEquals(1, dismissOffenceDecisionViews.size());
        assertEquals(1, withdrawOffenceDecisionViews.size());

        assertEquals("offence wording", dismissOffenceDetailsView.getWording());
        assertEquals("offence wording", withdrawOffenceDetailsView.getWording());

        assertEquals("Court decision", dismissOffenceDecisionView.getHeading());
        assertThat(dismissOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", DATE_FORMAT.format(savedAt)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(savedAt))));

        assertEquals("Court decision", withdrawOffenceDecisionView.getHeading());
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
                buildOffenceDecisionEntity(firstDecision.getId(), offence1Id, ADJOURN, null),
                buildOffenceDecisionEntity(firstDecision.getId(), offence2Id, DISMISS, null)));

        final CaseDecision secondDecision = buildCaseDecisionEntity(caseId, false, savedAt);
        secondDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(secondDecision.getId(), offence1Id, DISMISS, null)));


        final CaseDetail aCase = aCase()
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(firstDecision)
                .addCaseDecision(secondDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<OffenceDetailsView> offenceDetailsViews = courtExtractView.getOffences();

        assertEquals(2, offenceDetailsViews.size());

        final OffenceDetailsView offenceDetailsView1 = offenceDetailsViews.get(0);
        final OffenceDetailsView offenceDetailsView2 = offenceDetailsViews.get(1);

        assertEquals(1, offenceDetailsView1.getSequenceNumber());
        assertEquals(2, offenceDetailsView2.getSequenceNumber());

        assertEquals("offence wording", offenceDetailsView1.getWording());
        assertEquals("offence wording", offenceDetailsView2.getWording());

        final List<OffenceDecisionView> dismissAndAdjournOffenceDecisionViews = offenceDetailsView1.getOffenceDecisions();
        final List<OffenceDecisionView> dismissOffenceDecisionViews = offenceDetailsView2.getOffenceDecisions();

        assertEquals(2, dismissAndAdjournOffenceDecisionViews.size());
        assertEquals(1, dismissOffenceDecisionViews.size());

        final OffenceDecisionView dismissDecisionView = dismissAndAdjournOffenceDecisionViews.get(0);
        final OffenceDecisionView adjournOffenceDecisionView = dismissAndAdjournOffenceDecisionViews.get(1);
        final OffenceDecisionView dismissDecisionView2 = dismissOffenceDecisionViews.get(0);

        assertEquals("Court decision", dismissDecisionView.getHeading());
        assertEquals("Previous court decision", adjournOffenceDecisionView.getHeading());
        assertEquals("Court decision", dismissDecisionView2.getHeading());
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
        adjournedCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision.getId(), offence1Id, ADJOURN, null)));

        final CaseDecision withdrawnCaseDecision = buildCaseDecisionEntity(caseId, false, now().plusDays(1));
        withdrawnCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(withdrawnCaseDecision.getId(), offence1Id, WITHDRAW, null)));

        final CaseDetail aCase = aCase()
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(adjournedCaseDecision)
                .addCaseDecision(withdrawnCaseDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<OffenceDetailsView> offenceDetailsViews = courtExtractView.getOffences();

        assertEquals(1, offenceDetailsViews.size());

        final OffenceDetailsView offenceDetailsView = offenceDetailsViews.get(0);
        final List<OffenceDecisionView> offenceDecisionViews = offenceDetailsView.getOffenceDecisions();
        final OffenceDecisionView withdrawOffenceDecisionView = offenceDecisionViews.get(0);
        final OffenceDecisionView adjournOffenceDecisionView = offenceDecisionViews.get(1);

        assertEquals(1, offenceDetailsView.getSequenceNumber());
        assertEquals("offence wording", offenceDetailsView.getWording());

        assertEquals(2, offenceDecisionViews.size());

        assertEquals("Previous court decision", adjournOffenceDecisionView.getHeading());
        assertThat(adjournOffenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) adjournedCaseDecision.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(adjournedCaseDecision.getSavedAt()))));

        assertEquals("Court decision", withdrawOffenceDecisionView.getHeading());
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
        adjournedCaseDecision1.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision1.getId(), offence1Id, ADJOURN, null)));

        final CaseDecision adjournedCaseDecision2 = buildCaseDecisionEntity(caseId, false, now().plusDays(1));
        adjournedCaseDecision2.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision2.getId(), offence1Id, ADJOURN, null)));

        final CaseDetail aCase = aCase()
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(adjournedCaseDecision1)
                .addCaseDecision(adjournedCaseDecision2)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);

        final List<OffenceDetailsView> offenceDetailsViews = courtExtractView.getOffences();

        assertEquals(1, offenceDetailsViews.size());

        final OffenceDetailsView offenceDetailsView = offenceDetailsViews.get(0);

        assertEquals(1, offenceDetailsView.getSequenceNumber());
        assertEquals("offence wording", offenceDetailsView.getWording());

        final List<OffenceDecisionView> offenceDecisionViews = offenceDetailsView.getOffenceDecisions();

        assertEquals(2, offenceDecisionViews.size());

        final OffenceDecisionView adjournOffenceDecisionView1 = offenceDecisionViews.get(0);
        final OffenceDecisionView adjournOffenceDecisionView2 = offenceDecisionViews.get(1);

        assertEquals("Previous court decision", adjournOffenceDecisionView2.getHeading());
        assertThat(adjournOffenceDecisionView2.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) adjournedCaseDecision1.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", DATE_FORMAT.format(adjournedCaseDecision1.getSavedAt()))));

        assertEquals("Court decision", adjournOffenceDecisionView1.getHeading());
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
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(firstDecision)
                .build();

        final CaseCourtExtractView courtExtractView = new CaseCourtExtractView(aCase);
        final List<OffenceDetailsView> offenceDetailsViews = courtExtractView.getOffences();
        final OffenceDetailsView offenceDetailsView = offenceDetailsViews.get(0);
        final List<OffenceDecisionView> offenceDecisionViews = offenceDetailsView.getOffenceDecisions();
        final List<OffenceDecisionLineView> lines =  offenceDecisionViews.get(0).getLines();
        assertTrue(lines
                .stream()
                .map(OffenceDecisionLineView::getLabel)
                .noneMatch(label -> ("To pay compensation of".equalsIgnoreCase(label)) ||
                        "To pay a fine of".equalsIgnoreCase(label) ||
                        "Defendant's guilty plea".equalsIgnoreCase(label)));

    }

    private static CaseDetail buildSingleOffenceCaseWithSingleDecision(final UUID caseId,
                                                                       final UUID offenceId,
                                                                       final DecisionType decisionType,
                                                                       final ZonedDateTime savedAt,
                                                                       final PleaInfo pleaAtDecisionTime,
                                                                       boolean magistrate) {
        final DefendantDetail defendantDetail = aDefendantDetail().build();
        defendantDetail.setOffences(asList(buildOffenceDetailEntity(1, offenceId)));

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseId, magistrate, savedAt);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offenceId, decisionType, pleaAtDecisionTime)));

        return aCase()
                .withCaseId(caseId)
                .addDefendantDetail(defendantDetail)
                .addCaseDecision(caseDecision)
                .build();
    }

    private static CaseDecision buildCaseDecisionEntity(final UUID caseId, final boolean magistrate, final ZonedDateTime savedAt) {
        final Session session = new Session(randomUUID(), randomUUID(), "ASDF", "Lavender Hill",
                "YUIO", magistrate ? "Magistrate name" : null, now());

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setCaseId(caseId);
        caseDecision.setId(randomUUID());
        caseDecision.setSavedAt(savedAt);
        caseDecision.setSession(session);

        return caseDecision;
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
                null);
        if (nonNull(pleaAtDecisionTime)) {
            offenceDecision.setPleaAtDecisionTime(pleaAtDecisionTime.pleaType);
            offenceDecision.setPleaDate(pleaAtDecisionTime.pleaDate);
        }
        return offenceDecision;
    }

    private static OffenceDecision buildOffenceDecisionEntity(final UUID decisionId,
                                                              final UUID offenceId,
                                                              final DecisionType type,
                                                              final PleaInfo pleaAtDecisionTime) {
        OffenceDecision offenceDecision = null;
        switch (type) {
            case DISMISS:
                offenceDecision = new DismissOffenceDecision(offenceId,
                        decisionId,
                        VerdictType.FOUND_NOT_GUILTY);
                break;
            case ADJOURN:
                offenceDecision = new AdjournOffenceDecision(offenceId,
                        decisionId,
                        "",
                        now().plusDays(7).toLocalDate(),
                        VerdictType.NO_VERDICT);
                break;
            case REFER_FOR_COURT_HEARING:
                offenceDecision = new ReferForCourtHearingDecision(offenceId,
                        decisionId,
                        UUID.fromString("7e2f843e-d639-40b3-8611-8015f3a18957"),
                        10,
                        "",
                        VerdictType.NO_VERDICT);
                break;
            case WITHDRAW:
                offenceDecision = new WithdrawOffenceDecision(offenceId,
                        decisionId,
                        randomUUID(),
                        VerdictType.NO_VERDICT);
                break;
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
