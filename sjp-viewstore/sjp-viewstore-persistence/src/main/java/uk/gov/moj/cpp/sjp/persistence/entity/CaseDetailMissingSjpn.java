package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;


/**
 * pojo to hold details of a case
 * missing an SJPN and corresponding defendant
 * details
 */
public class CaseDetailMissingSjpn {

    private UUID id;
    private String urn;
    private LocalDate postingDate;
    private String firstName;
    private String lastName;

    public CaseDetailMissingSjpn() {

    }

    public CaseDetailMissingSjpn(UUID id, String urn, LocalDate postingDate, String firstName, String lastName) {
        this.id = id;
        this.urn = urn;
        this.postingDate = postingDate;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return "CaseDetailMissingSjpn{" +
                "id=" + id +
                ", urn=" + urn +
                ", postingDate=" + postingDate +
                ", firstName=" + firstName +
                ", lastName=" + lastName +
                '}';
    }

}
