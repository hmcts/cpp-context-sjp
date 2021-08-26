package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseHelper.addFinancialImpositionCorrelationId;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseCompleted;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.DischargeBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class CaseFinancialImpositionExportIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID defendantId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID correlationId;
    private String accountNumber = "123456780";

    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private final EventListener eventListener = new EventListener();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private final static LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final static User USER = new User("John", "Smith", randomUUID());
    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String DEFENDANT_REGION = "croydon";

    private static final String SJP_EVENT_FINANCIAL_IMPOSITION_CORRELATION_ID_ADDED = FinancialImpositionCorrelationIdAdded.EVENT_NAME;
    private static final String SJP_EVENT_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED = FinancialImpositionAccountNumberAdded.EVENT_NAME;
    private static final String PUBLIC_SJP_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED = "public.sjp.financial-imposition-account-number-added";

    @Before
    public void setUp() throws SQLException {
        caseId = randomUUID();
        defendantId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        correlationId = randomUUID();

        databaseCleaner.cleanViewStore();

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        AssignmentStub.stubAssignmentReplicationCommands();
        SchedulingStub.stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");
        stubResultDefinitions();
        stubResultIds();

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withDefendantId(defendantId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);

        startSession(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER.getUserId());

        final Discharge discharge = DischargeBuilder.withDefaults()
                .id(offenceId)
                .withVerdict(FOUND_GUILTY)
                .withDischargeType(CONDITIONAL)
                .withDischargedFor(new DischargePeriod(2, MONTH))
                .withCompensation(new BigDecimal(230))
                .build();
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, USER, asList(discharge), financialImposition());

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> saveDecision(decision));
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);
        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseCompleted(caseId, caseCompleted);
    }

    @Test
    public void shouldRecordCorrelationIdAndGobAccountNumber() {
        eventListener.subscribe(SJP_EVENT_FINANCIAL_IMPOSITION_CORRELATION_ID_ADDED)
                .run(() -> addFinancialImpositionCorrelationId(caseId, defendantId, correlationId));

        final Optional<JsonEnvelope> fiCorrelationIdAddedEnv = eventListener.popEvent(SJP_EVENT_FINANCIAL_IMPOSITION_CORRELATION_ID_ADDED);
        assertThat(fiCorrelationIdAddedEnv.isPresent(), is(true));
        assertThat(fiCorrelationIdAddedEnv.get(), jsonEnvelope(
                metadata().withName(SJP_EVENT_FINANCIAL_IMPOSITION_CORRELATION_ID_ADDED),
                payloadIsJson((allOf(
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.correlationId", is(correlationId.toString()))
                )
                ))));

        eventListener.subscribe(
                SJP_EVENT_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED,
                PUBLIC_SJP_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED)
                .run(this::publishStagingEnforcementAcknowledgement);

        final Optional<JsonEnvelope> fiAccountNumberAddedEnv = eventListener.popEvent(SJP_EVENT_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED);
        assertThat(fiAccountNumberAddedEnv.isPresent(), is(true));
        assertThat(fiAccountNumberAddedEnv.get(), jsonEnvelope(
                metadata().withName(SJP_EVENT_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED),
                payloadIsJson((allOf(
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.accountNumber", is(accountNumber))
                )
                ))));

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.defendant.gobAccountNumber", is(accountNumber))
        ));

        final Optional<JsonEnvelope> publicFiAccountNumberAddedEnv = eventListener
                .popEvent(PUBLIC_SJP_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED);
        assertThat(publicFiAccountNumberAddedEnv.isPresent(), is(true));

    }

    private void publishStagingEnforcementAcknowledgement() {
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.stagingenforcement.enforce-financial-imposition-acknowledgement",
                    enforceFinancialImpositionAcknowledgementPayload());
        }
    }

    private JsonObject enforceFinancialImpositionAcknowledgementPayload() {
        return createObjectBuilder()
                .add("originator", "ATCM")
                .add("requestId", correlationId.toString())
                .add("acknowledgement", createObjectBuilder()
                        .add("accountNumber", accountNumber))
                .add("exportStatus", "ENFORCEMENT_REQUEST_SELECTED")
                .add("updated", "2018-06-01T09:00:00Z")
                .build();
    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), (new CourtDetails(NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court"))
                ));
    }

}
