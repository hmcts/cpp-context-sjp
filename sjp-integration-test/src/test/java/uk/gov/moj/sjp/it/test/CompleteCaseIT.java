package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.Month.JULY;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompleteCaseIT extends BaseIntegrationTest {

    public static final String PUBLIC_SJP_ALL_OFFENCES_FOR_DEFENDANT_DISMISSED_OR_WITHDRAWN = "public.sjp.all-offences-for-defendant-dismissed-or-withdrawn";
    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final User user = new User("John", "Smith", USER_ID);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final String urn = generate(prosecutingAuthority);

    private final UUID magistrateSessionId = randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID defendantId = randomUUID();

    private final EventListener eventListener = new EventListener();
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeEach
    public void setUp() throws SQLException {
        cleanViewStore();

        stubForUserDetails(user, prosecutingAuthority.name());
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final CreateCase.CreateCasePayloadBuilder caseBuilder = withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceId(offenceId)
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);

        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        createCaseAndWaitUntilReady(caseBuilder);
    }

    @Test
    public void shouldGenerateAllOffencesWithdrawnOrDismissedEvent() {
        dismissCase();

        final Optional<JsonEnvelope> allOffencesDismissedOrWithdrawnEnvelope = eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_FOR_DEFENDANT_DISMISSED_OR_WITHDRAWN);
        assertThat(allOffencesDismissedOrWithdrawnEnvelope.isPresent(), is(true));

        final JsonEnvelope envelope = allOffencesDismissedOrWithdrawnEnvelope.get();
        assertThat(envelope,
                jsonEnvelope(
                        metadata().withName(PUBLIC_SJP_ALL_OFFENCES_FOR_DEFENDANT_DISMISSED_OR_WITHDRAWN),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.defendantId", equalTo(defendantId.toString()))
                        ))));

        final Optional<JsonEnvelope> jsonEnvelopePublicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(jsonEnvelopePublicHearingResulted.isPresent(), is(true));
    }

    @Test
    public void shouldNotGenerateAllOffencesWithdrawnOrDismissedEvent() {
        discharge();

        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_FOR_DEFENDANT_DISMISSED_OR_WITHDRAWN);
        assertThat(jsonEnvelope.isPresent(), is(false));

        final Optional<JsonEnvelope> jsonEnvelope2 = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(jsonEnvelope2.isPresent(), is(true));

        final JsonEnvelope envelope = jsonEnvelope2.get();
        assertThat(envelope,
                jsonEnvelope(
                        metadata().withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.sharedTime", is(notNullValue())),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].convictingCourt", notNullValue()),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].convictionDate", is(LocalDate.now().toString()))
                        ))));
    }

    private void dismissCase() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId(), caseId);

        final OffenceDecision offenceDecision = DismissBuilder.withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, "Test note", user, singletonList(offenceDecision), null);

        eventListener
                .subscribe(PUBLIC_SJP_ALL_OFFENCES_FOR_DEFENDANT_DISMISSED_OR_WITHDRAWN)
                .subscribe(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                .run(() -> saveDecision(decision));
    }

    private void discharge() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId(), caseId);

        final FinancialImposition financialImposition = buildFinancialImposition();
        final OffenceDecision offenceDecision = createDischarge(null, createOffenceDecisionInformation(offenceId, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false, null, null);
        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, "Test note", user, singletonList(offenceDecision), financialImposition);

        eventListener
                .subscribe(PUBLIC_SJP_ALL_OFFENCES_FOR_DEFENDANT_DISMISSED_OR_WITHDRAWN)
                .subscribe(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                .run(() -> saveDecision(decision));
    }

    private FinancialImposition buildFinancialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(40), null, new BigDecimal(100), null, "reason for reduced victim surcharge", false),
                new Payment(new BigDecimal(370), PAY_TO_COURT, "Reason for not attached", null,
                        new PaymentTerms(false,
                                new LumpSum(new BigDecimal(370), 5, LocalDate.of(2019, 7, 24)),
                                new Installments(new BigDecimal(30), InstallmentPeriod.WEEKLY, LocalDate.of(2019, 7, 23))
                        ), null)
        );
    }

    private static void assignCaseInMagistrateSession(final UUID sessionId, final UUID userId, final UUID caseId) {
        startSessionAndConfirm(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId, USER_ID, caseId);
    }

    private void createCaseAndWaitUntilReady(final CreateCase.CreateCasePayloadBuilder caseBuilder) {
        createCaseForPayloadBuilder(caseBuilder);
        pollUntilCaseReady(caseId);
    }

}
