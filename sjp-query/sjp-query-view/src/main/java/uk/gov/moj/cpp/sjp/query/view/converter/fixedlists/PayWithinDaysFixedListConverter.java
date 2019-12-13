package uk.gov.moj.cpp.sjp.query.view.converter.fixedlists;



import static uk.gov.moj.cpp.sjp.query.view.converter.FixedListConverterUtil.mapValue;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM_14_DAYS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM_28_DAYS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM_WITHIN_14_DAYS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.LUMP_SUM_WITHIN_28_DAYS;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;

public class PayWithinDaysFixedListConverter implements FixedListConverter {

    @SuppressWarnings("unchecked")
    private static final Map<String, String> dataMap = MapUtils.putAll(new HashMap<String, String>(), new String[] {
            LUMP_SUM_WITHIN_14_DAYS.toLowerCase(), LUMP_SUM_14_DAYS,
            LUMP_SUM_WITHIN_28_DAYS.toLowerCase(), LUMP_SUM_28_DAYS
    });

    @Override
    public Optional<String> convert(final String value) {
        return mapValue(value, dataMap);
    }
}
