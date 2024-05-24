package uk.gov.moj.cpp.sjp.event.processor.utils;

import static java.lang.String.format;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.exception.OffenceNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadHelper.class);

    public static final String TITLE = "title";
    public static final String WELSH_TITLE = "titleWelsh";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String LEGAL_ENTITY_NAME = "legalEntityName";
    private Table<String, Boolean, String> prosecutorDataTable;
    private Table<String, String, JsonObject> offenceDataTable;

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
        LOGGER.error("reference offences payload {}", offenceReferenceData);
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
}
