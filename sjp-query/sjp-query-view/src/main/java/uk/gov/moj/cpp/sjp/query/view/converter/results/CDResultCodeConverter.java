package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DAY;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DAYS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.INDEX;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LENGTH;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.MONTH;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.MONTHS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PROMPT_DEFINITION_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TERMINAL_ENTRIES;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.UNIT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.WEEK;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.WEEKS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.YEAR;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.YEARS;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.commons.collections.MapUtils;

public class CDResultCodeConverter extends ResultCodeConverter{

    private final List<Prompt> promptList = Arrays.asList(Prompt.DURATION_VALUE_OF_CONDITIONAL_DISCHARGE, Prompt.DURATION_UNIT_OF_CONDITIONAL_DISCHARGE);

    @Override
    public JsonArrayBuilder createPrompts(final JsonObject result, final OffenceDataSupplier offenceDataSupplier) {

        final JsonArrayBuilder promptsPayloadBuilder = createArrayBuilder();

        final Map<String, String> valueMap = new HashMap<>();

        result.getJsonArray(TERMINAL_ENTRIES).getValuesAs(JsonObject.class).forEach(
                terminalEntry -> {
                    final Integer index = terminalEntry.getInt(INDEX, 99999);
                    if (Prompt.DURATION_VALUE_OF_CONDITIONAL_DISCHARGE.getIndex().equals(index)) {
                        calculateLengthOfDischarge(terminalEntry).ifPresent(length -> valueMap.put(LENGTH, length.trim()));
                    } else if (Prompt.DURATION_UNIT_OF_CONDITIONAL_DISCHARGE.getIndex().equals(index)) {
                        calculateUnit(terminalEntry).ifPresent(unit -> valueMap.put(UNIT, unit.trim()));
                    }
                }
        );

        final String length = valueMap.get(LENGTH);
        final String unit = valueMap.get(UNIT);
        if (isNotBlank(length) && isNotBlank(unit)) {
            promptsPayloadBuilder.add(createObjectBuilder()
                    .add(PROMPT_DEFINITION_ID, Prompt.DURATION_VALUE_OF_CONDITIONAL_DISCHARGE.getId().toString())
                    .add(VALUE, length + " " + unit));
        }
        return promptsPayloadBuilder;
    }

    private Optional<String> calculateUnit(final JsonObject terminalEntry){
        final Map<String, String> dischargeUnitMapping = MapUtils.putAll(new HashMap<String, String>(), new String[] {
                YEAR.toLowerCase(), YEARS,
                MONTH.toLowerCase(), MONTHS,
                WEEK.toLowerCase(), WEEKS,
                DAY.toLowerCase(), DAYS
        });

        final Optional<String> value = ofNullable(terminalEntry.getString(VALUE, null));
        return value.map(v -> dischargeUnitMapping.get(v.trim().toLowerCase()));
    }

    private Optional<String> calculateLengthOfDischarge(final JsonObject terminalEntry){
        return ofNullable(terminalEntry.getString(VALUE, null));
    }

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
