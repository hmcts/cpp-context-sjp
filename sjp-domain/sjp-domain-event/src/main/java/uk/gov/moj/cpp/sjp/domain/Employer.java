package uk.gov.moj.cpp.sjp.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Employer {

    private UUID defendantId;

    private String name;

    private String employeeReference;

    private String phone;

    private Address address;

    @JsonCreator
    public Employer(@JsonProperty("defendantId") UUID defendantId,
                    @JsonProperty("name") String name,
                    @JsonProperty("employeeReference") String employeeReference,
                    @JsonProperty("phone") String phone,
                    @JsonProperty("address") Address address) {
        this.defendantId = defendantId;
        this.name = name;
        this.employeeReference = employeeReference;
        this.phone = phone;
        this.address = address;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getName() {
        return name;
    }

    public String getEmployeeReference() {
        return employeeReference;
    }

    public String getPhone() {
        return phone;
    }

    public Address getAddress() {
        return address;
    }
}