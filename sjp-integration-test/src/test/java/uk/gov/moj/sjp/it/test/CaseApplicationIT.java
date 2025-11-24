
package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.assignCaseToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseStatusCompleted;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.publishNotificationSentPublicEvent;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorSent;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
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

public class CaseApplicationIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID appId;
    private final UUID systemUserId = randomUUID();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private final EventListener eventListener = new EventListener();
    private final static LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final static User USER = new User("John", "Smith", randomUUID());
    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String DEFENDANT_REGION = "croydon";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final UUID REOPENING_TYPE_ID = fromString("44c238d9-3bc2-3cf3-a2eb-a7d1437b8383");
    private static final String REOPENING_TYPE_CODE = "MC80524";
    private static final String APP_STATUS = "DRAFT";
    private static final String createCaseApplicationFile = "CaseApplicationIT/sjp.command.create-case-application.json";

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
        stubGetFromIdMapper(PARTIAL_AOCP_CRITERIA_NOTIFICATION.name(), caseId.toString(),
                "CASE_ID", caseId.toString());
        stubAddMapping();

        createCasePayloadBuilder = withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);

        startSessionAndConfirm(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId, USER.getUserId(), caseId);

        final Discharge discharge = createDischarge(null, createOffenceDecisionInformation(offenceId, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false, null, null);
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, USER, asList(discharge), financialImposition());
        saveDecision(decision);
        pollUntilCaseStatusCompleted(caseId);
    }

    @Test
    public void shouldCreateStatDecApplicationAndResumeNormalOperations() {
        createCaseApplication(USER.getUserId(), caseId, appId,
                STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                createCaseApplicationFile);

        Matcher[] applicationMatchers = new Matcher[]{
                withJsonPath("$.caseApplication.applicationId", equalTo(appId.toString())),
                withJsonPath("$.caseApplication.dateReceived", equalTo(DATE_RECEIVED.toString())),
                withJsonPath("$.caseApplication.initiatedApplication.applicationReceivedDate", equalTo(DATE_RECEIVED.toString())),
                withJsonPath("$.caseApplication.initiatedApplication.applicationStatus", equalTo(APP_STATUS)),
                withJsonPath("$.caseApplication.initiatedApplication.applicationReference", notNullValue()),
                withJsonPath("$.caseApplication.initiatedApplication.type.code", equalTo(STAT_DEC_TYPE_CODE)),
                withJsonPath("$.caseApplication.initiatedApplication.applicant", notNullValue()),
                withJsonPath("$.defendant.defendantDetailUpdateRequest.address.address1", is("Test One")),
                withJsonPath("$.defendant.defendantDetailUpdateRequest.addressUpdated", is(true)),
                withJsonPath("$.readyForDecision", is(true)),
                withJsonPath("$.status", is(COMPLETED_APPLICATION_PENDING.name())),
        };

        pollForCase(caseId, applicationMatchers);

        pollUntilCaseReady(caseId);

        pollUntilCaseNotAssignedToUser(caseId, systemUserId);

        assignCaseToUser(caseId, USER.getUserId(), systemUserId, ACCEPTED);
        pollUntilCaseAssignedToUser(caseId, USER.getUserId());

        eventListener.subscribe(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME)
                .run(() -> publishNotificationSentPublicEvent(caseId));

        final Optional<JsonEnvelope> emailEvent = eventListener.popEvent(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME);
        assertThat(emailEvent.isPresent(), is(true));
    }

    @Test
    public void shouldRecordCaseFOR_Reopening_application() {

        createCaseApplication(USER.getUserId(), caseId, appId,
                REOPENING_TYPE_ID, REOPENING_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS, createCaseApplicationFile);

        Matcher[] applicationMatchers = new Matcher[]{
                withJsonPath("$.caseApplication.applicationId", equalTo(appId.toString())),
                withJsonPath("$.caseApplication.initiatedApplication.applicant", notNullValue())
        };

        pollForCase(caseId, applicationMatchers);
    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), (new CourtDetails(NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court"))
                ));
    }

}

