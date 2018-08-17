package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

public class OffenceToOffenceDetail implements Converter<Offence, OffenceDetail> {

    @Override
    public OffenceDetail convert(Offence offence) {
        return new OffenceDetail.OffenceDetailBuilder()
                .setId(offence.getId())
                .setCode(offence.getLibraOffenceCode())
                .setSequenceNumber(offence.getOffenceSequenceNo())
                .setWording(offence.getOffenceWording())
                .setStartDate(offence.getOffenceCommittedDate())
                .setChargeDate(offence.getChargeDate())
                .withLibraOffenceDateCode(offence.getLibraOffenceDateCode())
                .withProsecutionFacts(offence.getProsecutionFacts())
                .withWitnessStatement(offence.getWitnessStatement())
                .withCompensation(offence.getCompensation())
                .build();
    }

}