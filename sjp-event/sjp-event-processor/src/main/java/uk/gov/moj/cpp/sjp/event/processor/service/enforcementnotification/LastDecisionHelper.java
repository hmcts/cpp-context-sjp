package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public class LastDecisionHelper {

    public Optional<CaseDecision> getLastDecision(final CaseDetails caseDetail) {
        if (ofNullable(caseDetail.getCaseDecisions()).isPresent()) {
            final Function<CaseDecision, ZonedDateTime> getSavedAt = CaseDecision::getSavedAt;
            final Comparator<CaseDecision> comparing = comparing(getSavedAt);
            return caseDetail.getCaseDecisions().stream().max(comparing);
        } else {
            return empty();
        }
    }
}