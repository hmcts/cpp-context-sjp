package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static uk.gov.justice.core.courts.LegalEntityDefendant.*;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.json.schemas.domains.sjp.results.CorporateDefendant;

import javax.inject.Inject;

public class LegalEntityDefendantConverter {

    @Inject
    private AddressConverter addressConverter;

    @Inject
    private CorporateContactNumberConverter corporateContactNumberConverter;

    public LegalEntityDefendant getLegalEntityDefendant(final CorporateDefendant corporateDefendant) {
        if (corporateDefendant != null) {
            return legalEntityDefendant()
                    .withOrganisation(Organisation.organisation()//Mandatory
                            .withName(corporateDefendant.getOrganisationName())
                            .withAddress(addressConverter.getAddress(corporateDefendant.getAddress()))
                            .withContact(corporateContactNumberConverter.getContact(corporateDefendant))
                            .build())
                    .build();
        }
        return null;
    }
}
