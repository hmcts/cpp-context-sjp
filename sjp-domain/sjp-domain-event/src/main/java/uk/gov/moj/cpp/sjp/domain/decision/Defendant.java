package uk.gov.moj.cpp.sjp.domain.decision;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Defendant implements Serializable {

    private final CourtDetails court;

    public Defendant(@JsonProperty("court") final CourtDetails court) {
        this.court = court;
    }

    public CourtDetails getCourt() {
        return court;
    }
}
