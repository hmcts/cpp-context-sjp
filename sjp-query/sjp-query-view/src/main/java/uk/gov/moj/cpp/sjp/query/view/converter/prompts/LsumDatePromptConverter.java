package uk.gov.moj.cpp.sjp.query.view.converter.prompts;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static jdk.nashorn.internal.runtime.PropertyDescriptor.VALUE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FOURTEEN_DAYS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TWENTY_EIGHT_DAYS;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.time.LocalDate;
import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class LsumDatePromptConverter extends PromptConverter {

    private static Optional<String> getValue(final JsonObject terminalEntry, final OffenceDataSupplier offenceDataSupplier){

        final String value = terminalEntry.getString(VALUE);
        final String createDate = offenceDataSupplier.getSourceEnvelope().payloadAsJsonObject().getString("created", null);
        final LocalDate date = ZonedDateTimes.fromString(createDate).toLocalDate();
        if (value.trim().contains(FOURTEEN_DAYS.toString())) {
            return of(LocalDates.to(date.plusDays(FOURTEEN_DAYS)));
        } else if (value.trim().contains(TWENTY_EIGHT_DAYS.toString())) {
            return of(LocalDates.to(date.plusDays(TWENTY_EIGHT_DAYS)));
        }
        return empty();
    }

    @Override
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, final Prompt prompt, final OffenceDataSupplier offenceDataSupplier) {
        getValue(terminalEntry, offenceDataSupplier).ifPresent(value -> addPrompt(promptsPayloadBuilder, prompt.getId(), value));
    }
}
