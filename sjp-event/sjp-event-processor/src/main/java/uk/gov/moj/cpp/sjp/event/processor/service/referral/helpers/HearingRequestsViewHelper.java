package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
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

@SuppressWarnings("squid:S107")
public class HearingRequestsViewHelper {

    public List<HearingRequestView> createHearingRequestViews(final CaseDetails caseDetails,
                                                              final DefendantsOnlinePlea defendantPleaDetails,

                                                              final UUID caseId,
                                                              final UUID referralReasonId,
                                                              final DefendantCourtOptions defendantCourtOptions,
                                                              final Integer estimatedHearingDuration,
                                                              final String listingNotes,

                                                              final JsonObject hearingTypes,
                                                              final List<UUID> referredOffenceIds,
                                                              final JsonObject referralReason) {

        final Optional<PleaDetails> defendantPleaDetailsOptional = Optional.ofNullable(defendantPleaDetails).map(DefendantsOnlinePlea::getPleaDetails);

        final String defendantUnavailability = defendantPleaDetailsOptional.map(PleaDetails::getUnavailability).orElse(null);

        final DefendantRequestView defendantRequestView = createDefendantRequestView(
                caseDetails,
                defendantUnavailability,
                caseId,
                referralReasonId,
                defendantCourtOptions,
                referredOffenceIds,
                referralReason);

        final String hearingTypeId = extractHearingTypeId(
                referralReason, hearingTypes)
                .orElseThrow(() -> new IllegalStateException(
                        format("Hearing type Id not found for case %s and referral reason %s",
                                caseDetails.getId(),
                                referralReasonId))
                );

        final HearingRequestView listHearingRequestView = new HearingRequestView(
                "MAGISTRATES",
                estimatedHearingDuration,
                caseDetails.getDatesToAvoid(),
                listingNotes,
                new HearingTypeView(UUID.fromString(hearingTypeId)),
                singletonList(defendantRequestView));

        return singletonList(listHearingRequestView);
    }

    private DefendantRequestView createDefendantRequestView(
            final CaseDetails caseDetails,
            final String defendantUnavailability,
            final UUID caseId,
            final UUID referralReasonId,
            final DefendantCourtOptions defendantCourtOptions,
            final List<UUID> referredOffenceIds,
            final JsonObject referralReason) {

        final Defendant defendant = caseDetails.getDefendant();

        final ReferralReasonView referralReasonView = extractReferralReason(referralReason)
                .map(referralDescription -> new ReferralReasonView(referralReasonId, referralDescription, defendant.getId()))
                .orElseThrow(() ->
                        new IllegalStateException(
                                format("Referral reason not found for case %s and offence %s",
                                        caseDetails.getId(),
                                        referralReasonId)));

        return new DefendantRequestView(
                caseId,
                referralReasonView,
                defendantUnavailability,
                "SJP_REFERRAL",
                determineHearingLanguage(defendantCourtOptions, defendant.getSpeakWelsh()),
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

    private static Optional<String> extractReferralReason(final JsonObject referralReason) {
        if ( referralReason.getString("subReason", null) !=null  && !referralReason.get("subReason").toString().isEmpty()) {
            return Optional.ofNullable(String.format("%s (%s)", referralReason.getString("reason"), referralReason.getString("subReason")));
        } else {
            return Optional.ofNullable(referralReason.getString("reason"));
        }
    }

    private static String extractHearingCode(final JsonObject referralReason) {
        return referralReason.getString("hearingCode");
    }

    private static Optional<String> extractHearingTypeId(final JsonObject referralReason, final JsonObject hearingTypes) {
        final String hearingCode = extractHearingCode(referralReason);
        return hearingTypes
                .getJsonArray("hearingTypes")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(reason -> reason.getString("hearingCode").equals(hearingCode))
                .findFirst()
                .map(reason -> reason.getString("id"));
    }
}
