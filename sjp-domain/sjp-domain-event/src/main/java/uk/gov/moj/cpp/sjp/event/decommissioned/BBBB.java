package uk.gov.moj.cpp.sjp.event.decommissioned;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BBBB {

    private String a;
    private String b;
    @JsonProperty
    private String c;

    @JsonCreator
    public BBBB(@JsonProperty("a") String a, @JsonProperty("b") String b) {
        this.a = a;
        this.b = b;
        System.out.println("OOOOOOOOOOOOOO");
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

}
