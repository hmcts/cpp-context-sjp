package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.CASE_URN;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.COURT_CENTRE_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.FULL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.PROSECUTION_AUTHORITY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.SHORT_NAME;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseIdentifierConverterTest {

    @InjectMocks
    ProsecutionCaseIdentifierConverter prosecutionCaseIdentifierConverter;


    @Mock
    ReferenceDataService referenceDataService;


    @Test
    public void shouldConvertProsecutionCaseIdentifierWithPoliceFlag() {


        JsonArray value = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("policeFlag", true)
                        .add("id", ID_2.toString())
                        .add("shortName", SHORT_NAME)
                        .add("fullName", FULL_NAME)
                        .add("oucode", COURT_CENTRE_CODE)
                )
                .build();

        JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutors", value)
                .build();

        when(referenceDataService.getProsecutor(anyObject(), anyObject())).thenReturn(jsonObject);

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifierConverter.getProsecutionCaseIdentifier(PROSECUTION_AUTHORITY, CASE_URN);

        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityId(), is(ID_2));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityCode(), is(SHORT_NAME));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityName(), is(FULL_NAME));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode(), is(COURT_CENTRE_CODE));
        assertThat(prosecutionCaseIdentifier.getCaseURN(), is(CASE_URN));
    }

    @Test
    public void shouldConvertProsecutionCaseIdentifierWithoutPoliceFlag() {


        JsonArray value = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("policeFlag", false)
                        .add("id", ID_2.toString())
                        .add("shortName", SHORT_NAME)
                        .add("fullName", FULL_NAME)
                        .add("oucode", COURT_CENTRE_CODE)
                )
                .build();

        JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutors", value)
                .build();

        when(referenceDataService.getProsecutor(anyObject(), anyObject())).thenReturn(jsonObject);

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifierConverter.getProsecutionCaseIdentifier(PROSECUTION_AUTHORITY, CASE_URN);

        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityId(), is(ID_2));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityCode(), is(SHORT_NAME));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityName(), is(FULL_NAME));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode(), is(COURT_CENTRE_CODE));
        assertThat(prosecutionCaseIdentifier.getProsecutionAuthorityReference(), is(CASE_URN));


    }

}
