package uk.gov.moj.cpp.sjp.persistence.builder;


import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.json.schemas.domains.sjp.Gender.FEMALE;
import static uk.gov.moj.cpp.sjp.persistence.builder.PersonalDetailsBuilder.buildPersonalDetails;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class DefendantDetailBuilder {

    private static final String DEFAULT_POSTCODE = "CR0 1AB";

    private DefendantDetail defendantDetail;
    private OffenceDetail.OffenceDetailBuilder offenceBuilder;
    private List<OffenceDetail> offences;

    private DefendantDetailBuilder() {
        defendantDetail = new DefendantDetail();
        defendantDetail.setId(UUID.randomUUID());
        defendantDetail.setPersonalDetails(
               buildPersonalDetails()
                       .withTitle("Mrs")
                       .withFirstName("Theresa")
                       .withLastName("May")
                       .withDateOfBirth(LocalDate.of(10960,10,8))
                       .withGender(FEMALE)
                       .build());
        defendantDetail.setAddress(new Address("10 Downing St", "Westminster", "London", "Greater London", "England", DEFAULT_POSTCODE));
        defendantDetail.setUpdatesAcknowledgedAt(ZonedDateTime.now());
        offenceBuilder = prepareOffenceBuilder();
    }

    public static DefendantDetailBuilder aDefendantDetail() {
        return new DefendantDetailBuilder();
    }

    public DefendantDetailBuilder withId(final UUID defendantId) {
        defendantDetail.setId(defendantId);
        return this;
    }

    public DefendantDetailBuilder withPlea(final PleaType plea) {
        offenceBuilder.setPlea(plea);
        return this;
    }

    public DefendantDetailBuilder withOffenceCode(final String offenceCode) {
        offenceBuilder.setCode(offenceCode);
        return this;
    }

    public DefendantDetailBuilder withInterpreterLanguage(final String interpreterLanguage) {
        defendantDetail.setInterpreter(new InterpreterDetail(interpreterLanguage));
        return this;
    }

    public DefendantDetailBuilder withPostcode(final String postcode) {
        defendantDetail.setAddress(new Address("addr1", "addr2", "addr3", "addr4", "addr5", postcode));
        return this;
    }

    public DefendantDetailBuilder withCaseDetail(final CaseDetail caseDetail) {
        defendantDetail.setCaseDetail(caseDetail);
        return this;
    }

    public DefendantDetailBuilder withPersonalDetails(final PersonalDetails personalDetails) {
        defendantDetail.setPersonalDetails(personalDetails);
        return this;
    }

    public DefendantDetailBuilder withLegalEntityDetails(final LegalEntityDetails legalEntityDetails) {
        defendantDetail.setLegalEntityDetails(legalEntityDetails);
        return this;
    }
    public DefendantDetailBuilder withLastName(final String lastName) {
        defendantDetail.getPersonalDetails().setLastName(lastName);
        return this;
    }

    public DefendantDetailBuilder withNumberOfPreviousConvictions(final int numberOfPreviousConvictions) {
        defendantDetail.setNumPreviousConvictions(numberOfPreviousConvictions);
        return this;
    }

    public DefendantDetailBuilder withOffences(final List<OffenceDetail> offences) {
        this.offences = offences;
        return this;
    }

    public DefendantDetailBuilder withFirstName(final String firstName) {
        this.defendantDetail.getPersonalDetails().setFirstName(firstName);
        return this;
    }

    public DefendantDetailBuilder withRegion(final String region) {
        this.defendantDetail.setRegion(region);
        return this;
    }

    public DefendantDetailBuilder withUpdatesAcknowledgedAt(final ZonedDateTime updatesAcknowledgedAt) {
        this.defendantDetail.setUpdatesAcknowledgedAt(updatesAcknowledgedAt);
        return this;
    }

    public DefendantDetailBuilder withAddressUpdatedAt(final ZonedDateTime addressUpdatedAt) {
        this.defendantDetail.setAddressUpdatedAt(addressUpdatedAt);
        return this;
    }

    public DefendantDetailBuilder withNameUpdatedAt(final ZonedDateTime nameUpdatedAt) {
        this.defendantDetail.setNameUpdatedAt(nameUpdatedAt);
        return this;
    }

    public DefendantDetail build() {
        defendantDetail.setOffences(getOffences());
        return defendantDetail;
    }

    private List<OffenceDetail> getOffences() {
        return nonNull(offences) ? offences : singletonList(offenceBuilder.build());
    }

    private OffenceDetail.OffenceDetailBuilder prepareOffenceBuilder() {
        return new OffenceDetail.OffenceDetailBuilder()
                .setId(UUID.randomUUID())
                .setChargeDate(LocalDate.now())
                .setCode("")
                .setPlea(null)
                .setSequenceNumber(1)
                .setStartDate(LocalDate.now())
                .withLibraOffenceDateCode(30)
                .withWitnessStatement("witness statement")
                .withProsecutionFacts("prosecution facts")
                .setWording("");
    }
}
