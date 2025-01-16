package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.DEFENDANT_PENDING_CHANGES_ACCEPTED_PUBLIC_EVENT;
import static uk.gov.moj.sjp.it.Constants.DEFENDANT_PENDING_CHANGES_REJECTED_PUBLIC_EVENT;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.command.UpdateDefendantDetails.acceptDefendantPendingChangesForCaseAndPayload;
import static uk.gov.moj.sjp.it.command.UpdateDefendantDetails.rejectDefendantPendingChanges;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class DefendantPendingChangesIT extends BaseIntegrationTest {
    private final UUID caseIdOne = randomUUID();
    private final UUID tvlUserUid = randomUUID();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {

        createCasePayloadBuilder = withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(),
                "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCaseForPayloadBuilder(createCasePayloadBuilder.withId(caseIdOne));

        stubForUserDetails(tvlUserUid, ProsecutingAuthority.TVL);
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
    }

    @Test
    public void shouldAcceptDefendantChangesAndRaisePublicEvent() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder =
                UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));

        final EventListener defendantPendingChangesAcceptedListener = new EventListener()
                .subscribe(DEFENDANT_PENDING_CHANGES_ACCEPTED_PUBLIC_EVENT)
                .run(() -> acceptDefendantPendingChangesForCaseAndPayload(caseIdOne, defendantId, payloadBuilder));

        final Optional<JsonEnvelope> defendantPendingChangesAcceptedPublicEvent = defendantPendingChangesAcceptedListener.popEvent(DEFENDANT_PENDING_CHANGES_ACCEPTED_PUBLIC_EVENT);

        assertThat(defendantPendingChangesAcceptedPublicEvent.isPresent(), CoreMatchers.is(true));
        assertThat(defendantPendingChangesAcceptedPublicEvent.get().payloadAsJsonObject().getString("caseId"), CoreMatchers.is(caseIdOne.toString()));
    }

    @Test
    public void shouldRejectDefendantChangesAndRaisePublicEvent() {

        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));

        final EventListener defendantPendingChangesRejectedListener = new EventListener()
                .subscribe(DEFENDANT_PENDING_CHANGES_REJECTED_PUBLIC_EVENT)
                .run(() -> rejectDefendantPendingChanges(caseIdOne, defendantId));

        final Optional<JsonEnvelope> defendantPendingChangesRejectedPublicEvent = defendantPendingChangesRejectedListener.popEvent(DEFENDANT_PENDING_CHANGES_REJECTED_PUBLIC_EVENT);

        assertThat(defendantPendingChangesRejectedPublicEvent.isPresent(), CoreMatchers.is(true));
        assertThat(defendantPendingChangesRejectedPublicEvent.get().payloadAsJsonObject().getString("caseId"), CoreMatchers.is(caseIdOne.toString()));
    }
}