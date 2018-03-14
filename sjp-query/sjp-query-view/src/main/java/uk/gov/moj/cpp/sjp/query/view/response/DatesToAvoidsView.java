package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.util.List;

public class DatesToAvoidsView {

    private final List<PendingDatesToAvoid> pendingDatesToAvoid;

    public DatesToAvoidsView(final List<PendingDatesToAvoid> pendingDatesToAvoid) {
        this.pendingDatesToAvoid = pendingDatesToAvoid;
    }

    public List<PendingDatesToAvoid> getPendingDatesToAvoid() {
        return pendingDatesToAvoid;
    }

    public int getPendingDatesToAvoidCount() {
        return pendingDatesToAvoid.size();
    }
}