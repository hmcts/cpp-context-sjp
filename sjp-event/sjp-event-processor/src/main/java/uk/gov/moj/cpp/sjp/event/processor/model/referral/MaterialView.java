package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;

public class MaterialView {

    private final UUID id;
    private final String name;
    private final ZonedDateTime uploadDateTime;
    @JsonIgnore
    private final String mimeType;

    public MaterialView(final UUID id,
                        final String name,
                        final ZonedDateTime uploadDateTime,
                        final String mimeType) {
        this.id = id;
        this.name = name;
        this.uploadDateTime = uploadDateTime;
        this.mimeType = mimeType;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ZonedDateTime getUploadDateTime() {
        return uploadDateTime;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, uploadDateTime, mimeType);
    }

    @Override
    public String toString() {
        return "MaterialView{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", uploadDateTime=" + uploadDateTime +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
