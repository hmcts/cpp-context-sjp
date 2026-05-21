package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static java.util.UUID.randomUUID;

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
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

@ExtendWith(MockitoExtension.class)
public class DefendantAcceptedAocpProcessorTest {
    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private TimerService timerService;

    @InjectMocks
    private DefendantAcceptedAocpProcessor defendantAcceptedAocpProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    private static final String ENGLISH_TEMPLATE_ID = "2300fded-e52f-4564-a92a-a6412b1c0f09";
    private static final String WELSH_TEMPLATE_ID = "b327f28b-010d-47f9-954d-f21a4ee9ddfc";


    @BeforeEach
    public void before() {
        // use defaults
        defendantAcceptedAocpProcessor.replyToAddress = "noreply@cjscp.org.uk";
        defendantAcceptedAocpProcessor.englishTemplateId = ENGLISH_TEMPLATE_ID;
        defendantAcceptedAocpProcessor.welshTemplateId = WELSH_TEMPLATE_ID;
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
        DefendantAcceptedAocp defendantAcceptedAocp = generateOnlinePleaReceived(email, urn, postcode);
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantAcceptedAocp.class)).thenReturn(defendantAcceptedAocp);
        when(referenceDataService.getCountryByPostcode("W1 1AA", event)).thenReturn(country);
        when(systemIdMapperService.getSystemUserId()).thenReturn(randomUUID());

        defendantAcceptedAocpProcessor.sendPleaNotificationEmail(event);

        verify(sender, times(2)).send(argumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = argumentCaptor.getAllValues().get(0);
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

    private static DefendantAcceptedAocp generateOnlinePleaReceived(final String email, final String urn, final String postcode) {
        final Boolean outstandingFines = false;
        final PersonalDetails personalDetails = new PersonalDetails(
                "Bobby", "Davro",
                new Address("82 Old Rd", "Leicester", "London", "UK", "United Kingdom", postcode),
                new ContactDetails("07429 567901", "07429 567901", "07429 567999", email, null),
                LocalDate.of(1981, 1, 1),
                "JH41 1269B", "london", "TESTYJKZAAA09", null, null);
        return new DefendantAcceptedAocp(UUID.randomUUID(), UUID.randomUUID(),
                null, null, personalDetails, true, null, urn);
    }
}
