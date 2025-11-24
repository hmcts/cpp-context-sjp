package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class NOVSResultCodeConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.REASON_FOR_NOT_IMPOSING_OR_REDUCING_VICTIM_SURCHARGE);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
