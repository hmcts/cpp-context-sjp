package uk.gov.moj.cpp.sjp.persistence.builder;


import static java.util.Collections.singletonList;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
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
                        null,
                        null,
                        new Address("10 Downing St", "Westminster", "London", "Greater London", "England", DEFAULT_POSTCODE),
                        new ContactDetails(),
                        null
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
        defendantDetail.getPersonalDetails().setAddress(new Address("addr1", "addr2", "addr3", "addr4", "addr5", postcode));
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

    public DefendantDetailBuilder withLastName(final String lastName) {
        defendantDetail.getPersonalDetails().setLastName(lastName);
        return this;
    }

    public DefendantDetailBuilder withNumberOfPreviousConvictions(final int numberOfPreviousConvictions) {
        defendantDetail.setNumPreviousConvictions(numberOfPreviousConvictions);
        return this;
    }

    public DefendantDetail build() {
        defendantDetail.setOffences(singletonList(offenceBuilder.build()));
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
