package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingTypeView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferralReasonView;

import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

public class HearingRequestsViewHelper {

    public List<HearingRequestView> createHearingRequestViews(final CaseDetails caseDetails,
                                                              final JsonObject referralReasons,
                                                              final DefendantsOnlinePlea defendantPleaDetails,
                                                              final CaseReferredForCourtHearing caseReferredForCourtHearingEvent,
                                                              final JsonObject hearingTypes,
                                                              final List<UUID> referredOffenceIds) {

        final Optional<PleaDetails> defendantPleaDetailsOptional = Optional.ofNullable(defendantPleaDetails).map(DefendantsOnlinePlea::getPleaDetails);

        final String defendantUnavailability = defendantPleaDetailsOptional.map(PleaDetails::getUnavailability).orElse(null);

        final DefendantRequestView defendantRequestView = createDefendantRequestView(
                caseDetails,
                referralReasons,
                defendantUnavailability,
                caseReferredForCourtHearingEvent,
                referredOffenceIds);

        final String hearingTypeId = extractHearingTypeId(
                referralReasons,
                caseReferredForCourtHearingEvent.getReferralReasonId(), hearingTypes)
                .orElseThrow(() -> new IllegalStateException(
                        format("Hearing type Id not found for case %s and referral reason %s",
                                caseDetails.getId(),
                                caseReferredForCourtHearingEvent.getReferralReasonId()))
                );

        final HearingRequestView listHearingRequestView = new HearingRequestView(
                "MAGISTRATES",
                caseReferredForCourtHearingEvent.getEstimatedHearingDuration(),
                caseDetails.getDatesToAvoid(),
                caseReferredForCourtHearingEvent.getListingNotes(),
                new HearingTypeView(UUID.fromString(hearingTypeId)),
                singletonList(defendantRequestView));

        return singletonList(listHearingRequestView);
    }

    private DefendantRequestView createDefendantRequestView(
            final CaseDetails caseDetails,
            final JsonObject referralReasons,
            final String defendantUnavailability,
            final CaseReferredForCourtHearing referredForCourtHearing,
            final List<UUID> referredOffenceIds) {

        final Defendant defendant = caseDetails.getDefendant();
        final UUID referralReasonId = referredForCourtHearing.getReferralReasonId();

        final ReferralReasonView referralReasonView = extractReferralReason(
                referralReasons,
                referralReasonId)
                .map(referralDescription -> new ReferralReasonView(referralReasonId, referralDescription, defendant.getId()))
                .orElseThrow(() ->
                        new IllegalStateException(
                                format("Referral reason not found for case %s and offence %s",
                                        caseDetails.getId(),
                                        referralReasonId)));

        return new DefendantRequestView(
                referredForCourtHearing.getCaseId(),
                referralReasonView,
                defendantUnavailability,
                "SJP_REFERRAL",
                determineHearingLanguage(referredForCourtHearing.getDefendantCourtOptions(), defendant.getSpeakWelsh()),
                referredOffenceIds);
    }

    private static String determineHearingLanguage(final DefendantCourtOptions defendantCourtOptions,
                                                   final Boolean speaksWelsh) {
        Boolean welshHearing = speaksWelsh;
        if (defendantCourtOptions != null && defendantCourtOptions.getWelshHearing() != null) {
            welshHearing = defendantCourtOptions.getWelshHearing();
        }
        return (welshHearing != null && welshHearing) ? "WELSH" : "ENGLISH";
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

    private static Optional<String> extractHearingCode(final JsonObject allReferralReasons, final UUID referralReasonId) {
        return allReferralReasons
                .getJsonArray("referralReasons")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(reason -> reason.getString("id").equals(referralReasonId.toString()))
                .findFirst()
                .map(reason -> reason.getString("hearingCode"));
    }

    private static Optional<String> extractHearingTypeId(final JsonObject allReferralReasons, final UUID referralReasonId, final JsonObject hearingTypes) {

        final String hearingCode = extractHearingCode(
                allReferralReasons, referralReasonId)
                .orElseThrow(() -> new IllegalStateException(
                        format("Referral reason not found for referral reason %s",
                                referralReasonId)));

        return hearingTypes
                .getJsonArray("hearingTypes")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(reason -> reason.getString("hearingCode").equals(hearingCode))
                .findFirst()
                .map(reason -> reason.getString("id"));
    }
}
