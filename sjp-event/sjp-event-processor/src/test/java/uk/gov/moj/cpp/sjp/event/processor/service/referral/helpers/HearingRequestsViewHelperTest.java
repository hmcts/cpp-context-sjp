package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingRequestView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.HearingTypeView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ReferralReasonView;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;

public class HearingRequestsViewHelperTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID REFERRAL_REASON_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final String REFERRAL_REASON = "Equivocal plea";
    private static final String REFERRAL_SUB_REASON = "For Trial";
    private static final int ESTIMATED_HEARING_DURATION = 10;
    private static final String DEFENDANT_UNAVAILABILITY = "defendant unavailability";
    private static final String PROSECUTOR_DATES_TO_AVOID = "prosecutor dates to avoid";

    private HearingRequestsViewHelper hearingRequestsViewHelper = new HearingRequestsViewHelper();

    @Test
    public void shouldCreateHearingRequestViewIfAllDataPresent() {
        createHearingRequestAndVerifyAllDataPresent(createObjectBuilder()
                .add("hearingLanguage", "W")
                .build());
    }

    @Test
    public void shouldUseEnglishAsHearingLanguageNeedIfCaseFileDetailsNotPresent() {
        createHearingRequestAndVerifyAllDataPresent(null);
    }

    private void createHearingRequestAndVerifyAllDataPresent(final JsonObject caseFileDefendantDetails) {
        final CaseDetails caseDetails = createCaseDetails();
        final String listingNotes = "listing notes";
        final CaseReferredForCourtHearing caseReferredForCourtHearingEvent = createCourtHearingEvent(listingNotes);
        final JsonObject referralReasons = createReferralReasonsObject();
        final DefendantsOnlinePlea defendantPlea = DefendantsOnlinePlea.defendantsOnlinePlea()
                .withPleaDetails(PleaDetails.pleaDetails()
                        .withUnavailability(DEFENDANT_UNAVAILABILITY)
                        .build())
                .build();

        final List<HearingRequestView> hearingRequestViews = hearingRequestsViewHelper.createHearingRequestViews(
                caseDetails,
                referralReasons,
                defendantPlea,
                caseFileDefendantDetails,
                caseReferredForCourtHearingEvent);

        assertThat(hearingRequestViews.size(), is(1));

        final HearingRequestView hearingRequestView = hearingRequestViews.get(0);
        assertThat(hearingRequestView.getJurisdictionType(), is("MAGISTRATES"));
        assertThat(hearingRequestView.getEstimateMinutes(), is(ESTIMATED_HEARING_DURATION));
        assertThat(hearingRequestView.getProsecutorDatesToAvoid(), is(PROSECUTOR_DATES_TO_AVOID));
        assertThat(hearingRequestView.getListingDirections(), is(listingNotes));
        assertThat(hearingRequestView.getHearingType(), is(new HearingTypeView(HEARING_TYPE_ID)));
        assertThat(hearingRequestView.getListDefendantRequests().size(), is(1));

        final DefendantRequestView defendantRequest = hearingRequestView.getListDefendantRequests().get(0);
        assertThat(defendantRequest.getProsecutionCaseId(), is(CASE_ID));
        assertThat(defendantRequest.getReferralReason(), is(new ReferralReasonView(
                REFERRAL_REASON_ID,
                String.format("%s (%s)", REFERRAL_REASON, REFERRAL_SUB_REASON),
                DEFENDANT_ID)));
        assertThat(defendantRequest.getDatesToAvoid(), is(DEFENDANT_UNAVAILABILITY));
        assertThat(defendantRequest.getSummonsRequired(), is("SJP_REFERRAL"));
        assertThat(defendantRequest.getDefendantOffences(), is(singletonList(OFFENCE_ID)));
        assertThat(defendantRequest.getHearingLanguageNeeds(), is(nonNull(caseFileDefendantDetails) ? "WELSH" : "ENGLISH"));
    }

    private CaseDetails createCaseDetails() {
        return CaseDetails.caseDetails()
                .withDefendant(Defendant.defendant()
                        .withId(DEFENDANT_ID)
                        .withOffences(
                                singletonList(
                                        Offence.offence()
                                                .withId(OFFENCE_ID)
                                                .build()))
                        .build())
                .withDatesToAvoid(PROSECUTOR_DATES_TO_AVOID)
                .build();
    }

    private JsonObject createReferralReasonsObject() {
        return createObjectBuilder()
                .add("referralReasons", createArrayBuilder()
                        .add(
                                createObjectBuilder()
                                        .add("id", REFERRAL_REASON_ID.toString())
                                        .add("reason", REFERRAL_REASON)
                                        .add("subReason", REFERRAL_SUB_REASON)
                        ))
                .build();
    }

    private CaseReferredForCourtHearing createCourtHearingEvent(final String listingNotes) {
        return caseReferredForCourtHearing()
                .withReferralReasonId(REFERRAL_REASON_ID)
                .withCaseId(CASE_ID)
                .withEstimatedHearingDuration(ESTIMATED_HEARING_DURATION)
                .withListingNotes(listingNotes)
                .withHearingTypeId(HEARING_TYPE_ID)
                .build();
    }
}
