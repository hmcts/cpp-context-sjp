package uk.gov.moj.cpp.sjp.persistence.builder;


import static com.google.common.collect.Sets.newHashSet;

import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.time.LocalDate;
import java.util.UUID;

public class DefendantDetailBuilder {

    public final UUID DEFENDANT_ID = UUID.randomUUID();
    public final UUID PERSON_ID = UUID.randomUUID();

    private DefendantDetail defendantDetail;

    private OffenceDetail.OffenceDetailBuilder offenceBuilder;

    private DefendantDetailBuilder() {
        defendantDetail = new DefendantDetail();
        defendantDetail.setId(DEFENDANT_ID);
        defendantDetail.setPersonId(PERSON_ID);
        offenceBuilder = prepareOffenceBuilder();
    }

    public static DefendantDetailBuilder aDefendantDetail() {
        return new DefendantDetailBuilder();
    }

    public DefendantDetailBuilder withPersonId(UUID personId) {
        defendantDetail.setPersonId(personId);
        return this;
    }

    public DefendantDetailBuilder withOffencePendingWithdrawal(final boolean isOffencePendingWithdrawal) {
        offenceBuilder.setPendingWithdrawal(isOffencePendingWithdrawal);
        return this;
    }

    public DefendantDetailBuilder withPlea(final Plea.Type plea) {
        offenceBuilder.setPlea(plea.name());
        return this;
    }

    public DefendantDetailBuilder withInterpreterLanguage(final String interpreterLanguage) {
        defendantDetail.setInterpreter(new InterpreterDetail(true, interpreterLanguage));
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
