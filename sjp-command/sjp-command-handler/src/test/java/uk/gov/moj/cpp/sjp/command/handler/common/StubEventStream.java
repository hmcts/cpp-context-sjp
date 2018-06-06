package uk.gov.moj.cpp.sjp.command.handler.common;

import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.Tolerance;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class StubEventStream implements EventStream {

    private List<JsonEnvelope> events = new ArrayList<>();

    public List<JsonEnvelope> getEvents() {
        return events;
    }

    @Override
    public Stream<JsonEnvelope> read() {
        return null;
    }

    @Override
    public Stream<JsonEnvelope> readFrom(long l) {
        return null;
    }

    @Override
    public long append(Stream<JsonEnvelope> events) {
        events.forEach(this.events::add);
        return 0;
    }

    @Override
    public long append(Stream<JsonEnvelope> stream, Tolerance tolerance) {
        stream.forEach(this.events::add);
        return 0;
    }

    @Override
    public long appendAfter(Stream<JsonEnvelope> stream, long l) {
        return 0;
    }

    @Override
    public long getCurrentVersion() {
        return 0;
    }

    @Override
    public UUID getId() {
        return null;
    }
}
