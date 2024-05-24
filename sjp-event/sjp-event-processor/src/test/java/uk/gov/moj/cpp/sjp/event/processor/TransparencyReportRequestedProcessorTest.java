package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.streamToJsonObject;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.sjp.domain.DocumentFormat;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
public class TransparencyReportRequestedProcessorTest {

    private static final String PUBLIC_EVENT_SJP_PENDING_CASES_PUBLIC_LIST_GENERATED = "public.sjp.pending-cases-public-list-generated";

    @InjectMocks
    private TransparencyReportRequestedProcessor processor;

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
    private ArgumentCaptor<InputStream> payloadForFileServiceCaptor;

    @Captor
    private ArgumentCaptor<Envelope> payloadForDocumentGenerationCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> storeTransparencyReportCommandEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> sjpPendingListPublicEnvelopeCaptor;


    @Test
    public void shouldCreateTransparencyReport() throws FileServiceException {
        final String expectedEnglishTemplateName = "PendingCasesEnglish";
        final String expectedWelshTemplateName = "PendingCasesWelsh";

        final UUID englishPayloadFileUUID = randomUUID();
        final UUID welshPayloadFileUUID = randomUUID();
        final UUID transparencyReportId = randomUUID();

        final String offenceTitle = "OffenceTitle";
        final String prosecutorName = "TFL";
        final String prosecutorEnglish = "Transport For London";
        final String prosecutorWelsh = "Transport For London - Welsh";
        final Integer numberOfPendingCasesForExport = 9;
        final List<UUID> caseIds = range(0, numberOfPendingCasesForExport)
                .mapToObj(e -> randomUUID()).collect(toList());

        final Optional<JsonObject> CA03011_referenceDataPayload = Optional.of(createObjectBuilder()
                .add("title", offenceTitle)
                .build());

        // create 5 young offenders
        final List<UUID> youngOffenderCaseIds = range(0, 5)
                .mapToObj(e -> randomUUID()).collect(toList());

        // create 5 press restricted case ids
        final List<UUID> pressRestrictionCaseIds = range(0, 5)
                .mapToObj(e -> randomUUID()).collect(toList());

        when(referenceDataService.getProsecutor(eq(prosecutorName), eq(false), any())).thenReturn(prosecutorEnglish);
        when(referenceDataService.getProsecutor(eq(prosecutorName), eq(true), any())).thenReturn(prosecutorWelsh);
        when(referenceDataOffencesService.getOffenceReferenceData(any(), anyString(), anyString())).thenReturn(CA03011_referenceDataPayload);

        final List<JsonObject> pendingCasesList = pendingCasesList(caseIds, youngOffenderCaseIds, pressRestrictionCaseIds);
        when(sjpService.getPendingCases(any(), any())).thenReturn(pendingCasesList);
        when(fileStorer.store(any(), any()))
                .thenReturn(englishPayloadFileUUID)
                .thenReturn(welshPayloadFileUUID);

        final JsonEnvelope privateEventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.transparency-report-requested"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .build()
        );
        processor.createTransparencyReport(privateEventEnvelope);

        verify(fileStorer, times(2)).store(any(JsonObject.class), payloadForFileServiceCaptor.capture());
        verify(sender, times(2)).sendAsAdmin(payloadForDocumentGenerationCaptor.capture());

        final JsonObject payloadForEnglishPdf = streamToJsonObject(payloadForFileServiceCaptor.getAllValues().get(0));
        final JsonObject payloadForWelshPdf = streamToJsonObject(payloadForFileServiceCaptor.getAllValues().get(1));

        assertPayloadForDocumentGenerator(payloadForEnglishPdf, pendingCasesList, numberOfPendingCasesForExport, false);
        assertPayloadForDocumentGenerator(payloadForWelshPdf, pendingCasesList, numberOfPendingCasesForExport, true);
        verify(referenceDataService).getProsecutor(eq(prosecutorName), eq(false), any());
        verify(referenceDataService).getProsecutor(eq(prosecutorName), eq(true), any());

        assertPayloadForDocumentGenerator(payloadForDocumentGenerationCaptor.getAllValues().get(0), expectedEnglishTemplateName,
                transparencyReportId.toString(), englishPayloadFileUUID.toString());
        assertPayloadForDocumentGenerator(payloadForDocumentGenerationCaptor.getAllValues().get(1), expectedWelshTemplateName,
                transparencyReportId.toString(), welshPayloadFileUUID.toString());

        verify(sender, times(3)).send(storeTransparencyReportCommandEnvelopeCaptor.capture());
        assertTransparencyReportEnvelope(storeTransparencyReportCommandEnvelopeCaptor.getAllValues().get(0), transparencyReportId, caseIds);
        assertSJPPendingPublicListEnvelope(storeTransparencyReportCommandEnvelopeCaptor.getAllValues().get(1), "ENGLISH", numberOfPendingCasesForExport, prosecutorEnglish);
        assertSJPPendingPublicListEnvelope(storeTransparencyReportCommandEnvelopeCaptor.getAllValues().get(2), "WELSH", numberOfPendingCasesForExport, prosecutorWelsh);
    }

    private void assertSJPPendingPublicListEnvelope(final Envelope jsonEnvelope, final String language, final Integer numberOfPendingCasesForExport, final String prosecutorName) {
        assertThat(jsonEnvelope.metadata().name(), is(PUBLIC_EVENT_SJP_PENDING_CASES_PUBLIC_LIST_GENERATED));
        final JsonObject eventPayload = (JsonObject) jsonEnvelope.payload();
        assertThat(language, is(eventPayload.getString("language")));
        final JsonObject payload = eventPayload.getJsonObject("listPayload");
        assertThat(payload.getInt("totalNumberOfRecords"), is(numberOfPendingCasesForExport));
        final JsonArray readyCases = payload.getJsonArray("readyCases");
        assertThat(numberOfPendingCasesForExport, is(readyCases.size()));
        readyCases.getValuesAs(JsonObject.class).forEach(jsonObject -> {
            assertThat(jsonObject.getString("prosecutorName"), is(prosecutorName));
            assertThat(jsonObject.getJsonArray("sjpOffences"), notNullValue());
            assertThat(jsonObject.getString("firstName"), not(isEmptyOrNullString()));
            assertThat(jsonObject.getString("lastName"), not(isEmptyOrNullString()));
        });
    }


    private void assertPayloadForDocumentGenerator(final JsonObject payload, final List<JsonObject> pendingCasesList, final Integer totalNumberOfRecords, final Boolean isWelsh) {
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesList, "postcode");

        assertThat(getPropertyFromPayload(payload, "readyCases", "prosecutorName"),
                is(range(0, totalNumberOfRecords).mapToObj(e -> isWelsh ? "Transport For London - Welsh" : "Transport For London").collect(toList())));

        assertThat(payload.getJsonArray("readyCases").size(), is(totalNumberOfRecords));
        assertThat(payload.getInt("totalNumberOfRecords"), is(totalNumberOfRecords));
        assertThat(payload.getString("generatedDateAndTime"), is(notNullValue()));
    }

    private void assertReadyCasesPayloadWithPendingCases(final JsonObject payload, final List<JsonObject> pendingCasesList, final String field) {
        //  Check that the payload is contained in pendingCasesList
        assertThat(getPropertyFromPayload(payload, "readyCases", field),
                everyItem(isIn(getPropertyFromlist(pendingCasesList, field))));
    }

    private List<String> getPropertyFromPayload(final JsonObject payload, final String containerArray, final String jsonField) {
        return payload.getJsonArray(containerArray)
                .getValuesAs(JsonObject.class).stream()
                .map(e -> e.getString(jsonField, null))
                .collect(toList());
    }

    private List<String> getPropertyFromlist(final List<JsonObject> objects, final String jsonField) {
        return objects.stream()
                .map(e -> e.getString(jsonField, null))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private void assertTransparencyReportEnvelope(final JsonEnvelope storeTransparencyReportCommandEnvelope,
                                                  final UUID transparencyReportId,
                                                  final List<UUID> caseIds) {
        assertThat(storeTransparencyReportCommandEnvelope.metadata().name(), is("sjp.command.store-transparency-report-data"));
        final JsonObject payload = storeTransparencyReportCommandEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("transparencyReportId"), is(transparencyReportId.toString()));
        final JsonArray caseIdsJsonArray = payload.getJsonArray("caseIds");
        assertThat(caseIdsJsonArray.size(), is(caseIds.size()));
        assertThat(range(0, caseIds.size()).mapToObj(idx -> fromString(caseIdsJsonArray.getString(idx))).collect(toList()), is(caseIds));
    }

    private void assertPayloadForDocumentGenerator(final Envelope<JsonObject> envelopeForDocumentGenerator, final String templateName,
                                                   final String transparencyReportId, final String payloadFileServiceId) {
        final Metadata metadata = envelopeForDocumentGenerator.metadata();
        final JsonObject payload = envelopeForDocumentGenerator.payload();
        assertThat(metadata.name(), is("systemdocgenerator.generate-document"));
        assertThat(payload.getString("originatingSource"), is("sjp"));
        assertThat(payload.getString("templateIdentifier"), is(templateName));
        assertThat(payload.getString("conversionFormat"), is("pdf"));
        assertThat(payload.getString("sourceCorrelationId"), is(transparencyReportId));
        assertThat(payload.getString("payloadFileServiceId"), is(payloadFileServiceId));
    }

    private List<JsonObject> pendingCasesList(final List<UUID> caseIds,
                                              final List<UUID> youngOffendersCaseIds,
                                              final List<UUID> pressRestrictionCaseIds) {
        final List<JsonObject> pendingCasesList = new LinkedList<>();

        for (int caseNumber = 0; caseNumber < caseIds.size(); caseNumber++) {
            final JsonObjectBuilder pendingCase = getStandardPendingCase(caseIds, caseNumber);
            pendingCasesList.add(pendingCase.build());
        }

        // Attach cases by young offenders
        for (int caseNumber = 0; caseNumber < youngOffendersCaseIds.size(); caseNumber++) {
            final JsonObjectBuilder pendingCase = buildPendingCaseYouthDefendant(caseIds, youngOffendersCaseIds, caseNumber);
            pendingCasesList.add(pendingCase.build());
        }

        // Attach cases with press restrictions
        for (int caseNumber = 0; caseNumber < pressRestrictionCaseIds.size(); caseNumber++) {
            final JsonObjectBuilder pendingCase = getPendingCaseWithPressRestriction(pressRestrictionCaseIds, caseNumber);
            pendingCasesList.add(pendingCase.build());
        }

        return pendingCasesList;
    }

    private JsonObjectBuilder buildPendingCaseYouthDefendant(final List<UUID> caseIds, final List<UUID> youngOffendersCaseIds, final int caseNumber) {
        final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceCode", "CA03011")
                        .add("offenceStartDate", LocalDate.now().toString()))
                .add(createObjectBuilder()
                        .add("offenceCode", "CA03011")
                        .add("offenceStartDate", LocalDate.now().minusMonths(1).toString()));

        int adjustedCaseNumber = caseIds.size() + caseNumber;

        return createObjectBuilder()
                .add("caseId", youngOffendersCaseIds.get(caseNumber).toString())
                .add("defendantName", "J. Doe" + adjustedCaseNumber)
                .add("postcode", "SE1 1PJ" + adjustedCaseNumber)
                .add("offences", offenceArrayBuilder.build())
                // the defendant is 18 years and 10 days now, but is a minor when once of the offences occurred
                .add("defendantDateOfBirth", LocalDate.now().minusYears(18).plusDays(10).toString())
                .add("prosecutorName", "TFL");
    }

    private JsonObjectBuilder getStandardPendingCase(final List<UUID> caseIds, final int caseNumber) {
        boolean generateFullAddress = caseNumber % 2 == 0;

        final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceCode", "CA03011")
                        .add("offenceStartDate", LocalDate.now().toString())
                        .add("completed", false)
                        .add("pressRestriction", createObjectBuilder()
                                .add("requested", false)
                        ))
                .add(createObjectBuilder()
                        .add("offenceCode", "CA03011")
                        .add("offenceStartDate", LocalDate.now().minusMonths(1).toString())
                        .add("completed", false)
                        .add("pressRestriction", createObjectBuilder()
                                .add("requested", false)
                        ));

        final JsonObjectBuilder pendingCase = createObjectBuilder()
                .add("caseId", caseIds.get(caseNumber).toString())
                .add("defendantName", "J. Doe" + caseNumber)
                .add("postcode", "S" + caseNumber)
                .add("offences", offenceArrayBuilder.build())
                .add("prosecutorName", "TFL")
                .add("firstName", "ALEX")
                .add("lastName","SCHULTZ");

        if (generateFullAddress) {
            pendingCase
                    .add("town", "London" + caseNumber)
                    .add("county", "Greater London" + caseNumber);
        }
        return pendingCase;
    }

    private JsonObjectBuilder getPendingCaseWithPressRestriction(final List<UUID> caseIds, final int caseNumber) {

        final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceCode", "CA03011")
                        .add("offenceStartDate", LocalDate.now().toString())
                        .add("completed", false)
                        .add("pressRestriction", createObjectBuilder()
                                .add("requested", true)
                                .add("name", "Jaimie")
                        ))
                .add(createObjectBuilder()
                        .add("offenceCode", "CA03011")
                        .add("offenceStartDate", LocalDate.now().minusMonths(1).toString())
                        .add("completed", false)
                        .add("pressRestriction", createObjectBuilder()
                                .add("requested", false)
                        ));

        final JsonObjectBuilder pendingCase = createObjectBuilder()
                .add("caseId", caseIds.get(caseNumber).toString())
                .add("defendantName", "J. Doe" + caseNumber)
                .add("postcode", "SE1 1PJ" + caseNumber)
                .add("offences", offenceArrayBuilder.build())
                .add("prosecutorName", "TFL");


        pendingCase
                .add("town", "London" + caseNumber)
                .add("county", "Greater London" + caseNumber);

        return pendingCase;
    }

}
