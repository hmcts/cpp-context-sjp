package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;

import java.util.Optional;

import javax.inject.Inject;

public class ConvictingCourtHelper {

    @Inject
    private CourtCentreConverter courtCentreConverter;

    private ConvictingCourtHelper() {
    }

    public CourtCentre createConvictingCourt(final SessionCourt sessionCourt, final JsonEnvelope emptyEnvelope) {
        if (nonNull(sessionCourt)) {
            final Optional<CourtCentre> convictingCourtCentreOptional = courtCentreConverter.convertByCourtHouseCode(sessionCourt.getCourtHouseCode(), sessionCourt.getLjaCode(), emptyEnvelope.metadata());
            return Optional.ofNullable(convictingCourtCentreOptional)
                    .map(Optional::get)
                    .orElse(null);
        } else {
            return null;
        }
    }

}
