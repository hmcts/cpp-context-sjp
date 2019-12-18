package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import org.junit.Test;

import javax.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.DischargeConverter.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;

public class DischargeConverterTest {

    @Test
    public void convertDecisionWhichIsResultedWithCodeAD() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/ad-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "dischargeConverter/ad-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodesADAndGPTAC() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/ad-gptac-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "dischargeConverter/ad-gptac-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }


    @Test
    public void convertDecisionWhichIsResultedWithCodesCD() {

        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/cd-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "dischargeConverter/cd-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndexOneForResultsWithCodeCD() {
        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/cd-resultcode-with-no-terminal-entry-index1-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndexTwoForResultsWithCodeCD() {
        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/cd-resultcode-with-no-terminal-entry-index2-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);
    }


    @Test
    public void convertDecisionWhichIsResultedWithCodesCDAndFCOMP() {

        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/cd-fcomp-resultcode-input.json" +
                        "", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "dischargeConverter/cd-fcomp-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodesCDAndNCR() {

        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/cd-ncr-resultcode-input.json" +
                        "", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "dischargeConverter/cd-ncr-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }


    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndexOneForResultsWithCodeNCR() {
        JsonObject offenceDecisionJsonObject = readJson(
                "dischargeConverter/ncr-resultcode-with-no-terminal-entry-index1-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, "GUILTY", null);
    }

}