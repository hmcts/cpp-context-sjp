package uk.gov.moj.cpp.sjp.query.view.util.builders;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.util.List;
import java.util.UUID;

public class DefendantEntityBuilder {

    private final UUID id;
    private List<OffenceDetail> offences;
    private PersonalDetails personalDetails;
    private final Integer numPreviousConvictions;
    private final Boolean speakWelsh;
    private final String asn;
    private final String pncIdentifier;
    private final String region;
    private LegalEntityDetails legalEntityDetails;
    private Address address;
    private ContactDetails contactDetails;
    private UUID pcqId;

    private static final String ASN = "ASN";
    private static final String PSF_IDENTIFIER = "PCF IDENTIFIER";
    private static final String REGION = "LONDON";

    private DefendantEntityBuilder(final UUID id,
                                   final List<OffenceDetail> offences,
                                   final PersonalDetails personalDetails,
                                   final Integer numPreviousConvictions,
                                   final Boolean speakWelsh,
                                   final String asn,
                                   final String pcfIdentifier,
                                   final String region,
                                   final LegalEntityDetails legalEntityDetails,
                                   final Address address,
                                   final ContactDetails contactDetails,
                                   final UUID pcqId) {
        this.id = id;
        this.offences = offences;
        this.personalDetails = personalDetails;
        this.numPreviousConvictions = numPreviousConvictions;
        this.speakWelsh = speakWelsh;
        this.asn = asn;
        this.pncIdentifier = pcfIdentifier;
        this.region = region;
        this.legalEntityDetails = legalEntityDetails;
        this.address = address;
        this.contactDetails = contactDetails;
        this.pcqId = pcqId;
    }

    public static DefendantEntityBuilder withDefaults() {
        return new DefendantEntityBuilder(
                randomUUID(),
                null,
                null,
                1,
                false,
                ASN,
                PSF_IDENTIFIER,
                REGION,
                null,
                null,
                null,
                randomUUID());
    }

    public DefendantDetail build() {
        return new DefendantDetail(
                id,
                personalDetails,
                offences,
                numPreviousConvictions,
                speakWelsh,
                asn,
                pncIdentifier,
                region,
                legalEntityDetails,
                address,
                contactDetails,
                pcqId
        );
    }

    public DefendantEntityBuilder withPersonalDetails(final PersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
        return this;
    }

    public DefendantEntityBuilder withLegalEntityDetails(final LegalEntityDetails legalEntityDetails) {
        this.legalEntityDetails = legalEntityDetails;
        return this;
    }

    public DefendantEntityBuilder withAddress(final Address address) {
        this.address = address;
        return this;
    }

    public DefendantEntityBuilder withContactDetails(final ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
        return this;
    }

    public DefendantEntityBuilder withPcqId(final UUID pcqId) {
        this.pcqId = pcqId;
        return this;
    }
}
