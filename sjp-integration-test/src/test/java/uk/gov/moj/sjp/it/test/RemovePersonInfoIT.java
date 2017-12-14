package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;

import uk.gov.moj.sjp.it.helper.AddPersonInfoHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.RemovePersonInfoHelper;

import org.junit.Before;
import org.junit.Test;

public class RemovePersonInfoIT extends BaseIntegrationTest {

    private RemovePersonInfoHelper duplicatePersonInfoHelper;

    private AddPersonInfoHelper addPersonInfoHelper;

    private CaseSjpHelper caseSjpHelper;

    private String personInfoId;

    @Before
    public void setUp() {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();

        try (final AddPersonInfoHelper addPersonInfoHelper = new AddPersonInfoHelper(caseSjpHelper)) {
            addPersonInfoHelper.addPersonInfo(randomUUID().toString());
            addPersonInfoHelper.verifyInActiveMQ();
            addPersonInfoHelper.verifyPersonInfoAdded(1);
            personInfoId = randomUUID().toString();
            addPersonInfoHelper.addPersonInfo(personInfoId);
            addPersonInfoHelper.verifyPersonInfoAdded(2);
        }
    }

    @Test
    public void verifyEventPersonInfoRemovedCreated() {
        try (final RemovePersonInfoHelper removePersonInfoHelper = new RemovePersonInfoHelper(caseSjpHelper, personInfoId)) {
            removePersonInfoHelper.addRemovePersonInfo();;
            removePersonInfoHelper.verifyInActiveMQ();
            removePersonInfoHelper.verifyCaseSearchResultsCount(1);
        }
    }
}
