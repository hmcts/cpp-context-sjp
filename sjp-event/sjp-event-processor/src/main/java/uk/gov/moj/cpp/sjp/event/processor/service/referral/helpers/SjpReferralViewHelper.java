package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.lang.String.format;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.JudiciaryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferringJudicialDecisionView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.SjpReferralView;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

public class SjpReferralViewHelper {

    public SjpReferralView createSjpReferralView(
            final CaseDetails caseDetails,
            final JsonObject sessionDetails,
            final JsonObject legalAdviserDetails,
            final ZonedDateTime decisionCreationDate) {

        final String legalAdviserName = format("%s %s", legalAdviserDetails.getString("firstName"), legalAdviserDetails.getString("lastName"));
        final JudiciaryView legalAdviser = new JudiciaryView(legalAdviserName, "LEGAL_ADVISER");

        final Optional<JudiciaryView> magistrateViewOptional = Optional.ofNullable(sessionDetails.getString("magistrate", null))
                .map(magistrate -> new JudiciaryView(magistrate, "MAGISTRATE"));

        final List<JudiciaryView> judiciaries = new ArrayList<>();
        judiciaries.add(legalAdviser);
        magistrateViewOptional.ifPresent(judiciaries::add);

        final ReferringJudicialDecisionView referringJudicialDecisionView = new ReferringJudicialDecisionView(
                sessionDetails.getString("courtHouseName"),
                judiciaries);

        return new SjpReferralView(
                caseDetails.getPostingDate(),
                decisionCreationDate.toLocalDate(),
                referringJudicialDecisionView);
    }

}
