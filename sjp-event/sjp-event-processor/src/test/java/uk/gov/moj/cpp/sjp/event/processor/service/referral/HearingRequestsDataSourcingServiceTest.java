package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails.caseDetails;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.HearingRequestsViewHelper;

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

    @Test
    public void shouldCreateHearingRequestViews() {
        final JsonEnvelope emptyEnvelopeWithReferralEventMetadata = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .build();

        final CaseDetails caseDetails = caseDetails()
                .withId(CASE_ID)
                .build();
        final JsonObject caseFileDefendantDetails = createObjectBuilder().build();

        final JsonObject referralReasonsMock = createObjectBuilder().build();
        when(referenceDataService.getReferralReasons(emptyEnvelopeWithReferralEventMetadata)).thenReturn(referralReasonsMock);

        final DefendantsOnlinePlea defendantPlea = DefendantsOnlinePlea.defendantsOnlinePlea().build();

        hearingRequestsDataSourcingService.createHearingRequestViews(
                caseReferredForCourtHearing,
                caseDetails,
                defendantPlea,
                caseFileDefendantDetails,
                emptyEnvelopeWithReferralEventMetadata);

        verify(hearingRequestsViewHelper).createHearingRequestViews(
                caseDetails,
                referralReasonsMock,
                defendantPlea,
                caseFileDefendantDetails,
                caseReferredForCourtHearing);
    }

}
