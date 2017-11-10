package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.PersonalDetailsUpdatedProcessor;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PersonalDetailsUpdatedProcessorTest {

    @Mock
    private Requester requester;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private PersonalDetailsUpdatedProcessor personalDetailsUpdatedProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Test
    public void getsCaseIdAndSendsPersonInfoCommandAfterReceivingPersonalDetailsUpdatedEvent() {
        final UUID personId = UUID.randomUUID();
        final String firstName = "Teresa";
        final String lastName = "May";
        final String dateOfBirth = "1956-01-01";
        final String postCode = "SW1A 2AA";

        final JsonEnvelope personalDetailsUpdatedEvent = JsonEnvelopeBuilder.envelopeFrom(
            metadataWithRandomUUID("people.personal-details-updated"),
                createObjectBuilder()
                        .add("personId", personId.toString())
                        .add("firstName", firstName)
                        .add("lastName", lastName)
                        .add("dateOfBirth", dateOfBirth)
                        .add("nationality", "British") //this field is not used just for testing purposes
                        .add("address", createObjectBuilder()
                                .add("postCode", postCode)
                        )
                        .build()
        );

        personalDetailsUpdatedProcessor.personalDetailsUpdated(personalDetailsUpdatedEvent);

        verify(sender).sendAsAdmin(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope capturedRequestToUpdatePersonInfo = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(capturedRequestToUpdatePersonInfo, (jsonEnvelope(
                withMetadataEnvelopedFrom(personalDetailsUpdatedEvent).withName("sjp.command.update-person-info"),
                payload().isJson(allOf(
                        withJsonPath("$.personId", is(personId.toString())),
                        withJsonPath("$.firstName", is(firstName)),
                        withJsonPath("$.lastName", is(lastName)),
                        withJsonPath("$.dateOfBirth", is(dateOfBirth)),
                        withJsonPath("$.postCode", is(postCode))
                ))
        )));
    }
}