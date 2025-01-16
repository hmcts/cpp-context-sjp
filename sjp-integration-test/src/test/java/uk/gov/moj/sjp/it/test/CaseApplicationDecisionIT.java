package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.saveApplicationDecision;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleas;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleasAsUser;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleasPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseStatusCompleted;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.builders.DismissBuilder.withDefaults;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.SetPleasHelper.SetPleasPayloadBuilder;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.PleaInfo;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseApplicationDecisionIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID appId;

    private final EventListener eventListener = new EventListener();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private final static LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final static User USER = new User("John", "Smith", randomUUID());
    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String DEFENDANT_REGION = "croydon";
    private static final String createCaseApplicationFile = "CaseApplicationIT/sjp.command.create-case-application.json";
    private static final String PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE = "public.sjp.application-decision-set-aside";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String APP_STATUS = "DRAFT";

    @BeforeEach
    public void setUp() throws SQLException {
        caseId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        appId = randomUUID();

        cleanViewStore();

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);

        setPleas(caseId, createCasePayloadBuilder.getDefendantBuilder().getId(), new PleaInfo(offenceId, GUILTY));

        startSessionAndConfirm(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId, USER.getUserId(), caseId);

        final Discharge discharge = createDischarge(null, createOffenceDecisionInformation(offenceId, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false, null, null);
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, USER, asList(discharge), financialImposition());

        saveDecision(decision);

        pollUntilCaseStatusCompleted(caseId);
    }

    @Test
    public void shouldRecordApplicationDecisionGranted() {

        createCaseApplication(USER.getUserId(), caseId, appId,
                STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                createCaseApplicationFile);

        pollUntilCaseReady(caseId);

        final UUID sessionId2 = randomUUID();
        startSessionAndConfirm(sessionId2, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId2, USER.getUserId(), caseId);

        eventListener.subscribe(
                PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE
        ).run(() -> saveApplicationDecision(USER.getUserId(), caseId, appId, sessionId2,
                true, false, null, null));

        final Optional<JsonEnvelope> publicApplicationSetAside = eventListener.popEvent(PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE);
        assertThat(publicApplicationSetAside.isPresent(), is(true));

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.caseDecisions[1].applicationDecision.granted", is(true)),
                withJsonPath("$.caseDecisions[1].applicationDecision.outOfTime", is(false)),
                withJsonPath("$.caseDecisions[1].applicationDecision.applicationType", is("STAT_DEC")),
                withJsonPath("$.caseDecisions[1].applicationDecision.previousFinalDecision", notNullValue()),
                withJsonPath("$.caseApplication.applicationStatus", is("STATUTORY_DECLARATION_GRANTED")),
                withJsonPath("$.setAside", is(true)),
                withJsonPath("$.completed", is(false)),
                withJsonPath("$.defendant.offences[0].completed", is(false)),
                withJsonPath("$.defendant.offences[0].hasFinalDecision", is(false)),
                withoutJsonPath("$.defendant.offences[0].plea"),
                withoutJsonPath("$.defendant.offences[0].pleaMethod"),
                withoutJsonPath("$.defendant.offences[0].pleaDate"),
                withoutJsonPath("$.defendant.offences[0].conviction"),
                withoutJsonPath("$.defendant.offences[0].convictionDate"),
                withoutJsonPath("$.defendant.offences[0].convictingCourt")
        ));

        final SetPleasPayloadBuilder setPleas = setPleasPayloadBuilder()
                .welshHearing(false)
                .withInterpreter(null, false)
                .withPlea(offenceId, createCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY);
        setPleasAsUser(caseId, setPleas.build(), USER.getUserId());
        pollForCase(caseId, new Matcher[]{withJsonPath("$.defendant.offences[?(@.id == '" + offenceId + "')].plea", hasItem(NOT_GUILTY.toString()))});

        final Dismiss dismiss = withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(sessionId2, caseId, null, USER, asList(dismiss), null);

        saveDecision(decision);
        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.setAside", is(false)),
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.defendant.offences[0].completed", is(true)),
                withJsonPath("$.defendant.offences[0].hasFinalDecision", is(true)),
                withJsonPath("$.defendant.offences[0].plea", is("NOT_GUILTY"))
        ));
    }

    @Test
    public void shouldRecordApplicationDecisionRefusal() {
        createCaseApplication(USER.getUserId(), caseId, appId,
                STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                createCaseApplicationFile);

        pollUntilCaseReady(caseId);

        final UUID sessionId2 = randomUUID();
        startSessionAndConfirm(sessionId2, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId2, USER.getUserId(), caseId);

        saveApplicationDecision(USER.getUserId(), caseId, appId, sessionId2,
                false, null, null, "Insufficient evidence");

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.caseDecisions[1].applicationDecision.granted", is(false)),
                withJsonPath("$.caseDecisions[1].applicationDecision.rejectionReason", is("Insufficient evidence")),
                withJsonPath("$.caseDecisions[1].applicationDecision.applicationType", is("STAT_DEC")),
                withJsonPath("$.caseDecisions[1].applicationDecision.previousFinalDecision", notNullValue()),
                withJsonPath("$.caseDecisions[1].applicationDecision.previousFinalDecisionObject", notNullValue()),
                withJsonPath("$.caseApplication.applicationStatus", is("STATUTORY_DECLARATION_REFUSED")),
                withJsonPath("$.setAside", is(false)),
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.assigned", is(false)),
                withJsonPath("$.defendant.offences[0].conviction", is("FOUND_GUILTY")),
                withJsonPath("$.defendant.offences[0].convictionDate", notNullValue()),
                withJsonPath("$.defendant.offences[0].completed", is(true)),
                withJsonPath("$.defendant.offences[0].hasFinalDecision", is(true))
        ));

        pollUntilCaseNotReady(caseId);
    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), (new CourtDetails(NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court"))
                ));
    }

}
