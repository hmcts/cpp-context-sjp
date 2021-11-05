package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.ResultingToResultsConverterHelper.buildCaseDetails;

import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;


import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;

import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseListedProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private SjpService sjpService;

    final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

    @Mock
    protected Function function;

    @Mock
    private SjpToHearingConverter sjpToHearingConverter;

    @Mock
    private PublicHearingResulted publicHearingResultedPayload;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @InjectMocks
    private CaseListedProcessor caseListedProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PRIVATE_CASE_LISTED_IN_REFER_TO_COURT_EVENT = "sjp.events.case-listed-in-cc-for-refer-to-court";

    @Before
    public void setUp() {
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(jsonEnvelope);
    }

    @Test
    public void shouldHandleCaseListedInCCReferToCourt() {
        final UUID caseId = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_LISTED_IN_REFER_TO_COURT_EVENT,
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add("courtCentre", Json.createObjectBuilder()
                                .add("id", "cf73207f-3ced-488a-82a0-3fba79c2ce04")
                                .add("name", "Carmarthen Magistrates' Court")
                                .add("roomId", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a")
                                .add("roomName", "JK2Y7hu0Tc")
                                .add("welshName", "${welshName}")
                                .add("welshRoomName", "hm60SAXokc")
                                .add("roomId", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a"))
                        .add("hearingDays", populateCorrectedHearingDays())
                        .add("jurisdictionType", "MAGISTRATES")
                        .build());

        when(sjpToHearingConverter.convertCaseDecisionInCcForReferToCourt(privateEvent)).thenReturn(publicHearingResultedPayload);

        caseListedProcessor.handleCaseListedInCCReferToCourt(privateEvent);

        verify(sender, times(1)).send(jsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> eventEnvelopes = jsonEnvelopeCaptor.getAllValues();
        final Envelope<JsonValue> publicHearingResultedEvent = eventEnvelopes.get(0);

        assertThat(publicHearingResultedEvent.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(PUBLIC_HEARING_RESULTED_EVENT));

        final Envelope<JsonValue> hearingResultedPublicEvent = eventEnvelopes.get(0);

        assertThat(hearingResultedPublicEvent.payload(), is(publicHearingResultedPayload));
    }

    @Test
    public void handleCaseListedInCC_shouldInitiatePublicHearingResultedEvent() {

        final UUID caseId = randomUUID();

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_LISTED_IN_REFER_TO_COURT_EVENT,
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add("courtCentre", Json.createObjectBuilder()
                                .add("id", "cf73207f-3ced-488a-82a0-3fba79c2ce04")
                                .add("name", "Carmarthen Magistrates' Court")
                                .add("roomId", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a")
                                .add("roomName", "JK2Y7hu0Tc")
                                .add("welshName", "${welshName}")
                                .add("welshRoomName", "hm60SAXokc")
                                .add("roomId", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a"))
                        .add("hearingDays", populateCorrectedHearingDays())
                        .add("jurisdictionType", "MAGISTRATES")
                        .build());

        when(sjpToHearingConverter.convertCaseDecisionInCcForReferToCourt(privateEvent)).thenReturn(publicHearingResultedPayload);

        caseListedProcessor.handleCaseListedInCCReferToCourt(privateEvent);

        verify(sender, times(1)).send(jsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> eventEnvelopes = jsonEnvelopeCaptor.getAllValues();
        final Envelope<JsonValue> decisionSavedPublicEvent = eventEnvelopes.get(0);

        assertThat(decisionSavedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(PUBLIC_HEARING_RESULTED_EVENT));
    }

    private JsonArray populateCorrectedHearingDays() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("listedDurationMinutes", 1531791578)
                        .add("listingSequence", 810249414))
                        .build();
    }

}
