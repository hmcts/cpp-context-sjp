package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OffenceCode implements Serializable {

    private final UUID id;

    private final String code;

    @JsonCreator
    public OffenceCode(@JsonProperty(value = "id", required = true) final UUID id,
                       @JsonProperty(value = "code", required = true)final String code) {
        this.id = id;
        this.code = code;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

}
