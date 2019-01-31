package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.UsersGroupsService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.SjpReferralViewHelper;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpReferralDataSourcingServiceTest {

    @Mock
    private SjpService sjpService;
    @Mock
    private UsersGroupsService usersGroupsService;
    @Mock
    private SjpReferralViewHelper sjpReferralViewHelper;

    @InjectMocks
    private SjpReferralDataSourcingService sjpReferralDataSourcingService;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final ZonedDateTime REFERRAL_DATE_TIME = now();

    @Test
    public void shouldCreateSjpReferralView() {
        final JsonEnvelope emptyEnvelopeWithReferralEventMetadata = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withSessionId(SESSION_ID)
                .withReferredAt(REFERRAL_DATE_TIME)
                .build();

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(CASE_ID)
                .build();

        final JsonObject sessionDetails = createObjectBuilder()
                .add("userId", USER_ID.toString())
                .build();

        final JsonObject legalAdviserDetails = createObjectBuilder().build();

        mockServiceCalls(emptyEnvelopeWithReferralEventMetadata, sessionDetails, legalAdviserDetails);

        sjpReferralDataSourcingService.createSjpReferralView(
                caseReferredForCourtHearing,
                caseDetails,
                emptyEnvelopeWithReferralEventMetadata);

        verify(sjpReferralViewHelper).createSjpReferralView(caseDetails, sessionDetails, legalAdviserDetails, caseReferredForCourtHearing.getReferredAt());
    }

    private void mockServiceCalls(final JsonEnvelope emptyEnvelopeWithReferralEventMetadata, final JsonObject sessionDetails, final JsonObject legalAdviserDetails) {
        when(sjpService.getSessionDetails(
                SESSION_ID,
                emptyEnvelopeWithReferralEventMetadata)).thenReturn(sessionDetails);
        when(usersGroupsService.getUserDetails(
                USER_ID,
                emptyEnvelopeWithReferralEventMetadata)).thenReturn(legalAdviserDetails);
    }
}
