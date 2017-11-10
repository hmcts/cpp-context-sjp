package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import org.junit.Test;

/**
 * Integration test to create a case and verify the case can be read using ID and URN
 */
public class CreateCaseIT extends BaseIntegrationTest{

    @Test
    public void shouldAssociateEnterpriseIdWithCase() throws Exception {
        CaseSjpHelper caseHelper = new CaseSjpHelper();
        caseHelper.createCase();
        caseHelper.verifyInPrivateActiveMQ();
        caseHelper.verifyCaseCreatedUsingId();
        caseHelper.verifyCaseCreatedUsingUrn();

        caseHelper.associateEnterpriseIdWIthCase();
        caseHelper.verifyEnterpriseIdAssociatedWithCase("2K2SLYFC743H");
    }
}
