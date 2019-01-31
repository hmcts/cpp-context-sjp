package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Optional.ofNullable;

import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;

import java.util.List;

public class NotifiedPleaViewHelper {

    public NotifiedPleaView createNotifiedPleaView(
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final List<Offence> defendantOffences) {

        final Offence firstOffence = defendantOffences.get(0);

        return ofNullable(firstOffence.getPlea())
                .map(plea -> new NotifiedPleaView(
                        firstOffence.getId(),
                        firstOffence.getPleaDate().toLocalDate(),
                        getNotifiedPlea(plea)))
                .orElseGet(() -> new NotifiedPleaView(
                        firstOffence.getId(),
                        caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                        "NO_NOTIFICATION"));
    }

    private static String getNotifiedPlea(final PleaType pleaType) {
        switch (pleaType) {
            case NOT_GUILTY:
                return "NOTIFIED_NOT_GUILTY";
            case GUILTY:
            case GUILTY_REQUEST_HEARING:
                return "NOTIFIED_GUILTY";
            default:
                throw new UnsupportedOperationException("Notified plea not defined for " + pleaType);
        }
    }
}
