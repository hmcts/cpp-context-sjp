package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Objects.isNull;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.json.schemas.domains.sjp.LegalEntityDetails;

import javax.inject.Inject;

public class LegalEntityDefendantConverter {

    @Inject
    private AddressConverter addressConverter;

    @Inject
    private LegalEntityContactConverter legalEntityContactConverter;

    public LegalEntityDefendant getLegalEntityDefendant(final LegalEntityDetails legalEntityDetails) {
        if (isNull(legalEntityDetails)) {
            return null;
        }
        return legalEntityDefendant()
                .withOrganisation(Organisation.organisation()//Mandatory
                        .withName(legalEntityDetails.getLegalEntityName())
                        .withAddress(addressConverter.getAddress(legalEntityDetails.getAddress()))
                        .withContact(legalEntityContactConverter.getContact(legalEntityDetails.getContactDetails()))
                        .build())
                .build();


    }
}
