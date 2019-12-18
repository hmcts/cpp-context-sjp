package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import org.junit.Test;

import javax.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.FinancialImpositionConverter.INSTANCE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;

import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;

public class FinancialImpositionConverterTest {

    @Test
    public void convertDecisionWhichIsResultedWithCodeFCOST() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/fcost-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/fcost-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndexOneForResultsWithCodeFCOST() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/fcost-resultcode-with-no-terminal-entry-index1-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeFVS() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/fvs-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/fvs-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndexOneForResultsWithCodeFVS() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/fvs-resultcode-with-no-terminal-entry-index1-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeCOLLO() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/collo-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);


        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/collo-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeNCOLLO() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/ncollo-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/ncollo-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeABDC() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/abdc-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/abdc-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndexOneForResultsWithCodeABDC() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/abdc-resultcode-with-no-terminal-entry-index1-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeAEOC() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/aeoc-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/aeoc-resultcode-output.json",
                        JsonObject.class)));
    }


    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndex15ForResultsWithCodeAEOC() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/aeoc-resultcode-with-no-terminal-entry-index15-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeNOVS() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/novs-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/novs-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndex5ForResultsWithCodeNOVS() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/novs-resultcode-with-no-terminal-entry-index5-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }


    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndex10ForResultsWithCodeNOVS() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/novs-resultcode-with-no-terminal-entry-index10-input.json", JsonObject.class);

        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }


    @Test
    public void convertDecisionWhichIsResultedWithCodeNCOSTS() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/ncosts-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/ncosts-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeTFOOUT() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/tfoout-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/tfoout-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test(expected = TransformationException.class)
    public void shouldThrowExceptionWhenNoTerminalEntryWithIndex1ForResultsWithCodeTFOOUT() {
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/tfoout-resultcode-with-no-terminal-entry-index1-input.json", JsonObject.class);
        INSTANCE.convert(offenceDecisionJsonObject, null, null);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeRLSUM() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/rlsum-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/rlsum-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeRLSUMI() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/rlsumi-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/rlsumi-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeRINSTL() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/rinstl-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/rinstl-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeLSUM() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/lsum-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/lsum-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeLSUMI() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/lsumi-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/lsumi-resultcode-output.json",
                        JsonObject.class)));
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeINSTL() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "financialImpositionConverter/instl-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "financialImpositionConverter/instl-resultcode-output.json",
                        JsonObject.class)));
    }

}