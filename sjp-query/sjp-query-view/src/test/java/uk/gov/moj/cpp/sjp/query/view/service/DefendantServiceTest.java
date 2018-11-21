package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantServiceTest {

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @InjectMocks
    private DefendantService defendantService;

    @Test
    public void shouldFindDefendantUpdates() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("limit", Integer.MAX_VALUE));

        UpdatedDefendantDetails updatedDefendantDetails1 = createUpdatedDefendantDetails(ZonedDateTime.now());
        UpdatedDefendantDetails updatedDefendantDetails2 = createUpdatedDefendantDetails(ZonedDateTime.now().minusDays(2));
        UpdatedDefendantDetails updatedDefendantDetails3 = createUpdatedDefendantDetails(ZonedDateTime.now().minusDays(4));

        final List<UpdatedDefendantDetails> updatedDefendantDetails = newArrayList(
                updatedDefendantDetails1,
                updatedDefendantDetails3,
                updatedDefendantDetails2);
        when(defendantRepository.findUpdatedByCaseProsecutingAuthority(
                Matchers.anyString(),
                Matchers.any(ZonedDateTime.class),
                Matchers.any(ZonedDateTime.class)))
                .thenReturn(updatedDefendantDetails);

        final DefendantDetailsUpdatesView defendantDetailUpdates = defendantService.findDefendantDetailUpdates(envelope);

        assertThat(defendantDetailUpdates.getTotal(), is(3));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates(), iterableWithSize(3));

        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(0), is(DefendantDetailsUpdatesView.DefendantDetailsUpdate.of(updatedDefendantDetails3)));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(1), is(DefendantDetailsUpdatesView.DefendantDetailsUpdate.of(updatedDefendantDetails2)));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(2), is(DefendantDetailsUpdatesView.DefendantDetailsUpdate.of(updatedDefendantDetails1)));
    }

    @Test
    public void shouldObeyLimitProvided() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("limit", 1).build());

        final List<UpdatedDefendantDetails> updatedDefendantDetails = IntStream.range(0, 3)
                .mapToObj(i -> createUpdatedDefendantDetails(ZonedDateTime.now()))
                .collect(toList());
        when(defendantRepository.findUpdatedByCaseProsecutingAuthority(
                Matchers.anyString(),
                Matchers.any(ZonedDateTime.class),
                Matchers.any(ZonedDateTime.class)))
                .thenReturn(updatedDefendantDetails);

        final DefendantDetailsUpdatesView defendantDetailUpdates = defendantService.findDefendantDetailUpdates(envelope);
        assertThat(defendantDetailUpdates.getTotal(), is(3));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates(), iterableWithSize(1));
    }

    private String mockProsecutorAuthority(final JsonEnvelope envelope) {
        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope))
                .thenReturn(ProsecutingAuthorityAccess.ALL);

        String prosecutingAuthorityAccessFilter = "%";
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(ProsecutingAuthorityAccess.ALL))
                .thenReturn(prosecutingAuthorityAccessFilter);
        return prosecutingAuthorityAccessFilter;
    }

    private UpdatedDefendantDetails createUpdatedDefendantDetails(final ZonedDateTime updateTime) {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(updateTime);
        personalDetails.setDateOfBirth(LocalDate.now());

        return new UpdatedDefendantDetails(
                "firstName",
                "lastName",
                LocalDate.now(),
                UUID.randomUUID(),
                updateTime,
                null,
                null,
                "caseUrn",
                UUID.randomUUID());
    }
}
