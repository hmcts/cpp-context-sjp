package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_2;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import java.util.Arrays;
import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCasesConverterTest {

    @InjectMocks
    ProsecutionCasesConverter prosecutionCasesConverter;

    @Mock
    DefendantsConverter defendantsConverter;

    @Mock
    ProsecutionCaseIdentifierConverter pciConverter;

    @Mock
    JsonObject sjpSessionPayload;
    @Mock
    CaseDetails caseDetails;
    @Mock
    Metadata sourceMetadata;
    @Mock
    DecisionAggregate resultsAggregate;


    @Test
    public void shouldConvertProsecutionCaseIdentifierWithPoliceFlag() {

        final ProsecutionCaseIdentifier prosecutionCaseIdentified = ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build();
        final Defendant defandant = Defendant.defendant().withId(ID_1).build();

        when(pciConverter.getProsecutionCaseIdentifier(any(), any())).thenReturn(prosecutionCaseIdentified);
        when(defendantsConverter.getDefendants(any(), any(), any(), any())).thenReturn(Arrays.asList(defandant));

        when(caseDetails.getId()).thenReturn(ID_2);


        final List<ProsecutionCase> prosecutionCases = prosecutionCasesConverter.convert(sjpSessionPayload, caseDetails, sourceMetadata, resultsAggregate);

        assertThat(prosecutionCases.size(), is(1));
        assertThat(prosecutionCases.get(0).getId(), is(ID_2));
        assertThat(prosecutionCases.get(0).getProsecutionCaseIdentifier(), is(prosecutionCaseIdentified));
        assertThat(prosecutionCases.get(0).getInitiationCode(), is(InitiationCode.J));
        assertThat(prosecutionCases.get(0).getDefendants().size(), is(1));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getId(), is(ID_1));

    }

}
