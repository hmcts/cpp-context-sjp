package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaNotificationProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private PleaNotificationProcessor pleaNotificationProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    private static final String ENGLISH_TEMPLATE_ID = "ab3487de-a318-4364-9ded-f71c1420360f";
    private static final String WELSH_TEMPLATE_ID = "af48b904-3ee5-402b-bd82-0a80249c4405";


    @BeforeEach
    public void before() {
        // use defaults
        pleaNotificationProcessor.replyToAddress = "noreply@cjscp.org.uk";
        pleaNotificationProcessor.englishTemplateId = ENGLISH_TEMPLATE_ID;
        pleaNotificationProcessor.welshTemplateId = WELSH_TEMPLATE_ID;
    }

    @Test
    public void shouldSendEnglishPleaNotificationEmail() {
        shouldSendPleaNotificationEmail("England", ENGLISH_TEMPLATE_ID);
    }

    @Test
    public void shouldSendWelshPleaNotificationEmail() {
        shouldSendPleaNotificationEmail("Wales", WELSH_TEMPLATE_ID);
    }

    private void shouldSendPleaNotificationEmail(String country, String templateId) {

        final String email = "test@test.com";
        final String urn = "TFL123";
        final String postcode = "W1 1AA";

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("sjp.events.online-plea-received"),
                createObjectBuilder()
                        .add("urn", urn)
                        .build()
        );
        OnlinePleaReceived onlinePleaReceived = generateOnlinePleaReceived(email, urn, postcode);
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), OnlinePleaReceived.class)).thenReturn(onlinePleaReceived);
        when(referenceDataService.getCountryByPostcode("W1 1AA", event)).thenReturn(country);

        pleaNotificationProcessor.sendPleaNotificationEmail(event);

        verify(sender).send(argumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = argumentCaptor.getValue();
        assertThat(jsonEnvelope, (jsonEnvelope(
                withMetadataEnvelopedFrom(event).withName("notificationnotify.send-email-notification"),
                payload().isJson(allOf(
                        withJsonPath("$.notificationId", notNullValue()),
                        withJsonPath("$.templateId", is(templateId)),
                        withJsonPath("$.sendToAddress", is(email)),
                        withJsonPath("$.replyToAddress", is("noreply@cjscp.org.uk")),
                        withJsonPath("$.personalisation.urn", is(urn))
                ))
        )));
    }

    private static OnlinePleaReceived generateOnlinePleaReceived(final String email, final String urn, final String postcode) {
        final Boolean outstandingFines = false;
        final PersonalDetails personalDetails = new PersonalDetails(
                "Bobby", "Davro",
                new Address("82 Old Rd", "Leicester", "London", "UK", "United Kingdom", postcode),
                new ContactDetails("07429 567901", "07429 567901", "07429 567999", email, null),
                LocalDate.of(1981, 1, 1),
                "JH41 1269B", "london", "TESTYJKZAAA09", null,null);
        return new OnlinePleaReceived(urn, UUID.randomUUID(), UUID.randomUUID(),
                "6th March 2018", "French", TRUE, "Joe Cornish", "He was not there", outstandingFines,
                personalDetails, null, null, emptyList(), null, null, null);
    }
}