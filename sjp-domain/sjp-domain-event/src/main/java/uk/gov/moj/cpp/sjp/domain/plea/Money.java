package uk.gov.moj.cpp.sjp.domain.plea;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

public class Money implements Serializable {

    private String amount;

    public Money() {
        //default constructor
    }

    @JsonCreator
    public Money(String amount) {
        this.amount = amount;
    }

    @JsonValue
    public String getAmount() {
        return amount;
    }
}
