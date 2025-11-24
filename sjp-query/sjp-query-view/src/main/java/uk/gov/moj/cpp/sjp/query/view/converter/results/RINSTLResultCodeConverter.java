package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class RINSTLResultCodeConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.RINSTL_INSTALMENT_AMOUNT, Prompt.RINSTL_PAYMENT_FREQUENCY, Prompt.RINSTL_INSTALMENT_START_DATE);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
