package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsIT extends BaseIntegrationTest{

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @Test
    public void shouldUpdateDefendantDetails()  {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(createCasePayloadBuilder.getId(), UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId()).getString("defendant.id")), payloadBuilder);

        final JsonPath updatedCase = CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());
        final boolean nameChanged = updatedCase.getBoolean("defendant.personalDetails.nameChanged");
        final boolean dobChanged = updatedCase.getBoolean("defendant.personalDetails.dobChanged");
        final boolean addressChanged = updatedCase.getBoolean("defendant.personalDetails.addressChanged");

        assertThat(nameChanged, is(Boolean.TRUE));
        assertThat(dobChanged, is(Boolean.TRUE));
        assertThat(addressChanged, is(Boolean.TRUE));

    }


}
