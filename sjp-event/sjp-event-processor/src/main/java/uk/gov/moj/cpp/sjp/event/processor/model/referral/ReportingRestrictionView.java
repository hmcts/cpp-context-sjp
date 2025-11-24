package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.time.LocalDate;
import java.util.UUID;

public class ReportingRestrictionView {

    private UUID id;

    private String label;

    private LocalDate orderedDate;

    public ReportingRestrictionView(final UUID id, final String label, final LocalDate orderedDate) {
        this.id = id;
        this.label = label;
        this.orderedDate = orderedDate;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public LocalDate getOrderedDate() {
        return orderedDate;
    }
}
