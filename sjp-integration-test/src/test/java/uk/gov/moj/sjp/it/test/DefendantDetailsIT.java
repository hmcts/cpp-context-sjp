package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.DefendantDetailsHelper;
import uk.gov.moj.sjp.it.util.FileUtil;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsIT extends BaseIntegrationTest{

    private CaseSjpHelper caseSjpHelper;
    private DefendantDetailsHelper defendantDetailsHelper;

    @Before
    public void setUp() {
        caseSjpHelper = new CaseSjpHelper();
        defendantDetailsHelper = new DefendantDetailsHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
    }

    @After
    public void tearDown() throws Exception {
        caseSjpHelper.close();
        defendantDetailsHelper.close();
    }

    @Test
    public void shouldUpdateDefendantDetails()  throws IOException {
        JsonObject updatedDefendantPayload = FileUtil.givenPayload("/payload/sjp.update-defendant-details.json");
        defendantDetailsHelper.updateDefendantDetails(caseSjpHelper.getCaseId(), caseSjpHelper.getSingleDefendantId(), updatedDefendantPayload);

        final JsonPath updatedCase = caseSjpHelper.getCaseResponseUsingId();
        final boolean nameChanged = updatedCase.getBoolean("defendant.personalDetails.nameChanged");
        final boolean dobChanged = updatedCase.getBoolean("defendant.personalDetails.dobChanged");
        final boolean addressChanged = updatedCase.getBoolean("defendant.personalDetails.addressChanged");

        assertThat(nameChanged, is(Boolean.TRUE));
        assertThat(dobChanged, is(Boolean.TRUE));
        assertThat(addressChanged, is(Boolean.TRUE));

    }


}
