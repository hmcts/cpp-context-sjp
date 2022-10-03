package uk.gov.moj.cpp.sjp.query.view.response;

import java.time.LocalDate;
import java.util.UUID;

public class CaseWithoutDefendantPostcodeView {

    private final UUID id;

    private final String urn;

    private final LocalDate postingDate;

    private final String firstName;

    private final String lastName;

    private final String legalEntityName;

    private final String prosecutingAuthority;

    public CaseWithoutDefendantPostcodeView(final UUID id,
                                            final String urn,
                                            final LocalDate postingDate,
                                            final String firstName,
                                            final String lastName,
                                            final String prosecutingAuthority,
                                            final String legalEntityName) {
        this.id = id;
        this.urn = urn;
        this.postingDate = postingDate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.prosecutingAuthority = prosecutingAuthority;
        this.legalEntityName = legalEntityName;
    }

    public UUID getId() {
        return id;
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

    public String getUrn() {
        return urn;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }
}
