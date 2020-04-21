package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.*;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import com.google.common.collect.Sets;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AssignmentReplicationIT extends BaseIntegrationTest {

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private static final UUID CASE_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String NATIONAL_COURT_CODE = "1080";

    private static CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
            .withPostingDate(now().minusDays(30)).withId(CASE_ID).withOffenceId(OFFENCE_ID);

    @Before
    public void setUp() throws Exception {
        databaseCleaner.cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubGetEmptyAssignmentsByDomainObjectId(CASE_ID);
        stubAssignmentReplicationCommands();
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        createCaseAndWaitUntilReady(CASE_ID, OFFENCE_ID);
    }

    @Test
    public void shouldReplicateAssignmentEventsInAssignmentContext() {
        DecisionHelper.saveDefaultDecision(CASE_ID, OFFENCE_ID);

        AssignmentStub.verifyAddAssignmentCommandSent(CASE_ID, USER_ID, MAGISTRATE_DECISION);
        AssignmentStub.verifyRemoveAssignmentCommandSend(CASE_ID);
    }

    private static void createCaseAndWaitUntilReady(final UUID caseId, final UUID offenceId) {
        try (final MessageConsumerClient messageConsumerClient = new MessageConsumerClient()) {
            CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
            messageConsumerClient.startConsumer(CaseMarkedReadyForDecision.EVENT_NAME, "sjp.event");
            messageConsumerClient.retrieveMessage();
        }
    }
}
