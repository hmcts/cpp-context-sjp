package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class FOResultCodeConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.AMOUNT_OF_FINE);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
