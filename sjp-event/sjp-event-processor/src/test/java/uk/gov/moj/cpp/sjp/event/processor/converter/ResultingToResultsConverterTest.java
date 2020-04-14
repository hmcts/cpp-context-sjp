package uk.gov.moj.cpp.sjp.event.processor.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.assertSessionLocation;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCourt;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildDefendant;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildPersonalDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildSjpSession;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCaseFileDefendantDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCaseId;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCaseResults;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCountryIsoCode;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getCountryNationality;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSJPSessionJsonObject;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyCases;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyDefendants;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyPerson;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifySession;

import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseCaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.BasePersonDetail;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseSessionStructure;
import uk.gov.justice.json.schemas.domains.sjp.results.CaseDefendant;
import uk.gov.justice.json.schemas.domains.sjp.results.IndividualDefendant;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.json.schemas.domains.sjp.results.SessionLocation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.resulting.CaseResults;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultingToResultsConverterTest {

    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;
    @InjectMocks
    private ResultingToResultsConverter converter;
    @Mock
    private ProsecutionCaseFileService prosecutionCaseFileService;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Test
    public void shouldConvertResult() {

        final Optional<JsonObject> court = buildCourt();

        when(referenceDataService.getCourtByCourtHouseOUCode(any(), any())).thenReturn(court);
        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(getCaseFileDefendantDetails());
        when(referenceDataService.getNationality(any(), any())).thenReturn(getCountryNationality());

        final Envelope<CaseResults> caseResults = getCaseResults();
        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(caseResults.payload());
        final Envelope<JsonObject> caseResults2 = Envelope.envelopeFrom(caseResults.metadata(), jsonObject);
        final PublicSjpResulted publicSjpResulted = converter.convert(getCaseId(), caseResults2, buildCaseDetails(), getSJPSessionJsonObject());
        final BaseSessionStructure session = publicSjpResulted.getSession();
        final List<BaseCaseDetails> cases = publicSjpResulted.getCases();
        verifySession(session);
        verifyCases(cases, caseResults);
        verify(referenceDataService).getCourtByCourtHouseOUCode(any(), any());
        verify(prosecutionCaseFileService).getCaseFileDefendantDetails(any(), any());
        verify(referenceDataService).getNationality(any(), any());
    }


    @Test
    public void shouldBuildSessionLocation() {
        final SJPSession sjpSession = buildSjpSession();
        final Optional<JsonObject> court = buildCourt();
        final SessionLocation sessionLocation = converter.buildSessionLocation(sjpSession, court);
        assertSessionLocation(sessionLocation);
    }

    @Test
    public void shouldBuildSession() {
        final SJPSession sjpSession = buildSjpSession();
        final Optional<JsonObject> court = buildCourt();
        final BaseSessionStructure session = converter.buildSession(sjpSession, court);
        verifySession(session);
    }

    @Test
    public void shouldBuildCases() {
        final Envelope<CaseResults> caseResults = getCaseResults();

        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(getCaseFileDefendantDetails());
        when(referenceDataService.getNationality(any(), any())).thenReturn(getCountryNationality());
        final List<BaseCaseDetails> cases = converter.buildCases(getCaseId(), buildCaseDetails(), caseResults.payload(), buildSjpSession(), caseResults.metadata());
        verifyCases(cases, caseResults);
        verify(prosecutionCaseFileService).getCaseFileDefendantDetails(any(), any());
        verify(referenceDataService).getNationality(any(), any());
    }

    @Test
    public void shouldBuildDefendant() {
        final Envelope<CaseResults> caseResults = getCaseResults();
        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(getCaseFileDefendantDetails());
        when(referenceDataService.getNationality(any(), any())).thenReturn(getCountryNationality());
        final List<CaseDefendant> defendants = converter.buildDefendants(buildCaseDetails(), caseResults.payload(), buildSjpSession(), caseResults.metadata());
        verifyDefendants(defendants, caseResults);
        verify(prosecutionCaseFileService).getCaseFileDefendantDetails(any(), any());
        verify(referenceDataService).getNationality(any(), any());
    }

    @Test
    public void shouldBuildIndividualDefendant() {
        final IndividualDefendant individualDefendant = converter.buildIndividualDefendant(buildDefendant(), getCountryIsoCode());
        final BasePersonDetail person = individualDefendant.getBasePersonDetails();
        assertEquals(getCountryIsoCode(), individualDefendant.getPersonStatedNationality());
        verifyPerson(person);

    }

    @Test
    public void shouldBuildPerson() {
        final PersonalDetails personalDetails = buildPersonalDetails();
        final BasePersonDetail person = converter.buildPerson(personalDetails);
        verifyPerson(person);
    }

}
