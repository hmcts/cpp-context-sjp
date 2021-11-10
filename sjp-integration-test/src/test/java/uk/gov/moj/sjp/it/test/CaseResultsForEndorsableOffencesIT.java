package uk.gov.moj.sjp.it.test;

import static java.time.Month.JULY;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason.DIFFERENT_OCCASIONS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;
import static uk.gov.moj.sjp.it.util.matchers.ResultsMatcher.FO;
import static uk.gov.moj.sjp.it.util.matchers.ResultsMatcher.LEA;
import static uk.gov.moj.sjp.it.util.matchers.ResultsMatcher.LEN;
import static uk.gov.moj.sjp.it.util.matchers.ResultsMatcher.LEP;
import static uk.gov.moj.sjp.it.util.matchers.ResultsMatcher.NCR;
import static uk.gov.moj.sjp.it.util.matchers.ResultsMatcher.NSP;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.resulting.Offence;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.JsonHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.FinancialPenaltyBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class CaseResultsForEndorsableOffencesIT extends BaseIntegrationTest {

    private static final String NATIONAL_COURT_CODE = "1080";
    private static final boolean GUILTY_PLEA_TAKEN_INTO_ACCOUNT_FALSE = false;
    private static final boolean LICENCE_ENDORSEMENT_TRUE = true;
    private static final Boolean LICENCE_ENDORSEMENT_FALSE = false;
    private static final int POINTS_IMPOSED = 2;
    private final User user = new User("Integration", "Tester", DEFAULT_USER_ID);
    private final EventListener eventListener = new EventListener();
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final User laUser = new User("John", "Smith", USER_ID);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final String urn = generate(prosecutingAuthority);

    private final UUID caseId = UUID.randomUUID();
    private final UUID offenceId1 = randomUUID();
    private final UUID offenceId2 = randomUUID();
    private final UUID offenceId3 = randomUUID();
    private final UUID defendantId = randomUUID();
    private UUID sessionId = randomUUID();

    public static final String PUBLIC_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    private static JsonObject getCaseResults(final UUID caseId, final UUID userId) {
        final String url = String.format("/cases/%s/results", caseId);
        final Response response = makeGetCall(url, "application/vnd.sjp.query.case-results+json", userId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        return JsonHelper.getJsonObject(response.readEntity(String.class));
    }

    @Before
    public void beforeEveryTest() throws SQLException {
        databaseCleaner.cleanViewStore();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubAllReferenceData();
        stubResultIds();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, prosecutingAuthority.name());
        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", false);
        FeatureStubber.stubFeaturesFor("sjp", features);

        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceBuilders(
                        endorsableOffenceWithId(offenceId1),
                        endorsableOffenceWithId(offenceId2),
                        endorsableOffenceWithId(offenceId3)
                )
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

    @Test
    public void resultsForNoFinancialPenaltyWithEndorsementAndNoPenaltyPoints() {
        // GIVEN
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().id(offenceId1)
                .licenceEndorsed(true).build();
        final NoSeparatePenalty noSeparatePenalty1 = noSeparatePenalty(offenceId2, LICENCE_ENDORSEMENT_TRUE);
        final NoSeparatePenalty noSeparatePenalty2 = noSeparatePenalty(offenceId3, LICENCE_ENDORSEMENT_FALSE);
        final List<OffenceDecision> offencesDecisions = asList(financialPenalty, noSeparatePenalty1, noSeparatePenalty2);
        sendDecisionCommand(offencesDecisions);

        // WHEN
        final JsonObject payload = getCaseResults(caseId, laUser.getUserId());

        // THEN
        final Offence offence1Results = getOffenceResultsById(offenceId1, payload);
        assertThat(offence1Results.getResults(), containsInAnyOrder(
                FO(financialPenalty.getFine().toString()),
                NCR(financialPenalty.getNoCompensationReason()),
                LEN())
        );

        final Offence offence2Results = getOffenceResultsById(offenceId2, payload);
        assertThat(offence2Results.getResults(), containsInAnyOrder(LEN(), NSP()));

        final Offence offence3Results = getOffenceResultsById(offenceId3, payload);
        assertThat(offence3Results.getResults(), containsInAnyOrder(NSP()));
    }

    @Test
    public void resultsForFinancialPenaltiesWithPoints() {
        // GIVEN
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().id(offenceId1)
                .licenceEndorsed(true)
                .penaltyPointsImposed(POINTS_IMPOSED).build();
        final NoSeparatePenalty noSeparatePenalty1 = noSeparatePenalty(offenceId2, LICENCE_ENDORSEMENT_FALSE);
        final NoSeparatePenalty noSeparatePenalty2 = noSeparatePenalty(offenceId3, LICENCE_ENDORSEMENT_FALSE);
        final List<OffenceDecision> offencesDecisions = asList(financialPenalty, noSeparatePenalty1, noSeparatePenalty2);
        sendDecisionCommand(offencesDecisions);

        // WHEN
        final JsonObject payload = getCaseResults(caseId, laUser.getUserId());

        // THEN
        final Offence offence1Results = getOffenceResultsById(offenceId1, payload);
        assertThat(offence1Results.getResults(), containsInAnyOrder(
                FO(financialPenalty.getFine().toString()),
                NCR(financialPenalty.getNoCompensationReason()),
                LEP(POINTS_IMPOSED))
        );
    }

    @Test
    public void resultsForFinancialPenaltyWithAdditionalPointsAndReason() {

        // GIVEN
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().id(offenceId1)
                .licenceEndorsed(true)
                .penaltyPointsImposedWithReason(POINTS_IMPOSED, DIFFERENT_OCCASIONS).build();
        final NoSeparatePenalty noSeparatePenalty1 = noSeparatePenalty(offenceId2, LICENCE_ENDORSEMENT_FALSE);
        final NoSeparatePenalty noSeparatePenalty2 = noSeparatePenalty(offenceId3, LICENCE_ENDORSEMENT_FALSE);
        final List<OffenceDecision> offencesDecisions = asList(financialPenalty, noSeparatePenalty1, noSeparatePenalty2);
        sendDecisionCommand(offencesDecisions);

        // WHEN
        final JsonObject payload = getCaseResults(caseId, laUser.getUserId());

        // THEN
        final Offence offence1Results = getOffenceResultsById(offenceId1, payload);
        assertThat(offence1Results.getResults(), containsInAnyOrder(
                FO(financialPenalty.getFine().toString()),
                NCR(financialPenalty.getNoCompensationReason()),
                LEA(POINTS_IMPOSED))
        );
    }

    private void sendDecisionCommand(final List<OffenceDecision> offencesDecisions) {
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, laUser, offencesDecisions, financialImposition());
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe(PUBLIC_HEARING_RESULTED)
                .run(() -> DecisionHelper.saveDecision(decision));

        final Optional<JsonEnvelope> publicHearingResulted = eventListener.popEvent(PUBLIC_HEARING_RESULTED);
        final JsonObject hearingResultedPayload = publicHearingResulted.get().payloadAsJsonObject();
        final JsonArray prosecutionCasesArray = hearingResultedPayload.getJsonObject("hearing").getJsonArray("prosecutionCases");
        final JsonArray offences = prosecutionCasesArray.getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(offences.size(), Matchers.is(3));
        offences.forEach(item -> {
            JsonObject obj = (JsonObject) item;
            final JsonObject convictingCourtObj = obj.getJsonObject("convictingCourt");
            assertThat(!convictingCourtObj.isEmpty(), Matchers.is(true));
            assertThat(convictingCourtObj.getString("code"),Matchers.is("B01LY"));
            final String convictingDate = obj.getString("convictionDate");
            assertThat(!convictingDate.isEmpty(), Matchers.is(true));
            assertThat(convictingDate, Matchers.is(LocalDate.now().toString()));
        });
    }

    private Offence getOffenceResultsById(final UUID offenceId, final JsonObject payload) {
        final JsonArray offences = payload.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences");

        final List<Offence> offenceResults = offences.stream()
                .map(offence -> JsonHelper.fromJsonString(offence.toString(), Offence.class))
                .filter(offence -> Objects.equals(offence.getId(), offenceId))
                .collect(toList());

        assertThat(offenceResults, hasSize(1));

        return offenceResults.get(0);
    }

    private CreateCase.OffenceBuilder endorsableOffenceWithId(final UUID offenceId) {
        return CreateCase.OffenceBuilder.withDefaults().withEndorsable(true).withId(offenceId);
    }

    private NoSeparatePenalty noSeparatePenalty(final UUID id, final boolean licenceEndorsed) {
        return new NoSeparatePenalty(null,
                createOffenceDecisionInformation(id, VerdictType.PROVED_SJP),
                GUILTY_PLEA_TAKEN_INTO_ACCOUNT_FALSE,
                licenceEndorsed);
    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), null
                ));
    }
}
