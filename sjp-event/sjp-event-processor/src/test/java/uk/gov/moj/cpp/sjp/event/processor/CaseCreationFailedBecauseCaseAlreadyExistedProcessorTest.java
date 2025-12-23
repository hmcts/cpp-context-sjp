package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseCreationFailedBecauseCaseAlreadyExistedProcessorTest {

    private final String CASE_ID = UUID.randomUUID().toString();

    @InjectMocks
    private CaseCreationFailedBecauseCaseAlreadyExistedProcessor caseCreationFailedBecauseCaseAlreadyExistedProcessor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper envelopers = createEnveloper();

    @Test
    public void publish() {
        //given
        final String URN = "urn";

        final JsonEnvelope privateEvent = EnvelopeFactory
                .createEnvelope("sjp.events.case-creation-failed-because-case-already-existed",
                                JsonObjects.createObjectBuilder().add("caseId", CASE_ID).add("urn", URN)
                                                .build());

        //when
        caseCreationFailedBecauseCaseAlreadyExistedProcessor.publish(privateEvent);

        //then
        verify(sender).send(argThat(
                jsonEnvelope(metadata().withName("public.sjp.case-creation-failed-because-case-already-existed"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is(CASE_ID)),
                                        withJsonPath("$.urn", is(URN)))))
        ));
    }
}
