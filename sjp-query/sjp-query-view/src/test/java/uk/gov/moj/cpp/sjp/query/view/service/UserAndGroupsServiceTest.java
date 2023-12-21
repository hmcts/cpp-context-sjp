package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@RunWith(Parameterized.class)
public class UserAndGroupsServiceTest {

    @InjectMocks
    private UserAndGroupsService service;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Mock
    private JsonEnvelope inputJsonEnvelope;

    @Captor
    private ArgumentCaptor<List<String>> groupsWhatCanSeeFinancesCaptor;

    @Captor
    private ArgumentCaptor<Action> actionCaptor;

    @Parameter(0)
    public boolean expectedResult;

    @Parameter(1)
    public List<String> loggedUserGroups;

    private final UUID userId = randomUUID();
    private static final String USER_DETAILS_QUERY_NAME = "usersgroups.get-user-details";



    @Before
    public void setup() {
        initMocks(this);

        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), anyListOf(String.class)))
                .thenReturn(expectedResult);
    }

    @Parameters(name = "canSeeOnlinePleaFinances() returns {0} when user within groups {1}.")
    public static Collection<Object[]> data() {
        return asList(new Object[][] {
                // Non existing group
                {false, asList("foo", "bar")},

                // Real Groups:
                {true,  singletonList("Court Administrators")},
                {true,  singletonList("Legal Advisers")},
                {true,  singletonList("Magistrates")},
                {false, singletonList("SJP Prosecutors")},
                {false, singletonList("TFL Prosecutors")},
                {false, singletonList("TVL Prosecutors")},

                // Typo/case-insensitive/particular cases
                {false, emptyList()},
                {false, singletonList("LegalAdvisers")},
                {false, singletonList("Legal Advisers ")},
                {false, singletonList(" Legal Advisers")},
                {false, singletonList("Legal Adviser")},
                {false, singletonList("egal advisers")},

                // Combinations
                {true,  asList("TFL Prosecutors", "Court Administrators", "Legal Advisers")},
                {false,  asList("SJP Prosecutors", "TFL Prosecutors")},
                {false,  asList("TVL Prosecutors", "SJP Prosecutors")},
                {true,  asList("TVL Prosecutors", "SJP Prosecutors", "Legal Advisers")},
                {true,  asList("TVL Prosecutors", "Court Administrators")},
                {true,  asList("TFL Prosecutors", "Legal Advisers")},

                {true,  asList("Court Administrators", "foobar", "Legal Advisers")}
        });
    }

    @Test
    public void canSeeOnlinePleaFinances() {
        boolean actualResult = service.canSeeOnlinePleaFinances(inputJsonEnvelope);
        assertThat(actualResult, is(expectedResult));

        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(
                actionCaptor.capture(), groupsWhatCanSeeFinancesCaptor.capture());

        assertThat(actionCaptor.getValue().envelope(), equalTo(inputJsonEnvelope));
        assertThat(loggedUserGroups.stream().anyMatch(groupsWhatCanSeeFinancesCaptor.getValue()::contains), is(expectedResult));
    }

    @Test
    public void shouldGetUserDetails() {
        final JsonObject expectedUserDetails = createObjectBuilder()
                .add("firstName", "FN")
                .add("lastName", "LN")
                .build();

        final JsonEnvelope expected = envelopeFrom(metadataBuilder()
                .withName("usersgroups.get-user-details").withId(randomUUID()).build(), expectedUserDetails);

        when(requester.requestAsAdmin(any())).thenReturn(expected);

        final String userDetails = service.getUserDetails(userId);

        assertThat(userDetails, equalTo("FN LN"));
    }
}
