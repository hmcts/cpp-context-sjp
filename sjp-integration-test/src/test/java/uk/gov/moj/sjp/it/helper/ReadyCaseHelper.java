package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

public class ReadyCaseHelper implements AutoCloseable {

    private final MessageConsumer privateMessageConsumer;

    public ReadyCaseHelper() {
        privateMessageConsumer = TopicUtil.privateEvents.createConsumerForMultipleSelectors(
                CaseMarkedReadyForDecision.EVENT_NAME,
                CaseUnmarkedReadyForDecision.EVENT_NAME,
                CaseExpectedDateReadyChanged.EVENT_NAME);
    }

    @Override
    public void close() throws JMSException {
        privateMessageConsumer.close();
    }

    public void verifyCaseUnmarkedReadyForDecisionEventEmitted(final UUID caseId, final LocalDate expectedDateReady) {
        assert expectedDateReady != null;
        final Optional<JsonEnvelope> event = retrieveMessageAsJsonEnvelope(privateMessageConsumer);
        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), jsonEnvelope(
                metadata().withName(CaseUnmarkedReadyForDecision.EVENT_NAME),
                payloadIsJson((allOf(
                        withJsonPath("caseId", equalTo(caseId.toString()))
                        //withJsonPath("expectedDateReady", equalTo(expectedDateReady.toString()))  // TODO should be handled as part of ATCM-4395
                )))));
    }

    public void verifyCaseMarkedReadyForDecisionEventEmitted(final UUID caseId, final CaseReadinessReason readinessReason, final SessionType sessionType, final Priority priority) {
        final Optional<JsonEnvelope> event = retrieveMessageAsJsonEnvelope(privateMessageConsumer);
        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), jsonEnvelope(
                metadata().withName(CaseMarkedReadyForDecision.EVENT_NAME),
                payloadIsJson((allOf(
                        withJsonPath("caseId", equalTo(caseId.toString())),
                        withJsonPath("reason", equalTo(readinessReason.toString())),
                        withJsonPath("sessionType", equalTo(sessionType.toString())),
                        withJsonPath("priority", equalTo(priority.name()))
                )))));
    }
}
