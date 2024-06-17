package uk.gov.moj.cpp.sjp.event.processor.utils;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.LF;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.exception.OffenceNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class PayloadHelper {

    public static final String TITLE = "title";
    public static final String WELSH_TITLE = "titleWelsh";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String LEGAL_ENTITY_NAME = "legalEntityName";
    private Table<String, Boolean, String> prosecutorDataTable;
    private Table<String, String, JsonObject> offenceDataTable;
    private static final DateTimeFormatter START_DATE_FORMAT = ofPattern("dd MMMM yyyy");

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    // if a second request clears the cache when the first one is doing the processing it could lead to unintended results
    // or better make sure that there are no 2 processings happen at the same time. Can be handeled as part of the performance iteration
    public void initCache() {
        offenceDataTable = HashBasedTable.create();
        prosecutorDataTable = HashBasedTable.create();
    }

    public String buildProsecutorName(final String prosecutorName, final Boolean isWelsh, final JsonEnvelope envelope) {
        final String prosecutor;
        if (!prosecutorDataTable.contains(prosecutorName, isWelsh)) {
            prosecutor = referenceDataService.getProsecutor(prosecutorName, isWelsh, envelope);
            prosecutorDataTable.put(prosecutorName, isWelsh, prosecutor);
        } else {
            prosecutor = prosecutorDataTable.get(prosecutorName, isWelsh);
        }

        return prosecutor;
    }

    public String buildOffenceTitleFromOffenceArray(final JsonArray offenceJsonArray, final Boolean isWelsh, final JsonEnvelope envelope) {
        // REFDATA-219 -- Call reference data offences only once by passing all the offence codes and the service should return the offences including the legacy versions
        return offenceJsonArray.getValuesAs(JsonObject.class).stream()
                .map(e -> mapOffenceIntoOffenceTitleString(e, isWelsh, envelope))
                .reduce((offenceTitle1, offenceTitle2) -> offenceTitle1.concat(LF).concat(offenceTitle2))
                .orElseThrow(() -> new RuntimeException("Error during processing payload for document generator! "));
    }

    public String mapOffenceIntoOffenceTitleString(final JsonObject offence, final Boolean isWelsh, final JsonEnvelope envelope) {
        final String offenceCode = offence.getString("offenceCode");
        final String offenceStartDate = LocalDate.now().toString();

        final JsonObject offenceReferenceData;
        if (!offenceDataTable.contains(offenceCode, offenceStartDate)) {
            offenceReferenceData = referenceDataOffencesService
                    .getOffenceReferenceData(envelope, offenceCode, offenceStartDate)
                    .orElseThrow(() -> new OffenceNotFoundException(
                            format("Referral decision not found for case %s",
                                    offenceCode))
                    );
            offenceDataTable.put(offenceCode, offenceStartDate, offenceReferenceData);
        } else {
            offenceReferenceData = offenceDataTable.get(offenceCode, offenceStartDate);
        }
        return getOffenceTitle(offenceReferenceData, isWelsh);
    }

    private String getOffenceTitle(final JsonObject offenceReferenceData, final Boolean isWelsh) {
        if (!isWelsh) {
            return offenceReferenceData.getString(TITLE);
        }

        return offenceReferenceData.containsKey(WELSH_TITLE) ? offenceReferenceData.getString(WELSH_TITLE) : offenceReferenceData.getString(TITLE);
    }

    public String buildDefendantName(final JsonObject pendingCase) {
        if (pendingCase.containsKey(LEGAL_ENTITY_NAME)) {
            return pendingCase.getString(LEGAL_ENTITY_NAME).toUpperCase();
        } else {
            return format("%s %s", pendingCase.getString(FIRST_NAME, ""), pendingCase.getString(LAST_NAME, "").toUpperCase());
        }
    }

    public String getStartDate(boolean isWelsh) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(1);

        if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
            fromDate = fromDate.minusDays(2);
        }
        final Locale locale = isWelsh ? new Locale("cy") : ENGLISH;
        return fromDate.format(START_DATE_FORMAT.withLocale(locale));
    }

    public String getTemplateIdentifier(final String type, String lang, String exportType) {
        lang = lang.toLowerCase();
        lang = lang.substring(0, 1).toUpperCase() + lang.substring(1);
        return "PRESS".equalsIgnoreCase(exportType) ? "PressPendingCases" + type + lang : "PublicPendingCases" + type + lang;
    }
}
