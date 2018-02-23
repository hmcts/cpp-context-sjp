package uk.gov.moj.sjp.it.test;

import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.test.UpdatePleaIT.PLEA_NOT_GUILTY;
import static uk.gov.moj.sjp.it.test.UpdatePleaInterpreterIT.getPleaPayload;

import uk.gov.moj.sjp.it.helper.CaseCourtReferralHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CourtReferralIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;
    private UpdatePleaHelper updatePleaHelper;

    private String caseId;

    private final String interpreterLanguage = RandomStringUtils.randomAlphabetic(12);

    @Before
    public void createCaseSearchResultAndAddInterpreterLanguage() {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseId = caseSjpHelper.getCaseId();

        // needed before adding an interpreter language
        stubGetCaseDecisionsWithNoDecision(caseId);
        stubGetEmptyAssignmentsByDomainObjectId(caseId);

        // case needs to be created before adding an interpreter language
        caseSjpHelper.verifyCaseCreatedUsingId();
        updatePleaHelper = new UpdatePleaHelper(caseSjpHelper);
        updatePleaHelper.updatePlea(getPleaPayload(PLEA_NOT_GUILTY, true, interpreterLanguage));
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
        updatePleaHelper.close();
    }

    @Test
    public void shouldCreateActionAndQueryCourtReferral() {
        try (final CaseCourtReferralHelper caseCourtReferralHelper = new CaseCourtReferralHelper()) {
            caseCourtReferralHelper.createCourtReferral(caseId);
            caseCourtReferralHelper.verifyCaseCourtReferral(caseId, caseSjpHelper.getCaseUrn(),
                    "David", "LLOYD", interpreterLanguage);

            caseCourtReferralHelper.actionCourtReferral(caseId);
            caseCourtReferralHelper.verifyCaseCourtReferralActioned(caseId);
            caseCourtReferralHelper.verifyPublicTopicEvent(caseId);
        }
    }

}
