package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.persistence.builder.PersonalDetailsBuilder.buildPersonalDetails;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CasesPendingDatesToAvoidView;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatesToAvoidServiceTest {

    private static final UUID LONDON_REGION_ID = UUID.randomUUID();
    private static final UUID OXFORD_REGION_ID = UUID.randomUUID();
    private static final UUID LEICESTER_REGION_ID = UUID.randomUUID();
    private static final String TITLE = "title";
    private static final String FIRST_NAME = "firstname";
    private static final String LAST_NAME = "lastname";
    private static final LocalDate DATE_OF_BIRTH = now().minusYears(50);
    private static final String LEGAL_ENTITY_NAME = "legal entity name";

    private static final List<RegionalOrganisation> REGIONS = Arrays.asList(
            new RegionalOrganisation(LONDON_REGION_ID, "London", null, null),
            new RegionalOrganisation(OXFORD_REGION_ID, "Oxford", null, null),
            new RegionalOrganisation(LEICESTER_REGION_ID, "Leicester", null, null));

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;
    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;
    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;
    @Mock
    private ReferenceDataService referenceDataService;
    @InjectMocks
    private DatesToAvoidService datesToAvoidService;

    private List<PendingDatesToAvoid> pendingCases;

    public void setUpForPersonalDetails() {
        pendingCases = Arrays.asList(
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndPersonalDetails("London")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndPersonalDetails("Oxford")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndPersonalDetails("")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndPersonalDetails(" ")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndPersonalDetails("  ")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndPersonalDetails(null))
        );
        when(pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid(any())).thenReturn(pendingCases);
        when(referenceDataService.getRegionalOrganisations(any())).thenReturn(REGIONS);
    }

    public void setUpForLegalEntityDetails() {
        pendingCases = Arrays.asList(
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndLegalEntityDetails("London")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndLegalEntityDetails("Oxford")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndLegalEntityDetails("")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndLegalEntityDetails(" ")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndLegalEntityDetails("  ")),
                new PendingDatesToAvoid(createCaseDetailsWithRegionAndLegalEntityDetails(null))
        );
        when(pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid(any())).thenReturn(pendingCases);
        when(referenceDataService.getRegionalOrganisations(any())).thenReturn(REGIONS);
    }
    @Test
    public void shouldReturnFinancialMeansIfExists() {
        setUpForPersonalDetails();
        final JsonEnvelope envelope = createEnvelope();
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = ProsecutingAuthorityAccess.of("TFL");
        final String prosecutingAuthorityFilterValue = "TFL";
        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID());
        caseDetail.getDefendant().setAddress(new Address());
        final List<PendingDatesToAvoid> pendingDatesToAvoidList = Arrays.asList(new PendingDatesToAvoid(caseDetail));

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope)).thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess)).thenReturn(prosecutingAuthorityFilterValue);
        when(pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid(prosecutingAuthorityFilterValue)).thenReturn(pendingDatesToAvoidList);
        when(referenceDataService.getRegionalOrganisations(any())).thenReturn(REGIONS);

        final CasesPendingDatesToAvoidView datesToAvoidsView = datesToAvoidService.findCasesPendingDatesToAvoid(envelope);

        assertThat(datesToAvoidsView.getCases().size(), is(1));
        assertThat(datesToAvoidsView.getCases().get(0).getCaseId(), is(pendingDatesToAvoidList.get(0).getCaseId()));
        assertThat(datesToAvoidsView.getCount(), is(1));
    }

    @Test
    public void shouldFilterPendingCasesByRegionId() {
        setUpForPersonalDetails();
        final JsonEnvelope envelope = createEnvelope(LONDON_REGION_ID);

        final CasesPendingDatesToAvoidView datesToAvoidsView = datesToAvoidService.findCasesPendingDatesToAvoid(envelope);

        assertThat(datesToAvoidsView.getCases(), hasSize(1));
        assertThat(datesToAvoidsView.getCases(), hasItem(hasProperty("region", equalTo("London"))));
        assertEquals(FIRST_NAME, datesToAvoidsView.getCases().get(0).getFirstName());
        assertEquals(LAST_NAME, datesToAvoidsView.getCases().get(0).getLastName());
        assertEquals(DATE_OF_BIRTH, datesToAvoidsView.getCases().get(0).getDateOfBirth());
        assertThat(datesToAvoidsView.getCount(), is(pendingCases.size()));
    }

    @Test
    public void shouldFilterPendingCasesByRegionIdWithLegalEntityDetails() {
        setUpForLegalEntityDetails();
        final JsonEnvelope envelope = createEnvelope(LONDON_REGION_ID);

        final CasesPendingDatesToAvoidView datesToAvoidsView = datesToAvoidService.findCasesPendingDatesToAvoid(envelope);

        assertThat(datesToAvoidsView.getCases(), hasSize(1));
        assertThat(datesToAvoidsView.getCases(), hasItem(hasProperty("region", equalTo("London"))));
        assertEquals(LEGAL_ENTITY_NAME, datesToAvoidsView.getCases().get(0).getLegalEntityName());
        assertNull( datesToAvoidsView.getCases().get(0).getFirstName());
        assertEquals(pendingCases.size(),datesToAvoidsView.getCount());
    }

    @Test
    public void shouldReturnAllPendingCasesWhenFilterCriteriaIsNotProvided() {
        setUpForPersonalDetails();
        final JsonEnvelope envelope = createEnvelope();

        final CasesPendingDatesToAvoidView datesToAvoidsView = datesToAvoidService.findCasesPendingDatesToAvoid(envelope);

        assertThat(datesToAvoidsView.getCases(), containsInAnyOrder(
                hasProperty("region", equalTo("London")),
                hasProperty("region", equalTo("Oxford")),
                hasProperty("region", equalTo("")),
                hasProperty("region", equalTo(" ")),
                hasProperty("region", equalTo("  ")),
                hasProperty("region", nullValue())
        ));
        assertThat(datesToAvoidsView.getCount(), is(pendingCases.size()));
    }

    @Test
    public void shouldReturnPendingCasesWithEmptyRegionWhenFilterCriteriaIsUnknown() {
        setUpForPersonalDetails();
        final JsonEnvelope envelope = createEnvelope("UNKNOWN");

        final CasesPendingDatesToAvoidView datesToAvoidsView = datesToAvoidService.findCasesPendingDatesToAvoid(envelope);

        assertThat(datesToAvoidsView.getCases(), containsInAnyOrder(
                hasProperty("region", equalTo("")),
                hasProperty("region", equalTo(" ")),
                hasProperty("region", equalTo("  ")),
                hasProperty("region", nullValue())
        ));
        assertThat(datesToAvoidsView.getCount(), is(pendingCases.size()));
    }

    private JsonEnvelope createEnvelope(final UUID regionId) {
        return createEnvelope(regionId.toString());
    }

    private JsonEnvelope createEnvelope(final String regionId) {
        return envelope()
                .with(metadataWithRandomUUID("sjp.pending-dates-to-avoid"))
                .withPayloadOf(regionId, "regionId")
                .build();
    }

    private JsonEnvelope createEnvelope() {
        return envelope()
                .with(metadataWithRandomUUID("sjp.pending-dates-to-avoid"))
                .build();
    }

    private CaseDetail createCaseDetailsWithRegionAndPersonalDetails(final String region) {
        return CaseDetailBuilder.aCase().withDefendantDetail(
                DefendantDetailBuilder.aDefendantDetail()
                        .withRegion(region)
                        .withPersonalDetails(buildPersonalDetails()
                                .withTitle(TITLE)
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withDateOfBirth(DATE_OF_BIRTH)
                                .build())
                        .build()).build();
    }

    private CaseDetail createCaseDetailsWithRegionAndLegalEntityDetails(final String region) {
        return CaseDetailBuilder.aCase().withDefendantDetail(
                DefendantDetailBuilder.aDefendantDetail()
                        .withPersonalDetails(null)
                        .withRegion(region)
                        .withLegalEntityDetails(new LegalEntityDetails(LEGAL_ENTITY_NAME))
                        .build()).build();
    }
}

