package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDocumentBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseForSocCheck;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseWithoutDefendantPostcode;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceSummary;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantDetailUpdateRequestRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.query.view.ExportType;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseNotGuiltyPleaView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseWithoutDefendantPostcodeView;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final CaseStatus CASE_STATUS_REFERRED_FOR_COURT_HEARING = CaseStatus.REFERRED_FOR_COURT_HEARING;
    private static final String URN = "TFL1234";
    private static final UUID CORRELATION_ID = randomUUID();
    private static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    private static final Boolean COMPLETED = Boolean.TRUE;
    private static final String ENTERPRISE_ID = "2K2SLYFC743H";
    private static final LocalDate POSTING_DATE = now(UTC);
    private static final String FIRST_NAME = "Adam";
    private static final String LAST_NAME = "Zuma";
    private static final String POSTCODE = "AB1 2CD";
    private static final String PROSECUTOR_TFL = "TFL";
    private final LocalDate DATE_OF_BIRTH = now(UTC).minusYears(30);
    private final Clock clock = new StoppedClock(new UtcClock().now());
    private final JsonArray PROSECUTORS = createArrayBuilder().add(
            createObjectBuilder()
                    .add("id", "31af405e-7b60-4dd8-a244-c24c2d3fa595")
                    .add("sequenceNumber", 1)
                    .add("majorCreditorCode", "TFL2")
                    .add("shortName", PROSECUTOR_TFL)
                    .add("fullName", "Transport for London")
                    .add("policeFlag", false)
                    .add("oucode", "GAFTL00")
                    .add("nameWelsh", "Transport for London")
                    .add("address", createObjectBuilder()
                            .add("address1", "6th Floor Windsor House")
                            .add("address2", "42-50 Victoria Street")
                            .add("postcode", "SW1H 0TL")
                    )
    ).build();

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private CaseDocumentRepository caseDocumentRepository;

    @Mock
    private CaseSearchResultRepository caseSearchResultRepository;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonEnvelope envelope;

    @InjectMocks
    private CaseService service;

    @Mock
    private ListToJsonArrayConverter<CaseNotGuiltyPleaView> notGuiltyCasesListToJsonArrayConverter;

    @Mock
    private ListToJsonArrayConverter<CaseWithoutDefendantPostcodeView> noPostcodeCaseListToJsonArrayConverter;

    @Mock
    private ReferenceDataCachingService referenceDataCachingService;

    @Mock
    private UserDetailsCacheService userDetailsCacheService;

    @Mock
    private ListToJsonArrayConverter<CaseForSocCheck> caseForSockCheckListToJsonArrayConverter;

    @Mock
    private DefendantDetailUpdateRequestRepository defendantDetailUpdateRequestRepository;

    @Test
    public void shouldFindCaseViewWithDocumentsWherePostalPlea() {
        assertExpectationsForFindCaseViewWithDocuments(false);
    }

    @Test
    public void shouldFindCaseViewWithDocumentsWhereOnlinePlea() {
        assertExpectationsForFindCaseViewWithDocuments(true);
    }

    private void assertExpectationsForFindCaseViewWithDocuments(final boolean onlinePleaReceived) {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes(onlinePleaReceived, "FINANCIAL_MEANS", "OTHER", "Travelcard");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCase(CASE_ID);
        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getCaseDocuments(), hasSize(3));
        assertThat(caseView.isOnlinePleaReceived(), is(onlinePleaReceived));
        assertThat(caseView.getStatus(), is(CASE_STATUS_REFERRED_FOR_COURT_HEARING));
        assertThat(caseView.getCcApplicationStatus(), is(ApplicationStatus.APPEAL_ALLOWED));
    }

    @Test
    public void shouldFindCaseViewWithFilteringOfOtherAndFinancialMeansDocuments() {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes("FINANCIAL_MEANS", "OTHER", "OTHER-Travelcard");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));

        final CaseView caseView = service.findCaseAndFilterOtherAndFinancialMeansDocuments(CASE_ID.toString());
        assertThat(caseView.getCaseDocuments().size(), is(0));
    }

    @Test
    public void shouldFindCaseViewWithFilteringWhichLeavesRequiredDocuments() {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes("PLEA", "CITN", "SJPN", "APPLICATION");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseAndFilterOtherAndFinancialMeansDocuments(CASE_ID.toString());
        assertThat(caseView.getCaseDocuments().size(), is(4));
    }

    @Test
    public void shouldFindCaseViewWithFilteringOfUnwantedDocuments() {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes("PLEA", "OTHER", "FINANCIAL_MEANS", "APPLICATION");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));

        final CaseView caseView = service.findCaseAndFilterOtherAndFinancialMeansDocuments(CASE_ID.toString());
        assertThat(caseView.getCaseDocuments().size(), is(2));
    }

    @Test
    public void shouldFindCaseByUrn() {
        final CaseDetail caseDetail = createCaseDetail(true);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getDateTimeCreated(), is(clock.now()));
        assertThat(caseView.getCosts(), nullValue());
        assertThat(caseView.isOnlinePleaReceived(), is(true));
    }

    @Test
    public void shouldFindCaseByCorrelationId() {
        final CaseDetail caseDetail = createCaseDetail(true);

        given(defendantRepository.findCaseIdByCorrelationId(CORRELATION_ID)).willReturn(CASE_ID);
        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByCorrelationId(CORRELATION_ID);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getDateTimeCreated(), is(clock.now()));
        assertThat(caseView.getCosts(), nullValue());
        assertThat(caseView.isOnlinePleaReceived(), is(true));
    }

    @Test
    public void shouldFindCaseByUrnAndContainsReopenedDateAndLibraCaseNumber() {
        final CaseDetail caseDetail = createCaseDetail(true);
        final LocalDate reopenedDate = now();
        final String reason = "REASON";
        caseDetail.setReopenedDate(reopenedDate);
        caseDetail.setLibraCaseNumber("LIBRA12345");
        caseDetail.setReopenedInLibraReason(reason);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getLibraCaseNumber(), is("LIBRA12345"));
        assertThat(caseView.getReopenedInLibraReason(), is(reason));
        assertThat(caseView.getReopenedDate(), is(reopenedDate));
        assertThat(caseView.getDateTimeCreated(), is(clock.now()));
        assertThat(caseView.getCosts(), nullValue());
        assertThat(caseView.isOnlinePleaReceived(), is(true));
    }

    @Test
    public void shouldFindCaseByUrnAndContainsSpeakWelsh() {
        final CaseDetail caseDetail = createCaseDetail(true);
        caseDetail.getDefendant().setSpeakWelsh(Boolean.TRUE);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getDefendant(), notNullValue());
        assertThat(caseView.getDefendant().getSpeakWelsh(), is(Boolean.TRUE));

    }

    @Test
    public void shouldFindCaseByUrnAndDoesNotContainSpeakWelsh() {
        final CaseDetail caseDetail = createCaseDetail(true);
        assertThat(caseDetail.getDefendant().getSpeakWelsh(), nullValue());

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getDefendant(), notNullValue());
        assertThat(caseView.getDefendant().getSpeakWelsh(), is(false));
    }

    @Test
    public void shouldFindCaseByUrnAndContainsSpeakWelshFalse() {
        final CaseDetail caseDetail = createCaseDetail(true);
        caseDetail.getDefendant().setSpeakWelsh(false);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getDefendant().getSpeakWelsh(), is(Boolean.FALSE));
    }


    @Test
    public void shouldHandleWhenNoCaseFoundForUrn() {
        given(caseRepository.findByUrn(URN)).willThrow(new NoResultException("boom"));
        assertThat(service.findCaseByUrn(URN), nullValue());
    }

    @Test
    public void shouldFindByUrnPostcode() {
        final CaseDetail caseDetail = createCaseDetail();

        given(caseRepository.findByUrnPostcode(URN, POSTCODE)).willReturn(caseDetail);
        given(referenceDataService.getProsecutorsByProsecutorCode(anyString())).willReturn(of(PROSECUTORS));
        final CaseView caseView = service.findCaseByUrnPostcode(URN, POSTCODE);

        assertThat(caseView.getId(), is(CASE_ID.toString()));
    }

    @Test
    public void shouldReturnNullWhenNoCaseFoundForUrnAndPostcode() {
        given(caseRepository.findByUrnPostcode(URN, POSTCODE)).willReturn(null);
        assertThat(service.findCaseByUrnPostcode(URN, POSTCODE), nullValue());
    }

    @Test
    public void shouldReturnNullWhenMultipleCaseFoundForUrnAndPostcode() {
        given(caseRepository.findByUrnPostcode(URN, POSTCODE)).willThrow(new NonUniqueResultException());
        assertThat(service.findCaseByUrnPostcode(URN, POSTCODE), nullValue());
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenTVL() {
        final UUID materialId = UUID.randomUUID();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority("TVL");

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is("TVL"));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenDVLA() {
        final UUID materialId = UUID.randomUUID();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority("DVLA");

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is("DVLA"));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenTFL() {
        final UUID materialId = UUID.randomUUID();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(PROSECUTOR_TFL);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), is(PROSECUTOR_TFL));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenCaseIdAndProsecutingAuthorityAreNull() {
        final UUID materialId = UUID.randomUUID();

        when(caseRepository.findByMaterialId(materialId)).thenReturn(null);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), nullValue());
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), nullValue());
    }


    @Test
    public void shouldReturnNullIfCaseNotFoundSearchCaseByMaterialId() {
        final UUID materialId = UUID.randomUUID();

        when(caseRepository.findByMaterialId(materialId)).thenThrow(new NoResultException());

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(nullValue()));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), is(nullValue()));
    }


    @Test
    public void shouldFindCaseDocuments() {
        final List<CaseDocument> caseDocumentList = new ArrayList<>();
        final UUID documentId = randomUUID();
        final CaseDocument caseDocument = new CaseDocument(documentId, randomUUID(), "SJPN", clock.now(), CASE_ID, 2);
        caseDocumentList.add(caseDocument);
        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(caseDocumentList);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocuments(CASE_ID);
        assertThat(caseDocumentsView, notNullValue());
        final CaseDocumentView firstCaseDocument = caseDocumentsView.getCaseDocuments().get(0);
        assertThat(firstCaseDocument.getId().toString(), is(documentId.toString()));
        assertThat(firstCaseDocument.getDocumentNumber(), is(2));
    }

    @Test
    public void shouldFindCaseDocument() {
        final CaseDocument caseDocument = new CaseDocument(randomUUID(), randomUUID(), "SJPN", clock.now(), CASE_ID, 2);

        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(singletonList(caseDocument));

        final Optional<CaseDocumentView> caseDocumentView = service.findCaseDocument(CASE_ID, caseDocument.getId());

        assertThat(caseDocumentView.isPresent(), is(true));
        assertThat(caseDocumentView.get().getId(), is(caseDocument.getId()));
        assertThat(caseDocumentView.get().getMaterialId(), is(caseDocument.getMaterialId()));
        assertThat(caseDocumentView.get().getDocumentType(), is(caseDocument.getDocumentType()));
        assertThat(caseDocumentView.get().getDocumentNumber(), is(caseDocument.getDocumentNumber()));
    }

    @Test
    public void shouldReturnEmptyCaseDocumentIfDocumentDoesNotExist() {
        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(emptyList());

        final Optional<CaseDocumentView> caseDocumentView = service.findCaseDocument(CASE_ID, randomUUID());

        assertThat(caseDocumentView.isPresent(), is(false));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringOfOtherAndFinancialMeansDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("FINANCIAL_MEANS", "OTHER", "OTHER-Travelcard");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID);
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(0));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringWhichLeavesRequiredDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("PLEA", "CITN", "SJPN");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID);
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(3));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringOfUnwantedDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("PLEA", "OTHER", "FINANCIAL_MEANS");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID);
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(1));
    }

    @Test
    public void shouldFindPublicTransparencyReportPendingCases() {
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID caseId3 = randomUUID();
        final UUID caseId4 = randomUUID();
        final UUID caseId5 = randomUUID();
        final UUID caseId6 = randomUUID();
        final PendingCaseToPublishPerOffence pendingCaseToPublishWith5LinesOfAddressOffence1 = new PendingCaseToPublishPerOffence("Mr","John", "Doe", null, LocalDate.of(1980, 6, 20),
                caseId1, "TVL1", "address line 1", "address line 2", "Lant Street", "London", "Greater London",
                "SE1 1PJ", "CA03014", LocalDate.of(2018, 12, 15), "offence wording", false, null, false, "TVL","offence welsh wording");

        final PendingCaseToPublishPerOffence pendingCaseToPublishWith5LinesOfAddressOffence2 = new PendingCaseToPublishPerOffence("Mr","John", "Doe", null, LocalDate.of(1980, 6, 20),
                caseId1, "TVL1", "address line 1", "address line 2", "Lant Street", "London", "Greater London",
                "SE1 1PJ", "CA03014", LocalDate.of(2018, 11, 11), "offence wording", false, null, false, "TVL","offence welsh wording");

        final PendingCaseToPublishPerOffence pendingCaseToPublishWith4LinesOfAddressAndWithoutFirstName = new PendingCaseToPublishPerOffence("Mr","", "Doe", null, null,
                caseId2, "TVL2", "address line 1", "address line 2", "London", "Greater London", "",
                "S", "CA03014", LocalDate.of(2018, 8, 2), "offence wording", true, "Person 1", false, "TVL","offence welsh wording");

        final PendingCaseToPublishPerOffence pendingCaseToPublishWithoutAddress3and4and5 = new PendingCaseToPublishPerOffence("Ms","Emma", "White", null, LocalDate.of(1980, 6, 20),
                caseId3, "TVL3", "address line 1", "address line 2", null, null, null,
                "CR0 2GE", "CA03011", LocalDate.of(2018, 8, 2), "offence wording", false, null, false, "TVL","offence welsh wording");

        final PendingCaseToPublishPerOffence pendingCaseToPublishWithSingleLetterFirstName = new PendingCaseToPublishPerOffence("Mr","X", "Doe", null, null,
                caseId4, "TVL2", "address line 1", "address line 2", "London", "Greater London", "",
                "S", "CA03014", LocalDate.of(2018, 8, 2), "offence wording", false, null, false, "TVL","offence welsh wording");

        final PendingCaseToPublishPerOffence pendingCaseToPublishWithSingleLetterFirstNameNoLastNameAndNullPostcode = new PendingCaseToPublishPerOffence("Mr","X", null, null, null,
                caseId5, "TVL2", "address line 1", "address line 2", "London", "Greater London", "",
                null, "CA03014", LocalDate.of(2018, 8, 2), "offence wording", false, null, false, "TVL","offence welsh wording");
        final PendingCaseToPublishPerOffence pendingCaseToPublishWithLegalEntityNameNoFirstNameNoLastNameAndNullPostcode = new PendingCaseToPublishPerOffence(null,null, null, "Ministry Of Justice", null,
                caseId6, "TVL2", "address line 1", "address line 2", "London", "Greater London", "",
                null, "CA03014", LocalDate.of(2018, 8, 2), "offence wording", false, null, false, "TVL","offence welsh wording");

        when(caseRepository.findPublicTransparencyReportPendingCases()).thenReturn(newArrayList(
                pendingCaseToPublishWith5LinesOfAddressOffence1,
                pendingCaseToPublishWith5LinesOfAddressOffence2,
                pendingCaseToPublishWith4LinesOfAddressAndWithoutFirstName,
                pendingCaseToPublishWithoutAddress3and4and5,
                pendingCaseToPublishWithSingleLetterFirstName,
                pendingCaseToPublishWithSingleLetterFirstNameNoLastNameAndNullPostcode,
                pendingCaseToPublishWithLegalEntityNameNoFirstNameNoLastNameAndNullPostcode
        ));

        final JsonObject pendingCasesToPublish = service.findPendingCasesToPublish(ExportType.PUBLIC);

        final List<JsonObject> pendingCaseToPublish = pendingCasesToPublish.getJsonArray("pendingCases")
                .getValuesAs(JsonObject.class);

        containsAndAssertPendingCaseToPublish(
                pendingCaseToPublish,
                caseId1,
                "J Doe",
                "London",
                "Greater London",
                "SE1 1PJ",
                "TVL",
                newArrayList(
                        Pair.of("CA03014", "2018-11-11"),
                        Pair.of("CA03014", "2018-12-15")));

        containsAndAssertPendingCaseToPublish(
                pendingCaseToPublish,
                caseId2,
                "Doe",
                "London",
                "Greater London",
                "S",
                "TVL",
                newArrayList(
                        Pair.of("CA03014", "2018-08-02")));

        containsAndAssertPendingCaseToPublish(
                pendingCaseToPublish,
                caseId3,
                "E White",
                null,
                null,
                "CR0 2GE",
                "TVL",
                newArrayList(
                        Pair.of("CA03011", "2018-08-02")));


        containsAndAssertPendingCaseToPublish(
                pendingCaseToPublish,
                caseId4,
                "X Doe",
                "London",
                "Greater London",
                "S",
                "TVL",
                newArrayList(
                        Pair.of("CA03014", "2018-08-02")));

        containsAndAssertPendingCaseToPublish(
                pendingCaseToPublish,
                caseId5,
                "X",
                "London",
                "Greater London",
                null,
                "TVL",
                newArrayList(
                        Pair.of("CA03014", "2018-08-02")));

        containsAndAssertPendingCaseToPublish(
                pendingCaseToPublish,
                caseId6,
                "Ministry Of Justice",
                "London",
                "Greater London",
                null,
                "TVL",
                newArrayList(
                        Pair.of("CA03014", "2018-08-02")));
    }

    @Test
    public void shouldFindPressTransparencyReportPendingCases() {
        service.findPendingCasesToPublish(ExportType.PRESS);

        verify(caseRepository).findPressTransparencyReportPendingCases();
    }

    @Test
    public void shouldFindPressTransparencyDeltaReportPendingCases() {

        service.findPendingDeltaCasesToPublish(ExportType.PRESS);
        LocalDate fromDate = LocalDate.now().minusDays(1);
        if (now().getDayOfWeek() == DayOfWeek.MONDAY) {
            fromDate = fromDate.minusDays(2);
        }
        verify(caseRepository).findPressTransparencyDeltaReportPendingCases(fromDate, now());
    }

    @Test
    public void shouldFindPublicTransparencyDeltaReportPendingCases() {

        service.findPendingDeltaCasesToPublish(ExportType.PUBLIC);
        LocalDate fromDate = LocalDate.now().minusDays(1);
        if (now().getDayOfWeek() == DayOfWeek.MONDAY) {
            fromDate = fromDate.minusDays(2);
        }
        verify(caseRepository).findPublicTransparencyDeltaReportPendingCases(fromDate, now());
    }

    @Test
    public void shouldFindResultOrders() {
        //given
        final LocalDate FROM_DATE = LocalDates.from("2017-01-01");
        final LocalDate TO_DATE = LocalDates.from("2017-01-10");
        final Address address = new Address();
        address.setAddress1("address1");
        address.setAddress2("address2");
        address.setPostcode("postcode");
        final DefendantDetail defendantDetail = new DefendantDetail();
        defendantDetail.setAddress(address);

        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "TFL1234", "2K2SLYFC743H", null, null, null, null, defendantDetail, null, null, null);

        final CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                clock.now(), caseDetail.getId(), null);

        final ZonedDateTime FROM_DATE_TIME = FROM_DATE.atStartOfDay(UTC);
        final ZonedDateTime TO_DATE_TIME = TO_DATE.atStartOfDay(UTC);
        when(caseDocumentRepository.findCaseDocumentsOrderedByAddedByDescending(FROM_DATE_TIME,
                TO_DATE_TIME, CaseDocument.RESULT_ORDER_DOCUMENT_TYPE)).thenReturn(singletonList(caseDocument));
        when(caseRepository.findBy(caseDetail.getId())).thenReturn(caseDetail);
        //when
        final ResultOrdersView resultOrdersView = service.findResultOrders(FROM_DATE, TO_DATE);
        //then
        assertEquals(caseDocument.getCaseId(),
                resultOrdersView.getResultOrders().get(0).getCaseId());
        assertEquals(caseDetail.getUrn(),
                resultOrdersView.getResultOrders().get(0).getUrn());
        assertEquals(caseDocument.getId(),
                resultOrdersView.getResultOrders().get(0).getOrder().getDocumentId());
        assertEquals(caseDocument.getAddedAt(),
                resultOrdersView.getResultOrders().get(0).getOrder().getAddedAt());
    }

    @Test
    public void shouldNotFindResultOrders() {
        //given
        final LocalDate FROM_DATE = LocalDates.from("2017-01-01");
        final LocalDate TO_DATE = LocalDates.from("2017-01-10");

        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "TFL1234", "2K2SLYFC743H", null, null, null,
                null, new DefendantDetail(), null, null, null);
        final CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                clock.now(), caseDetail.getId(), null);

        final ZonedDateTime FROM_DATE_TIME = FROM_DATE.atStartOfDay(UTC);
        final ZonedDateTime TO_DATE_TIME = TO_DATE.atStartOfDay(UTC);
        when(caseDocumentRepository.findCaseDocumentsOrderedByAddedByDescending(FROM_DATE_TIME,
                TO_DATE_TIME, CaseDocument.RESULT_ORDER_DOCUMENT_TYPE)).thenReturn(
                singletonList(caseDocument));
        when(caseRepository.findBy(caseDetail.getId())).thenReturn(null);
        //when
        final ResultOrdersView resultOrdersView = service.findResultOrders(FROM_DATE, TO_DATE);
        //then
        assertEquals(0, resultOrdersView.getResultOrders().size());
    }

    @Test
    public void shouldSearchCasesByUrn() {
        final String query = URN;
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = mock(ProsecutingAuthorityAccess.class);
        final String prosecutingAuthorityAccessFilterValue = "SOME_FILTER";

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope))
                .thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess))
                .thenReturn(prosecutingAuthorityAccessFilterValue);
        when(caseSearchResultRepository.findByUrn(prosecutingAuthorityAccessFilterValue, query))
                .thenReturn(singletonList(createCaseSearchResult()));

        final CaseSearchResultsView cases = service.searchCases(envelope, query);

        assertThat(cases.getResults().get(0).getUrn(), equalTo(URN));

        verify(prosecutingAuthorityProvider).getCurrentUsersProsecutingAuthorityAccess(envelope);
    }

    @Test
    public void shouldSearchCasesByLastName() {
        final String query = LAST_NAME;
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = mock(ProsecutingAuthorityAccess.class);
        final String prosecutingAuthorityAccessFilterValue = "SOME_FILTER";

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope))
                .thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess))
                .thenReturn(prosecutingAuthorityAccessFilterValue);

        when(caseSearchResultRepository.findByUrn(prosecutingAuthorityAccessFilterValue, query))
                .thenReturn(emptyList());
        when(caseSearchResultRepository.findByLastName(prosecutingAuthorityAccessFilterValue, query))
                .thenReturn(singletonList(createCaseSearchResult()));

        final CaseSearchResultsView cases = service.searchCases(envelope, query);

        verify(prosecutingAuthorityProvider).getCurrentUsersProsecutingAuthorityAccess(envelope);

        final CaseSearchResultsView.CaseSearchResultView result = cases.getResults().get(0);
        assertThat(result.getCaseId(), equalTo(CASE_ID));
        assertThat(result.getUrn(), equalTo(URN));
        assertThat(result.getEnterpriseId(), equalTo(ENTERPRISE_ID));
        assertThat(result.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY));
        assertThat(result.getPostingDate(), equalTo(POSTING_DATE));
        assertThat(result.getStatus(), equalTo(CASE_STATUS_REFERRED_FOR_COURT_HEARING));
        assertThat(result.getDefendant().getFirstName(), equalTo(FIRST_NAME));
        assertThat(result.getDefendant().getLastName(), equalTo(LAST_NAME));
        assertThat(result.getDefendant().getDateOfBirth(), equalTo(DATE_OF_BIRTH));
    }

    @Test
    public void shouldReturnCaseDetailsWhenCaseExists() {
        final CaseDetail caseDetail = createCaseDetail();
        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);

        assertThat(service.getCase(CASE_ID).get(), equalTo(caseDetail));
    }


    @Test
    public void shouldReturnEmptyCaseWhenCaseDoesNotExist() {
        when(caseRepository.findBy(CASE_ID)).thenReturn(null);

        assertThat(service.getCase(CASE_ID).isPresent(), is(false));
    }

    @Test
    public void shouldFindNotGuiltyPleaCases() {
        final ZonedDateTime pleaDate = ZonedDateTime.parse("2018-03-20T18:14:29.894Z");
        final CaseNotGuiltyPlea caseNotGuiltyPlea = new CaseNotGuiltyPlea(CASE_ID, URN, pleaDate, "Hakan", "Kurtulus", null, "TVL", IN_PROGRESS);

        final List<CaseNotGuiltyPlea> caseList = singletonList(caseNotGuiltyPlea);
        final JsonArray prosecutors = createArrayBuilder()
                .add(createObjectBuilder().add("sequenceNumber", 1).add("shortName", "TFL").add("fullName", "Transport for London"))
                .add(createObjectBuilder().add("sequenceNumber", 2).add("shortName", "TVL").add("fullName", "TV License"))
                .build();

        when(referenceDataService.getAllProsecutors()).thenReturn(of(prosecutors));
        when(caseRepository.findCasesNotGuiltyPlea()).thenReturn(caseList);

        final JsonArray buildNotGuiltyPleaCases = buildNotGuiltyPleaCases(pleaDate);
        when(notGuiltyCasesListToJsonArrayConverter.convert(any())).thenReturn(buildNotGuiltyPleaCases);

        final JsonObject result = service.buildNotGuiltyPleaCasesView("", 1, 1);

        assertThat(result.getInt("results"), is(1));
        assertThat(result.getInt("pageCount"), is(1));
        assertThat(result.getJsonArray("cases").size(), is(1));
        assertThat(result.getJsonArray("cases").getJsonObject(0).getString("firstName"), is("Hakan"));
        assertThat(result.getJsonArray("cases").getJsonObject(0).getString("lastName"), is("Kurtulus"));
    }

    @Test
    public void shouldFindNotGuiltyPleaCasesWhenDefendantIsCompany() {
        final ZonedDateTime pleaDate = ZonedDateTime.parse("2018-03-20T18:14:29.894Z");
        final CaseNotGuiltyPlea caseNotGuiltyPlea = new CaseNotGuiltyPlea(CASE_ID, URN, pleaDate, null, null, "Samba LTD", "TVL", IN_PROGRESS);

        final List<CaseNotGuiltyPlea> caseList = singletonList(caseNotGuiltyPlea);
        final JsonArray prosecutors = createArrayBuilder()
                .add(createObjectBuilder().add("sequenceNumber", 1).add("shortName", "TFL").add("fullName", "Transport for London"))
                .add(createObjectBuilder().add("sequenceNumber", 2).add("shortName", "TVL").add("fullName", "TV License"))
                .build();

        when(referenceDataService.getAllProsecutors()).thenReturn(of(prosecutors));
        when(caseRepository.findCasesNotGuiltyPlea()).thenReturn(caseList);

        final JsonArray buildNotGuiltyPleaCases = buildNotGuiltyPleaCasesWhenDefendantIsCompany(pleaDate);
        when(notGuiltyCasesListToJsonArrayConverter.convert(any())).thenReturn(buildNotGuiltyPleaCases);

        final JsonObject result = service.buildNotGuiltyPleaCasesView("", 1, 1);

        assertThat(result.getInt("results"), is(1));
        assertThat(result.getInt("pageCount"), is(1));
        assertThat(result.getJsonArray("cases").size(), is(1));
        assertThat(result.getJsonArray("cases").getJsonObject(0).getString("legalEntityName"), is("Samba LTD"));
    }

    @Test
    public void shouldFindCasesWithoutDefendantPostcode() {
        final LocalDate postingDate = LocalDate.parse("2018-03-20");
        final CaseWithoutDefendantPostcode withoutDefendantPostcode = new CaseWithoutDefendantPostcode(CASE_ID, URN, postingDate, "Hakan", "Kurtulus", "TVL", null);

        final List<CaseWithoutDefendantPostcode> caseList = singletonList(withoutDefendantPostcode);
        final JsonArray prosecutors = createArrayBuilder()
                .add(createObjectBuilder().add("sequenceNumber", 1).add("shortName", "TFL").add("fullName", "Transport for London"))
                .add(createObjectBuilder().add("sequenceNumber", 2).add("shortName", "TVL").add("fullName", "TV License"))
                .build();

        when(referenceDataService.getAllProsecutors()).thenReturn(of(prosecutors));
        when(caseRepository.findCasesWithoutDefendantPostcode()).thenReturn(caseList);

        final JsonArray buildNoPostcodeCases = buildWithoutPostcodeCases(postingDate);
        when(noPostcodeCaseListToJsonArrayConverter.convert(any())).thenReturn(buildNoPostcodeCases);

        final JsonObject result = service.buildCasesWithoutDefendantPostcodeView(1, 1);

        assertThat(result.getInt("results"), is(1));
        assertThat(result.getInt("pageCount"), is(1));
        assertThat(result.getJsonArray("cases").size(), is(1));
    }

    @Test
    public void shouldBuildCasesForSOCView() {
        final String loggedInUserId = "f7da3cea-fcba-44e5-b589-3415dbf8340b";
        final String ljaCode = "2905";
        final String courtHouseCode = "B23HS00";
        final LocalDate fromDate = LocalDate.now().minusYears(1);
        final LocalDate toDate = LocalDate.now();
        final String columnSortedOn = "lastupdatedDate";
        final String sortOrder = "asc";
        when(referenceDataCachingService.getAllProsecutors()).thenReturn(buildProsecutorsMap());
        when(userDetailsCacheService.getUserName(any(), any())).thenReturn("TEST USER");
        when(caseRepository.findCasesForSOCCheck(anyString(), anyString(), anyString(), any(), any(),
                anyString(), anyString())).thenReturn(getCasesForSOCCheck());
        when(caseForSockCheckListToJsonArrayConverter.convert(any())).thenReturn(buildCasesForSOCCheck());
        final JsonObject result = service.buildCasesForSOCCheckView(loggedInUserId, courtHouseCode, ljaCode, fromDate, toDate,
                100, 20, 1, columnSortedOn, sortOrder, null);
        assertThat(result.getJsonArray("cases").size(), is(2));
        assertThat(result.getInt("numberOfPages"), is(1));
        assertThat(result.getInt("pageNumber"), is(1));
        assertThat(result.getInt("totalCount"), is(2));
        assertThat(result.getInt("pageSize"), is(20));
    }

    @Test
    public void shouldBuildCasesForSOCViewInValidPageNumber() {
        final String loggedInUserId = "f7da3cea-fcba-44e5-b589-3415dbf8340b";
        final String ljaCode = "2905";
        final String courtHouseCode = "B23HS00";
        final LocalDate fromDate = LocalDate.now().minusYears(1);
        final LocalDate toDate = LocalDate.now();
        final String columnSortedOn = "lastupdatedDate";
        final String sortOrder = "asc";
        when(referenceDataCachingService.getAllProsecutors()).thenReturn(buildProsecutorsMap());
        when(userDetailsCacheService.getUserName(any(), any())).thenReturn("TEST USER");
        when(caseRepository.findCasesForSOCCheck(anyString(), anyString(), anyString(), any(), any(),
                anyString(), anyString())).thenReturn(getCasesForSOCCheck());
        when(caseForSockCheckListToJsonArrayConverter.convert(any())).thenReturn(buildCasesForSOCCheck());
        final JsonObject result = service.buildCasesForSOCCheckView(loggedInUserId, courtHouseCode, ljaCode, fromDate, toDate,
                100, 20, 10, columnSortedOn, sortOrder, null);
        assertThat(result.getJsonArray("cases").size(), is(2));
        assertThat(result.getInt("numberOfPages"), is(1));
        assertThat(result.getInt("pageNumber"), is(10));
        assertThat(result.getInt("totalCount"), is(2));
        assertThat(result.getInt("pageSize"), is(20));
    }

    private List<Object[]> getCasesForSOCCheck() {
        final List<Object[]> results = new ArrayList<>();
        final Object[] result1 = {"7f2909c4-bd7c-44f5-9850-772c4a115656", "TVL4535369459",
                Timestamp.valueOf(LocalDateTime.now().minusYears(2)),
                "TFL", "31ec3a16-8721-498c-8da5-f099390ee254", "31ec3a16-8721-498c-8da5-f099390ee254"};
        final Object[] result2 = {"7f2909c4-bd7c-44f5-9850-772c4a115656", "TVL4535369459",
                Timestamp.valueOf(LocalDateTime.now().minusYears(2)),
                "DVLA", "31ec3a16-8721-498c-8da5-f099390ee254", "31ec3a16-8721-498c-8da5-f099390ee254"};
        results.add(result1);
        results.add(result2);
        return results;
    }

    private Map<String, String> buildProsecutorsMap() {
        final Map<String, String> prosecutorsMap = new HashMap<>();
        prosecutorsMap.put("TFL", "Transport for London");
        prosecutorsMap.put("DVLA", "Driver and Vehicle Licensing Agency");
        return prosecutorsMap;
    }

    private JsonArray buildCasesForSOCCheck() {
        return createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("lastUpdatedDate", Timestamp.valueOf(LocalDateTime.now()).toString())
                        .add("magistrate", "Manav")
                        .add("legalAdvisor", "legalAdvisor")
                        .add("prosecutingAuthority", "Transport for London"))
                .add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("lastUpdatedDate", Timestamp.valueOf(LocalDateTime.now()).toString())
                        .add("magistrate", "Manav")
                        .add("legalAdvisor", "legalAdvisor")
                        .add("prosecutingAuthority", "Driver and Vehicle Licensing Agency"))
                .build();
    }

    private JsonArray buildNotGuiltyPleaCases(final ZonedDateTime pleaDate) {
        return createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("firstName", "Hakan")
                        .add("lastName", "Kurtulus")
                        .add("pleaDate", pleaDate.toString())
                        .add("prosecutingAuthority", "Transport for London")
                        .add("caseManagementStatus", IN_PROGRESS.name()))
                .build();
    }

    private JsonArray buildNotGuiltyPleaCasesWhenDefendantIsCompany(final ZonedDateTime pleaDate) {
        return createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("legalEntityName", "Samba LTD")
                        .add("pleaDate", pleaDate.toString())
                        .add("prosecutingAuthority", "Transport for London")
                        .add("caseManagementStatus", IN_PROGRESS.name()))
                .build();
    }


    private JsonArray buildWithoutPostcodeCases(final LocalDate postingDate) {
        return createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("firstName", "Hakan")
                        .add("lastName", "Kurtulus")
                        .add("postingDate", postingDate.toString())
                        .add("prosecutingAuthority", "Transport for London"))
                .build();
    }

    private CaseDetail createCaseDetail() {
        return createCaseDetail(false);
    }

    private CaseDetail createCaseDetail(final boolean onlinePleaReceived) {
        final CaseDetail caseDetail = new CaseDetail(CASE_ID, URN, ENTERPRISE_ID, "CPS", COMPLETED,
                null, clock.now(), DefendantDetailBuilder.aDefendantDetail().build(), null, now().minusDays(5), null);
        caseDetail.setOnlinePleaReceived(onlinePleaReceived);
        caseDetail.setCaseStatus(CASE_STATUS_REFERRED_FOR_COURT_HEARING);
        caseDetail.setCcApplicationStatus(ApplicationStatus.APPEAL_ALLOWED);
        return caseDetail;
    }

    private CaseDetail createCaseDetailWithDocumentTypes(final String... documentTypes) {
        return createCaseDetailWithDocumentTypes(false, documentTypes);
    }

    private CaseDetail createCaseDetailWithDocumentTypes(final boolean onlinePlea, final String... documentTypes) {
        final CaseDetail caseDetail = createCaseDetail(onlinePlea);
        createCaseDocuments(documentTypes).forEach(caseDetail::addCaseDocuments);
        return caseDetail;
    }

    private List<CaseDocument> createCaseDocuments(final String... documentTypes) {
        return Arrays.stream(documentTypes)
                .map(this::createCaseDocument)
                .collect(Collectors.toList());
    }

    private CaseDocument createCaseDocument(final String documentType) {
        return CaseDocumentBuilder.aCaseDocument().withDocumentType(documentType).build();
    }

    private CaseSearchResult createCaseSearchResult() {
        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        final CaseSummary caseSummary = new CaseSummary();
        caseSummary.setId(CASE_ID);
        caseSummary.setUrn(URN);
        caseSummary.setEnterpriseId(ENTERPRISE_ID);
        caseSummary.setProsecutingAuthority(PROSECUTING_AUTHORITY);
        caseSummary.setPostingDate(POSTING_DATE);
        caseSummary.setCaseStatus(CASE_STATUS_REFERRED_FOR_COURT_HEARING);
        caseSearchResult.setId(randomUUID());
        caseSearchResult.setCaseId(CASE_ID);
        caseSearchResult.setCaseSummary(caseSummary);
        caseSearchResult.setCurrentFirstName(FIRST_NAME);
        caseSearchResult.setCurrentLastName(LAST_NAME);
        caseSearchResult.setDateOfBirth(DATE_OF_BIRTH);
        final Set<OffenceSummary> offenceSummary = new HashSet<>();
        offenceSummary.add(new OffenceSummary());
        caseSearchResult.setOffenceSummary(offenceSummary);
        // not resulted
        return caseSearchResult;
    }

    private void containsAndAssertPendingCaseToPublish(final List<JsonObject> pendingCases,
                                                       final UUID caseId,
                                                       final String name,
                                                       final String town,
                                                       final String county,
                                                       final String postcode,
                                                       final String prosecutor,
                                                       final List<Pair<String, String>> offencesData) {
        final JsonObject pendingCaseToPublish = pendingCases.stream()
                .filter(pendingCase -> caseId.toString().equals(pendingCase.getString("caseId")))
                .findAny()
                .orElse(null);

        if (Objects.isNull(pendingCaseToPublish)) {
            fail(format("Cannot find case with %s", caseId.toString()));
        }

        assertThat(pendingCaseToPublish.getString("caseId"), is(caseId.toString()));
        assertThat(pendingCaseToPublish.getString("town", null), is(town));
        assertThat(pendingCaseToPublish.getString("county", null), is(county));
        assertThat(pendingCaseToPublish.getString("postcode", null), is(postcode));
        assertThat(pendingCaseToPublish.getString("prosecutorName"), is(prosecutor));
        final List<JsonObject> offenceForPendingCaseToPublish
                = pendingCaseToPublish.getJsonArray("offences").getValuesAs(JsonObject.class);

        assertThat(offenceForPendingCaseToPublish.size(), is(offencesData.size()));
        offencesData.forEach(offence -> assertContainsOffence(offenceForPendingCaseToPublish, offence.getLeft(), offence.getRight()));
    }

    private void assertContainsOffence(final List<JsonObject> offences, final String code, final String startDate) {
        final Optional<JsonObject> offence = offences.stream().filter(e -> e.getString("offenceCode").equals(code)
                && e.getString("offenceStartDate").equals(startDate)).findAny();

        if (!offence.isPresent()) {
            fail(format("%s does not contain offence with code %s and start date %s", offences.toString(), code, startDate));
        }
    }

}
