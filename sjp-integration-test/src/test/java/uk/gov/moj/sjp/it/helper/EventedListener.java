package uk.gov.moj.sjp.it.helper;

import static java.util.stream.Collectors.toMap;
import static uk.gov.moj.sjp.it.Constants.MESSAGE_QUEUE_TIMEOUT;
import static uk.gov.moj.sjp.it.Constants.PRIVATE_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_ACTIVE_MQ_TOPIC;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.google.common.base.Strings;

/**
 * EventedListener
 *
 * An instantiated helper which makes it easier listen and receive the events happening in result of an asynchronous request.
 * It exposes a fluent API, in which it expects you to call a few methods in a subsequent fashion.
 *
 * First, it expects you to specify probable events that you are interested in by calling 'subscribe' methods.
 * For each event you subscribed, execution will wait for those to occur (or timeout) synchronously before continuing.
 * Each subscribe call creates a separate event stack for that event type, in the events map.
 * When subscribing, it decides the topic of the event based on the event naming convention.
 *
 * Then, it expects you to call 'run' method with runnables or callables which would possibly execute asynchronous behaviour that may result in the subscribed events.
 * If the passed lambda returns a value then it is interpreted as a callable and runner expects that return value to be correlationId to filter coming events against.
 * If the passed lambda does not return a value it is interpreted as a runnable and runner treats the first incoming subscribed event to be the relevant one.
 *
 * Finally, it expect you to call 'popEvent' with the wanted event type,
 * to access event payloads that has been pushed to several different stacks.
 *
 * Same instance could be used with different jobs by calling 'reset' in between.
 *
 * Example usage;
 *  EventedListener runner = new EventedListener()
 *      .subscribe("event")
 *      .subscribe("differentEvent")
 *      .subscribe("totallyDifferentEvent")
 *      .run(() -> runnableThatDoesSomethingAsync())
 *      .run(() -> anotherRunnableThatDoesSomethingAsyncAlso())
 *
 * JsonObject event = runner.popEvent("event");
 * JsonObject anotherInstanceOfTheSameEvent = runner.popEvent("event");
 * JsonObject differentEvent = runner.popEvent("differentEvent");
 * JsonObject totallyDifferentEvent = runner.popEvent("totallyDifferentEvent");
 *
 */
public class EventedListener {

    private final Map<String,LinkedList<JsonEnvelope>> eventsByName;

    public EventedListener() {
        this.eventsByName = new HashMap<>();
    }

    public EventedListener subscribe(String eventName) {
        this.eventsByName.putIfAbsent(eventName, new LinkedList<>());
        return this;
    }

    public EventedListener unsubscribe(String eventName) {
        this.eventsByName.remove(eventName);
        return this;
    }

    public EventedListener run(final Runnable action){
        Map<String, MessageConsumerClient> consumers = eventsByName.keySet().parallelStream().collect(toMap(p -> p, this::startConsumer));

        action.run();

        consumers.entrySet().parallelStream().forEach(this::receiveAndClose);

        return this;
    }

    public EventedListener run(final Callable action){
        Map<String, MessageConsumerClient> consumers = eventsByName.keySet().parallelStream().collect(toMap(p -> p, this::startConsumer));

        Optional<UUID> correlationId = runAndCheckForCorrelationId(action);

        if( correlationId.isPresent() ){
            consumers.entrySet().parallelStream().forEach((entry) -> receiveAndClose(entry, correlationId.get()));
        } else {
            consumers.entrySet().parallelStream().forEach(this::receiveAndClose);
        }

        return this;
    }

    public Optional<JsonEnvelope> popEvent(String eventName){
        return Optional.ofNullable(this.eventsByName.get(eventName).poll());
    }

    public EventedListener reset(){
        this.eventsByName.clear();
        return this;
    }

    private MessageConsumerClient startConsumer(String eventName) {
        MessageConsumerClient messageConsumer = new MessageConsumerClient();
        messageConsumer.startConsumer(eventName, determineTopic(eventName));
        return messageConsumer;
    }

    private Optional<UUID> runAndCheckForCorrelationId(final Callable action){
        try {
            return Optional.of((UUID) action.call());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void receiveAndClose(Map.Entry<String, MessageConsumerClient> entry, UUID correlationId) {
        try( MessageConsumerClient consumer = entry.getValue() ){

            while( true ){
                Optional<String> optionalMessage = consumer.retrieveMessage(MESSAGE_QUEUE_TIMEOUT);
                if(!optionalMessage.isPresent()) {
                    break;
                }

                JsonEnvelope jsonEnvelope = new DefaultJsonObjectEnvelopeConverter().asEnvelope(optionalMessage.get());
                Optional<String> eventCorrelationId = jsonEnvelope.metadata().clientCorrelationId();
                boolean matched = eventCorrelationId.isPresent() && correlationId.equals(UUID.fromString(eventCorrelationId.get()));

                if( matched ){
                    eventsByName.get(entry.getKey()).add(jsonEnvelope);
                    break;
                }
            }
        }
    }

    private void receiveAndClose(Map.Entry<String, MessageConsumerClient> entry) {
        try( MessageConsumerClient consumer = entry.getValue() ) {
            String eventName = entry.getKey();

            Optional<String> optionalMessage = consumer.retrieveMessage(MESSAGE_QUEUE_TIMEOUT);
            optionalMessage.ifPresent((message) -> {
                JsonEnvelope jsonEnvelope = new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
                eventsByName.get(eventName).add(jsonEnvelope);
            });
        }
    }

    private String determineTopic(String eventName){
        if (Strings.isNullOrEmpty(eventName)){
            throw new RuntimeException("Event topic could not be determined");
        } else if ( eventName.startsWith("public") ){
            return PUBLIC_ACTIVE_MQ_TOPIC;
        } else if ( eventName.contains(PRIVATE_ACTIVE_MQ_TOPIC) ) {
            return PRIVATE_ACTIVE_MQ_TOPIC;
        } else {
            throw new RuntimeException("Event topic could not be determined");
        }
    }

}
