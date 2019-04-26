package uk.gov.moj.cpp.sjp.event.processor.converter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.assertSessionLocation;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCaseDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildCourt;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildDefendant;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildPersonalDetails;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.buildSjpSession;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getReferenceDecisionSaved;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.getSJPSessionJsonObject;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyCases;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyDefendants;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifyPerson;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverterHelper.verifySession;

import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
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

    @Test
    public void shouldConvertResult() {

        final Optional<JsonObject> court = buildCourt();

        when(referenceDataService.getCourtByCourtHouseOUCode(any(), any())).thenReturn(court);

        JsonObject response = converter.convert(CASE_ID, getReferenceDecisionSaved(), buildCaseDetails(), getSJPSessionJsonObject());
        JsonObject session = response.getJsonObject("session");
        JsonArray cases = response.getJsonArray("cases");
        verifySession(session);
        verifyCases(cases);
        verify(referenceDataService).getCourtByCourtHouseOUCode(any(), any());
    }

    @Test
    public void shouldBuildSessionLocation() {
        final SJPSession sjpSession = buildSjpSession();
        final Optional<JsonObject> court = buildCourt();
        JsonObject sessionLocation = converter.buildSessionLocation(sjpSession, court);
        assertSessionLocation(sessionLocation);
    }

    @Test
    public void shouldBuildSession() {
        final SJPSession sjpSession = buildSjpSession();
        final Optional<JsonObject> court = buildCourt();
        JsonObject session = converter.buildSession(sjpSession, court);
        verifySession(session);
    }

    @Test
    public void shouldBuildCases() {
        JsonArray cases = converter.buildCases(CASE_ID, buildCaseDetails(), getReferenceDecisionSavedAsObject(), buildSjpSession());
        verifyCases(cases);
    }

    @Test
    public void shouldBuildDefendant() {
        JsonArray defendants = converter.buildDefendants(buildCaseDetails(), getReferenceDecisionSavedAsObject(), buildSjpSession());
        verifyDefendants(defendants);
    }

    @Test
    public void shouldBuildIndividualDefendant() {
        JsonObject individualDefendant = converter.buildIndividualDefendant(buildDefendant());
        JsonObject person = individualDefendant.getJsonObject("basePersonDetails");
        verifyPerson(person);
    }

    @Test
    public void shouldBuildPerson() {
        PersonalDetails personalDetails = buildPersonalDetails();
        JsonObject person = converter.buildPerson(personalDetails);
        verifyPerson(person);
    }

    private ReferencedDecisionsSaved getReferenceDecisionSavedAsObject() {
        final JsonObject jsonPayload = getReferenceDecisionSaved().payloadAsJsonObject();
        return converter.extractReferenceDecisionSaves(CASE_ID, jsonPayload);
    }

}
