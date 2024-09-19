package uk.gov.moj.sjp.it.test;

import static java.time.Month.JULY;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.Constants.OFFENCE_DATE_CODE_FOR_BETWEEN;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder;
import uk.gov.moj.sjp.it.util.builders.FinancialPenaltyBuilder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseResultsIT extends BaseIntegrationTest {

    private static final int POINTS = 2;
    private static final String NATIONAL_COURT_CODE = "1080";

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

    @BeforeEach
    public void beforeEveryTest() throws SQLException {
        databaseCleaner.cleanViewStore();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubResultIds();
        stubFixedLists();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        stubBailStatuses();
        stubResultIds();

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));


        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceId(offenceId)
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withLibraOffenceDateCode(OFFENCE_DATE_CODE_FOR_BETWEEN)
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
    public void testCaseResults() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().id(offenceId).build();
        final List<FinancialPenalty> offencesDecisions = singletonList(financialPenalty);
        final FinancialImposition financialImposition = FinancialImpositionBuilder.withDefaults();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, laUser, offencesDecisions, financialImposition);


        // When
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe("public.events.hearing.hearing-resulted")
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then

        final Optional<JsonEnvelope> jsonEnvelopePublicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        final JsonObject hearingResultedPayload = jsonEnvelopePublicHearingResulted.get().payloadAsJsonObject();
        final JsonArray prosecutionCasesArray = hearingResultedPayload.getJsonObject("hearing").getJsonArray("prosecutionCases");
        final JsonObject convictingCourt = prosecutionCasesArray.getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonObject("convictingCourt");
        final String convictingDate = prosecutionCasesArray.getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("convictionDate");
        assertThat(!convictingCourt.isEmpty(), is(true));
        assertThat(convictingCourt.getString("code"), is("B01LY"));
        assertThat(!convictingDate.isEmpty(), is(true));
        assertThat(convictingDate, is(LocalDate.now().toString()));


    }

    @Test
    public void testCaseResultsForEndorsement() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults()
                .id(offenceId)
                .penaltyPointsImposedWithReason(POINTS, PenaltyPointsReason.DIFFERENT_OCCASIONS)
                .build();
        final List<FinancialPenalty> offencesDecisions = singletonList(financialPenalty);
        final FinancialImposition financialImposition = FinancialImpositionBuilder.withDefaults();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, laUser, offencesDecisions, financialImposition);

        // When
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe("public.events.hearing.hearing-resulted")
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then

        final Optional<JsonEnvelope> jsonEnvelopePublicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(jsonEnvelopePublicHearingResulted.isPresent(), is(true));
    }

    @Test
    public void testCaseResultsForDisqualification() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults()
                .id(offenceId)
                .disqualificationType(DisqualificationType.DISCRETIONARY)
                .disqualificationPeriodInMonths(2)
                .build();
        final FinancialImposition financialImposition = FinancialImpositionBuilder.withDefaults();
        final List<FinancialPenalty> offencesDecisions = singletonList(financialPenalty);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, laUser, offencesDecisions, financialImposition);

        // When
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe("public.events.hearing.hearing-resulted")
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        // Then

        final Optional<JsonEnvelope> jsonEnvelopePublicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(jsonEnvelopePublicHearingResulted.isPresent(), is(true));
    }


}
