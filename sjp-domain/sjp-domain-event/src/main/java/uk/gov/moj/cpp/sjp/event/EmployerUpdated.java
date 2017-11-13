package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.employer-updated")
public class EmployerUpdated {

    private UUID defendantId;

    private String name;

    private String employeeReference;

    private String phone;

    private Address address;

    @JsonCreator
    public EmployerUpdated(@JsonProperty ("defendantId") final UUID defendantId,
                           @JsonProperty ("name") final String name,
                           @JsonProperty ("employeeReference") final String employeeReference,
                           @JsonProperty ("phone") final String phone,
                           @JsonProperty ("address") final Address address) {
        this.defendantId = defendantId;
        this.name = name;
        this.employeeReference = employeeReference;
        this.phone = phone;
        this.address = address;
    }

    public EmployerUpdated(final Employer employer) {
        this(employer.getDefendantId(),
                employer.getName(),
                employer.getEmployeeReference(),
                employer.getPhone(),
                employer.getAddress());
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