package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.time.Month.JULY;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.*;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.JsonHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseResultsIT extends BaseIntegrationTest {

    private final User user = new User("Integration", "Tester", DEFAULT_USER_ID);
    private final EventListener eventListener = new EventListener();
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private UUID sessionId = randomUUID();

    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final User laUser = new User("John", "Smith", USER_ID);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final String urn = generate(prosecutingAuthority);

    private final UUID caseId = UUID.randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID defendantId = randomUUID();

    private static final String NATIONAL_COURT_CODE = "1080";

    @Before
    public void beforeEveryTest() throws SQLException {

        databaseCleaner.cleanAll();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubResultDefinitions();
        stubResultIds();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, prosecutingAuthority.name());

        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceId(offenceId)
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);

        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(caseBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);

        startSession(sessionId, laUser.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER_ID);
    }

    private static JsonObject getCaseResults(final UUID caseId, final UUID userId, final Response.Status expectedStatus) {
        final String url = String.format("/cases/%s/results", caseId);
        final Response response = makeGetCall(url, "application/vnd.sjp.query.case-results+json", userId);
        assertThat(response.getStatus(), is(expectedStatus.getStatusCode()));
        return JsonHelper.getJsonObject(response.readEntity(String.class));
    }

    @Test
    public void testCaseResults() {

        final FinancialPenalty financialPenalty = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offenceId, PROVED_SJP),
                BigDecimal.TEN, BigDecimal.ZERO, "Not Requested", false);

        final List<FinancialPenalty> offencesDecisions = Collections.singletonList(financialPenalty);

        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), null
                ));
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, laUser, offencesDecisions, financialImposition);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final JsonObject payload = getCaseResults(caseId, laUser.getUserId(), Response.Status.OK);

        Matcher<? super Object> allMatchers = allOf(
                hasJsonPath("$.caseId", equalTo(caseId.toString())),
                hasJsonPath("$.caseDecisions[0].sjpSessionId", equalTo(sessionId.toString())),
                hasJsonPath("$.caseDecisions[0].offences[*]", hasSize(greaterThan(0))),
                hasJsonPath("$.caseDecisions[0].offences[0].id", equalTo(offenceId.toString()))
        );

        assertThat(payload.toString(), allMatchers);

    }
}
