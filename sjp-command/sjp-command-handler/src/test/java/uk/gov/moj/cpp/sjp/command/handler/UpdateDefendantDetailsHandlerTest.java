package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.aggregate.DefendantAggregate;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantDetailsHandlerTest extends BasePersonInfoHandlerTest{

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantDetailsUpdated.class);

    @InjectMocks
    private UpdateDefendantDetailsHandler updateDefendantDetailsHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    private final UUID defendantId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID personId = randomUUID();
    private final String title = "Mr";
    private final String firstName = "test";
    private final String lastName = "lastName";
    private final String email = "email";
    private final String gender = "gender";
    private final String nationalInsuranceNumber = "nationalInsuranceNumber";
    private final String homeNumber = "homeNumber";
    private final Address address = new Address(ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE);
    private final String dateOfBirth = "1980-07-15";

    @Test
    public void verifyExistenceOfDefendantDetailsUpdatedEvent() throws EventStreamException {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        LocalDate birthDate = LocalDate.parse(dateOfBirth);

        final JsonEnvelope command = createDefendantIdHandlerCommand(caseId,defendantId,personId, firstName,lastName,email,
                gender,nationalInsuranceNumber,homeNumber,address,birthDate);

        EventStream eventStream = Mockito.mock(EventStream.class);
        when(eventSource.getStreamById(personId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);

        updateDefendantDetailsHandler.updateDefendantDetails(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("structure.events.defendant-details-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),

                                        withJsonPath("$.firstName", equalTo(firstName)),
                                        withJsonPath("$.lastName", equalTo(lastName)),
                                        withJsonPath("$.email", equalTo(email)),
                                        withJsonPath("$.gender", equalTo(gender)),
                                        withJsonPath("$.nationalInsuranceNumber", equalTo(nationalInsuranceNumber)),
                                        withJsonPath("$.contactNumber.home", equalTo(homeNumber)),
                                        withJsonPath("$.contactNumber.mobile", equalTo(mobileNumber)),
                                        withJsonPath("$.dateOfBirth", equalTo(dateOfBirth))
                                )))
                )));
    }

    private JsonEnvelope createDefendantIdHandlerCommand(UUID caseId, final UUID defendantId, final UUID personId, String firstName,
                                                           String lastName, String email, String gender,
                                                           String nationalInsuranceNumber, String homeNumber, Address address,
                                                           LocalDate dateOfBirth) {
        final JsonObject contactNumber = createObjectBuilder()
                .add("home", homeNumber)
                .add("mobile", mobileNumber)
                .build();
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("caseId", caseId.toString())
                .add("personId", personId.toString())
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("dateOfBirth", dateOfBirth.toString())
                .add("email", email)
                .add("gender", gender)
                .add("nationalInsuranceNumber", nationalInsuranceNumber)
                .add("contactNumber", contactNumber)
                .add("address", addAddressToPayload(address));

        return JsonEnvelopeBuilder.envelopeFrom(
                metadataOf(randomUUID(), "structure.command.update-defendant-details"),
                payload.build());
    }

}
