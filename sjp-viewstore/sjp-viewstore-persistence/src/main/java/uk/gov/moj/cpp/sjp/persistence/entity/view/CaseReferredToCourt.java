package uk.gov.moj.cpp.sjp.persistence.entity.view;

import java.time.LocalDate;
import java.util.UUID;

public class CaseReferredToCourt {

    private UUID caseId;
    private String urn;
    private String firstName;
    private String lastName;
    private String interpreterLanguage;
    private LocalDate hearingDate;

    public CaseReferredToCourt(final UUID caseId, final String urn, final String firstName,
                               final String lastName, final String interpreterLanguage,
                               final LocalDate hearingDate) {
        this.caseId = caseId;
        this.urn = urn;
        this.firstName = firstName;
        this.lastName = lastName;
        this.interpreterLanguage = interpreterLanguage;
        this.hearingDate = hearingDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }
}
