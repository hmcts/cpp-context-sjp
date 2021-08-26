package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved.applicationDecisionSaved;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;

import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationDecisionProcessorTest {

    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    @Mock
    private Sender sender;

    @Mock
    private SjpToHearingConverter sjpToHearingConverter;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @InjectMocks
    private ApplicationDecisionProcessor applicationDecisionProcessor;

    @Mock
    private PublicHearingResulted publicHearingResultedPayload;

    @Mock
    private Envelope<ApplicationDecisionSaved> envelope;

    @Captor
    private ArgumentCaptor<Envelope<PublicHearingResulted>> jsonEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<HearingResulted>> hearingResultedJsonEnvelopeCaptor;

    @Test
    public void handleApplicationDecisionSaved() {
        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSaved().withApplicationId(randomUUID()).build();

        final Metadata metadata = getMetadata();
        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.payload()).thenReturn(applicationDecisionSaved);
        when(sjpToHearingConverter.convertApplicationDecision(envelope)).thenReturn(publicHearingResultedPayload);

        applicationDecisionProcessor.handleApplicationDecisionSaved(envelope);

        verify(sjpToHearingConverter).convertApplicationDecision(envelope);
        verify(sender).send(jsonEnvelopeCaptor.capture());

        final Envelope<PublicHearingResulted> hearingResultedPublicEvent = jsonEnvelopeCaptor.getValue();

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(envelope)
                        .withName(PUBLIC_HEARING_RESULTED_EVENT));

        assertThat(hearingResultedPublicEvent.payload(), is(publicHearingResultedPayload));

    }

    @Test
    public void handleApplicationDecisionSavedWhenAmendReShareEnabled() {
        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSaved().withApplicationId(randomUUID()).build();

        final Metadata metadata = getMetadata();
        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.payload()).thenReturn(applicationDecisionSaved);
        when(publicHearingResultedPayload.getSharedTime()).thenReturn(ZonedDateTime.now());
        when(publicHearingResultedPayload.getHearing()).thenReturn(Hearing.hearing().build());
        when(sjpToHearingConverter.convertApplicationDecision(envelope)).thenReturn(publicHearingResultedPayload);
        when(featureControlGuard.isFeatureEnabled("amendReshare")).thenReturn(true);

        applicationDecisionProcessor.handleApplicationDecisionSaved(envelope);

        verify(sjpToHearingConverter).convertApplicationDecision(envelope);
        verify(sender).send(hearingResultedJsonEnvelopeCaptor.capture());

        final Envelope<HearingResulted> hearingResultedPublicEvent = hearingResultedJsonEnvelopeCaptor.getValue();

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(envelope)
                        .withName(PUBLIC_EVENTS_HEARING_RESULTED));

        assertNotNull(hearingResultedPublicEvent.payload().getHearing());
        assertNotNull(hearingResultedPublicEvent.payload().getSharedTime());
        assertThat(hearingResultedPublicEvent.payload().getIsReshare(), is(false));
        assertNotNull(hearingResultedPublicEvent.payload().getHearingDay());
    }

    private Metadata getMetadata() {
        return DefaultJsonMetadata.metadataBuilder().withName("sjp.events.application-decision-saved").withId(randomUUID()).createdAt(ZonedDateTime.now()).build();
    }

}