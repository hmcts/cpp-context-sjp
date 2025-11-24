package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Optional.ofNullable;

import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.moj.cpp.sjp.model.prosecution.NotifiedPleaView;

import java.time.LocalDate;

public class NotifiedPleaViewHelper {

    private NotifiedPleaViewHelper() {
    }

    public static NotifiedPleaView createNotifiedPleaView(
            final LocalDate referredAt,
            final Offence offence) {

        return ofNullable(offence.getPlea())
                .map(plea -> new NotifiedPleaView(
                        offence.getId(),
                        offence.getPleaDate().toLocalDate(),
                        getNotifiedPlea(plea)))
                .orElseGet(() -> new NotifiedPleaView(
                        offence.getId(),
                        referredAt,
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
