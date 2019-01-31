package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingTypeView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferralReasonView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class HearingRequestsViewHelper {

    private static final String WELSH_HEARING_LANGUAGE_CODE = "W";

    public List<HearingRequestView> createHearingRequestViews(
            final CaseDetails caseDetails,
            final JsonObject referralReasons,
            final DefendantsOnlinePlea defendantPleaDetails,
            final JsonObject caseFileDefendantDetails,
            final CaseReferredForCourtHearing caseReferredForCourtHearingEvent) {

        final Optional<PleaDetails> defendantPleaDetailsOptional = Optional.ofNullable(defendantPleaDetails)
                .map(DefendantsOnlinePlea::getPleaDetails);

        final String defendantUnavailability = defendantPleaDetailsOptional
                .map(PleaDetails::getUnavailability)
                .orElse(null);

        final DefendantRequestView defendantRequestView = crateDefendantRequestView(
                caseDetails,
                referralReasons,
                defendantUnavailability,
                caseFileDefendantDetails,
                caseReferredForCourtHearingEvent);

        final HearingRequestView listHearingRequestView = new HearingRequestView(
                "MAGISTRATES",
                caseReferredForCourtHearingEvent.getEstimatedHearingDuration(),
                caseDetails.getDatesToAvoid(),
                caseReferredForCourtHearingEvent.getListingNotes(),
                new HearingTypeView(caseReferredForCourtHearingEvent.getHearingTypeId()),
                singletonList(defendantRequestView));

        return singletonList(listHearingRequestView);
    }

    private DefendantRequestView crateDefendantRequestView(
            final CaseDetails caseDetails,
            final JsonObject referralReasons,
            final String defendantUnavailability,
            final JsonObject caseFileDefendantDetails,
            final CaseReferredForCourtHearing caseReferredForCourtHearingEvent) {

        final Defendant defendant = caseDetails.getDefendant();
        final UUID referralReasonId = caseReferredForCourtHearingEvent.getReferralReasonId();

        final ReferralReasonView referralReasonView = extractReferralReason(
                referralReasons,
                referralReasonId)
                .map(referralDescription -> new ReferralReasonView(
                        referralReasonId,
                        referralDescription,
                        defendant.getId()))
                .orElseThrow(() -> new IllegalStateException(
                        format("Referral reason not found for case %s and offence %s",
                                caseDetails.getId(),
                                referralReasonId)));

        final String hearingLanguage = Optional.ofNullable(caseFileDefendantDetails)
                .map(details -> details.getString("hearingLanguage"))
                .map(language -> WELSH_HEARING_LANGUAGE_CODE.equals(language) ? "WELSH" : "ENGLISH")
                .orElse("ENGLISH");

        return new DefendantRequestView(
                caseReferredForCourtHearingEvent.getCaseId(),
                referralReasonView,
                defendantUnavailability,
                "SJP_REFERRAL",
                hearingLanguage,
                singletonList(defendant.getOffences().get(0).getId()));
    }

    private static Optional<String> extractReferralReason(final JsonObject allReferralReasons, final UUID referralReasonId) {
        return allReferralReasons
                .getJsonArray("referralReasons")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(reason -> reason.getString("id").equals(referralReasonId.toString()))
                .findFirst()
                .map(reason -> {
                    if (reason.containsKey("subReason")) {
                        return String.format("%s (%s)", reason.getString("reason"), reason.getString("subReason"));
                    } else {
                        return reason.getString("reason");
                    }
                });
    }
}
