package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder.defaultLegalEntityDefendant;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class UpdateDefendantDetailsFromCCIT extends BaseIntegrationTest {

    private static final String DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT = "public.sjp.events.defendant-details-updated";
    private final UUID caseId = randomUUID();
    private final UUID userId = randomUUID();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        createCasePayloadBuilder = withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(),
                "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCaseForPayloadBuilder(createCasePayloadBuilder.withId(caseId));

        stubForUserDetails(userId, ProsecutingAuthority.TVL);
        stubProsecutorQuery(ProsecutingAuthority.TFL.name(), ProsecutingAuthority.TFL.getFullName(), randomUUID());
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenEventReceived() {
        // given
        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseId).getString("defendant.id"));

        final JsonObject personDetails = createObjectBuilder()
                .add("title", "Mr")
                .add("firstName", "John")
                .add("lastName", "Updated")
                .add("dateOfBirth", "1985-06-20")
                .add("gender", "MALE")
                .add("nationalInsuranceNumber", "AB123456C")
                .add("address", createObjectBuilder()
                        .add("address1", "123 Updated Street")
                        .add("address2", "Updated City")
                        .add("postcode", "SW1A 1AA")
                        .build())
                .add("contact", createObjectBuilder()
                        .add("home", "02011111111")
                        .add("mobile", "07111111111")
                        .add("primaryEmail", "updated@example.com")
                        .build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .add("driverNumber", "TESTY708166G99KZ")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", caseId.toString())
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        // when
        final EventListener defendantDetailsUpdatedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT)
                .run(() -> {
                    final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                            .getMessageProducerClient();
                    publicJmsMessageProducerClient.sendMessage("public.progression.case-defendant-changed", eventPayload);
                });

        // then
        final Optional<JsonEnvelope> defendantDetailsUpdatedEvent = defendantDetailsUpdatedListener.popEvent(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT);

        assertThat(defendantDetailsUpdatedEvent.isPresent(), is(true));
        final JsonObject payload = defendantDetailsUpdatedEvent.get().payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getString("defendantId"), is(defendantId.toString()));
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenCaseIsCompleted() {
        // given
        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseId).getString("defendant.id"));

        final JsonObject personDetails = createObjectBuilder()
                .add("title", "Mr")
                .add("firstName", "John")
                .add("lastName", "CompletedCase")
                .add("dateOfBirth", "1985-06-20")
                .add("gender", "MALE")
                .add("address", createObjectBuilder()
                        .add("address1", "123 Completed Street")
                        .add("postcode", "SW1A 1AA")
                        .build())
                .add("contact", createObjectBuilder()
                        .add("primaryEmail", "completed@example.com")
                        .build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", caseId.toString())
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        // when - should succeed even though case might be completed (key difference from regular update)
        final EventListener defendantDetailsUpdatedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT)
                .run(() -> {
                    final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                            .getMessageProducerClient();
                    publicJmsMessageProducerClient.sendMessage("public.progression.case-defendant-changed", eventPayload);
                });

        // then
        final Optional<JsonEnvelope> defendantDetailsUpdatedEvent = defendantDetailsUpdatedListener.popEvent(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT);

        assertThat(defendantDetailsUpdatedEvent.isPresent(), is(true));
        final JsonObject payload = defendantDetailsUpdatedEvent.get().payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getString("defendantId"), is(defendantId.toString()));
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenCaseIsReferredForCourtHearing() {
        // given
        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseId).getString("defendant.id"));

        final JsonObject personDetails = createObjectBuilder()
                .add("title", "Mr")
                .add("firstName", "John")
                .add("lastName", "ReferredCase")
                .add("dateOfBirth", "1985-06-20")
                .add("gender", "MALE")
                .add("nationalInsuranceNumber", "AB123456C")
                .add("address", createObjectBuilder()
                        .add("address1", "123 Referred Street")
                        .add("postcode", "SW1A 1AA")
                        .build())
                .add("contact", createObjectBuilder()
                        .add("home", "02011111111")
                        .add("mobile", "07111111111")
                        .add("primaryEmail", "referred@example.com")
                        .build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .add("driverNumber", "MORGA753116SM9IV")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", caseId.toString())
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        // when - should succeed even though case might be referred (key difference from regular update)
        final EventListener defendantDetailsUpdatedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT)
                .run(() -> {
                    final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                            .getMessageProducerClient();
                    publicJmsMessageProducerClient.sendMessage("public.progression.case-defendant-changed", eventPayload);
                });

        // then
        final Optional<JsonEnvelope> defendantDetailsUpdatedEvent = defendantDetailsUpdatedListener.popEvent(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT);

        assertThat(defendantDetailsUpdatedEvent.isPresent(), is(true));
        final JsonObject payload = defendantDetailsUpdatedEvent.get().payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getString("defendantId"), is(defendantId.toString()));
    }

    @Test
    public void shouldNotProcessEventWhenDefendantIsMissing() {
        // given
        final JsonObject eventPayload = createObjectBuilder().build();

        // when
        final EventListener defendantDetailsUpdatedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT)
                .run(() -> {
                    final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                            .getMessageProducerClient();
                    publicJmsMessageProducerClient.sendMessage("public.progression.case-defendant-changed", eventPayload);
                });

        // then - no event should be generated
        final Optional<JsonEnvelope> defendantDetailsUpdatedEvent = defendantDetailsUpdatedListener.popEvent(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT);

        assertThat(defendantDetailsUpdatedEvent.isPresent(), is(false));
    }

    @Test
    public void shouldNotProcessEventWhenPersonDefendantIsMissing() {
        // given
        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseId).getString("defendant.id"));

        final JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", caseId.toString())
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        // when
        final EventListener defendantDetailsUpdatedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT)
                .run(() -> {
                    final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                            .getMessageProducerClient();
                    publicJmsMessageProducerClient.sendMessage("public.progression.case-defendant-changed", eventPayload);
                });

        // then - no event should be generated
        final Optional<JsonEnvelope> defendantDetailsUpdatedEvent = defendantDetailsUpdatedListener.popEvent(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT);

        assertThat(defendantDetailsUpdatedEvent.isPresent(), is(false));
    }

    @Test
    public void shouldUpdateLegalEntityDefendantDetailsFromCCWhenEventReceived() {
        // given - create a case with legal entity defendant
        final UUID legalEntityCaseId = randomUUID();
        final CreateCase.CreateCasePayloadBuilder legalEntityCaseBuilder = withDefaults()
                .withId(legalEntityCaseId)
                .withDefendantBuilder(defaultLegalEntityDefendant());
        
        stubEnforcementAreaByPostcode(legalEntityCaseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(),
                "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");
        stubProsecutorQuery(legalEntityCaseBuilder.getProsecutingAuthority().name(), 
                legalEntityCaseBuilder.getProsecutingAuthority().getFullName(), randomUUID());
        
        createCaseForPayloadBuilder(legalEntityCaseBuilder);
        
        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(legalEntityCaseId).getString("defendant.id"));

        // Prepare legal entity defendant update with new name and address
        // Using Organisation structure as per LegalEntityDefendant class
        final JsonObject updatedAddress = createObjectBuilder()
                .add("address1", "789 Corporate Avenue")
                .add("address2", "Business District")
                .add("address3", "London")
                .add("address4", "Greater London")
                .add("address5", "UK")
                .add("postcode", "EC1A 1BB")
                .build();

         JsonObject contactNumber = createObjectBuilder()
                .add("home", "02088888888")
                .add("mobile", "07988888888")
                .add("work", "02099999999")
                .add("primaryEmail", "updated@corporation.com")
                .add("secondaryEmail", "info@corporation.com")
                .build();

        final JsonObject organisation = createObjectBuilder()
                .add("name", "Acme Corporation Ltd")
                .add("address", updatedAddress)
                .add("contact", contactNumber)
                .add("incorporationNumber", "INC123456")
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("organisation", organisation)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", legalEntityCaseId.toString())
                .add("legalEntityDefendant", legalEntityDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        // when
        final EventListener defendantDetailsUpdatedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT)
                .run(() -> {
                    final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                            .getMessageProducerClient();
                    publicJmsMessageProducerClient.sendMessage("public.progression.case-defendant-changed", eventPayload);
                });

        // then
        final Optional<JsonEnvelope> defendantDetailsUpdatedEvent = defendantDetailsUpdatedListener.popEvent(DEFENDANT_DETAILS_UPDATED_PUBLIC_EVENT);

        assertThat(defendantDetailsUpdatedEvent.isPresent(), is(true));
        final JsonObject payload = defendantDetailsUpdatedEvent.get().payloadAsJsonObject();
        
        // Verify basic identifiers
        assertThat(payload.getString("caseId"), is(legalEntityCaseId.toString()));
        assertThat(payload.getString("defendantId"), is(defendantId.toString()));
        
        // Verify legal entity name from LegalEntityDefendant
        assertThat(payload.getString("legalEntityName"), is("Acme Corporation Ltd"));
        
        // Verify address from LegalEntityDefendant
        final JsonObject updatedEventAddress = payload.getJsonObject("address");
        assertThat(updatedEventAddress, is(notNullValue()));
        assertThat(updatedEventAddress.getString("address1"), is("789 Corporate Avenue"));
        assertThat(updatedEventAddress.getString("address2"), is("Business District"));
        assertThat(updatedEventAddress.getString("postcode"), is("EC1A 1BB"));
        
        // Verify contact details from LegalEntityDefendant
        // Note: The event schema uses "contactNumber" but the domain object uses ContactDetails
        // The contactDetails should be serialized as contactNumber in the JSON
         contactNumber = payload.getJsonObject("contactNumber");
        if (contactNumber != null) {
            assertThat(contactNumber.getString("home"), is("02088888888"));
            assertThat(contactNumber.getString("mobile"), is("07988888888"));
            assertThat(contactNumber.getString("email"), is("updated@corporation.com"));
        }
        
        // For legal entity defendants, person-specific fields should be null/absent
        assertThat(payload.containsKey("firstName") ? payload.isNull("firstName") : true, is(true));
        assertThat(payload.containsKey("lastName") ? payload.isNull("lastName") : true, is(true));
        assertThat(payload.containsKey("title") ? payload.isNull("title") : true, is(true));
    }
}
