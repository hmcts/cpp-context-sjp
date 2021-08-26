package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision.caseDecision;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails.caseDetails;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.HearingRequestsViewHelper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingRequestsDataSourcingServiceTest {

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private HearingRequestsViewHelper hearingRequestsViewHelper;

    @InjectMocks
    private HearingRequestsDataSourcingService hearingRequestsDataSourcingService;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final UUID DECISION_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final ZonedDateTime REFERRAL_DATE_TIME = now();
    private static final String COURT_HOUSE_NAME = "Leamington Spa Magistrates' Court";
    private static final String MAGISTRATE = "magistrate";

    @Test
    public void shouldCreateHearingRequestViews() {
        final JsonEnvelope emptyEnvelopeWithReferralEventMetadata = envelopeFrom(metadataWithRandomUUIDAndName(), NULL);

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredOffences(getReferredOffencesWithVerdict())
                .build();

        final CaseDetails caseDetails = caseDetails()
                .withId(CASE_ID)
                .withCaseDecisions(mockCaseDecisions())
                .build();

        final JsonObject referralReasonsMock = createObjectBuilder().build();
        when(referenceDataService.getReferralReasons(emptyEnvelopeWithReferralEventMetadata)).thenReturn(referralReasonsMock);

        final JsonObject hearingTypesMock = createObjectBuilder().build();
        when(referenceDataService.getHearingTypes(emptyEnvelopeWithReferralEventMetadata)).thenReturn(hearingTypesMock);

        final DefendantsOnlinePlea defendantPlea = DefendantsOnlinePlea.defendantsOnlinePlea().build();

        hearingRequestsDataSourcingService.createHearingRequestViews(
                caseReferredForCourtHearing,
                caseDetails,
                defendantPlea,
                emptyEnvelopeWithReferralEventMetadata);

        verify(hearingRequestsViewHelper).createHearingRequestViews(
                caseDetails,
                referralReasonsMock,
                defendantPlea,
                caseReferredForCourtHearing,
                hearingTypesMock,
                singletonList(OFFENCE_ID));
    }

    private List<CaseDecision> mockCaseDecisions() {
        final List<CaseDecision> casedDecisions = new ArrayList<>();
        final CaseDecision caseDecision = caseDecision()
                .withId(DECISION_ID)
                .withSavedAt(REFERRAL_DATE_TIME)
                .withSession(mockSessionDetails())
                .build();
        casedDecisions.add(caseDecision);
        return casedDecisions;
    }

    private Session mockSessionDetails() {
        return
                Session.session()
                        .withSessionId(SESSION_ID)
                        .withCourtHouseName(COURT_HOUSE_NAME)
                        .withMagistrate(MAGISTRATE)
                        .build();
    }

    private static List<OffenceDecisionInformation> getReferredOffencesWithVerdict() {
        return singletonList(OffenceDecisionInformation.createOffenceDecisionInformation(OFFENCE_ID, VerdictType.NO_VERDICT));
    }
}