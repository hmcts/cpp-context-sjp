package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.DefendantDetailsHelper;
import uk.gov.moj.sjp.it.util.FileUtil;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SearchCasesIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;
    private CaseSearchResultHelper caseSearchResultHelper;
    private DefendantDetailsHelper defendantDetailsHelper;

    @Before
    public void createSjpCaseAndVerifyInQueue() {
        defendantDetailsHelper = new DefendantDetailsHelper();
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        //This is required, otherwise get method for defendant id can't be invoked
        caseSjpHelper.verifyCaseCreatedUsingId();

        caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper);
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
        caseSearchResultHelper.close();
        defendantDetailsHelper.close();
    }

    @Test
    public void verifyInitialSearchDetailsAndUpdateToDefendantDetails() throws IOException {
        caseSearchResultHelper.verifyPersonInfoByUrn();
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(caseSearchResultHelper.getLastName(), caseSearchResultHelper.getDateOfBirth());

        JsonObject updatedDefendantPayload = FileUtil.givenPayload("/payload/sjp.update-defendant-details.json");

        defendantDetailsHelper.updateDefendantDetails(caseSjpHelper.getCaseId(), caseSjpHelper.getSingleDefendantId(), updatedDefendantPayload);
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth("SMITH", LocalDates.from("1980-07-15"));
        caseSearchResultHelper.verifyPersonNotFound(caseSjpHelper.getCaseUrn(), caseSearchResultHelper.getLastName());

        final JsonPath updatedCase = caseSjpHelper.getCaseResponseUsingId();
        final String firstName = updatedCase.getString("defendant.personalDetails.firstName");
        final String lastName = updatedCase.getString("defendant.personalDetails.lastName");

        assertThat(firstName, is("David"));
        assertThat(lastName, is("SMITH"));
    }

    @Test
    public void verifyAssignmentCreationAndDeletionIsReflected() {
        //given case is created
        // then
        caseSearchResultHelper.verifyAssignment(false);

        // when
        caseSearchResultHelper.assignmentCreated();
        // then
        caseSearchResultHelper.verifyAssignment(true);

        // when
        caseSearchResultHelper.assignmentDeleted();
        // then
        caseSearchResultHelper.verifyAssignment(false);
    }
}
