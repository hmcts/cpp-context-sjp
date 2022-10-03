package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.persistence.builder.UpdatedDefendantDetailsBuilder.anUpdatedDefendantDetails;
import static uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView.DefendantDetailsUpdate;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequestsQueryResult;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantServiceTest {

    private static final UUID LONDON_REGION_ID = UUID.randomUUID();

    private static final List<RegionalOrganisation> REGIONS = Arrays.asList(
            new RegionalOrganisation(LONDON_REGION_ID, "London", null, null),
            new RegionalOrganisation(UUID.randomUUID(), "Oxford", null, null),
            new RegionalOrganisation(UUID.randomUUID(), "Leicester", null, null));

    @Mock
    private DefendantRepository defendantRepository;
    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;
    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;
    @Mock
    private ReferenceDataService referenceDataService;
    @InjectMocks
    private DefendantService defendantService;

    private List<UpdatedDefendantDetails> updatedDefendantDetails;
    private UpdatedDefendantDetails updatedDefendantDetails1;
    private UpdatedDefendantDetails updatedDefendantDetails2;
    private UpdatedDefendantDetails updatedDefendantDetails3;
    private UpdatedDefendantDetails updatedDefendantDetails4;
    private UpdatedDefendantDetails updatedDefendantDetails5;

    @Before
    public void setup() {
        updatedDefendantDetails1 = createUpdatedDefendantDetailsWithRegion("London");
        updatedDefendantDetails2 = createUpdatedDefendantDetailsWithRegion("Oxford");
        updatedDefendantDetails3 = createUpdatedDefendantDetailsWithRegion(" ");
        updatedDefendantDetails4 = createUpdatedDefendantDetailsWithRegion("");
        updatedDefendantDetails5 = createUpdatedDefendantDetailsWithRegion(null);

        updatedDefendantDetails = newArrayList(
                updatedDefendantDetails1,
                updatedDefendantDetails2,
                updatedDefendantDetails3,
                updatedDefendantDetails4,
                updatedDefendantDetails5);

        when(referenceDataService.getRegionalOrganisations(Matchers.any())).thenReturn(REGIONS);
        when(defendantRepository.findUpdatedByCaseProsecutingAuthority(
                Matchers.anyString(),
                Matchers.any(ZonedDateTime.class),
                Matchers.any(ZonedDateTime.class)))
                .thenReturn(updatedDefendantDetails);
    }

    @Test
    public void shouldFindDefendantUpdates() {
        final JsonEnvelope envelope = envelope();

        final UpdatedDefendantDetails updatedDefendantDetails1 = createUpdatedDefendantDetails(ZonedDateTime.now());
        final UpdatedDefendantDetails updatedDefendantDetails2 = createUpdatedDefendantDetails(ZonedDateTime.now().minusDays(2));
        final UpdatedDefendantDetails updatedDefendantDetails3 = createUpdatedDefendantDetails(ZonedDateTime.now().minusDays(4));

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

        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(0), is(DefendantDetailsUpdate.of(updatedDefendantDetails3)));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(1), is(DefendantDetailsUpdate.of(updatedDefendantDetails2)));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(2), is(DefendantDetailsUpdate.of(updatedDefendantDetails1)));
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

    @Test
    public void shouldFindDefendantListByReadyCasesWithEmptyResults() {
        when(defendantRepository.findByReadyCases()).thenReturn(null);

        final DefendantOutstandingFineRequestsQueryResult outstandingFineRequests = defendantService.getOutstandingFineRequests();

        assertThat(outstandingFineRequests.getDefendantDetails(), emptyIterable());
    }

    @Test
    public void shouldFindDefendantListByReadyCases() {
        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withFirstName("Defen")
                .build();
        final CaseDetail caseDetail = CaseDetailBuilder.aCase().withDefendantDetail(defendantDetail).build();
        defendantDetail.setCaseDetail(caseDetail);
        when(defendantRepository.findByReadyCases()).thenReturn(asList(defendantDetail));

        final DefendantOutstandingFineRequestsQueryResult outstandingFineRequests = defendantService.getOutstandingFineRequests();

        assertThat(outstandingFineRequests.getDefendantDetails(), hasSize(1));
        assertThat(outstandingFineRequests.getDefendantDetails(), hasItem(hasProperty("firstName", equalTo("Defen"))));
    }

    @Test
    public void shouldReturnDefendantDetailUpdatesUnfilteredWhenNoFilterCriteriaIsProvided() {
        final JsonEnvelope envelope = envelope();

        final DefendantDetailsUpdatesView defendantDetailUpdates = defendantService.findDefendantDetailUpdates(envelope);

        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates(), containsInAnyOrder(
                DefendantDetailsUpdate.of(updatedDefendantDetails1),
                DefendantDetailsUpdate.of(updatedDefendantDetails2),
                DefendantDetailsUpdate.of(updatedDefendantDetails3),
                DefendantDetailsUpdate.of(updatedDefendantDetails4),
                DefendantDetailsUpdate.of(updatedDefendantDetails5)
        ));
        assertThat(defendantDetailUpdates.getTotal(), is(updatedDefendantDetails.size()));
    }

    @Test
    public void shouldReturnDefendantUpdatesFilteredByRegionId() {
        final JsonEnvelope envelope = envelope(LONDON_REGION_ID.toString());

        final DefendantDetailsUpdatesView defendantDetailUpdates = defendantService.findDefendantDetailUpdates(envelope);

        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates(), containsInAnyOrder(
                DefendantDetailsUpdate.of(updatedDefendantDetails1)));
        assertThat(defendantDetailUpdates.getTotal(), is(updatedDefendantDetails.size()));
    }

    @Test
    public void shouldFindDefendantUpdatesFilteredEmptyList() {
        final JsonEnvelope envelope = envelope("UNKNOWN");

        final DefendantDetailsUpdatesView defendantDetailUpdates = defendantService.findDefendantDetailUpdates(envelope);

        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates(), containsInAnyOrder(
                DefendantDetailsUpdate.of(updatedDefendantDetails3),
                DefendantDetailsUpdate.of(updatedDefendantDetails4),
                DefendantDetailsUpdate.of(updatedDefendantDetails5)
        ));
        assertThat(defendantDetailUpdates.getTotal(), is(updatedDefendantDetails.size()));
    }

    @Test
    public void shouldApplyLimitAfterFilteredByRegionAndOrderByLastUpdated() {
        final int limit = 2;
        final JsonEnvelope envelope = envelope("UNKNOWN", limit);
        final UpdatedDefendantDetails update1 = anUpdatedDefendantDetails().withUpdateTime(ZonedDateTime.now()).withRegion("").build();
        final UpdatedDefendantDetails update2 = anUpdatedDefendantDetails().withUpdateTime(ZonedDateTime.now().minusDays(1)).withRegion("").build();
        final UpdatedDefendantDetails update3 = anUpdatedDefendantDetails().withUpdateTime(ZonedDateTime.now().minusDays(2)).withRegion("").build();
        when(defendantRepository.findUpdatedByCaseProsecutingAuthority(Matchers.anyString(),
                Matchers.any(ZonedDateTime.class),
                Matchers.any(ZonedDateTime.class)))
                .thenReturn(asList(update3, update2, update1));

        final DefendantDetailsUpdatesView defendantDetailUpdates = defendantService.findDefendantDetailUpdates(envelope);

        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates(), hasSize(2));
        assertThat(defendantDetailUpdates.getTotal(), is(3));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(0).getUpdatedOn(), equalTo(
                update3.getMostRecentUpdateDate().get().toLocalDate().toString()));
        assertThat(defendantDetailUpdates.getDefendantDetailsUpdates().get(1).getUpdatedOn(), equalTo(
                update2.getMostRecentUpdateDate().get().toLocalDate().toString()));
    }

    @Test
    public void shouldFindDefendantListByReadyCasesWithOnlyRequiredFields() {

        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(),
                new PersonalDetails(null, null, "Dant",
                        null, Gender.MALE,
                        "54321", null, null),
                null, 2,null,null,null);
        final CaseDetail caseDetail = CaseDetailBuilder.aCase().withCaseId(UUID.randomUUID()).build();
        defendantDetail.setCaseDetail(caseDetail);


        when(defendantRepository.findByReadyCases()).thenReturn(asList(defendantDetail));

        final DefendantOutstandingFineRequestsQueryResult outstandingFineRequests = defendantService.getOutstandingFineRequests();

        assertThat(outstandingFineRequests.getDefendantDetails().size() , greaterThan(0));
        assertThat(outstandingFineRequests.getDefendantDetails().get(0).getCaseId(), is(caseDetail.getId()));
        assertThat(outstandingFineRequests.getDefendantDetails().get(0).getLastName(), is("Dant"));

    }

    private UpdatedDefendantDetails createUpdatedDefendantDetailsWithRegion(final String region) {
        return anUpdatedDefendantDetails()
                .withRegion(region)
                .build();
    }

    private UpdatedDefendantDetails createUpdatedDefendantDetails(final ZonedDateTime updateTime) {
        return anUpdatedDefendantDetails()
                .withUpdateTime(updateTime)
                .build();
    }

    private JsonEnvelope envelope(final String regionFilterCriteria) {
        return envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder()
                        .add("limit", Integer.MAX_VALUE)
                        .add("regionId", regionFilterCriteria)
        );
    }

    private JsonEnvelope envelope(final String regionFilterCriteria, final int limit) {
        return envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder()
                        .add("limit", limit)
                        .add("regionId", regionFilterCriteria)
        );
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder()
                        .add("limit", Integer.MAX_VALUE)
        );
    }

}
