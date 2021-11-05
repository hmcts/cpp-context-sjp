package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionSavedToJudicialResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpCaseDecisionToHearingResultConverterTest {

    @InjectMocks
    SjpCaseDecisionToHearingResultConverter sjpCaseDecisionToHearingResultConverter;

    @Mock
    CourtCentreConverter courtCenterConverter;

    @Mock
    HearingDaysConverter hearingDaysConverter;

    @Mock
    SjpService sjpService;

    @Mock
    ProsecutionCasesConverter prosecutionCasesConverter;

    @Mock
    DecisionSavedToJudicialResultsConverter referencedDecisionSavedOffenceConverter;

    @Mock
    Envelope<DecisionSaved> decisionSavedEventEnvelop;
    @Mock
    DecisionSaved decisionSaved;
    @Mock
    JsonEnvelope sjpSessionEnvelope;
    @Mock
    CaseDetails caseDetails;
    @Mock
    uk.gov.justice.json.schemas.domains.sjp.queries.Defendant defendant;
    @Mock
    PersonalDetails personalDetails;

    DecisionAggregate resultsAggregate;

    private HearingDay hearingDay = HearingDay.hearingDay().build();

    private CourtCentre courtCentre = courtCentre().build();

    @Before
    public void setUp() {
        resultsAggregate = new DecisionAggregate();
    }

    @Test
    public void shouldConvertSjpCaseDecisionToHearingResultWithMandatoryField() {
        Metadata sourceMetadata = metadataWithRandomUUID(RANDOM_TEXT).build();

        when(decisionSavedEventEnvelop.payload()).thenReturn(decisionSaved);
        when(sjpService.getSessionInformation(anyObject(), anyObject())).thenReturn(sjpSessionEnvelope);
        when(decisionSaved.getSessionId()).thenReturn(ID_2);
        when(decisionSaved.getDecisionId()).thenReturn(ID_2);
        when(decisionSavedEventEnvelop.metadata()).thenReturn(sourceMetadata);
        when(sjpService.getCaseDetails(anyObject(), anyObject())).thenReturn(caseDetails);
        when(caseDetails.getDefendant()).thenReturn(defendant);
        when(defendant.getId()).thenReturn(ID_1);
        when(defendant.getPersonalDetails()).thenReturn(personalDetails);
        when(personalDetails.getDriverNumber()).thenReturn(RANDOM_TEXT);
        when(referencedDecisionSavedOffenceConverter.convertOffenceDecisions(anyObject(), anyObject(), anyObject(), anyObject(), anyObject(), anyString())).thenReturn(resultsAggregate);
        when(hearingDaysConverter.convert(anyObject())).thenReturn(Arrays.asList(hearingDay));
        when(courtCenterConverter.convert(anyObject(), anyObject())).thenReturn(courtCentre);

        final PublicHearingResulted publicHearingResulted = sjpCaseDecisionToHearingResultConverter.convertCaseDecision(decisionSavedEventEnvelop);

        assertThat(publicHearingResulted, is(notNullValue()));
        assertThat(publicHearingResulted.getHearing(), is(notNullValue()));
        assertThat(publicHearingResulted.getHearing().getId(), is(ID_2));
        assertThat(publicHearingResulted.getHearing().getCourtCentre(), is(courtCentre));
        assertThat(publicHearingResulted.getHearing().getJurisdictionType(), is(MAGISTRATES));



    }
    @Test
    public void shouldConvertSjpCaseDecisionToHearingResultWithOptionalField() {
        Metadata sourceMetadata = metadataWithRandomUUID(RANDOM_TEXT).build();

        when(decisionSavedEventEnvelop.payload()).thenReturn(decisionSaved);
        when(sjpService.getSessionInformation(anyObject(), anyObject())).thenReturn(sjpSessionEnvelope);
        when(decisionSaved.getSessionId()).thenReturn(ID_2);
        when(decisionSaved.getDecisionId()).thenReturn(ID_2);
        when(decisionSavedEventEnvelop.metadata()).thenReturn(sourceMetadata);
        when(sjpService.getCaseDetails(anyObject(), anyObject())).thenReturn(caseDetails);
        when(caseDetails.getDefendant()).thenReturn(defendant);
        when(defendant.getId()).thenReturn(ID_1);
        when(defendant.getPersonalDetails()).thenReturn(personalDetails);
        when(personalDetails.getDriverNumber()).thenReturn(RANDOM_TEXT);
        when(referencedDecisionSavedOffenceConverter.convertOffenceDecisions(anyObject(), anyObject(), anyObject(), anyObject(), anyObject(), anyString())).thenReturn(resultsAggregate);
        when(hearingDaysConverter.convert(anyObject())).thenReturn(Arrays.asList(hearingDay));
        when(courtCenterConverter.convert(anyObject(), anyObject())).thenReturn(courtCentre);

        final PublicHearingResulted publicHearingResulted = sjpCaseDecisionToHearingResultConverter.convertCaseDecision(decisionSavedEventEnvelop);

        assertThat(publicHearingResulted, is(notNullValue()));
        assertThat(publicHearingResulted.getHearing(), is(notNullValue()));
        assertThat(publicHearingResulted.getHearing().getId(), is(ID_2));
        assertThat(publicHearingResulted.getHearing().getCourtCentre(), is(courtCentre));
        assertThat(publicHearingResulted.getHearing().getJurisdictionType(), is(MAGISTRATES));
        assertThat(publicHearingResulted.getHearing().getIsSJPHearing(), is(true));
        assertThat(publicHearingResulted.getHearing().getHearingLanguage(), is(HearingLanguage.ENGLISH));
        assertThat(publicHearingResulted.getHearing().getHearingDays().size(), is(1));
        assertThat(publicHearingResulted.getHearing().getHasSharedResults(), is(false));
        assertThat(publicHearingResulted.getHearing().getHearingCaseNotes(), is(nullValue()));
        assertThat(publicHearingResulted.getHearing().getIsBoxHearing(), is(false));


    }

}
