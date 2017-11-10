package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SearchCasesIT extends BaseIntegrationTest {

    private static CaseSjpHelper caseSjpHelper;
    private static CaseSearchResultHelper caseSearchResultHelper;

    @Before
    public void createSjpCaseAndVerifyInQueue() {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        //TODO: looks like we are testing case creation not search here
        caseSjpHelper.verifyInPrivateActiveMQ();

        caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper);
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
        caseSearchResultHelper.close();
    }

    @Test
    public void verifyAddAndUpdatePersonInfo() {
        caseSearchResultHelper.addPersonInfo();
        caseSearchResultHelper.verifyPersonInfoByUrn();
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(caseSearchResultHelper.getLastName(), caseSearchResultHelper.getDateOfBirth());

        caseSearchResultHelper.updatePersonInfo();
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(caseSearchResultHelper.getUpdatedLastName(), caseSearchResultHelper.getUpdatedDateOfBirth());
        caseSearchResultHelper.verifyPersonNotFound(caseSearchResultHelper.getLastName());
    }

}
