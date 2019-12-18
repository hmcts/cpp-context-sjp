package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collection;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SUMRCCResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter
    public String referralReasonIdTerminalEntry;

    @Parameterized.Parameter(1)
    public String reasonPromptValue;


    @Parameterized.Parameters(name = "Terminal entry with value {0} should be converted to prompt {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"0f03e10f-d9a5-47f9-92ba-a8e220448451", "Equivocal plea - Defendant to attend to clarify plea"},
                {"33e0aeae-ef9f-40e3-8032-49d50a5a0904", "Equivocal plea - For trial"},
                {"798e3d82-44fa-40e6-a93d-4c8f5322fa66", "Defence request"},
                {"809f7aac-d285-43a5-9fb1-3a894db71530", "For trial"},
                {"9753b66a-8845-491f-a42b-7fc207ae6b1b", "For a case management hearing - Defendant to attend"},
                {"23121983-9c84-4e1e-8e5f-9b1d81124204", "For a case management hearing - No need for defendant to attend"},
                {"bc5c3ce5-6029-489f-b149-bc59efca17d1", "For sentencing hearing - defendant to attend"},
                {"cb23156c-fa9d-48d7-bac6-4d900d237ba0", "For disqualification - defendant to attend"},
                {"d10c5cc4-ec2a-41ac-bd6e-a3659c5cfeb1", "Case unsuitable for SJP"}
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertReferCaseToCCResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "SUMRCC")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", -1)
                                        .add("value", referralReasonIdTerminalEntry)
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "bca4e07c-17e0-48f1-84f4-7b6ff8bab5e2")
                        .add("value", reasonPromptValue)
                ).build();
    }

}
