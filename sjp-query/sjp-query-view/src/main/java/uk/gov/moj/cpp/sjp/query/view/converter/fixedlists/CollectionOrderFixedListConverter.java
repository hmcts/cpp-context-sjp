package uk.gov.moj.cpp.sjp.query.view.converter.fixedlists;

import static uk.gov.moj.cpp.sjp.query.view.converter.FixedListConverterUtil.mapValue;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ATTACHMENT_OF_EARNINGS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DEDUCTIONS_FROM_BENEFIT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.MAKE_PAYMENTS_AS_ORDERED;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PAY_DIRECTLY_TO_COURT;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;

public class CollectionOrderFixedListConverter implements FixedListConverter {

    @SuppressWarnings("unchecked")
    private static final Map<String, String> dataMap = MapUtils.putAll(new HashMap<String, String>(), new String[] {
            PAY_DIRECTLY_TO_COURT.toLowerCase(), MAKE_PAYMENTS_AS_ORDERED,
            "Deduct from benefits".toLowerCase(), DEDUCTIONS_FROM_BENEFIT,
            "Attach to earnings".toLowerCase(), ATTACHMENT_OF_EARNINGS
    });

    @Override
    public Optional<String> convert(final String value) {
        return mapValue(value, dataMap);
    }
}
