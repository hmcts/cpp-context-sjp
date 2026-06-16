package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

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

    private final UUID userId = randomUUID();
    private static final String USER_DETAILS_QUERY_NAME = "usersgroups.get-user-details";

    @BeforeEach
    public void setup() {
        initMocks(this);
    }

    static Stream<Arguments> data() {
        return Stream.of(
                // Non existing group
                Arguments.of(false, asList("foo", "bar")),

                // Real Groups:
                Arguments.of(true,  singletonList("Court Administrators")),
                Arguments.of(true,  singletonList("Legal Advisers")),
                Arguments.of(true,  singletonList("Magistrates")),
                Arguments.of(false, singletonList("SJP Prosecutors")),
                Arguments.of(false, singletonList("TFL Prosecutors")),
                Arguments.of(false, singletonList("TVL Prosecutors")),

                // Typo/case-insensitive/particular cases
                Arguments.of(false, emptyList()),
                Arguments.of(false, singletonList("LegalAdvisers")),
                Arguments.of(false, singletonList("Legal Advisers ")),
                Arguments.of(false, singletonList(" Legal Advisers")),
                Arguments.of(false, singletonList("Legal Adviser")),
                Arguments.of(false, singletonList("egal advisers")),

                // Combinations
                Arguments.of(true,  asList("TFL Prosecutors", "Court Administrators", "Legal Advisers")),
                Arguments.of(false,  asList("SJP Prosecutors", "TFL Prosecutors")),
                Arguments.of(false,  asList("TVL Prosecutors", "SJP Prosecutors")),
                Arguments.of(true,  asList("TVL Prosecutors", "SJP Prosecutors", "Legal Advisers")),
                Arguments.of(true,  asList("TVL Prosecutors", "Court Administrators")),
                Arguments.of(true,  asList("TFL Prosecutors", "Legal Advisers")),

                Arguments.of(true,  asList("Court Administrators", "foobar", "Legal Advisers"))
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void canSeeOnlinePleaFinances(boolean expectedResult, List<String> loggedUserGroups) {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), anyList()))
                .thenReturn(expectedResult);

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
