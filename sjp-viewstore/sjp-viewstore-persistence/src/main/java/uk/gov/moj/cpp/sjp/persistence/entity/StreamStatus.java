package uk.gov.moj.cpp.sjp.persistence.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "stream_status")
public class StreamStatus {

    @Id
    @Column(name = "stream_id")
    private UUID streamId;

    @Column(name = "source")
    private String source;

    @Column(name = "component")
    private String component;

    @Column(name = "position")
    private int position;

    public StreamStatus() {
    }

    public StreamStatus(final UUID streamId, final String source, final String component, final int position) {
        this.streamId = streamId;
        this.source = source;
        this.component = component;
        this.position = position;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public String getSource() {
        return source;
    }

    public String getComponent() {
        return component;
    }

    public int getPosition() {
        return position;
    }
}
