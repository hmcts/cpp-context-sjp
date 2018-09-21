package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaNotificationProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private PleaNotificationProcessor pleaNotificationProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    @Before
    public void before() {
        // use defaults
        pleaNotificationProcessor.replyToAddress = "noreply@cjscp.org.uk";
        pleaNotificationProcessor.templateId = "32d520ca-4d6e-4b5c-a9f3-e761d4ffd9a2";
    }

    @Test
    public void shouldSendPleaNotificationEmail() {

        final String email = "test@test.com";
        final String urn = "TFL123";

        final JsonEnvelope event = JsonEnvelopeBuilder.envelopeFrom(
                metadataWithRandomUUID("sjp.events.online-plea-received"),
                createObjectBuilder()
                        .add("urn", urn)
                        .build()
        );
        OnlinePleaReceived onlinePleaReceived = generateOnlinePleaReceived(email, urn);
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), OnlinePleaReceived.class)).thenReturn(onlinePleaReceived);

        pleaNotificationProcessor.sendPleaNotificationEmail(event);

        verify(sender).send(argumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = argumentCaptor.getValue();
        assertThat(jsonEnvelope, (jsonEnvelope(
                withMetadataEnvelopedFrom(event).withName("notificationnotify.send-email-notification"),
                payload().isJson(allOf(
                        withJsonPath("$.notificationId", notNullValue()),
                        withJsonPath("$.templateId", is("32d520ca-4d6e-4b5c-a9f3-e761d4ffd9a2")),
                        withJsonPath("$.sendToAddress", is(email)),
                        withJsonPath("$.replyToAddress", is("noreply@cjscp.org.uk")),
                        withJsonPath("$.personalisation.urn", is(urn))
                ))
        )));
    }

    private static OnlinePleaReceived generateOnlinePleaReceived(final String email, final String urn) {
        final PersonalDetails personalDetails = new PersonalDetails(
                "Bobby", "Davro",
                new Address("82 Old Rd", "Leicester", "London", "UK", "United Kingdom", "W1 1AA"),
                new ContactDetails("07429 567901", "07429 567901", "07429 567999", email, null),
                LocalDate.of(1981, 1, 1),
                "JH41 1269B");
        return new OnlinePleaReceived(urn, UUID.randomUUID(), UUID.randomUUID(),
                "6th March 2018", "French", TRUE, "Joe Cornish", "He was not there",
                personalDetails, null, null, emptyList());
    }
}