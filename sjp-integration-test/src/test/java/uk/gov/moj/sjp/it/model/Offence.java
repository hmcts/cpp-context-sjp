package uk.gov.moj.sjp.it.model;

import uk.gov.justice.services.common.converter.LocalDates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.Builder;

@Builder
public class Offence {

    @Builder.Default
    @NotNull(message = "ID is required for offence")
    public String id = UUID.randomUUID().toString();

    @Builder.Default
    public Integer offenceSequenceNo = 1;

    @Builder.Default
    @NotNull(message = "Libra offence code is required for offence")
    public String libraOffenceCode = "PS00001";

    @Builder.Default
    @NotNull(message = "Charge date is required for offence")
    public String chargeDate = LocalDates.to(LocalDate.of(2016, 1, 1));

    @Builder.Default
    public Integer libraOffenceDateCode = 1;

    @Builder.Default
    @NotNull(message = "Offence committed date is required for offence")
    public String offenceCommittedDate = LocalDates.to(LocalDate.of(2016, 1, 1));

    @Builder.Default
    @NotNull(message = "Offence wording is required for offence")
    public String offenceWording = "Committed some offence";

    @Builder.Default
    public String offenceWordingWelsh = "Wedi ymrwymo rhywfaint o drosedd";

    @Builder.Default
    public String prosecutionFacts = "No ticket at the gates, forgery";

    @Builder.Default
    public String witnessStatement = "Jumped over the barriers";

    @Builder.Default
    public BigDecimal compensation = BigDecimal.valueOf(2.34);

    @Builder.Default
    public BigDecimal backDuty = BigDecimal.valueOf(250.25);

    @Builder.Default
    public String backDutyDateFrom = "2018-01-01";

    @Builder.Default
    public String backDutyDateTo = "2018-05-01";

}
