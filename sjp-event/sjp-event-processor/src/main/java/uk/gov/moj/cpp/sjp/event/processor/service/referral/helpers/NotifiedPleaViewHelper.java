package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static com.google.common.collect.Iterables.getFirst;
import static java.util.UUID.fromString;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;

import java.util.List;
import java.util.Optional;

public class NotifiedPleaViewHelper {

    public NotifiedPleaView createNotifiedPleaView(
            final CaseDetails caseDetails,
            final CaseReferredForCourtHearing eventPayload,
            final DefendantsOnlinePlea defendantsOnlinePlea,
            final List<Offence> defendantOffences) {

        final String offenceId = Optional.ofNullable(getFirst(defendantOffences, null))
                .map(Offence::getId)
                .orElseThrow(() -> new IllegalStateException("Offence not found"));

        return Optional.ofNullable(defendantsOnlinePlea)
                .map(plea -> new NotifiedPleaView(
                        fromString(offenceId),
                        caseDetails.getDefendant().getOffences().get(0).getPleaDate().toLocalDate(),
                        String.format("NOTIFIED_%s", plea.getPleaDetails().getPlea())))
                .orElseGet(() -> new NotifiedPleaView(
                        fromString(offenceId),
                        eventPayload.getReferredAt().toLocalDate(),
                        "NO_NOTIFICATION"));
    }
}
