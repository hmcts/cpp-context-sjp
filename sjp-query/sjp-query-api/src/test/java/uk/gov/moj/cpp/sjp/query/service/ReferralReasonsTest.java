package uk.gov.moj.cpp.sjp.query.service;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferralReasonsTest {

    final UUID referralReasonId1 = randomUUID();
    final UUID referralReasonId2 = randomUUID();
    final JsonObject referralReason1 = createObjectBuilder().add("id", referralReasonId1.toString()).build();
    final JsonObject referralReason2 = createObjectBuilder().add("id", referralReasonId2.toString()).build();

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @BeforeEach
    public void init() {
        when(referenceDataService.getReferralReasons(jsonEnvelope)).thenReturn(asList(referralReason1, referralReason2));
    }

    @Test
    public void shouldGetCachedReferralReason() {

        final ReferralReasons referralReasons = new ReferralReasons(referenceDataService, jsonEnvelope);

        assertThat(referralReasons.getReferralReason(referralReasonId1), is(Optional.of(referralReason1)));
        assertThat(referralReasons.getReferralReason(referralReasonId1), is(Optional.of(referralReason1)));
        assertThat(referralReasons.getReferralReason(referralReasonId2), is(Optional.of(referralReason2)));

        verify(referenceDataService, times(1)).getReferralReasons(any());
    }

    @Test
    public void shouldGetEmptyResultIfReferralReasonNotPresent() {
        final ReferralReasons referralReasons = new ReferralReasons(referenceDataService, jsonEnvelope);

        assertThat(referralReasons.getReferralReason(randomUUID()), is(Optional.empty()));

        verify(referenceDataService, times(1)).getReferralReasons(any());
    }
}
