package uk.gov.moj.cpp.sjp.query.view.converter.results;


import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class FVSResultCodeConverter extends ResultCodeConverter{

    @Override
    public List<Prompt> getPromptList() {
        return Arrays.asList(Prompt.AMOUNT_OF_SURCHARGE);
    }
}
