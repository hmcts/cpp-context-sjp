package uk.gov.moj.cpp.sjp.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_CASE_ID;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_QUERY;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_URN;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequest;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequestsQueryResult;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesPendingDatesToAvoidView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantProfilingView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.service.DatesToAvoidService;
import uk.gov.moj.cpp.sjp.query.view.service.DefendantService;
import uk.gov.moj.cpp.sjp.query.view.service.EmployerService;
import uk.gov.moj.cpp.sjp.query.view.service.FinancialMeansService;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.view.service.UserAndGroupsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryViewTest {

    private static final String URN = "urn";
    private static final UUID CASE_ID = UUID.randomUUID();

    @Spy
    private Clock clock = new UtcClock();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope envelope, outputEnvelope;

    @Mock
    private Metadata metadata;

    @Mock
    private CaseService caseService;

    @Mock
    private DefendantService defendantService;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @Mock
    private FinancialMeansService financialMeansService;

    @Mock
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Mock
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Mock
    private DatesToAvoidService datesToAvoidService;

    @Mock
    private EmployerService employerService;

    @Mock
    private JsonObject payloadObject;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private CaseSearchResultsView caseSearchResultsView;

    @Mock
    private OffenceRepository offenceRepository;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    @Test
    public void shouldFindCase() {
        setupCaseExpectations();
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(caseService.findCase(CASE_ID)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCase(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCase(CASE_ID);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldFindCaseByUrn() {
        setupExpectations();
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(payloadObject.getString(FIELD_URN)).thenReturn(URN);
        when(caseService.findCaseByUrn(URN)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrn(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseByUrn(URN);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldFindCaseByUrnPostcode() {

        final String urn = "TFL1234567";
        final String postcode = "AB1 2CD";

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode"))
                .withPayloadOf(urn, "urn")
                .withPayloadOf(postcode, "postcode")
                .build();

        final CaseDetail caseDetail = CaseDetailBuilder.aCase().addDefendantDetail(
                aDefendantDetail().withPostcode(postcode).withId(UUID.randomUUID()).build())
                .withCompleted(false).withProsecutingAuthority("TFL")
                .withCaseId(UUID.randomUUID()).withUrn(urn).build();

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "Transport for London")
                .add("policeFlag", false)
                .build();

        final CaseView caseView = new CaseView(caseDetail, prosecutorPayload);

        when(caseService.findCaseByUrnPostcode(urn, postcode)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrnPostcode(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.case-response"),
                payload().isJson(allOf(
                        withJsonPath("$.urn", is(urn)),
                        withJsonPath("$.defendant.personalDetails.address.postcode", is(postcode)),
                        withJsonPath("$.completed", is(false))
                ))
        ));
    }

    @Test
    public void shouldFindCaseSearchResults() {
        setupExpectations();
        final String query = "query";

        when(caseService.searchCases(envelope, query)).thenReturn(caseSearchResultsView);
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(query);

        final JsonEnvelope result = sjpQueryView.findCaseSearchResults(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).searchCases(envelope, query);
        verify(function).apply(caseSearchResultsView);
    }

    @Test
    public void shouldSearchCaseByMaterialId() {
        setupExpectations();
        final UUID query = UUID.fromString("dc1c7baf-5230-4580-877d-b4ee25bc7188");
        final UUID caseId = UUID.randomUUID();
        final SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseId, null);
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(query.toString());
        when(caseService.searchCaseByMaterialId(query)).thenReturn(searchCaseByMaterialIdView);

        final JsonEnvelope result = sjpQueryView.searchCaseByMaterialId(envelope);

        assertThat(result, equalTo(outputEnvelope));
        verify(caseService).searchCaseByMaterialId(query);
        verify(function).apply(searchCaseByMaterialIdView);
    }

    @Test
    public void shouldFindCaseDocuments() {
        setupCaseExpectations();
        final CaseDocumentsView caseDocumentsView = new CaseDocumentsView(emptyList());
        when(caseService.findCaseDocuments(CASE_ID)).thenReturn(caseDocumentsView);

        final JsonEnvelope result = sjpQueryView.findCaseDocuments(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseDocuments(CASE_ID);
        verify(function).apply(caseDocumentsView);
    }

    @Test
    public void shouldReturnFinancialMeans() {

        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.financial-means"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1.1));
        final Benefits benefit = new Benefits(true, "benefitType", null);
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefit, "EMPLOYED");

        when(financialMeansService.getFinancialMeans(defendantId)).thenReturn(of(financialMeans));

        final JsonEnvelope result = sjpQueryView.findFinancialMeans(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.financial-means"),
                payload().isJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.income.amount", is(income.getAmount().doubleValue())),
                        withJsonPath("$.income.frequency", equalTo(income.getFrequency().name())),
                        withJsonPath("$.benefits.claimed", is(benefit.getClaimed())),
                        withJsonPath("$.benefits.type", is(benefit.getType())),
                        withJsonPath("$.employmentStatus", is(financialMeans.getEmploymentStatus()))
                )))
        );
    }

    @Test
    public void shouldReturnEmptyPayloadIfFinancialMeansDoesNotExits() {
        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.financial-means"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        when(financialMeansService.getFinancialMeans(defendantId)).thenReturn(Optional.empty());

        final JsonEnvelope result = sjpQueryView.findFinancialMeans(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.financial-means"),
                payload().isJson(hasJsonPath("$.*", empty()))
        ));
    }

    @Test
    public void shouldReturnEmployer() {

        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.employer"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final Address address = new Address("address 1", "address 2", "address 3", "address 4", "address 5", "AB3 4CD");
        final Employer employer = new Employer(defendantId, "KFC", "abcdef", "02020202020", address);

        when(employerService.getEmployer(defendantId)).thenReturn(of(employer));

        final JsonEnvelope result = sjpQueryView.findEmployer(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.employer"),
                payload().isJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.name", is(employer.getName())),
                        withJsonPath("$.employeeReference", equalTo(employer.getEmployeeReference())),
                        withJsonPath("$.phone", is(employer.getPhone())),
                        withJsonPath("$.address.address1", is(address.getAddress1())),
                        withJsonPath("$.address.address2", is(address.getAddress2())),
                        withJsonPath("$.address.address3", is(address.getAddress3())),
                        withJsonPath("$.address.address4", is(address.getAddress4())),
                        withJsonPath("$.address.address5", is(address.getAddress5())),
                        withJsonPath("$.address.postcode", is(address.getPostcode()))
                ))));
    }

    @Test
    public void shouldReturnEmptyPayloadIfEmployerDoesNotExits() {
        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.employer"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        when(employerService.getEmployer(defendantId)).thenReturn(Optional.empty());

        final JsonEnvelope result = sjpQueryView.findEmployer(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.employer"),
                payload().isJson(hasJsonPath("$.*", empty()))
        ));
    }

    @Test
    public void shouldGetPendingCases() {
        setupExpectations();
        when(envelope.metadata()).thenReturn(metadata);
        final JsonObject payload = createObjectBuilder().build();
        when(caseService.findPendingCasesToPublish(ExportType.PUBLIC)).thenReturn(payload);

        final JsonEnvelope result = sjpQueryView.getPendingCasesToPublish(envelope);

        verify(function).apply(payload);
        assertThat(result, is(outputEnvelope));
    }

    @Test
    public void getPendingCasesToPublishShouldExportPublicReportByDefault() {
        setupExpectations();

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void getPendingCasesToPublishShouldExportPressReport() {
        setupExpectations();
        final JsonObject payload = createObjectBuilder().add("export", "press").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PRESS);
    }

    @Test
    public void getPendingCasesToPublishExportParamShouldBeCaseInsensitive() {
        setupExpectations();
        final JsonObject payload = createObjectBuilder().add("export", "Public").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void getPendingCasesToPublishShouldUseDefaultIfTypeIsUnknown() {
        setupExpectations();
        final JsonObject payload = createObjectBuilder().add("export", "UnknownType").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void shouldFindPendingDatesToAvoid() {
        final UUID caseId = randomUUID();
        final JsonEnvelope response = mockAndVerifyPendingDatesToAvoid(caseId, UUID.randomUUID());
        assertThat(response, jsonEnvelope(metadata().withName("sjp.pending-dates-to-avoid"), payload().isJson(allOf(
                withJsonPath("$.cases[0].caseId", equalTo(caseId.toString())),
                withJsonPath("$.count", equalTo(2))
        ))));
    }

    @Test
    public void shouldNotFindPendingDatesToAvoid() {
        final JsonEnvelope response = mockAndVerifyPendingDatesToAvoid();
        assertThat(response, jsonEnvelope(metadata().withName("sjp.pending-dates-to-avoid"), payload().isJson(
                withJsonPath("$.count", equalTo(0))
        )));
    }

    @Test
    public void shouldFindDefendantsOnlinePlea() {
        final UUID caseId = randomUUID();
        final UUID defendantId = UUID.fromString("4a950d66-b95f-459b-b77d-5ed308c3be02");
        final String offenceCode = "OffenceCode";
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendants-online-plea"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final UUID offenceId = UUID.fromString("8a962d66-b95f-69b-b77d-9ed308c3be02");
        final OnlinePlea onlinePlea = stubOnlinePlea(caseId, defendantId, offenceId);

        when(userAndGroupsService.canSeeOnlinePleaFinances(queryEnvelope)).thenReturn(true);
        when(onlinePleaRepository.findBy(caseId)).thenReturn(onlinePlea);

        final List<OnlinePleaDetail> onlinePleaDetails = getOnlinePleaDetails(offenceId);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseId, defendantId)).thenReturn(onlinePleaDetails);

        final OffenceDetail offenceDetail = new OffenceDetail();
        offenceDetail.setCode(offenceCode);
        when(offenceRepository.findBy(offenceId)).thenReturn(offenceDetail);

        final JsonObject offenceData = Json.createObjectBuilder()
                .add("title", "Offence title").build();
        when(referenceDataService.getOffenceData(offenceCode)).thenReturn(of(offenceData));

        final JsonEnvelope response = sjpQueryView.findDefendantsOnlinePlea(queryEnvelope);

        verify(onlinePleaRepository).findBy(caseId);
        verify(onlinePleaRepository, never()).findOnlinePleaWithoutFinances(any());

        assertThat(response, jsonEnvelope(metadata().withName("sjp.query.defendants-online-plea"), payload().isJson(allOf(
                withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.onlinePleaDetails[0].plea", equalTo(GUILTY.name())),
                withJsonPath("$.onlinePleaDetails[0].mitigation", equalTo("mitigation")),
                withJsonPath("$.onlinePleaDetails[1].plea", equalTo(NOT_GUILTY.name())),
                withJsonPath("$.onlinePleaDetails[1].notGuiltyBecause", equalTo("Not Guilty Because"))
        ))));
    }

    @Test
    public void shouldFindDefendantsOnlinePleaWithoutFinancesForProsecutor() {
        final UUID caseId = randomUUID();
        final UUID defendantId = UUID.fromString("4a950d66-b95f-459b-b77d-5ed308c3be02");
        final String offenceCode = "OffenceCode";
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendants-online-plea"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final UUID offenceId = UUID.fromString("8a962d66-b95f-69b-b77d-9ed308c3be02");
        final OnlinePlea onlinePlea = stubOnlinePlea(caseId, defendantId, offenceId);

        when(userAndGroupsService.canSeeOnlinePleaFinances(queryEnvelope)).thenReturn(false);
        when(onlinePleaRepository.findOnlinePleaWithoutFinances(caseId)).thenReturn(onlinePlea);

        final List<OnlinePleaDetail> onlinePleaDetails = getOnlinePleaDetails(offenceId);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseId, defendantId)).thenReturn(onlinePleaDetails);

        final OffenceDetail offenceDetail = new OffenceDetail();
        offenceDetail.setCode(offenceCode);
        when(offenceRepository.findBy(offenceId)).thenReturn(offenceDetail);

        final JsonObject offenceData = Json.createObjectBuilder()
                .add("title", "Offence title").build();
        when(referenceDataService.getOffenceData(offenceCode)).thenReturn(of(offenceData));

        final JsonEnvelope response = sjpQueryView.findDefendantsOnlinePlea(queryEnvelope);

        verify(onlinePleaRepository, never()).findBy(any());
        verify(onlinePleaRepository).findOnlinePleaWithoutFinances(caseId);

        assertThat(response, jsonEnvelope(metadata().withName("sjp.query.defendants-online-plea"), payload().isJson(allOf(
                withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.onlinePleaDetails[0].plea", equalTo(GUILTY.name())),
                withJsonPath("$.onlinePleaDetails[0].mitigation", equalTo("mitigation")),
                withJsonPath("$.onlinePleaDetails[1].plea", equalTo(NOT_GUILTY.name())),
                withJsonPath("$.onlinePleaDetails[1].notGuiltyBecause", equalTo("Not Guilty Because"))
        ))));
    }

    @Test
    public void shouldFindDetailDetailUpdates() {

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendant-details-updates"))
                .build();

        LocalDate dateOfBirth = now().minusYears(30);

        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("firstName");
        personalDetails.setLastName("lastName");
        personalDetails.setDateOfBirth(dateOfBirth);
        personalDetails.markNameUpdated(ZonedDateTime.now());
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now());
        personalDetails.markAddressUpdated(ZonedDateTime.now());

        String updatedOn = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        DefendantDetailsUpdatesView.DefendantDetailsUpdate defendantDetailsUpdate = new DefendantDetailsUpdatesView.DefendantDetailsUpdate(
                "firstName",
                "lastName",
                "defendantId",
                "caseId",
                "caseUrn",
                dateOfBirth.format(DateTimeFormatter.ISO_LOCAL_DATE),
                true,
                true,
                true,
                updatedOn,
                "region");

        when(defendantService.findDefendantDetailUpdates(queryEnvelope)).thenReturn(new DefendantDetailsUpdatesView(
                1,
                newArrayList(defendantDetailsUpdate)));

        final JsonEnvelope response = sjpQueryView.findDefendantDetailUpdates(queryEnvelope);

        assertThat(response,
                jsonEnvelope(
                        metadata().withName("sjp.query.defendant-details-updates"),
                        payload().isJson(allOf(
                                withJsonPath("$.total", equalTo(1)),
                                withJsonPath("$.defendantDetailsUpdates[0].firstName", equalTo("firstName")),
                                withJsonPath("$.defendantDetailsUpdates[0].lastName", equalTo("lastName")),
                                withJsonPath("$.defendantDetailsUpdates[0].defendantId", equalTo("defendantId")),
                                withJsonPath("$.defendantDetailsUpdates[0].caseUrn", equalTo("caseUrn")),
                                withJsonPath("$.defendantDetailsUpdates[0].caseId", equalTo("caseId")),
                                withJsonPath("$.defendantDetailsUpdates[0].dateOfBirth", equalTo(dateOfBirth.format(DateTimeFormatter.ISO_LOCAL_DATE))),
                                withJsonPath("$.defendantDetailsUpdates[0].nameUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[0].dateOfBirthUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[0].addressUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[0].updatedOn", equalTo(updatedOn))
                        ))));
    }

    @Test
    public void shouldFindDefendantProfilingView() {
        UUID defendantId = randomUUID();
        Metadata metadata = metadataOf(
                randomUUID(), "sjp.query.defendant-profile"
        ).build();
        final JsonEnvelope query = envelopeFrom(metadata,
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .build());

        DefendantProfilingView defendantProfilingView = DefendantProfilingView.newBuilder()
                                                                .withFirstName("name")
                                                                .build();
        when(defendantService.getDefendantProfilingView(defendantId)).thenReturn(
                defendantProfilingView
        );

        final JsonEnvelope response = sjpQueryView.getDefendantProfile(query);

        assertThat(response,
                jsonEnvelope(
                        metadata().withName("sjp.query.defendant-profile"),
                        payload().isJson(allOf(
                                withJsonPath("$.firstName", equalTo("name")))
                        )));

    }

    @Test
    public void shouldGetNotGuiltyPleaCases() {
        final String prosecutingAuthority = "TFL";
        ZonedDateTime pleaDate = ZonedDateTime.parse("2018-03-20T18:14:29.894Z");

        final JsonObject casesJson = buildNotGuiltyPleaCases(pleaDate);

        when(caseService.buildNotGuiltyPleaCasesView(prosecutingAuthority, 1, 1)).thenReturn(casesJson);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.not-guilty-plea-cases"))
                .withPayloadOf(prosecutingAuthority, "prosecutingAuthority")
                .withPayloadOf(1, "pageSize")
                .withPayloadOf(1, "pageNumber")
                .build();

        final JsonEnvelope responseEnvelope = sjpQueryView.getNotGuiltyPleaCases(queryEnvelope);

        assertThat(responseEnvelope.metadata().name(), is("sjp.query.not-guilty-plea-cases"));

        assertThat(responseEnvelope.payloadAsJsonObject().toString(),
                isJson(Matchers.allOf(
                        withJsonPath("results", is(1)),
                        withJsonPath("pageCount", is(1)),
                        withJsonPath("cases[0].id", is(CASE_ID.toString())),
                        withJsonPath("cases[0].urn", is(URN)),
                        withJsonPath("cases[0].firstName", is("Hakan")),
                        withJsonPath("cases[0].lastName", is("Kurtulus")),
                        withJsonPath("cases[0].pleaDate", is(pleaDate.toString())),
                        withJsonPath("cases[0].prosecutingAuthority", is("Transport for London")),
                        withJsonPath("cases[0].caseManagementStatus", is(IN_PROGRESS.name())))));
    }
    @Test
    public void shouldGetCasesWithoutDefendantPostcode() {
        final LocalDate postingDate = LocalDate.parse("2018-03-20");

        final JsonObject casesJson = buildCasesWithoutPostcode(postingDate);
        final int pageSize = 20;
        final int pageNumber = 1;
        final int totalResults = 1;
        final int pageCount = 1;

        when(caseService.buildCasesWithoutDefendantPostcodeView(pageSize, pageNumber)).thenReturn(casesJson);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.cases-without-defendant-postcode"))
                .withPayloadOf(pageSize, "pageSize")
                .withPayloadOf(pageNumber, "pageNumber")
                .build();

        final JsonEnvelope responseEnvelope = sjpQueryView.getCasesWithoutDefendantPostcode(queryEnvelope);

        assertThat(responseEnvelope.metadata().name(), is("sjp.query.cases-without-defendant-postcode"));

        assertThat(responseEnvelope.payloadAsJsonObject().toString(),
                isJson(Matchers.allOf(
                        withJsonPath("results", is(totalResults)),
                        withJsonPath("pageCount", is(pageCount)),
                        withJsonPath("cases[0].id", is(CASE_ID.toString())),
                        withJsonPath("cases[0].urn", is(URN)),
                        withJsonPath("cases[0].firstName", is("Hakan")),
                        withJsonPath("cases[0].lastName", is("Kurtulus")),
                        withJsonPath("cases[0].postingDate", is(postingDate.toString())),
                        withJsonPath("cases[0].prosecutingAuthority", is("Transport for London"))
                ))
        );
    }

    @Test
    public void getOutstandingFineRequestsWithNoResults() {

        when(defendantService.getOutstandingFineRequests()).thenThrow(NoResultException.class);

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.outstanding-fine-requests"),
                createObjectBuilder()
                        .build());

        final JsonEnvelope result = sjpQueryView.getOutstandingFineRequests(query);

        Assert.assertThat(result.metadata().name(), is("sjp.query.outstanding-fine-requests"));
        assertTrue(result.payloadAsJsonObject().isEmpty());
    }

    @Test
    public void getOutstandingFineRequestsWithResults() {

        when(defendantService.getOutstandingFineRequests()).thenReturn(createDefendantRequestProfile());

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.outstanding-fine-requests"),
                createObjectBuilder()
                        .build());

        final JsonEnvelope result = sjpQueryView.getOutstandingFineRequests(query);
        Assert.assertThat(result.metadata().name(), is("sjp.query.outstanding-fine-requests"));
        assertTrue(result.payloadAsJsonObject().getJsonArray("defendantDetails").size() == 3);

    }

    private JsonObject buildNotGuiltyPleaCases(final ZonedDateTime pleaDate) {
        return createObjectBuilder()
                .add("results", 1)
                .add("pageCount", 1)
                .add("cases", createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("firstName", "Hakan")
                        .add("lastName", "Kurtulus")
                        .add("pleaDate", pleaDate.toString())
                        .add("prosecutingAuthority", "Transport for London")
                        .add("caseManagementStatus", IN_PROGRESS.name())))
                .build();
    }

    private JsonObject buildCasesWithoutPostcode(final LocalDate postingDate) {
        return createObjectBuilder()
                .add("results", 1)
                .add("pageCount", 1)
                .add("cases", createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("firstName", "Hakan")
                        .add("lastName", "Kurtulus")
                        .add("postingDate", postingDate.toString())
                        .add("prosecutingAuthority", "Transport for London")
                ))
                .build();
    }

    private OnlinePlea stubOnlinePlea(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        final OnlinePlea onlinePlea = new OnlinePlea(
                new PleaUpdated(caseId, offenceId, NOT_GUILTY,
                        null, "I was not there, they are lying", PleaMethod.ONLINE, clock.now())
        );
        onlinePlea.setDefendantId(defendantId);

        return onlinePlea;
    }

    private void setupCaseExpectations() {
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID.toString());
        setupExpectations();
    }

    private void setupExpectations() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
    }

    private DefendantOutstandingFineRequestsQueryResult createDefendantRequestProfile() {
        return new DefendantOutstandingFineRequestsQueryResult(
                Arrays.asList(
                        DefendantOutstandingFineRequest.newBuilder().withDefendantId(UUID.randomUUID()).withDateOfBirth("1980-06-25 00:00:00").withFirstName("Mr").withLastName("Brown").build(),
                        DefendantOutstandingFineRequest.newBuilder().withDefendantId(UUID.randomUUID()).withFirstName("Mrs").withLastName("Brown").withNationalInsuranceNumber("AB123456Z").build(),
                        DefendantOutstandingFineRequest.newBuilder().withDefendantId(UUID.randomUUID()).withLegalEntityDefendantName("ACME").build()
                )
        );
    }

    private List<OnlinePleaDetail> getOnlinePleaDetails(final UUID offenceId) {
        final List<OnlinePleaDetail> onlinePleaDetails = new ArrayList<>();
        final OnlinePleaDetail onlinePleaDetailGuilty = new OnlinePleaDetail();
        onlinePleaDetailGuilty.setOffenceId(offenceId);
        onlinePleaDetailGuilty.setPlea(GUILTY);
        onlinePleaDetailGuilty.setMitigation("mitigation");
        onlinePleaDetails.add(onlinePleaDetailGuilty);

        final OnlinePleaDetail onlinePleaDetailNotGuilty = new OnlinePleaDetail();
        onlinePleaDetailNotGuilty.setOffenceId(offenceId);
        onlinePleaDetailNotGuilty.setPlea(NOT_GUILTY);
        onlinePleaDetailNotGuilty.setNotGuiltyBecause("Not Guilty Because");
        onlinePleaDetails.add(onlinePleaDetailNotGuilty);

        return onlinePleaDetails;
    }

    private JsonEnvelope mockAndVerifyPendingDatesToAvoid(UUID... caseIds) {
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.pending-dates-to-avoid"))
                .build();
        final List<PendingDatesToAvoid> pendingDatesToAvoidList = Stream.of(caseIds)
                .map(CaseDetail::new)
                .map(PendingDatesToAvoid::new)
                .collect(toList());

        when(datesToAvoidService.findCasesPendingDatesToAvoid(queryEnvelope)).thenReturn(new CasesPendingDatesToAvoidView(pendingDatesToAvoidList));

        final JsonEnvelope response = sjpQueryView.findPendingDatesToAvoid(queryEnvelope);

        verify(datesToAvoidService).findCasesPendingDatesToAvoid(queryEnvelope);

        return response;
    }
}
