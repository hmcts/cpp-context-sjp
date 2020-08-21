package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.LocalTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.streamToJsonObject;
import org.junit.Before;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportRequestedProcessorTest {

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @InjectMocks
    private PressTransparencyReportRequestedProcessor processor;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private Sender sender;

    @Mock
    private SjpService sjpService;

    @Captor
    private ArgumentCaptor<InputStream> payloadForDocumentGenerationCaptor;

    @Captor
    private ArgumentCaptor<Envelope> documentGenerationRequestCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> storePressTransparencyReportCommandEnvelopeCaptor;

    private static final String EXPECTED_TEMPLATE_NAME = "PressPendingCasesEnglish";

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MM yyyy");
    private static final String OFFENCE_TITLE = "OffenceTitle";
    private static final String PROSECUTOR_NAME = "TFL";
    private static final String PROSECUTOR = "Transport For London";
    private static final UUID REPORT_ID = randomUUID();
    private static final UUID PAYLOAD_DOCUMENT_GENERATOR_FILE_ID = randomUUID();
    private static final int NUMBER_OF_PENDING_CASES_FOR_EXPORT = 9;
    private static final List<UUID> CASE_IDS = range(0, NUMBER_OF_PENDING_CASES_FOR_EXPORT)
            .mapToObj(e -> randomUUID()).collect(toList());
    private static final Optional<JsonObject> CA_03011_REFERENCE_DATA_PAYLOAD = Optional.of(createObjectBuilder()
            .add("title", OFFENCE_TITLE)
            .build());
    private static final JsonEnvelope PRIVATE_EVENT_ENVELOPE = envelopeFrom(
            metadataWithRandomUUID("sjp.events.press-transparency-report-requested"),
            createObjectBuilder()
                    .add("pressTransparencyReportId", REPORT_ID.toString())
                    .build()
    );
    private static final JsonObject EXPECTED_DOC_GENERATION_PAYLOAD = createObjectBuilder()
            .add("originatingSource", "sjp")
            .add("templateIdentifier", EXPECTED_TEMPLATE_NAME)
            .add("conversionFormat", "pdf")
            .add("sourceCorrelationId", REPORT_ID.toString())
            .add("payloadFileServiceId", PAYLOAD_DOCUMENT_GENERATOR_FILE_ID.toString())
            .build();

    @Before
    public void setUp() throws Exception {
        mockReferenceData();
        mockFileStore();
    }


    @Test
    public void shouldCreatePressTransparencyReport() throws FileServiceException {

        final String defendantDateOfBirth = "1980-06-12";

        final List<JsonObject> pendingCasesList = pendingCasesList(CASE_IDS, false, defendantDateOfBirth);

        mockSjpService(pendingCasesList);

        processor.handlePressTransparencyRequest(PRIVATE_EVENT_ENVELOPE);

        final JsonObject actual = getDocumentGeneratorPayloadFromFileStorer();

        actual.getJsonArray("readyCases").forEach(rc ->
                assertThat(rc.toString(), isJson(withJsonPath("$.dateOfBirth", equalTo("1980-06-12 (%s)".format(getAge(defendantDateOfBirth)))))));

        assertPayloadForDocumentGenerator(actual, pendingCasesList, NUMBER_OF_PENDING_CASES_FOR_EXPORT);

        verify(referenceDataService).getProsecutor(eq(PROSECUTOR_NAME), eq(false), any());


        verify(sender).sendAsAdmin(documentGenerationRequestCaptor.capture());

        assertDocumentGenerationRequest(documentGenerationRequestCaptor.getValue(), EXPECTED_DOC_GENERATION_PAYLOAD);

        verify(sender).send(storePressTransparencyReportCommandEnvelopeCaptor.capture());
        assertPressTransparencyReportEnvelope(storePressTransparencyReportCommandEnvelopeCaptor.getValue(), REPORT_ID, CASE_IDS);
    }

    private void mockFileStore() throws FileServiceException {
        when(fileStorer.store(any(), any())).thenReturn(PAYLOAD_DOCUMENT_GENERATOR_FILE_ID);
    }

    private void mockSjpService(List<JsonObject> pendingCasesList) {
        when(sjpService.getPendingCases(any(), any())).thenReturn(pendingCasesList);
    }

    private void mockReferenceData() {
        when(referenceDataService.getProsecutor(eq(PROSECUTOR_NAME), eq(false), any())).thenReturn(PROSECUTOR);
        when(referenceDataOffencesService.getOffenceReferenceData(any(), anyString(), anyString())).thenReturn(CA_03011_REFERENCE_DATA_PAYLOAD);
    }

    private JsonObject getDocumentGeneratorPayloadFromFileStorer() throws FileServiceException {
        verify(fileStorer).store(any(JsonObject.class), payloadForDocumentGenerationCaptor.capture());

        final InputStream payloadBytes = payloadForDocumentGenerationCaptor.getValue();
        return streamToJsonObject(payloadBytes);
    }

    @Test
    public void shouldCreatePressTransparencyWhenNoDateOfBirthReport() throws FileServiceException {

        final String defendantDateOfBirth = "";

        final List<JsonObject> pendingCasesList = pendingCasesList(CASE_IDS, false, defendantDateOfBirth);

        mockSjpService(pendingCasesList);

        processor.handlePressTransparencyRequest(PRIVATE_EVENT_ENVELOPE);

        final JsonObject actual = getDocumentGeneratorPayloadFromFileStorer();

        assertThat(actual.containsKey("dateOfBirth"), is(false));


    }

    @Test
    public void shouldExcludeYouthDefendants() throws FileServiceException {

        final String defendantDateOfBirth = "1980-06-12";

        final List<JsonObject> pendingCasesList = pendingCasesList(CASE_IDS, true, defendantDateOfBirth);
        mockSjpService(pendingCasesList);

        processor.handlePressTransparencyRequest(PRIVATE_EVENT_ENVELOPE);

        verify(fileStorer).store(any(JsonObject.class), payloadForDocumentGenerationCaptor.capture());

        final InputStream payloadBytes = payloadForDocumentGenerationCaptor.getValue();
        final JsonObject payloadForDocumentGenerator = streamToJsonObject(payloadBytes);

        assertRootValuesOfPayloadForDocumentGenerator(payloadForDocumentGenerator, NUMBER_OF_PENDING_CASES_FOR_EXPORT / 2);
        verify(referenceDataService).getProsecutor(eq(PROSECUTOR_NAME), eq(false), any());

        verify(sender).sendAsAdmin(documentGenerationRequestCaptor.capture());
        assertDocumentGenerationRequest(documentGenerationRequestCaptor.getValue(), EXPECTED_DOC_GENERATION_PAYLOAD);

        verify(sender).send(storePressTransparencyReportCommandEnvelopeCaptor.capture());
        assertPressTransparencyReportEnvelope(storePressTransparencyReportCommandEnvelopeCaptor.getValue(), REPORT_ID, CASE_IDS);
    }


    private void assertPayloadForDocumentGenerator(final JsonObject payload, final List<JsonObject> pendingCasesList, final Integer totalNumberOfRecords) {
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesList, "caseUrn");
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesList, "adress");
        assertReadyCasesPayloadWithPropertyValue(payload, totalNumberOfRecords, "prosecutorName", "Transport For London");
        assertReadyCasesPayloadWithPropertyValue(payload, totalNumberOfRecords, "dateOfBirth", String.format("12 06 1980 (%d)",
                YEARS.between(LocalDate.of(1980,6,12), LocalDate.now()))
        );
        assertReadyCasesPayloadWithPropertyValueMatching(payload, "defendantName", "John DOE\\w*");
        assertPressRestrictionValues(payload, pendingCasesList);

        assertRootValuesOfPayloadForDocumentGenerator(payload, totalNumberOfRecords);
    }


    private String getAge(String defendantDateOfBirth) {
        final LocalDate defendantDob = parse(defendantDateOfBirth);
        final Optional<Long> defendantAge = Optional.of(YEARS.between(defendantDob, LocalDate.now()));
        return format("%s (%d)", DATE_FORMAT.format(defendantDob), defendantAge.get());
    }

    private void assertPressRestrictionValues(final JsonObject payload, final List<JsonObject> pendingCasesList) {

        final List<JsonObject> payloadOffences = getPropertyFromPayload(payload, "readyCases",
                payloadCase -> payloadCase.getJsonArray("offences").getValuesAs(JsonObject.class))
                .stream()
                .flatMap(Collection::stream)
                .collect(toList());

        final List<JsonObject> pendingCasesListOffences = getPropertyFromPendingCasesList(pendingCasesList,
                pendingCase -> pendingCase.getJsonArray("offences").getValuesAs(JsonObject.class))
                .stream()
                .flatMap(Collection::stream)
                .collect(toList());

        final List<Boolean> payloadPressRestrictionRequestedFlags = payloadOffences.stream().map(payloadOffence -> payloadOffence.getBoolean("pressRestrictionRequested")).collect(toList());
        final List<Boolean> pendingCasesPressRestrictionRequestedFlags = pendingCasesListOffences.stream().map(pendingCaseOffence -> pendingCaseOffence.getJsonObject("pressRestriction").getBoolean("requested")).collect(toList());
        assertThat(payloadPressRestrictionRequestedFlags, is(pendingCasesPressRestrictionRequestedFlags));

        final List<String> payloadPressRestrictionNames = payloadOffences.stream().map(payloadOffence -> payloadOffence.getString("pressRestrictionName", null)).collect(toList());
        final List<String> pendingCasesPressRestrictionNames = pendingCasesListOffences.stream().map(pendingCaseOffence -> pendingCaseOffence.getJsonObject("pressRestriction").getString("name", null)).collect(toList());
        assertThat(payloadPressRestrictionNames, is(pendingCasesPressRestrictionNames));
    }

    private void assertRootValuesOfPayloadForDocumentGenerator(final JsonObject payload, final Integer totalNumberOfRecords) {
        assertThat(payload.getJsonArray("readyCases").size(), is(totalNumberOfRecords));
        assertThat(payload.getInt("totalNumberOfRecords"), is(totalNumberOfRecords));
        assertThat(payload.getString("generatedDateAndTime"), is(notNullValue()));
    }

    private void assertReadyCasesPayloadWithPropertyValue(final JsonObject payload,
                                                          final int numberOfRecords,
                                                          final String jsonField,
                                                          final String expectedValue) {
        assertThat(getPropertyFromPayload(payload, "readyCases", jsonField),
                is(range(0, numberOfRecords).mapToObj(e -> expectedValue).collect(toList())));
    }

    private void assertReadyCasesPayloadWithPropertyValueMatching(final JsonObject payload,
                                                                  final String jsonField,
                                                                  final String expectedValuePattern) {
        assertTrue(getPropertyFromPayload(payload, "readyCases", jsonField)
                .stream()
                .allMatch(fieldValue -> fieldValue.matches(expectedValuePattern)));
    }

    private void assertReadyCasesPayloadWithPendingCases(final JsonObject payload, final List<JsonObject> pendingCasesList, final String field) {
        assertThat(getPropertyFromPayload(payload, "readyCases", field),
                is(getPropertyFromPendingCasesList(pendingCasesList, field)));
    }

    private List<String> getPropertyFromPayload(final JsonObject payload, final String containerArray, final String jsonField) {
        return getPropertyFromPayload(payload, containerArray, e -> e.getString(jsonField, null));
    }

    private <T> List<T> getPropertyFromPayload(final JsonObject payload, final String containerArray, final Function<JsonObject, T> queryFunction) {
        return payload.getJsonArray(containerArray)
                .getValuesAs(JsonObject.class).stream()
                .map(queryFunction::apply)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private <T> List<T> getPropertyFromPendingCasesList(final List<JsonObject> objects, final Function<JsonObject, T> queryFunction) {
        return objects.stream()
                .map(queryFunction::apply)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private List<String> getPropertyFromPendingCasesList(final List<JsonObject> objects, final String jsonField) {
        return getPropertyFromPendingCasesList(objects, e -> e.getString(jsonField, null));
    }

    private void assertPressTransparencyReportEnvelope(final JsonEnvelope storeTransparencyReportCommandEnvelope,
                                                       final UUID reportId,
                                                       final List<UUID> caseIds) {
        assertThat(storeTransparencyReportCommandEnvelope.metadata().name(), is("sjp.command.store-press-transparency-report-data"));
        assertThat(storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getString("pressTransparencyReportId"), is(reportId.toString()));

        final JsonArray caseIdsJsonArray = storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getJsonArray("caseIds");
        assertThat(caseIdsJsonArray.size(), is(caseIds.size()));
        assertThat(range(0, caseIds.size()).mapToObj(idx -> fromString(caseIdsJsonArray.getString(idx))).collect(toList()), is(caseIds));
    }

    private void assertDocumentGenerationRequest(final Envelope<JsonObject> documentGenerationEnvelope, final JsonObject expectedPayloadForDocumentGeneration) {
        assertThat(documentGenerationEnvelope.metadata().name(), is("systemdocgenerator.generate-document"));
        assertThat(documentGenerationEnvelope.payload(), is(expectedPayloadForDocumentGeneration));
    }

    private List<JsonObject> pendingCasesList(final List<UUID> caseIds, final boolean halfNotAdult, final String dateOfBirth) {
        final List<JsonObject> pendingCasesList = new LinkedList<>();

        for (int caseNumber = 0; caseNumber < caseIds.size(); caseNumber++) {
            boolean generateFullAddress = caseNumber % 2 == 0;

            final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder()
                    .add(createObjectBuilder()
                            .add("offenceCode", "CA03011")
                            .add("offenceStartDate", now().toString())
                            .add("offenceWording", "offence wording")
                            .add("completed", false)
                            .add("pressRestriction", createObjectBuilder().add("requested", false))
                    )
                    .add(createObjectBuilder()
                            .add("offenceCode", "CA03011")
                            .add("offenceStartDate", LocalDateTime.now().minusMonths(1).toLocalDate().toString())
                            .add("offenceWording", "offence wording")
                            .add("completed", false)
                            .add("pressRestriction", createObjectBuilder()
                                    .add("requested", true)
                                    .add("name", "Michelle Jenner")
                            )

                    );

            final String defendantDateOfBirth = (halfNotAdult && caseNumber % 2 == 0) ?
                    LocalDate.now().minusYears(15).format(DOB_FORMAT) : dateOfBirth;

            final JsonObjectBuilder pendingCase = createObjectBuilder()
                    .add("caseId", caseIds.get(caseNumber).toString())
                    .add("caseUrn", "TFL000" + caseNumber)
                    .add("defendantName", "J. Doe" + caseNumber)
                    .add("firstName", "John")
                    .add("lastName", "DOE" + caseNumber)
                    .add("defendantDateOfBirth", defendantDateOfBirth)
                    .add("addressLine1", "Flat 7 Hermit House")
                    .add("addressLine2", "3, Long Road")
                    .add("postcode", "SE1 1PJ" + caseNumber)
                    .add("offences", offenceArrayBuilder)
                    .add("prosecutorName", "TFL");

            if (generateFullAddress) {
                pendingCase
                        .add("town", "London" + caseNumber)
                        .add("county", "Greater London" + caseNumber);
            }

            pendingCasesList.add(pendingCase.build());
        }

        return pendingCasesList;
    }

}