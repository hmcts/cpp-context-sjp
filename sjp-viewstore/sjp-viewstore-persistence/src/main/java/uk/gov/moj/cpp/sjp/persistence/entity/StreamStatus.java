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

    @Column(name = "version")
    private int version;

    public StreamStatus() {
    }

    public StreamStatus(UUID streamId, int version) {
        this.streamId = streamId;
        this.version = version;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public int getVersion() {
        return version;
    }
}
