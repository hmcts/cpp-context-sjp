package uk.gov.moj.cpp.sjp.event.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonValue;
import java.util.Arrays;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted.publicHearingResulted;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.G;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP;


@ExtendWith(MockitoExtension.class)
public class HearingResultedProcessorTest {

    @Mock
    protected Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private HearingResultedProcessor hearingResultReceivedProcessor;

    private JsonObjectToObjectConverter jsonObjectConverter;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    private final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().
            withMigrationSourceSystem(new MigrationSourceSystem("1234","Exhibit"))
            .build();

    final UUID caseId = randomUUID();

    @BeforeEach
    public void setUp() {
        objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
        jsonObjectConverter = new JsonObjectToObjectConverter();
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        setField(this.objectToJsonObjectConverter, "mapper", objectMapper);
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
        setField(this.hearingResultReceivedProcessor, "jsonObjectConverter", jsonObjectConverter);
    }

    private JsonEnvelope populateHearing(String applicationType, UUID caseId, boolean isSJP, String resultDefinitionId, final boolean hasMigration) {

        JudicialResult judicialResult = new JudicialResult.Builder()
                .withJudicialResultTypeId(UUID.fromString(resultDefinitionId))
                .build();

        CourtApplicationType courtApplicationType = new CourtApplicationType.Builder()
                .withType(applicationType)

                .build();
        CourtApplication courtApplication = courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationType)
                .withJudicialResults(singletonList(judicialResult))
                .build();

        final PublicHearingResulted publicHearingResulted = publicHearingResulted()
                .withHearing(getHearing(isSJP, courtApplication,hasMigration))
                .build();


        return envelopeFrom(metadataWithRandomUUID(HearingResultReceivedProcessor.PUBLIC_HEARING_RESULTED),
                objectToJsonObjectConverter.convert(publicHearingResulted));
    }

    private Hearing getHearing(final boolean isSJP, final CourtApplication courtApplication, final boolean hasMigration) {
        return !hasMigration ? hearing().withCourtApplications(singletonList(courtApplication)).withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase().build())).withIsSJPHearing(isSJP).build() :
                hearing().withCourtApplications(singletonList(courtApplication)).withProsecutionCases(singletonList(prosecutionCase)).withIsSJPHearing(isSJP).build();
    }


    private void runTest(String applicationType, String resultDefinitionId, ApplicationStatus applicationStatus, final boolean isSJP,final boolean hasMigration) {
        JsonEnvelope hearingJsonEnvelope = populateHearing(applicationType, caseId, isSJP, resultDefinitionId, hasMigration);
        hearingResultReceivedProcessor.hearingResultReceived(hearingJsonEnvelope);

        verify(sender, never()).send(envelopeCaptor.capture());
    }



    @Test
    public void shouldNotThrowExceptionForEmptyApplicationCases() {

            runTest(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), G.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED, true,false);
    }

    @Test
    public void shouldNotProceedWithMigrationSystem() {

        runTest(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType(), G.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED, false,true);
    }
}
