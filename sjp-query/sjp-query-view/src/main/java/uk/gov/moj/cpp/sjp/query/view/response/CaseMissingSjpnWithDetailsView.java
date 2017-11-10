package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn;

import java.time.LocalDate;
import java.util.UUID;

public class CaseMissingSjpnWithDetailsView {

    private final UUID id;
    private final String urn;
    private final LocalDate postingDate;
    private final String firstName;
    private final String lastName;

    public CaseMissingSjpnWithDetailsView(CaseDetailMissingSjpn caseDetailMissingSjpn){
        this.id = caseDetailMissingSjpn.getId();
        this.urn = caseDetailMissingSjpn.getUrn();
        this.postingDate = caseDetailMissingSjpn.getPostingDate();
        this.firstName = caseDetailMissingSjpn.getFirstName();
        this.lastName = caseDetailMissingSjpn.getLastName();
    }

    public CaseMissingSjpnWithDetailsView(UUID id, String urn, LocalDate postingDate, String firstName, String lastName) {
        this.id = id;
        this.urn = urn;
        this.postingDate = postingDate;
        this.firstName = firstName;
        this.lastName = lastName;
    }


    public UUID getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

}
