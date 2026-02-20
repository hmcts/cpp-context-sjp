package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.event.processor .EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.utils.FileUtil.getFileContentAsJson;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.JsonArray;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCasesReferredToCourtProcessorTest {

    private final UUID caseId = randomUUID();

    private final UUID defendantId = randomUUID();

    private final UUID hearingId = randomUUID();

    private final String hearingTime = ZonedDateTimes.toString(now());
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @InjectMocks
    private ProsecutionCasesReferredToCourtProcessor prosecutionCasesReferredToCourtProcessor;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldSendUpdateCaseListedInCriminalCourtsCommand() {

        objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        final String hearingCourtName = "Carmarthen Magistrates' Court";
        final UUID offenceId = UUID.randomUUID();
        final JsonEnvelope prosecutionCasesReferredToCourtEvent = envelopeFrom(metadataWithRandomUUID(ProsecutionCasesReferredToCourtProcessor.EVENT_NAME),
                getFileContentAsJson("ProsecutionCasesReferredToCourtProcessorTest/case-listed-in-criminal-courts.json",
                        ImmutableMap.<String, Object>builder()
                                .put("prosecutionCaseId", caseId)
                                .put("defendantId", defendantId)
                                .put("offenceId", offenceId)
                                .put("welshName", "Welsh Name")
                                .put("name", hearingCourtName)
                                .put("hearingId", hearingId)
                                .put("sittingDay", hearingTime)
                                .build()));

        prosecutionCasesReferredToCourtProcessor.handleProsecutionCasesReferredToCourtEvent(prosecutionCasesReferredToCourtEvent);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(),
                is("sjp.command.update-case-listed-in-criminal-courts")
        );
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject(),
                is(createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(DEFENDANT_ID, defendantId.toString())
                        .add("defendantOffences", createArrayBuilder().add(offenceId.toString()))
                        .add("courtCentre", Json.createObjectBuilder()
                                .add("id", "cf73207f-3ced-488a-82a0-3fba79c2ce04")
                                .add("name", "Carmarthen Magistrates' Court")
                                .add("roomId", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a")
                                .add("roomName", "JK2Y7hu0Tc")
                                .add("welshName", "Welsh Name")
                                .add("welshRoomName", "hm60SAXokc")
                                .add("roomId", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a"))
                        .add("hearingId", hearingId.toString())
                        .add("hearingDays", populateCorrectedHearingDays())
                        .add("hearingType", createObjectBuilder()
                                .add("description", "trial")
                                .add("id", "3f0e7f4e-098b-4e51-95c6-483711f49f21")
                                .build())
                        .build()
                )
        );
    }

    @Test
    public void shouldHandleProsecutionCasesReferredToCourtEventMessage() {
        assertThat(ProsecutionCasesReferredToCourtProcessor.class, isHandlerClass(EVENT_PROCESSOR).
                with(method("handleProsecutionCasesReferredToCourtEvent").
                        thatHandles(ProsecutionCasesReferredToCourtProcessor.EVENT_NAME)));
    }

    private JsonArray populateCorrectedHearingDays() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("listedDurationMinutes", 1531791578)
                        .add("listingSequence", 810249414)
                        .add("sittingDay", hearingTime).build()).build();
    }


}
