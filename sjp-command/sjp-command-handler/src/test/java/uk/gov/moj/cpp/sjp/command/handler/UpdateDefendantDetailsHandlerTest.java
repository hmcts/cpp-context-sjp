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

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;

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
public class UpdateDefendantDetailsHandlerTest {

    @Spy
    private Clock clock = new UtcClock();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantPersonalNameUpdated.class, DefendantDetailsUpdated.class);

    @InjectMocks
    private UpdateDefendantDetailsHandler updateDefendantDetailsHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    private static final UUID defendantId = randomUUID();
    private static final UUID caseId = randomUUID();
    private static final String firstName = "test";
    private static final String lastName = "lastName";
    private static final String email = "email";
    private static final String mobileNumber = "mobileNumber";
    private static final String gender = "gender";
    private static final String nationalInsuranceNumber = "nationalInsuranceNumber";
    private static final String homeNumber = "homeNumber";
    private static final String dateOfBirth = LocalDate.parse("1980-07-15").toString();

    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "England";
    private static final String ADDRESS_4 = "UK";
    private static final String POSTCODE = "W1T 1JY";
    private static final Address address = new Address(ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE);

    @Test
    public void shouldUpdateDefendantDetails() throws EventStreamException {

        final CaseAggregate caseAggregate = new CaseAggregate();

        final JsonEnvelope command = createUpdateDefendantDetailsCommand(caseId, defendantId, firstName,
                lastName, email, gender, nationalInsuranceNumber, homeNumber, mobileNumber, address, dateOfBirth);

        final EventStream eventStream = Mockito.mock(EventStream.class);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsHandler.updateDefendantDetails(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-personal-name-updated"),
                                payloadIsJson(withJsonPath("$.newPersonalName.firstName", equalTo(firstName)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.firstName", equalTo(firstName)),
                                        withJsonPath("$.lastName", equalTo(lastName)),
                                        withJsonPath("$.gender", equalTo(gender)),
                                        withJsonPath("$.nationalInsuranceNumber", equalTo(nationalInsuranceNumber)),
                                        withJsonPath("$.contactDetails.email", equalTo(email)),
                                        withJsonPath("$.contactDetails.home", equalTo(homeNumber)),
                                        withJsonPath("$.contactDetails.mobile", equalTo(mobileNumber)),
                                        withJsonPath("$.dateOfBirth", equalTo(dateOfBirth)))))
                )));
    }

    private static JsonEnvelope createUpdateDefendantDetailsCommand(UUID caseId, UUID defendantId, String firstName,
                                                                    String lastName, String email, String gender,
                                                                    String nationalInsuranceNumber, String homeNumber,
                                                                    String mobileNumber, Address address,
                                                                    String dateOfBirth) {
        final JsonObject contactNumber = createObjectBuilder()
                .add("home", homeNumber)
                .add("mobile", mobileNumber)
                .build();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("caseId", caseId.toString())
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("dateOfBirth", dateOfBirth)
                .add("email", email)
                .add("gender", gender)
                .add("nationalInsuranceNumber", nationalInsuranceNumber)
                .add("contactNumber", contactNumber)
                .add("address", toJsonObject(address));

        return JsonEnvelopeBuilder.envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-defendant-details"),
                payload.build());
    }

    private static JsonObject toJsonObject(final Address address){
        return createObjectBuilder()
                .add("address1", address.getAddress1())
                .add("address2", address.getAddress2())
                .add("address3", address.getAddress3())
                .add("address4", address.getAddress4())
                .add("postcode", address.getPostcode())
                .build();
    }

}
