package uk.gov.moj.cpp.sjp.query.view.service.defendantcase;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.CASE_STATUS_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.DEFENDANTS_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.OFFENCES_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.PROSECUTION_CASE_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType.COURT_CASE_OPEN;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType.SJP_OPEN;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.ADDRESS_LINE1_QUERY_PARAM;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_DEFAULT_PAGE_SIZE;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_PAGE_SIZE;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_QUERY_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_START_FROM;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.DOB_QUERY_PARAM;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.PARTY_NAME_QUERY_PARAM;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.POSTCODE_QUERY_PARAM;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.additionalproperties.AdditionalPropertiesModule;
import uk.gov.justice.services.common.converter.jackson.jsr353.InclusionAwareJSR353Module;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;
import uk.gov.moj.cpp.sjp.query.view.service.DefendantService;
import uk.gov.moj.cpp.sjp.query.view.service.ProgressionService;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleResult;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleUtils;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQueryResult;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.UnifiedDefendantCaseSearcher;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantPotentialCaseServiceTest {

    public static final UUID CASE_ID = UUID.randomUUID();
    public static final String CASE_REF = "Case-Ref-1";
    public static final int DATE_OFFSET_BEYOND_28_DAYS = 32;
    public static final int DATE_OFFSET_WITHIN_28_DAYS = 10;

    @Mock
    private Requester requester;

    @Mock
    private Envelope<?> envelope;

    @Mock
    private Envelope potentialCaseSearchResponse;

    @Mock
    private CaseDecisionRepository caseDecisionRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private ReadyCaseRepository readyCaseRepository;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private DefendantService defendantService;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> unifiedSearchQueryParamCapture;

    @InjectMocks
    private UnifiedDefendantCaseSearcher defendantCaseSearcher;

    @InjectMocks
    private DefendantPotentialCaseServiceImpl defendantCaseService;

    private final DefendantDetail defendant = createDefendant(UUID.randomUUID(),
            "John",
            "Smith",
            "1980-01-05",
            "2 Abc Street", "EC1 1NN", "ASN-1");

    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule(PROPERTIES))
                .registerModule(new JavaTimeModule())
                .registerModule(new InclusionAwareJSR353Module())
                .registerModule(new AdditionalPropertiesModule());
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
        setField(this.defendantCaseService, "defendantCaseSearcher", defendantCaseSearcher);

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(CASES_QUERY_NAME);
        when(envelope.metadata()).thenReturn(metadataBuilder.build());
        when(defendantService.findDefendantDetailById(defendant.getId())).thenReturn(defendant);

        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setPostingDate(LocalDate.now().minusDays(10));
        caseDetail.setDefendant(defendant);
        caseDetail.setProsecutingAuthority("TFL");
        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);

        JsonObject progCaseSearchResult = createProgressionCaseSearchJsonObject(CASE_ID, "ACTIVE");
        when(progressionService.findCaseById(CASE_ID)).thenReturn(Optional.of(progCaseSearchResult));
        when(progressionService.findDefendantOffences(CASE_ID, defendant)).thenReturn(
                defendant.getOffences().
                        stream().
                        map(OffenceDetail::getWording).
                        collect(Collectors.toList()));
    }

    @Test
    public void shouldDefendantQueryDetailsSubmittedToUnifiedSearch() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                CaseStatus.NO_PLEA_RECEIVED.toString(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);
        defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        final JsonObject queryParamPayload = unifiedSearchQueryParamCapture.getAllValues().get(0).payload();
        assertEquals(CASES_DEFAULT_PAGE_SIZE, queryParamPayload.getInt(CASES_PAGE_SIZE));
        assertEquals(0, queryParamPayload.getInt(CASES_START_FROM));
        final PersonalDetails personalDetails = defendant.getPersonalDetails();
        assertEquals(queryParamPayload.getString(PARTY_NAME_QUERY_PARAM), String.join(" ", personalDetails.getFirstName(), personalDetails.getLastName()));
        assertEquals(queryParamPayload.getString(DOB_QUERY_PARAM), personalDetails.getDateOfBirth().toString());
        assertEquals(queryParamPayload.getString(ADDRESS_LINE1_QUERY_PARAM), defendant.getAddress().getAddress1());
        assertEquals(queryParamPayload.getString(POSTCODE_QUERY_PARAM), defendant.getAddress().getPostcode());
    }

    @Test
    public void shouldDefendantHasPotentialCase_WhenCaseSjpOpen() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                CaseStatus.NO_PLEA_RECEIVED.toString(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        ReadyCase readyCase = new ReadyCase();
        readyCase.setMarkedAt(LocalDate.now());
        when(readyCaseRepository.findBy(any())).thenReturn(readyCase);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);
        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertTrue(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertEquals(true, potentialCaseResult.isMatch());
        });


        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getSjpOpenCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getSjpOpenCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertThat(caseOffenceDetails.getProsecutorName(), is("TFL"));
        assertThat(caseOffenceDetails.getExpiryDate(), is(""));
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasPotentialCase_WhenCaseSjpClosedWithin28Days() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                CaseStatus.COMPLETED.toString(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setCaseId(CASE_ID);
        ZonedDateTime weekAgo = ZonedDateTime.now().minusDays(7);
        caseDecision.setSavedAt(weekAgo);

        ReadyCase readyCase = new ReadyCase();
        readyCase.setMarkedAt(LocalDate.now());
        when(readyCaseRepository.findBy(any())).thenReturn(readyCase);

        when(caseDecisionRepository.findCaseDecisionById(CASE_ID)).thenReturn(caseDecision);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertTrue(hasDefendantPotentialCase);


        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertEquals(true, potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getSjpClosedCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getSjpClosedCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertThat(caseOffenceDetails.getProsecutorName(), is("TFL"));
        assertThat(caseOffenceDetails.getExpiryDate(), is(""));
        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasNoPotentialCase_WhenSjpClosedBeyond28Days() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                CaseStatus.COMPLETED.toString(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);

        CaseDecision caseDecision = new CaseDecision();
        caseDecision.setCaseId(CASE_ID);
        ZonedDateTime twoMonthsAgo = ZonedDateTime.now().minusMonths(2);
        caseDecision.setSavedAt(twoMonthsAgo);
        when(caseDecisionRepository.findCaseDecisionById(CASE_ID)).thenReturn(caseDecision);
        when(progressionService.findCaseById(CASE_ID)).thenReturn(Optional.empty());
        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertFalse(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertEquals(CaseRuleType.NONE, potentialCaseResult.getRuleType());
            assertEquals(false, potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasPotentialCase_WhenCourtCaseOpen() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                false,
                "ACTIVE",
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);
        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertFalse(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertTrue(potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getCourtOpenCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getCourtOpenCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasPotentialCase_WhenSjpReferredAndCourtCaseOpen() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                REFERRED_FOR_COURT_HEARING.name(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertFalse(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertTrue(potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getCourtOpenCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getCourtOpenCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasPotentialCase_WhenCourtCaseClosedWithin28Days() {
        JsonObject sjpOpenCase = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                false,
                CaseRuleUtils.CC_INACTIVE_STATUS,
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(sjpOpenCase, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertFalse(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertTrue(potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getCourtClosedCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getCourtClosedCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasPotentialCase_WhenSjpReferredAndCourtCaseClosedWithin28Days() {
        JsonObject sjpOpenCase = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                REFERRED_FOR_COURT_HEARING.name(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(sjpOpenCase, DefendantCaseQueryResult.class);

        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        JsonObject progCaseSearchResult = createProgressionCaseSearchJsonObject(CASE_ID, "INACTIVE");
        when(progressionService.findCaseById(CASE_ID)).thenReturn(Optional.of(progCaseSearchResult));
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertFalse(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertTrue(potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getCourtClosedCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getCourtClosedCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasNoPotentialCase_WhenSjpReferredAndCourtCaseClosedBeyond28Days() {
        JsonObject sjpOpenCase = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                REFERRED_FOR_COURT_HEARING.name(),
                defendant,
                DATE_OFFSET_BEYOND_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(sjpOpenCase, DefendantCaseQueryResult.class);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        JsonObject progCaseSearchResult = createProgressionCaseSearchJsonObject(CASE_ID, "INACTIVE");
        when(progressionService.findCaseById(CASE_ID)).thenReturn(Optional.of(progCaseSearchResult));
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertFalse(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertEquals(CaseRuleType.NONE, potentialCaseResult.getRuleType());
            assertFalse(potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertTrue(potentialCases.getSjpOpenCases().isEmpty());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    @Test
    public void shouldDefendantHasMultiplePotentialCasesAndSorted() {
        final List<String> allCases = new LinkedList<>();
        final UUID sjoOpenCaseId = randomUUID();
        final String sjpOpenCase =
                createAndInterpolateSingleCase(sjoOpenCaseId,
                        CASE_REF + "-" + SJP_OPEN,
                        true,
                        CaseStatus.NO_PLEA_RECEIVED.toString(),
                        defendant,
                        5);
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setPostingDate(LocalDate.now().minusDays(10));
        caseDetail.setDefendant(defendant);
        when(caseRepository.findBy(sjoOpenCaseId)).thenReturn(caseDetail);
        allCases.add(sjpOpenCase);

        final UUID sjoOpenCaseId2 = randomUUID();
        final String sjpOpenCase2 =
                createAndInterpolateSingleCase(sjoOpenCaseId2,
                        CASE_REF + "-" + SJP_OPEN,
                        true,
                        CaseStatus.APPEALED.toString(),
                        defendant,
                        8);
        final CaseDetail caseDetail2 = new CaseDetail();
        caseDetail2.setPostingDate(LocalDate.now().minusDays(9));
        caseDetail2.setDefendant(defendant);
        when(caseRepository.findBy(sjoOpenCaseId2)).thenReturn(caseDetail2);
        allCases.add(sjpOpenCase2);

        final UUID sjoOpenCaseId3 = randomUUID();
        final String sjpOpenCase3 =
                createAndInterpolateSingleCase(sjoOpenCaseId3,
                        CASE_REF + "-" + SJP_OPEN,
                        true,
                        CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.toString(),
                        defendant,
                        12);
        final CaseDetail caseDetail3 = new CaseDetail();
        caseDetail3.setPostingDate(LocalDate.now().minusDays(7));
        caseDetail3.setDefendant(defendant);
        when(caseRepository.findBy(sjoOpenCaseId3)).thenReturn(caseDetail3);
        allCases.add(sjpOpenCase3);

        final UUID courtCaseOpenId = randomUUID();
        final String courCaseOpen =
                createAndInterpolateSingleCase(courtCaseOpenId,
                        CASE_REF + "-" + COURT_CASE_OPEN,
                        false,
                        "ACTIVE",
                        defendant,
                        15);
        allCases.add(courCaseOpen);
        JsonObject progCaseSearchResult = createProgressionCaseSearchJsonObject(courtCaseOpenId, "ACTIVE");
        when(progressionService.findCaseById(courtCaseOpenId)).thenReturn(Optional.of(progCaseSearchResult));
        when(progressionService.findDefendantOffences(courtCaseOpenId, defendant)).thenReturn(
                defendant.getOffences().
                        stream().
                        map(OffenceDetail::getWording).
                        collect(Collectors.toList()));

        final UUID courtCaseOpenId2 = randomUUID();
        final String courCaseOpen2 =
                createAndInterpolateSingleCase(courtCaseOpenId2,
                        CASE_REF + "-" + COURT_CASE_OPEN,
                        false,
                        "ACTIVE",
                        defendant,
                        5);
        allCases.add(courCaseOpen2);
        JsonObject progCaseSearchResult2 = createProgressionCaseSearchJsonObject(courtCaseOpenId2, "ACTIVE");
        when(progressionService.findCaseById(courtCaseOpenId2)).thenReturn(Optional.of(progCaseSearchResult2));
        when(progressionService.findDefendantOffences(courtCaseOpenId2, defendant)).thenReturn(
                defendant.getOffences().
                        stream().
                        map(OffenceDetail::getWording).
                        collect(Collectors.toList()));

        final String[] casesArray = new String[allCases.size()];
        allCases.toArray(casesArray);
        final JsonObject multiCaseSearchResult = createMultiCaseUnifiedSearchResult(5, casesArray);

        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(multiCaseSearchResult, DefendantCaseQueryResult.class);

        ReadyCase readyCase = new ReadyCase();
        readyCase.setMarkedAt(LocalDate.now());
        when(readyCaseRepository.findBy(any())).thenReturn(readyCase);

        when(potentialCaseSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);
        when(defendantCaseService.findProgressionCaseById(any())).thenReturn(Optional.of(createObjectBuilder()
                .add("defendants", createArrayBuilder().add(createObjectBuilder()))
                .build()));

        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertTrue(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertTrue(potentialCaseResult.isMatch());
        });

        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(3, potentialCases.getSjpOpenCases().size());
        assertEquals(2, potentialCases.getCourtOpenCases().size());
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());

        CaseOffenceDetails[] sjpOpenCaseOffenceDetails =
                new CaseOffenceDetails[potentialCases.getSjpOpenCases().size()];
        potentialCases.getSjpOpenCases().toArray(sjpOpenCaseOffenceDetails);
        assertCaseOffenceDetails(sjoOpenCaseId, CASE_REF + "-" + SJP_OPEN, 10, sjpOpenCaseOffenceDetails[0]);
        assertCaseOffenceDetails(sjoOpenCaseId2, CASE_REF + "-" + SJP_OPEN, 9, sjpOpenCaseOffenceDetails[1]);
        assertCaseOffenceDetails(sjoOpenCaseId3, CASE_REF + "-" + SJP_OPEN, 7, sjpOpenCaseOffenceDetails[2]);

        CaseOffenceDetails[] courtCaseOpenCaseOffenceDetails =
                new CaseOffenceDetails[potentialCases.getCourtOpenCases().size()];
        potentialCases.getCourtOpenCases().toArray(courtCaseOpenCaseOffenceDetails);
        assertCaseOffenceDetails(courtCaseOpenId2, CASE_REF + "-" + COURT_CASE_OPEN, 5, courtCaseOpenCaseOffenceDetails[0]);
        assertCaseOffenceDetails(courtCaseOpenId, CASE_REF + "-" + COURT_CASE_OPEN, 15, courtCaseOpenCaseOffenceDetails[1]);
    }

    @Test
    public void shouldHaveExpiryDate_WhenSJPOpenCaseIsNotReady() {
        JsonObject searchResult = createSingleCaseUnifiedSearchResult(CASE_ID,
                CASE_REF,
                true,
                CaseStatus.NO_PLEA_RECEIVED.toString(),
                defendant,
                DATE_OFFSET_WITHIN_28_DAYS);
        DefendantCaseQueryResult result = jsonObjectConverter.
                convert(searchResult, DefendantCaseQueryResult.class);
        ReadyCase readyCase = null;
        when(readyCaseRepository.findBy(any())).thenReturn(readyCase);
        when(potentialCaseSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(),
                eq(DefendantCaseQueryResult.class))).thenReturn(potentialCaseSearchResponse);
        boolean hasDefendantPotentialCase = defendantCaseService.hasDefendantPotentialCase(envelope, defendant.getId());
        assertTrue(hasDefendantPotentialCase);

        List<CaseRuleResult> potentialCaseResults = defendantCaseService.findDefendantCaseMatchingRule(envelope, defendant);
        potentialCaseResults.forEach(potentialCaseResult -> {
            assertEquals(true, potentialCaseResult.isMatch());
        });


        final PotentialCases potentialCases = defendantCaseService.findDefendantPotentialCases(envelope, defendant.getId());
        assertEquals(1, potentialCases.getSjpOpenCases().size());
        final CaseOffenceDetails caseOffenceDetails = potentialCases.getSjpOpenCases().get(0);
        assertCaseOffenceDetails(CASE_ID, CASE_REF, DATE_OFFSET_WITHIN_28_DAYS, caseOffenceDetails);

        assertThat(caseOffenceDetails.getProsecutorName(), is("TFL"));
        assertThat(caseOffenceDetails.getExpiryDate(), not(isEmptyString()));
        assertTrue(potentialCases.getSjpClosedCases().isEmpty());
        assertTrue(potentialCases.getCourtOpenCases().isEmpty());
        assertTrue(potentialCases.getCourtClosedCases().isEmpty());
    }

    private void assertCaseOffenceDetails(UUID caseId,
                                          String caseRef,
                                          int dateOffset, CaseOffenceDetails caseOffenceDetails) {
        assertEquals(caseId, caseOffenceDetails.getCaseId());
        assertEquals(caseRef, caseOffenceDetails.getCaseRef());
        assertEquals(LocalDate.now().minusDays(dateOffset), caseOffenceDetails.getPostingOrHearingDate());
        final List<String> offenceList = defendant.getOffences().
                stream().
                map(OffenceDetail::getWording).
                collect(Collectors.toList());
        assertEquals(offenceList, caseOffenceDetails.getOffenceTitles());
    }

    private JsonObject createProgressionCaseSearchJsonObject(UUID caseId, String status) {
        return createObjectBuilder().
                add(PROSECUTION_CASE_FIELD_NAME,
                        createObjectBuilder().
                                add("id", caseId.toString()).
                                add(CASE_STATUS_FIELD_NAME, status).
                                add(DEFENDANTS_FIELD_NAME,
                                        createArrayBuilder().
                                                add(createObjectBuilder().
                                                        add(OFFENCES_FIELD_NAME,
                                                                createArrayBuilder().
                                                                        add(createObjectBuilder().
                                                                                add("wording", "Offence-1").
                                                                                build()).
                                                                        add(createObjectBuilder().
                                                                                add("wording", "Offence-2").
                                                                                build()).
                                                                        build()).
                                                        build()).
                                                build()).
                                build()).
                build();
    }

    private DefendantDetail createDefendant(final UUID id,
                                            String firstName,
                                            String lastName,
                                            String dob,
                                            String addressLine1, String postCode, String asn) {
        final DefendantDetail defendantDetail = new DefendantDetail(id);
        defendantDetail.setAsn(asn);

        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName(firstName);
        personalDetails.setLastName(lastName);
        personalDetails.setDateOfBirth(LocalDate.parse(dob));
        defendantDetail.setAddress(new Address(addressLine1, "", "", "", "", postCode));
        defendantDetail.setPersonalDetails(personalDetails);

        final List<OffenceDetail> offences = new LinkedList<>();
        final OffenceDetail offenceDetail = new OffenceDetail();
        int offenceSeqNo = 1;
        offenceDetail.setSequenceNumber(offenceSeqNo++);
        offenceDetail.setWording("Offence-1");
        offences.add(offenceDetail);
        final OffenceDetail offenceDetail2 = new OffenceDetail();
        offenceDetail2.setWording("Offence-2");
        offences.add(offenceDetail2);
        offenceDetail2.setSequenceNumber(offenceSeqNo);
        defendantDetail.setOffences(offences);

        return defendantDetail;
    }

    private JsonObject createSingleCaseUnifiedSearchResult(UUID caseId,
                                                           String caseRef,
                                                           boolean sjp,
                                                           String status,
                                                           DefendantDetail defendant,
                                                           int dateOffset) {
        String searchResult = "{ \n" +
                "  \"totalResults\": 1, \n" +
                "  \"cases\": [\n" +
                createAndInterpolateSingleCase(caseId, caseRef, sjp, status, defendant, dateOffset) +
                "  ]\n" +
                "}\n";

        return stringToJsonObjectConverter.convert(searchResult);
    }

    private JsonObject createMultiCaseUnifiedSearchResult(int numOfCases, String... cases) {
        final String multiCases = Arrays.stream(cases).collect(Collectors.joining(","));
        String searchResult = "{ \n" +
                "  \"totalResults\": " + numOfCases + ", \n" +
                "  \"cases\": [\n" +
                multiCases +
                "  ]\n" +
                "}\n";

        return stringToJsonObjectConverter.convert(searchResult);
    }

    private String createAndInterpolateSingleCase(UUID caseId,
                                                  String caseRef,
                                                  boolean sjp,
                                                  String status,
                                                  DefendantDetail defendant, int dateOffset) {
        LocalDate today = LocalDate.now();
        List<String> hearingDates = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            hearingDates.add(today.minusDays(dateOffset++).toString());
        }
        final String hearingDateStr = hearingDates.stream().collect(Collectors.joining("\",\n\""));
        String caseResult = "    {\n" +
                "      \"caseId\": \"%s\",\n" +
                "      \"caseReference\": \"%s\",\n" +
                "      \"sjp\": %s,\n" +
                "      \"crownCourt\": true,\n" +
                "      \"magistrateCourt\": true,\n" +
                "      \"sjpNoticeServed\": \"2019-05-07\",\n" +
                "      \"caseStatus\": \"%s\",\n" +
                "      \"caseType\": \"PROSECUTION\",\n" +
                "      \"parties\": [\n" +
                "        {\n" +
                "          \"firstName\": \"%s\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"%s\",\n" +
                "          \"partyType\": \"APPLICANT\",\n" +
                "          \"organisationName\": \"Some Org Name\",\n" +
                "          \"dateOfBirth\": \"%s\",\n" +
                "          \"addressLines\": \"%s\",\n" +
                "          \"postCode\": \"%s\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"hearings\": [\n" +
                "        {\n" +
                "          \"hearingId\": \"70baba30-f45c-4c1e-971e-4581fc2ec30b\",\n" +
                "          \"courtId\": \"b58bdc19-2fb8-4841-bff3-b99f3442c646\",\n" +
                "          \"courtCentreName\": \"Liverpool Magistrates Court\",\n" +
                "          \"hearingTypeId\": \"79bdc7ce-b00b-4d91-b74c-f41aee572a1c\",\n" +
                "          \"hearingTypeLabel\": \"HEARING TYPE LABEL 1\",\n" +
                "          \"hearingDates\": [\n" +
                "            \"%s\"\n" +
                "          ],\n" +
                "          \"isBoxHearing\": true,\n" +
                "          \"isVirtualBoxHearing\": true,\n" +
                "          \"hearingDay\": [\n" +
                "            {\n" +
                "              \"sittingDay\": \"2019-01-22T10:00:00Z\",\n" +
                "              \"listingSequence\": 1,\n" +
                "              \"listedDurationMinutes\": 60\n" +
                "            }\n" +
                "          ],\n" +
                "          \"jurisdictionType\": \"4e2bddef-9797-424b-980c-95c467b84e86\",\n" +
                "          \"judiciaryTypes\": []\n" +
                "        }\n" +
                "      ],\n" +
                "      \"applications\": [\n" +
                "        {\n" +
                "          \"applicationId\": \"ab746921-d839-4867-bcf9-b41db8ebc853\",\n" +
                "          \"applicationReference\": \"CJ03510\",\n" +
                "          \"applicationType\": \"Application within criminal proceedings\",\n" +
                "          \"receivedDate\": \"2019-01-01\",\n" +
                "          \"decisionDate\": \"2019-04-07\",\n" +
                "          \"dueDate\": \"2019-04-08\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n";
        PersonalDetails personalDetails = defendant.getPersonalDetails();
        caseResult = String.format(caseResult,
                caseId,
                caseRef,
                sjp,
                status,
                personalDetails.getFirstName(),
                personalDetails.getLastName(),
                personalDetails.getDateOfBirth(),
                defendant.getAddress().getAddress1(),
                defendant.getAddress().getPostcode(),
                hearingDateStr);

        return caseResult;
    }
}
