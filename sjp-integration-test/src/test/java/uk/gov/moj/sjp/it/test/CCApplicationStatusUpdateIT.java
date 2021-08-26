package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.Month.JULY;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class CCApplicationStatusUpdateIT extends BaseIntegrationTest  {

    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final User user = new User("John", "Smith", USER_ID);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final String urn = generate(prosecutingAuthority);

    private final UUID magistrateSessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final  UUID appId = randomUUID();

    private final EventListener eventListener = new EventListener();
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String REOPENING_TYPE_CODE = "MC80524";

    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);




    @Before
    public void setUp() throws SQLException {
        databaseCleaner.cleanViewStore();
        stubFixedLists();
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        createCase();
        dismissCase();
    }

    @Test
    public void  should_createCCApplicationStatus_WhenCCApplicationCreated () {

        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", "")
                        .put("APPLICATION_TYPE", "Appeal against Sentence")
                        .put("LINK_TYPE", "SJP")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.progression.court-application-created", payload);
        }

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.APPEAL_PENDING.name()))));



    }

    @Test
    public void  should_createCCApplicationStatus_WhenCCApplicationCreatedWithAppearanceToMakeStatutoryDeclaration () {

        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", REOPENING_TYPE_CODE)
                        .put("APPLICATION_TYPE", "Application to reopen case")
                        .put("LINK_TYPE", "LINKED")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.progression.court-application-created", payload);
        }

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.REOPENING_PENDING.name()))));

    }

    @Test
    public void  should_createCCApplicationStatus_WhenCCApplicationToReopenCase () {

        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", STAT_DEC_TYPE_CODE)
                        .put("APPLICATION_TYPE", "Appearance to make statutory declaration")
                        .put("LINK_TYPE", "LINKED")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.progression.court-application-created", payload);
        }

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_PENDING.name()))));

    }

    private void createCase() {
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubResultDefinitions();
        stubResultIds();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");

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
                .withUrn(urn);

        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        createCaseAndWaitUntilReady(caseBuilder);
    }

    private void createCaseAndWaitUntilReady(final CreateCase.CreateCasePayloadBuilder caseBuilder) {
        new EventListener().subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(caseBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

    private static void assignCaseInMagistrateSession(final UUID sessionId, final UUID userId) {
        startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER_ID);
    }


    private void dismissCase() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId());

        final OffenceDecision offenceDecision = DismissBuilder.withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, "Test note", user, asList(offenceDecision), null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn")
                .subscribe("public.hearing.resulted")
                .subscribe(FinancialMeansDeleteDocsStarted.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));
    }

}
