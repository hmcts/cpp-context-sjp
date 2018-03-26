package uk.gov.moj.sjp.it.test;

import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.test.UpdatePleaIT.PLEA_NOT_GUILTY;
import static uk.gov.moj.sjp.it.test.UpdatePleaInterpreterIT.getPleaPayload;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseCourtReferralHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CourtReferralIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private UpdatePleaHelper updatePleaHelper;

    private UUID caseId;

    private final String interpreterLanguage = RandomStringUtils.randomAlphabetic(12);

    @Before
    public void createCaseSearchResultAndAddInterpreterLanguage() {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        caseId = createCasePayloadBuilder.getId();

        // needed before adding an interpreter language
        stubGetCaseDecisionsWithNoDecision(caseId);
        stubGetEmptyAssignmentsByDomainObjectId(caseId);

        // case needs to be created before adding an interpreter language
        updatePleaHelper = new UpdatePleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId());
        updatePleaHelper.updatePlea(getPleaPayload(PLEA_NOT_GUILTY, true, interpreterLanguage));
    }

    @After
    public void tearDown() {
        updatePleaHelper.close();
    }

    @Test
    public void shouldCreateActionAndQueryCourtReferral() {
        try (final CaseCourtReferralHelper caseCourtReferralHelper = new CaseCourtReferralHelper()) {
            caseCourtReferralHelper.createCourtReferral(caseId.toString());
            caseCourtReferralHelper.verifyCaseCourtReferral(caseId.toString(), createCasePayloadBuilder.getUrn(),
                    "David", "LLOYD", interpreterLanguage);

            caseCourtReferralHelper.actionCourtReferral(caseId.toString());
            caseCourtReferralHelper.verifyCaseCourtReferralActioned(caseId.toString());
            caseCourtReferralHelper.verifyPublicTopicEvent(caseId.toString());
        }
    }

}
