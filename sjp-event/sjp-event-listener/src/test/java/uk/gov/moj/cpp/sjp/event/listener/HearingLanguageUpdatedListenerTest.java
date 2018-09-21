package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingLanguageUpdatedListenerTest {

    private UUID caseId = randomUUID();
    private UUID defendantId = randomUUID();
    private Clock clock = new UtcClock();

    @InjectMocks
    private HearingLanguageUpdatedListener listenerUnderTest;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    @Captor
    private ArgumentCaptor<Boolean> speakWelshCaptor;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseDetail caseDetail;

    @Mock
    private DefendantDetail defendant;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private OnlinePleaRepository.HearingLanguageOnlinePleaRepository onlinePleaRepository;

    @Mock
    private OnlinePlea onlinePlea;

    @Before
    public void setUp() {
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendant);
    }

    @Test
    public void shouldUpdateHearingLanguage() {
        final Boolean speakWelsh = true;
        final ZonedDateTime now = clock.now();
        final JsonEnvelope envelope = commonSetupForHearingLanguage(speakWelsh, false, now);

        listenerUnderTest.hearingLanguagePreferenceUpdated(envelope);

        verify(defendant).setSpeakWelsh(speakWelshCaptor.capture());
        verify(onlinePleaRepository, never()).saveOnlinePlea(anyObject());
        verify(jsonObjectToObjectConverter).convert(envelope.payloadAsJsonObject(), HearingLanguagePreferenceUpdatedForDefendant.class);

        assertThat(speakWelshCaptor.getValue(), is(true));
    }

    @Test
    public void shouldUpdateInterpreterForOnlinePlea() {
        final Boolean speakWelsh = false;
        final ZonedDateTime now = clock.now();
        final JsonEnvelope envelope = commonSetupForHearingLanguage(speakWelsh, true, now);

        listenerUnderTest.hearingLanguagePreferenceUpdated(envelope);

        verify(defendant).setSpeakWelsh(speakWelshCaptor.capture());
        verify(jsonObjectToObjectConverter).convert(envelope.payloadAsJsonObject(), HearingLanguagePreferenceUpdatedForDefendant.class);
        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());

        assertThat(speakWelshCaptor.getValue(), is(false));
    }

    @Test
    public void shouldCancelInterpreter() {
        final JsonEnvelope envelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.hearing-language-preference-for-defendant-cancelled"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId.toString())
                        .build());

        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingLanguagePreferenceCancelledForDefendant.class)).thenReturn(
                new HearingLanguagePreferenceCancelledForDefendant(caseId, defendantId));

        listenerUnderTest.hearingLanguagePreferenceCancelled(envelope);

        verify(jsonObjectToObjectConverter).convert(envelope.payloadAsJsonObject(), HearingLanguagePreferenceCancelledForDefendant.class);
        verify(defendant).setSpeakWelsh(null);
    }

    private JsonEnvelope commonSetupForHearingLanguage(Boolean speakWelsh, boolean updatedByOnlinePlea, ZonedDateTime now) {
        final HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant;
        if (updatedByOnlinePlea) {
            hearingLanguagePreferenceUpdatedForDefendant = HearingLanguagePreferenceUpdatedForDefendant.createEventForOnlinePlea(
                    caseId, defendantId, speakWelsh, now);
        } else {
            hearingLanguagePreferenceUpdatedForDefendant = HearingLanguagePreferenceUpdatedForDefendant.createEvent(
                    caseId, defendantId, speakWelsh);
        }

        final JsonEnvelope envelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.hearing-language-preference-for-defendant-updated"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId.toString())
                        .add("speakWelsh", speakWelsh)
                        .build());


        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingLanguagePreferenceUpdatedForDefendant.class))
                .thenReturn(hearingLanguagePreferenceUpdatedForDefendant);
        return envelope;
    }


}