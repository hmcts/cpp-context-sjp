package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class LSUMIResultCodeConverter extends ResultCodeConverter{

    private static final List<Prompt> promptList = Arrays.asList(Prompt.LSUMI_INSTALMENT_AMOUNT, Prompt.LSUMI_PAYMENT_FREQUENCY, Prompt.LSUMI_INSTALMENT_START_DATE, Prompt.LSUMI_LUMP_SUM_AMOUNT);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
