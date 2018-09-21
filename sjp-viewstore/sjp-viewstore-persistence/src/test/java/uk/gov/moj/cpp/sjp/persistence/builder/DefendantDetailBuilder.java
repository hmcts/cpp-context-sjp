package uk.gov.moj.cpp.sjp.persistence.builder;


import static com.google.common.collect.Sets.newHashSet;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.util.UUID;

public class DefendantDetailBuilder {

    private static final String DEFAULT_POSTCODE = "CR0 1AB";

    private DefendantDetail defendantDetail;

    private OffenceDetail.OffenceDetailBuilder offenceBuilder;

    private DefendantDetailBuilder() {
        defendantDetail = new DefendantDetail();
        defendantDetail.setId(UUID.randomUUID());
        defendantDetail.setPersonalDetails(
                new PersonalDetails(
                        "Mrs",
                        "Theresa",
                        "May",
                        LocalDate.of(1960, 10, 8),
                        Gender.FEMALE,
                        null,
                        new Address("10 Downing St", "Westminster", "London", "England", DEFAULT_POSTCODE),
                        new ContactDetails()
                )
        );
        offenceBuilder = prepareOffenceBuilder();
    }

    public static DefendantDetailBuilder aDefendantDetail() {
        return new DefendantDetailBuilder();
    }

    public DefendantDetailBuilder withId(final UUID defendantId) {
        defendantDetail.setId(defendantId);
        return this;
    }

    public DefendantDetailBuilder withOffencePendingWithdrawal(final boolean isOffencePendingWithdrawal) {
        offenceBuilder.setPendingWithdrawal(isOffencePendingWithdrawal);
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
        defendantDetail.getPersonalDetails().setAddress(new Address("addr1", "addr2", "addr3", "addr4", postcode));
        return this;
    }

    public DefendantDetailBuilder withLastName(final String lastName) {
        defendantDetail.getPersonalDetails().setLastName(lastName);
        return this;
    }

    public DefendantDetail build() {
        defendantDetail.setOffences(newHashSet(offenceBuilder.build()));
        return defendantDetail;
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
