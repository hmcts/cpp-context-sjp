package uk.gov.moj.cpp.sjp.query.view.service.defendantcase;

import uk.gov.justice.services.messaging.Envelope;

import java.util.UUID;

public interface DefendantPotentialCaseService {

    boolean hasDefendantPotentialCase(Envelope<?> envelope, UUID defendantId);
    PotentialCases findDefendantPotentialCases(Envelope<?> envelope, UUID defendantId);
}
