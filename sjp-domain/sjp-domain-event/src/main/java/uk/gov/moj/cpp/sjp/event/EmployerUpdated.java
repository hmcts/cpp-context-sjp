package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.employer-updated")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployerUpdated {

    private UUID defendantId;
    private String name;
    private String employeeReference;
    private String phone;
    private Address address;
    private boolean updatedByOnlinePlea;
    private ZonedDateTime updatedDate;

    @JsonCreator
    private EmployerUpdated(@JsonProperty("defendantId") final UUID defendantId,
                            @JsonProperty("name") final String name,
                            @JsonProperty("employeeReference") final String employeeReference,
                            @JsonProperty("phone") final String phone,
                            @JsonProperty("address") final Address address,
                            @JsonProperty("updatedByOnlinePlea") final boolean updatedByOnlinePlea,
                            @JsonProperty("updatedDate") final ZonedDateTime updatedDate) {
        this.defendantId = defendantId;
        this.name = name;
        this.employeeReference = employeeReference;
        this.phone = phone;
        this.address = address;
        this.updatedByOnlinePlea = updatedByOnlinePlea;
        this.updatedDate = updatedDate;
    }

    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    private EmployerUpdated(final UUID defendantId, final Employer employer, final boolean updatedByOnlinePlea, final ZonedDateTime updatedDate) {
        this(defendantId,
                employer.getName(),
                employer.getEmployeeReference(),
                employer.getPhone(),
                employer.getAddress(),
                updatedByOnlinePlea,
                updatedDate);
    }

    public static EmployerUpdated createEvent(final UUID defendantId, final Employer employer) {
        return new EmployerUpdated(defendantId, employer,false, null);
    }

    public static EmployerUpdated createEventForOnlinePlea(final UUID defendantId, final Employer employer, final ZonedDateTime updatedDate) {
        return new EmployerUpdated(defendantId, employer, true, updatedDate);
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

    public boolean isUpdatedByOnlinePlea() {
        return updatedByOnlinePlea;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }
}