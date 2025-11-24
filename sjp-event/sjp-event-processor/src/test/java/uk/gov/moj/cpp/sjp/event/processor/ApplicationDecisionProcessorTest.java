package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved.applicationDecisionSaved;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cpp.sjp.domain.ApplicationOffencesResults;
import uk.gov.moj.cpp.sjp.event.ApplicationOffenceResultsSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationDecisionProcessorTest {

    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    @Mock
    private Sender sender;

    @Mock
    private SjpToHearingConverter sjpToHearingConverter;

    @InjectMocks
    private ApplicationDecisionProcessor applicationDecisionProcessor;

    @Mock
    private PublicHearingResulted publicHearingResultedPayload;

    @Mock
    private Envelope<ApplicationDecisionSaved> envelope;

    @Mock
    private Envelope<ApplicationOffenceResultsSaved> envelopeApplicationOffenceResultsSaved;

    @Captor
    private ArgumentCaptor<Envelope<HearingResulted>> hearingResultedJsonEnvelopeCaptor;

    @Test
    public void handleApplicationDecisionSaved_Granted() {
        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSaved()
                .withApplicationDecision(ApplicationDecision.applicationDecision().withGranted(true).build()).withApplicationId(randomUUID()).build();

        final Metadata metadata = getMetadata();
        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.payload()).thenReturn(applicationDecisionSaved);
        when(publicHearingResultedPayload.getSharedTime()).thenReturn(ZonedDateTime.now());
        when(publicHearingResultedPayload.getHearing()).thenReturn(Hearing.hearing().build());
        when(sjpToHearingConverter.convertApplicationDecision(envelope)).thenReturn(publicHearingResultedPayload);

        applicationDecisionProcessor.handleApplicationDecisionSaved(envelope);

        verify(sjpToHearingConverter).convertApplicationDecision(envelope);
        verify(sender).send(hearingResultedJsonEnvelopeCaptor.capture());

        final Envelope<HearingResulted> hearingResultedPublicEvent = hearingResultedJsonEnvelopeCaptor.getValue();

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(envelope)
                        .withName("sjp.command.record-granted-application-results"));

        assertNotNull(hearingResultedPublicEvent.payload().getHearing());
        assertNotNull(hearingResultedPublicEvent.payload().getSharedTime());
        assertThat(hearingResultedPublicEvent.payload().getIsReshare(), is(false));
        assertNotNull(hearingResultedPublicEvent.payload().getHearingDay());
    }

    @Test
    public void handleApplicationDecisionSaved_Rejected() {
        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSaved()
                .withApplicationDecision(ApplicationDecision.applicationDecision()
                        .withGranted(false)
                        .withRejectionReason("Rejected").build()).withApplicationId(randomUUID()).build();

        final Metadata metadata = getMetadata();
        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.payload()).thenReturn(applicationDecisionSaved);
        when(publicHearingResultedPayload.getSharedTime()).thenReturn(ZonedDateTime.now());
        when(publicHearingResultedPayload.getHearing()).thenReturn(Hearing.hearing().build());
        when(sjpToHearingConverter.convertApplicationDecision(envelope)).thenReturn(publicHearingResultedPayload);

        applicationDecisionProcessor.handleApplicationDecisionSaved(envelope);

        verify(sjpToHearingConverter).convertApplicationDecision(envelope);
        verify(sender).send(hearingResultedJsonEnvelopeCaptor.capture());

        final Envelope<HearingResulted> hearingResultedPublicEvent = hearingResultedJsonEnvelopeCaptor.getValue();

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(envelope)
                        .withName("public.events.hearing.hearing-resulted"));

        assertNotNull(hearingResultedPublicEvent.payload().getHearing());
        assertNotNull(hearingResultedPublicEvent.payload().getSharedTime());
        assertThat(hearingResultedPublicEvent.payload().getIsReshare(), is(false));
        assertNotNull(hearingResultedPublicEvent.payload().getHearingDay());
    }

    @Test
    public void handleApplicationOffencesResultsSaved() {
        final ApplicationOffenceResultsSaved applicationOffenceResultsSaved = new ApplicationOffenceResultsSaved(Hearing.hearing().build(),"2024-09-27", false, null,ZonedDateTime.now());

        final Metadata metadata = DefaultJsonMetadata.metadataBuilder().withName("sjp.events.application-offence-results-saved").withId(randomUUID()).createdAt(ZonedDateTime.now()).build();
        when(envelopeApplicationOffenceResultsSaved.metadata()).thenReturn(metadata);
        when(envelopeApplicationOffenceResultsSaved.payload()).thenReturn(applicationOffenceResultsSaved);

        applicationDecisionProcessor.handleApplicationOffencesResultsSaved(envelopeApplicationOffenceResultsSaved);

        verify(sender).send(hearingResultedJsonEnvelopeCaptor.capture());

        final Envelope<HearingResulted> hearingResultedPublicEvent = hearingResultedJsonEnvelopeCaptor.getValue();

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(envelopeApplicationOffenceResultsSaved)
                        .withName("public.events.hearing.hearing-resulted"));

        assertNotNull(hearingResultedPublicEvent.payload().getHearing());
        assertNotNull(hearingResultedPublicEvent.payload().getSharedTime());
        assertThat(hearingResultedPublicEvent.payload().getIsReshare(), is(false));
        assertNotNull(hearingResultedPublicEvent.payload().getHearingDay());
    }

    private Metadata getMetadata() {
        return DefaultJsonMetadata.metadataBuilder().withName("sjp.events.application-decision-saved").withId(randomUUID()).createdAt(ZonedDateTime.now()).build();
    }

}