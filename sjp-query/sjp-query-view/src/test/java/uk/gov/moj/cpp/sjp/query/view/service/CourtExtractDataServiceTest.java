package uk.gov.moj.cpp.sjp.query.view.service;


import static java.lang.String.format;
import static java.math.BigDecimal.*;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createReader;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.*;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.*;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.*;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;
import static uk.gov.moj.cpp.sjp.query.view.helper.PleaInfo.plea;

import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.helper.PleaInfo;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.CaseCourtExtractView;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.OffenceDecisionLineView;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.OffenceDecisionView;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtExtractDataServiceTest {

    @InjectMocks
    private CourtExtractDataService courtExtractDataService;

    @Mock
    private CaseService caseService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    private final UUID caseId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private static final String LEGAL_ADVISER_NAME = "Legal adviser name";
    private static final String MAGISTRATE_NAME = "Legal adviser name";

    private final ZonedDateTime decisionSavedAt = ZonedDateTime.now();
    private final String formattedSavedAt = DATE_FORMAT.format(decisionSavedAt);

    @Before
    public void setUp() {
        when(referenceDataService.getProsecutorsByProsecutorCode(anyString())).thenReturn(referenceProsecutorDataResponse());
        when(referenceDataService.getOffenceData(anyString())).thenReturn(referenceOffenceResponse());
        when(referenceDataService.getReferralReasons()).thenReturn(referenceReferralReasonsDataResponse());
        when(userAndGroupsService.getUserDetails(any())).thenReturn("Legal adviser name");
    }

    @Test
    public void shouldGetCaseData() {
        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, DISMISS, decisionSavedAt, null, true);

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());
        assertEquals(caseDetail.getUrn(), caseCourtExtractView.get().getCaseDetails().getReference());
        assertEquals("Transport for London", caseCourtExtractView.get().getCaseDetails().getProsecutor());
        assertEquals("Theresa", caseCourtExtractView.get().getDefendant().getFirstName());
        assertEquals("May", caseCourtExtractView.get().getDefendant().getLastName());
        assertEquals("6th Floor Windsor House", caseCourtExtractView.get().getCaseDetails().getProsecutorAddress().getLine1());
        assertEquals("42-50 Victoria Street", caseCourtExtractView.get().getCaseDetails().getProsecutorAddress().getLine2());
        assertEquals("London", caseCourtExtractView.get().getCaseDetails().getProsecutorAddress().getLine3());
        assertEquals("SW1H 0TL", caseCourtExtractView.get().getCaseDetails().getProsecutorAddress().getPostcode());
    }

    @Test
    public void shouldVerifyDismissCaseWithMagistrateSession() {
        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, DISMISS, decisionSavedAt, null, true);

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView offenceDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", formattedSavedAt),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, true))));
    }

    @Test
    public void shouldVerifyDismissGuiltyCaseWithMagistrateSession() {
        final ZonedDateTime pleaDate = now().minusDays(1);

        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, DISMISS, decisionSavedAt, plea(GUILTY, pleaDate), true);

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView offenceDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "Guilty"),
                new OffenceDecisionLineView("Plea date", DATE_FORMAT.format(pleaDate)),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", formattedSavedAt),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, true))));
    }

    @Test
    public void shouldVerifyWithdrawCaseWithDelegatedPowers() {
        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, WITHDRAW, decisionSavedAt, null, false);

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView offenceDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))));
    }

    @Test
    public void shouldVerifyAdjournCaseWithMagistrateSession() {
        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, ADJOURN, decisionSavedAt, null, true);

        final CaseDecision caseDecision = caseDetail.getCaseDecisions().get(0);

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView offenceDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);
        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) caseDecision.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, true))));
    }

    @Test
    public void shouldVerifyReferForHearingCaseWithDelegatedPowers() {
        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, REFER_FOR_COURT_HEARING, decisionSavedAt, null, false);

        when(caseService.getCase(any(UUID.class))).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView offenceDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Referred for court hearing.\nReason: Sections 135"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))));
    }

    @Test
    public void shouldVerifyReopenedInLibraCaseWithDelegatedPowers() {
        final CaseDetail caseDetail = buildCaseWithSingleDecisionAndSingleOffence(caseId, WITHDRAW, decisionSavedAt, null, false);
        caseDetail.setCaseStatus(CaseStatus.REOPENED_IN_LIBRA);
        caseDetail.setLibraCaseNumber("12345");
        caseDetail.setReopenedDate(now().toLocalDate());

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService.getCourtExtractData(caseId);

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView offenceDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision (set aside)", offenceDecisionView.getHeading());
        assertThat(offenceDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false)),
                new OffenceDecisionLineView("Decision set aside",
                        format("Case reopened %s%nLibra account no. %s",
                                DATE_FORMAT.format(caseDetail.getReopenedDate()), caseDetail.getLibraCaseNumber()))));
    }

    @Test
    public void shouldVerifySingleCaseDecisionWithMultipleOffenceDecisions() {
        final CaseDetail caseDetail = aCase()
                .withCaseId(caseId)
                .withProsecutingAuthority("TFL")
                .build();

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        final List<OffenceDetail> offenceDetails = new ArrayList<>();
        offenceDetails.add(buildOffenceDetailEntity(1, offence1Id));
        offenceDetails.add(buildOffenceDetailEntity(2, offence2Id));
        defendantDetail.setOffences(offenceDetails);
        caseDetail.setDefendant(defendantDetail);

        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, DISMISS, null),
                buildOffenceDecisionEntity(caseDecision.getId(), offence2Id, WITHDRAW, null)));

        caseDetail.setCaseDecisions(asList(caseDecision));

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService
                .getCourtExtractData(caseDetail.getId());

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView dismissedDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);
        final OffenceDecisionView withdrawnDecisionView = caseCourtExtractView.get().getOffences().get(1).getOffenceDecisions().get(0);

        assertEquals("Court decision", dismissedDecisionView.getHeading());
        assertThat(dismissedDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Found not guilty"),
                new OffenceDecisionLineView("Date of verdict", caseDecision.getSavedAt().toLocalDate().format(DATE_FORMAT)),
                new OffenceDecisionLineView("Result", "Dismissed"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))));

        assertEquals("Court decision", withdrawnDecisionView.getHeading());
        assertThat(withdrawnDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))));
    }

    @Test
    public void shouldVerifyMultipleCaseDecisionsWithSingleOffenceDecision() {
        final CaseDetail caseDetail = aCase()
                .withCaseId(caseId)
                .withProsecutingAuthority("TFL")
                .build();

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        final List<OffenceDetail> offenceDetails = new ArrayList<>();
        offenceDetails.add(buildOffenceDetailEntity(1, offence1Id));
        defendantDetail.setOffences(offenceDetails);
        caseDetail.setDefendant(defendantDetail);

        final CaseDecision adjournedCaseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        adjournedCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(adjournedCaseDecision.getId(), offence1Id, ADJOURN, null)));
        final CaseDecision withdrawnCaseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now().plusDays(1));
        withdrawnCaseDecision.setOffenceDecisions(asList(buildOffenceDecisionEntity(withdrawnCaseDecision.getId(), offence1Id, WITHDRAW, null)));

        caseDetail.setCaseDecisions(asList(adjournedCaseDecision, withdrawnCaseDecision));

        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService
                .getCourtExtractData(caseDetail.getId());

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView withdrawnDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);
        final OffenceDecisionView adjournedDecisionView = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(1);

        assertEquals("Previous court decision", adjournedDecisionView.getHeading());
        assertThat(adjournedDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", format("Adjourn to later SJP hearing.%nOn or after %s",
                        DATE_FORMAT.format(((AdjournOffenceDecision) adjournedCaseDecision.getOffenceDecisions().get(0)).getAdjournedTo()))),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(adjournedCaseDecision.getSavedAt(), false))));

        assertEquals("Court decision", withdrawnDecisionView.getHeading());
        assertThat(withdrawnDecisionView.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Result", "Withdrawn"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(withdrawnCaseDecision.getSavedAt(), false))));
    }

    private CaseDetail buildCaseWithSingleDecisionAndSingleOffence(final UUID caseId,
                                                                   final DecisionType decisionType,
                                                                   final ZonedDateTime savedAt,
                                                                   final PleaInfo pleaAtDecisionTime,
                                                                   boolean magistrate) {
        final CaseDetail entity = aCase()
                .withCaseId(caseId)
                .withProsecutingAuthority("TFL")
                .build();

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        final List<OffenceDetail> offenceDetails = new ArrayList<>();
        offenceDetails.add(buildOffenceDetailEntity(1, offence1Id));
        defendantDetail.setOffences(offenceDetails);
        entity.setDefendant(defendantDetail);

        final CaseDecision caseDecision = buildCaseDecisionEntity(entity.getId(), magistrate, savedAt);
        caseDecision.setOffenceDecisions(asList(
                buildOffenceDecisionEntity(caseDecision.getId(), offence1Id, decisionType, pleaAtDecisionTime)));

        entity.setCaseDecisions(asList(caseDecision));
        return entity;
    }


    private CaseDecision buildCaseDecisionEntity(UUID caseId, boolean magistrate, ZonedDateTime savedAt) {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setCaseId(caseId);
        caseDecision.setId(randomUUID());
        caseDecision.setSavedAt(savedAt);

        caseDecision.setSession(new Session(randomUUID(), randomUUID(), "ASDF", "Lavender Hill",
                "YUIO", magistrate ? MAGISTRATE_NAME : null, now()));
        return caseDecision;
    }

    private OffenceDecision buildOffenceDecisionEntity(final UUID decisionId,
                                                       final UUID offenceId,
                                                       final DecisionType type,
                                                       final PleaInfo pleaAtDecisionTime) {
        OffenceDecision offenceDecision = null;
        switch (type) {
            case DISMISS:
                offenceDecision = new DismissOffenceDecision(offenceId,
                        decisionId,
                        FOUND_NOT_GUILTY, null);
                break;
            case ADJOURN:
                offenceDecision = new AdjournOffenceDecision(offenceId,
                        decisionId,
                        "",
                        now().plusDays(7).toLocalDate(),
                        NO_VERDICT, null, null);
                break;
            case REFER_FOR_COURT_HEARING:
                offenceDecision = new ReferForCourtHearingDecision(offenceId,
                        decisionId,
                        UUID.fromString("7e2f843e-d639-40b3-8611-8015f3a18957"),
                        10,
                        "",
                        NO_VERDICT, null, null);
                break;
            case WITHDRAW:
                offenceDecision = new WithdrawOffenceDecision(offenceId,
                        decisionId,
                        randomUUID(),
                        NO_VERDICT, null);
                break;
            case DISCHARGE:
                offenceDecision = new DischargeOffenceDecision(offenceId,
                        decisionId,
                        NO_VERDICT,
                        new DischargePeriod(WEEK, 10),
                        true,
                        TEN,
                        null,
                        CONDITIONAL, null, null);
                break;
        }

        if (nonNull(pleaAtDecisionTime)) {
            offenceDecision.setPleaAtDecisionTime(pleaAtDecisionTime.pleaType);
            offenceDecision.setPleaDate(pleaAtDecisionTime.pleaDate);
        }
        return offenceDecision;
    }

    private OffenceDetail buildOffenceDetailEntity(final int sequenceNumber, final UUID offenceId) {
        final OffenceDetail.OffenceDetailBuilder offenceDetailBuilder = OffenceDetail.builder().
                setId(offenceId).
                setCode("CA03013").
                setWording("offence wording").
                setSequenceNumber(sequenceNumber).
                setPlea(PleaType.values()[nextInt(PleaType.values().length)]).
                setPleaDate(now().minusDays(3));

        return offenceDetailBuilder.build();
    }

    private Optional<JsonArray> referenceProsecutorDataResponse() {
        return Optional.of(createReader(getClass().getClassLoader().
                getResourceAsStream("prosecutors-ref-data.json")).
                readObject().getJsonArray("prosecutors"));
    }

    private JsonArray referenceReferralReasonsDataResponse() {
        return createReader(getClass().getClassLoader().
                getResourceAsStream("referral-reasons-data.json")).
                readObject().getJsonArray("referralReasons");
    }

    private Optional<JsonObject> referenceOffenceResponse() {
        return Optional.of(createReader(getClass().getClassLoader().
                getResourceAsStream("offence-ref-data.json")).
                readObject().getJsonArray("offences").getJsonObject(0));
    }

    private static String expectedDecisionMade(final ZonedDateTime decisionSavedAt, boolean magistrate) {
        final String decisionDate = DATE_FORMAT.format(decisionSavedAt);
        final String userName = magistrate ? MAGISTRATE_NAME : LEGAL_ADVISER_NAME;
        final String userType = magistrate ? "Magistrate" : "Legal Adviser";
        return String.format("%s\n%s (%s)", decisionDate, userName, userType);
    }

    @Test
    public void shouldVerifyForDischargeOffenceDecisions() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();

        final CaseDecision dischargeDecision1 = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        dischargeDecision1.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        dischargeDecision1.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        ZERO,
                        null,
                        ABSOLUTE, null, null)));

        caseDetail.setCaseDecisions(asList(dischargeDecision1));
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService
                .getCourtExtractData(caseDetail.getId());

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView dischargeDecisionViewNoPlea = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", dischargeDecisionViewNoPlea.getHeading());
        assertThat(dischargeDecisionViewNoPlea.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Guilty plea accepted"),
                new OffenceDecisionLineView("Date of verdict", formattedSavedAt),
                new OffenceDecisionLineView("Result", "Discharged absolutely"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))
                )
        );
    }

    @Test
    public void shouldVerifyForDischargeOffenceDecisionsConditionallyCharged() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();

        final CaseDecision dischargeDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        dischargeDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        dischargeDecision.getId(),
                        FOUND_GUILTY,
                        new DischargePeriod(WEEK, 10),
                        true,
                        BigDecimal.valueOf(2000.236),
                        "Limited means of defendant",
                        CONDITIONAL, null, null)));

        caseDetail.setCaseDecisions(asList(dischargeDecision));
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));

        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService
                .getCourtExtractData(caseDetail.getId());

        assertTrue(caseCourtExtractView.isPresent());
        final OffenceDecisionView dischargeDecisionViewNoPlea = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", dischargeDecisionViewNoPlea.getHeading());
        assertThat(dischargeDecisionViewNoPlea.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Guilty plea accepted"),
                new OffenceDecisionLineView("Date of verdict", formattedSavedAt),
                new OffenceDecisionLineView("Result", "Discharged conditionally"),
                new OffenceDecisionLineView("Period", "10 weeks"),
                new OffenceDecisionLineView("To pay compensation of", "£2,000.24"),
                new OffenceDecisionLineView("No compensation ordered\nbecause", "Limited means of defendant"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))
                )
        );
    }

    @Test
    public void shouldVerifyForFinancialPenaltyOffenceDecisionsConditionallyCharged() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();

        final CaseDecision financialDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        financialDecision.setOffenceDecisions(asList(
                new FinancialPenaltyOffenceDecision(offence1Id,
                        financialDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1000.987),
                        "Limited means of defendant",
                        BigDecimal.valueOf(2000.236), null, null, null, null)));

        caseDetail.setCaseDecisions(asList(financialDecision));
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));
        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService
                .getCourtExtractData(caseDetail.getId());

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView dischargeDecisionViewNoPlea = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", dischargeDecisionViewNoPlea.getHeading());
        assertThat(dischargeDecisionViewNoPlea.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Guilty plea accepted"),
                new OffenceDecisionLineView("Date of verdict", formattedSavedAt),
                new OffenceDecisionLineView("To pay a fine of", "£2,000.24"),
                new OffenceDecisionLineView("To pay compensation of", "£1,000.99"),
                new OffenceDecisionLineView("No compensation ordered\nbecause", "Limited means of defendant"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))
                )
        );
    }

    @Test
    public void shouldVerifyForExcisePenaltyAndBackDutyOffenceDecisionsConditionallyCharged() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();

        final CaseDecision financialDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        financialDecision.setOffenceDecisions(asList(
                new FinancialPenaltyOffenceDecision(offence1Id,
                        financialDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1000.987),
                        null,
                        null, BigDecimal.valueOf(123.45), BigDecimal.valueOf(234.56), null, null)));

        caseDetail.setCaseDecisions(asList(financialDecision));
        when(caseService.getCase(caseId)).thenReturn(Optional.of(caseDetail));
        final Optional<CaseCourtExtractView> caseCourtExtractView = this.courtExtractDataService
                .getCourtExtractData(caseDetail.getId());

        assertTrue(caseCourtExtractView.isPresent());

        final OffenceDecisionView dischargeDecisionViewNoPlea = caseCourtExtractView.get().getOffences().get(0).getOffenceDecisions().get(0);

        assertEquals("Court decision", dischargeDecisionViewNoPlea.getHeading());
        assertThat(dischargeDecisionViewNoPlea.getLines(), contains(
                new OffenceDecisionLineView("Plea", "No plea received"),
                new OffenceDecisionLineView("Verdict", "Guilty plea accepted"),
                new OffenceDecisionLineView("Date of verdict", formattedSavedAt),
                new OffenceDecisionLineView("To pay an excise penalty of", "£234.56"),
                new OffenceDecisionLineView("To pay back duty of", "£123.45"),
                new OffenceDecisionLineView("To pay compensation of", "£1,000.99"),
                new OffenceDecisionLineView("Defendant's guilty plea", "Taken into account when imposing sentence"),
                new OffenceDecisionLineView("Decision made", expectedDecisionMade(decisionSavedAt, false))
                )
        );
    }

    private CaseDetail buildCaseDetailWithOneOffence() {
        final CaseDetail caseDetail = aCase()
                .withCaseId(caseId)
                .withProsecutingAuthority("TFL")
                .build();

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        final List<OffenceDetail> offenceDetails = asList(buildOffenceDetailEntity(1, offence1Id));
        defendantDetail.setOffences(offenceDetails);
        caseDetail.setDefendant(defendantDetail);
        return caseDetail;
    }


}
