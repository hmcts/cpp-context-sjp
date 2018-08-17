package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class OffenceToOffenceDetailTest {

    private OffenceToOffenceDetail offenceToOffenceDetail = new OffenceToOffenceDetail();

    @Test
    public void shouldConvertOffenceToOffenceDetail() {
        Offence inputOffence = new Offence(UUID.randomUUID(), 1, "PS00001",
                LocalDate.of(2016, 1, 1), 6,
                LocalDate.of(2016, 1, 2), "Committed some offence",
                "Prosecution facts", "Witness statement", BigDecimal.ONE);

        OffenceDetail outputOffence = offenceToOffenceDetail.convert(inputOffence);

        OffenceDetail expectedOffence = OffenceDetail.builder()
                .setId(inputOffence.getId())
                .setCode(inputOffence.getLibraOffenceCode())
                .setSequenceNumber(inputOffence.getOffenceSequenceNo())
                .setWording(inputOffence.getOffenceWording())
                .setStartDate(inputOffence.getOffenceCommittedDate())
                .setChargeDate(inputOffence.getChargeDate())
                .withLibraOffenceDateCode(inputOffence.getLibraOffenceDateCode())
                .withProsecutionFacts(inputOffence.getProsecutionFacts())
                .withWitnessStatement(inputOffence.getWitnessStatement())
                .withCompensation(inputOffence.getCompensation())
                .build();

        assertTrue(reflectionEquals(outputOffence, expectedOffence));
    }
}