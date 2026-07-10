package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.DDP_DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.core.courts.JudicialResultPrompt;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JudicialResultHelperTest {

    private static final String RESULT_DEFINITION_FILE = "ddq-result-definition.json";
    private static final UUID DISQUALIFICATION_PERIOD_PROMPT_ID = fromString("2bf54447-328c-4c1b-a123-341adbd52172");

    private JsonObject resultDefinition;

    @BeforeEach
    public void setUp() {
        resultDefinition = getFileContentAsJson(RESULT_DEFINITION_FILE, new HashMap<>());
    }

    @Test
    public void shouldPopulatePromptDefinitionAttributesForYearsDuration() {
        final JudicialResultPrompt.Builder builder = JudicialResultHelper.populatePromptDefinitionAttributesBasedOnDuration(
                DDP_DISQUALIFICATION_PERIOD, resultDefinition, "Year(s)");

        final JudicialResultPrompt prompt = builder.build();

        assertThat(prompt.getJudicialResultPromptTypeId(), is(DISQUALIFICATION_PERIOD_PROMPT_ID));
        assertThat(prompt.getLabel(), is("Disqualification period"));
        assertThat(prompt.getWelshLabel(), is("Cyfnod gwahardd"));
        assertThat(prompt.getPromptReference(), is("disqualificationPeriod"));
        assertThat(prompt.getType(), is("INTM"));
        assertThat(prompt.getCourtExtract(), is("Y"));
        assertThat(prompt.getDurationSequence(), is(1));
        assertThat(prompt.getPromptSequence(), is(new BigDecimal(100)));
        assertThat(prompt.getIsFinancialImposition(), is(false));
    }

    @Test
    public void shouldPopulatePromptDefinitionAttributesForMonthsDuration() {
        final JudicialResultPrompt.Builder builder = JudicialResultHelper.populatePromptDefinitionAttributesBasedOnDuration(
                DDP_DISQUALIFICATION_PERIOD, resultDefinition, "MONTH");

        final JudicialResultPrompt prompt = builder.build();

        assertThat(prompt.getJudicialResultPromptTypeId(), is(DISQUALIFICATION_PERIOD_PROMPT_ID));
        assertThat(prompt.getLabel(), is("Disqualification period"));
        assertThat(prompt.getWelshLabel(), is("Cyfnod gwahardd"));
        assertThat(prompt.getPromptReference(), is("disqualificationPeriod"));
        assertThat(prompt.getType(), is("INTM"));
        assertThat(prompt.getCourtExtract(), is("Y"));
        assertThat(prompt.getDurationSequence(), is(1));
        assertThat(prompt.getPromptSequence(), is(new BigDecimal(100)));
        assertThat(prompt.getIsFinancialImposition(), is(false));
    }

    @Test
    public void shouldPopulatePromptDefinitionAttributesForDaysDuration() {
        final JudicialResultPrompt.Builder builder = JudicialResultHelper.populatePromptDefinitionAttributesBasedOnDuration(
                DDP_DISQUALIFICATION_PERIOD, resultDefinition, "Day(s)");

        final JudicialResultPrompt prompt = builder.build();

        assertThat(prompt.getJudicialResultPromptTypeId(), is(DISQUALIFICATION_PERIOD_PROMPT_ID));
        assertThat(prompt.getLabel(), is("Disqualification period"));
        assertThat(prompt.getWelshLabel(), is("Cyfnod gwahardd"));
        assertThat(prompt.getPromptReference(), is("disqualificationPeriod"));
        assertThat(prompt.getType(), is("INTM"));
        assertThat(prompt.getCourtExtract(), is("Y"));
        assertThat(prompt.getDurationSequence(), is(1));
        assertThat(prompt.getPromptSequence(), is(new BigDecimal(100)));
        assertThat(prompt.getIsFinancialImposition(), is(false));
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenPromptNotFoundForDuration() {
        assertThrows(RuntimeException.class, () ->
                JudicialResultHelper.populatePromptDefinitionAttributesBasedOnDuration(
                        JPrompt.AMOUNT_OF_FINE, resultDefinition, "Year(s)"));
    }
}
