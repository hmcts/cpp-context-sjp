package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DefendantDetailsHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.FileUtil;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsIT extends BaseIntegrationTest{

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private DefendantDetailsHelper defendantDetailsHelper;

    @Before
    public void setUp() {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        defendantDetailsHelper = new DefendantDetailsHelper();
    }

    @Test
    public void shouldUpdateDefendantDetails()  throws IOException {
        JsonObject updatedDefendantPayload = FileUtil.givenPayload("/payload/sjp.update-defendant-details.json");
        defendantDetailsHelper.updateDefendantDetails(createCasePayloadBuilder.getId(), CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId()).getString("defendant.id"), updatedDefendantPayload);

        final JsonPath updatedCase = CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());
        final boolean nameChanged = updatedCase.getBoolean("defendant.personalDetails.nameChanged");
        final boolean dobChanged = updatedCase.getBoolean("defendant.personalDetails.dobChanged");
        final boolean addressChanged = updatedCase.getBoolean("defendant.personalDetails.addressChanged");

        assertThat(nameChanged, is(Boolean.TRUE));
        assertThat(dobChanged, is(Boolean.TRUE));
        assertThat(addressChanged, is(Boolean.TRUE));

    }


}
