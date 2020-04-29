package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class AddRequestForOutstandingFinesApiTest {

    private static final String COMMAND_NAME = "sjp.add-request-for-outstanding-fines";
    private static final String NEW_COMMAND_NAME = "sjp.command.add-request-for-outstanding-fines";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private AddRequestForOutstandingFinesApi addRequestForOutstandingFinesApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void addRequestForOutstandingFinesWithoutHearingDate() {

        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), requestPayload);

        addRequestForOutstandingFinesApi.addRequestForOutstandingFines(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope jsonEnvelopOut = envelopeCaptor.getValue();
        //check that payload was passed through and meta data name was changed
        assertThat(jsonEnvelopOut, is(jsonEnvelope(
                withMetadataEnvelopedFrom(commandJsonEnvelope).withName(NEW_COMMAND_NAME),
                payloadIsJson(hasJsonPath("$.*", empty())))
        ));
    }
}