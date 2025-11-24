package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.util.UUID;

public class ConvictionInfo {

    private UUID offenceId;

    private VerdictType verdictType;

    private LocalDate convictionDate;

    private CourtCentre convictingCourt;

    public ConvictionInfo(final UUID offenceId,
                          final VerdictType verdictType,
                          final LocalDate convictionDate,
                          final CourtCentre convictingCourt) {
        this.offenceId = offenceId;
        this.verdictType = verdictType;
        this.convictionDate = convictionDate;
        this.convictingCourt = convictingCourt;
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

    public CourtCentre getConvictingCourt() {
        return convictingCourt;
    }
}
