package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.UsersGroupsService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.SjpReferralViewHelper;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class SjpReferralDataSourcingService {

    @Inject
    private SjpService sjpService;

    @Inject
    private UsersGroupsService usersGroupsService;

    @Inject
    private SjpReferralViewHelper sjpReferralViewHelper;

    public SjpReferralView createSjpReferralView(
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final CaseDetails caseDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final JsonObject sessionDetails = sjpService.getSessionDetails(
                caseReferredForCourtHearing.getSessionId(),
                emptyEnvelopeWithReferralEventMetadata);
        final JsonObject legalAdviserDetails = usersGroupsService.getUserDetails(
                UUID.fromString(sessionDetails.getString("userId")),
                emptyEnvelopeWithReferralEventMetadata);

        return sjpReferralViewHelper.createSjpReferralView(
                caseDetails,
                sessionDetails,
                legalAdviserDetails,
                caseReferredForCourtHearing.getReferredAt());
    }

}
