package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;

import java.util.HashMap;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


public class OffencesConverterTest {

    final OffencesConverter offencesConverter = new OffencesConverter();

    @Test
    public void shouldReturnDvlaCode() {

        final JsonObject offencesReferenceData = getFileContentAsJson("resultsconverter/offenceReferenceData.json", new HashMap<>());

        assertThat(offencesConverter.getDVLAOffenceCode(offencesReferenceData), is("TS30"));

    }

}