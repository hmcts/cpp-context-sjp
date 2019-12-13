package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.HearingRequestsViewHelper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class HearingRequestsDataSourcingService {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private HearingRequestsViewHelper hearingRequestsViewHelper;

    public List<HearingRequestView> createHearingRequestViews(
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final CaseDetails caseDetails,
            final DefendantsOnlinePlea defendantPleaDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final JsonObject referralReasons = referenceDataService.getReferralReasons(
                emptyEnvelopeWithReferralEventMetadata);

        final JsonObject hearingTypes = referenceDataService.getHearingTypes(
                emptyEnvelopeWithReferralEventMetadata);

        final List<UUID> referredOffenceIds = getReferredOffenceIds(
                caseReferredForCourtHearing.getReferredOffences());

        return hearingRequestsViewHelper.createHearingRequestViews(
                caseDetails,
                referralReasons,
                defendantPleaDetails,
                caseReferredForCourtHearing,
                hearingTypes,
                referredOffenceIds);
    }

    private static List<UUID> getReferredOffenceIds(
            final List<OffenceDecisionInformation> referredOffencesWithVerdict) {
        return referredOffencesWithVerdict
                .stream()
                .map(OffenceDecisionInformation::getOffenceId)
                .collect(Collectors.toList());
    }
}
