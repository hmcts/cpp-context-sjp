package uk.gov.moj.cpp.sjp.domain.decision;

import java.time.LocalDate;

public interface ConvictingDecision {

    public abstract LocalDate getConvictionDate();

    public abstract SessionCourt getConvictingCourt();

}
