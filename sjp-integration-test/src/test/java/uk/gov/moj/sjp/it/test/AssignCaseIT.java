package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.assignCaseToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;

import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import uk.gov.moj.sjp.it.helper.EventListener;

import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Enable this when merging to master")
public class AssignCaseIT extends BaseIntegrationTest {

    private final UUID caseId1 = randomUUID();
    private final UUID caseId2 = randomUUID();

    private UUID systemUserId = randomUUID();
    private UUID legalAdviserId = randomUUID();
    private UUID courtAdminId = randomUUID();
    private UUID prosecutorId = randomUUID();
    private static final String DEFENDANT_REGION = "london";
    private static final String NATIONAL_COURT_CODE = "1080";

    @Before
    public void setUp() {
        stubGroupForUser(systemUserId, "System Users");
        stubGroupForUser(legalAdviserId, "Legal Advisers");
        stubGroupForUser(courtAdminId, "Court Administrators");
        stubGroupForUser(prosecutorId, "SJP Prosecutors");

        final CreateCasePayloadBuilder createCasePayloadBuilder = defaultCaseBuilder();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseAndWaitUntilReady(defaultCaseBuilder().withId(caseId1));
        createCaseAndWaitUntilReady(defaultCaseBuilder().withId(caseId2));
    }

    @Test
    public void shouldAssignReadyCaseToUser() {
        pollUntilCaseNotAssignedToUser(caseId1, systemUserId);
        pollUntilCaseNotAssignedToUser(caseId2, systemUserId);

        assignCaseToUser(caseId1, legalAdviserId, systemUserId, ACCEPTED);

        pollUntilCaseAssignedToUser(caseId1, legalAdviserId);
        pollUntilCaseNotAssignedToUser(caseId2, legalAdviserId);

        assignCaseToUser(caseId2, legalAdviserId, systemUserId, ACCEPTED);

        pollUntilCaseNotAssignedToUser(caseId1, legalAdviserId);
        pollUntilCaseAssignedToUser(caseId2, legalAdviserId);
    }

    @Test
    public void shouldRejectAssignmentRequestedByNonSystemUser() {
        assignCaseToUser(caseId1, legalAdviserId, legalAdviserId, FORBIDDEN);
        assignCaseToUser(caseId1, legalAdviserId, courtAdminId, FORBIDDEN);
        assignCaseToUser(caseId1, legalAdviserId, prosecutorId, FORBIDDEN);
    }

    private static void createCaseAndWaitUntilReady(final CreateCasePayloadBuilder createCasePayloadBuilder) {
        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

}