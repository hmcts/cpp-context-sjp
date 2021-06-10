package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.util.UUID;

public class ConvictionInfo {

    private UUID offenceId;

    private VerdictType verdictType;

    private LocalDate convictionDate;

    public ConvictionInfo(final UUID offenceId,
                          final VerdictType verdictType,
                          final LocalDate convictionDate) {
        this.offenceId = offenceId;
        this.verdictType = verdictType;
        this.convictionDate = convictionDate;
    }


    public UUID getOffenceId() {
        return offenceId;
    }

    public VerdictType getVerdictType() {
        return verdictType;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }


}
