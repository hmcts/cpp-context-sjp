package uk.gov.moj.cpp.sjp.query.view.converter.results;


import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class RLSUMIResultCodeConverter extends ResultCodeConverter{

    @Override
    public List<Prompt> getPromptList() {
        return Arrays.asList(Prompt.RLSUMI_LUMP_SUM_AMOUNT, Prompt.RLSUMI_INSTALMENT_AMOUNT, Prompt.RLSUMI_INSTALMENT_START_DATE, Prompt.RLSUMI_PAYMENT_FREQUENCY);
    }
}
