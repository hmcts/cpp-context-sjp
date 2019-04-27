package uk.gov.moj.cpp.sjp.event.processor.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.assertSessionLocation;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCourt;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildDefendant;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildPersonalDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildSjpSession;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCaseFileDefendantDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCaseId;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCountryCjsCode;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCountryNationality;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getEmptyEnvelop;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getReferenceDecisionSaved;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSJPSessionJsonObject;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyCases;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyDefendants;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyPerson;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifySession;

import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultingToResultsConverterTest {

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private ResultingToResultsConverter converter;

    @Mock
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Test
    public void shouldConvertResult() {

        final Optional<JsonObject> court = buildCourt();

        when(referenceDataService.getCourtByCourtHouseOUCode(any(), any())).thenReturn(court);
        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(getCaseFileDefendantDetails());
        when(referenceDataService.getNationality(any(), any())).thenReturn(getCountryNationality());

        final JsonObject response = converter.convert(getCaseId(), getReferenceDecisionSaved(), buildCaseDetails(), getSJPSessionJsonObject());
        final JsonObject session = response.getJsonObject("session");
        final JsonArray cases = response.getJsonArray("cases");
        verifySession(session);
        verifyCases(cases);
        verify(referenceDataService).getCourtByCourtHouseOUCode(any(), any());
        verify(prosecutionCaseFileService).getCaseFileDefendantDetails(any(), any());
        verify(referenceDataService).getNationality(any(), any());
    }

    @Test
    public void shouldBuildSessionLocation() {
        final SJPSession sjpSession = buildSjpSession();
        final Optional<JsonObject> court = buildCourt();
        final JsonObject sessionLocation = converter.buildSessionLocation(sjpSession, court);
        assertSessionLocation(sessionLocation);
    }

    @Test
    public void shouldBuildSession() {
        final SJPSession sjpSession = buildSjpSession();
        final Optional<JsonObject> court = buildCourt();
        final JsonObject session = converter.buildSession(sjpSession, court);
        verifySession(session);
    }

    @Test
    public void shouldBuildCases() {
        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(getCaseFileDefendantDetails());
        when(referenceDataService.getNationality(any(), any())).thenReturn(getCountryNationality());
        final JsonArray cases = converter.buildCases(getCaseId(), buildCaseDetails(), getReferenceDecisionSavedAsObject(), buildSjpSession(), getEmptyEnvelop());
        verifyCases(cases);
        verify(prosecutionCaseFileService).getCaseFileDefendantDetails(any(), any());
        verify(referenceDataService).getNationality(any(), any());
    }

    @Test
    public void shouldBuildDefendant() {
        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(getCaseFileDefendantDetails());
        when(referenceDataService.getNationality(any(), any())).thenReturn(getCountryNationality());
        final JsonArray defendants = converter.buildDefendants(buildCaseDetails(), getReferenceDecisionSavedAsObject(), buildSjpSession(), getEmptyEnvelop());
        verifyDefendants(defendants);
        verify(prosecutionCaseFileService).getCaseFileDefendantDetails(any(), any());
        verify(referenceDataService).getNationality(any(), any());
    }

    @Test
    public void shouldBuildIndividualDefendant() {
        final JsonObject individualDefendant = converter.buildIndividualDefendant(buildDefendant(), getCountryCjsCode());
        final JsonObject person = individualDefendant.getJsonObject("basePersonDetails");
        assertEquals(getCountryCjsCode(), individualDefendant.getString("personStatedNationality"));
        verifyPerson(person);

    }

    @Test
    public void shouldBuildPerson() {
        final PersonalDetails personalDetails = buildPersonalDetails();
        final JsonObject person = converter.buildPerson(personalDetails);
        verifyPerson(person);
    }

    private ReferencedDecisionsSaved getReferenceDecisionSavedAsObject() {
        final JsonObject jsonPayload = getReferenceDecisionSaved().payloadAsJsonObject();
        return converter.extractReferenceDecisionSaves(getCaseId(), jsonPayload);
    }

}
