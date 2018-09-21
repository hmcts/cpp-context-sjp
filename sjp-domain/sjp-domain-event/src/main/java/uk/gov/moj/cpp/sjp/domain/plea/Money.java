package uk.gov.moj.cpp.sjp.domain.plea;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Money implements Serializable {

    private static final long serialVersionUID = 1L;

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
