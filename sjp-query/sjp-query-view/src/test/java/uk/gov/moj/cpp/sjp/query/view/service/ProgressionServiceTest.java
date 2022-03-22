package uk.gov.moj.cpp.sjp.query.view.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope envelope;

    @InjectMocks
    private ProgressionService progressionService;

    private DefendantDetail defaultDefendant;

    @Before
    public void setup() {
        defaultDefendant = createDefaultDefendantDetail();
        when(envelope.payloadAsJsonObject()).thenReturn(createProsecutionCase(defaultDefendant.getPersonalDetails()));
        when(requester.requestAsAdmin(any())).thenReturn(envelope);
    }

    @Test
    public void shouldFindMatchingDefendantOffencesFromProsecutionCase() {
        final DefendantDetail defendantDetail = createDefaultDefendantDetail();
        final List<String> defendantOffences =
                       progressionService.findDefendantOffences(UUID.randomUUID(), defendantDetail);
        final List<String> expectedOffences = new LinkedList<>();
        expectedOffences.add("Offence-1");
        expectedOffences.add("Offence-2");
        assertEquals(expectedOffences, defendantOffences);
    }

    @Test
    public void shouldFindNoMatchingDefendantOffencesFromProsecutionCase() {
        final DefendantDetail defendantDetail = createDefaultDefendantDetail();
        defendantDetail.getPersonalDetails().setFirstName("Jenny");
        final List<String> defendantOffences =
                       progressionService.findDefendantOffences(UUID.randomUUID(), defendantDetail);
        assertTrue(defendantOffences.isEmpty());
    }

    private DefendantDetail createDefaultDefendantDetail() {
        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID());

        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("John");
        personalDetails.setLastName("Smith");
        personalDetails.setDateOfBirth(LocalDate.parse("1980-10-12"));
        personalDetails.setAddress(new Address("2 Kings Avenue", "", "", "", "", "WC1E 1EE"));
        defendantDetail.setPersonalDetails(personalDetails);

        return defendantDetail;
    }

    private JsonObject createProsecutionCase(PersonalDetails personalDetails) {
        final JsonArray offences = createArrayBuilder().
                                     add(createObjectBuilder().add("wording", "Offence-1").build()).
                                     add(createObjectBuilder().add("wording", "Offence-2").build()).
                                   build();
        final JsonObject personDetails = createObjectBuilder().
                                           add("firstName", personalDetails.getFirstName()).
                                           add("lastName", personalDetails.getLastName()).
                                           add("dateOfBirth", personalDetails.getDateOfBirth().toString()).
                                           add("address", createObjectBuilder().
                                                            add("address1", personalDetails.getAddress().getAddress1()).
                                                            add("postcode", personalDetails.getAddress().getPostcode()).
                                                          build()).
                                         build();

        final JsonObject personDefendant = createObjectBuilder().
                add("personDetails", personDetails).
                add("bailStatus", createObjectBuilder().
                        add("code", "A").
                        add("description", "Not applicable").
                        add("id", UUID.randomUUID().toString()).
                        build()).
                build();

        final JsonArray defendants = createArrayBuilder().
                                       add(createObjectBuilder().
                                               add("offences", offences).
                                               add("personDefendant", personDefendant).
                                           build()).
                                     build();



        return createObjectBuilder().add("prosecutionCase",
                                         createObjectBuilder().add("defendants", defendants)).build();
    }

}
