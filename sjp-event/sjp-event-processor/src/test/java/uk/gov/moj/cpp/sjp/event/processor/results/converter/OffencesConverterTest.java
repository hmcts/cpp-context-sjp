package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffencesConverterTest {


    @Mock
    private OffenceFactsConverter offenceFactsConverter;

    @Mock
    private PleaConverter pleaConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Mock
    private VerdictConverter verdictConverter;

    @Mock
    DecisionAggregate resultsAggregate;

    @InjectMocks
    private  OffencesConverter offencesConverter;

    private CourtCentre courtCentre = courtCentre().build();

    @Test
    public void shouldReturnDvlaCode() {

        final JsonObject offencesReferenceData = getFileContentAsJson("resultsconverter/offenceReferenceData.json", new HashMap<>());

        assertThat(offencesConverter.getDVLAOffenceCode(offencesReferenceData), is("TS30"));

    }

    @Test
    public void shouldReturnOffenceFact() {

        UUID offenceId = UUID.randomUUID();
        Offence offence = Offence.offence()
                .withId(offenceId)
                .withVehicleMake("AAA")
                .withVehicleRegistrationMark("BBB")
                .build();

        final JsonObject offencesReferenceData = getFileContentAsJson("resultsconverter/offenceReferenceData.json", new HashMap<>());


        OffenceFacts offenceFacts = OffenceFacts.offenceFacts()
                .withVehicleMake(offence.getVehicleMake())
                .withVehicleRegistration(offence.getVehicleRegistrationMark())
                .build();

        ConvictionInfo convictionInfo = new ConvictionInfo(UUID.randomUUID(), VerdictType.FOUND_GUILTY, LocalDate.now(), courtCentre);
        resultsAggregate.putConvictionInfo(offenceId,convictionInfo);
        when(offenceFactsConverter.getOffenceFacts(any())).thenReturn(offenceFacts);


        assertThat(offencesConverter.getOffenceBuilderWithPopulation(offence,offencesReferenceData,resultsAggregate,null).build().getOffenceFacts(), is(offenceFacts));

    }

}