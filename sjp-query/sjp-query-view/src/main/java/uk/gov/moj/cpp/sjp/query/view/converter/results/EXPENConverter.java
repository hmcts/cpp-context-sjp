package uk.gov.moj.cpp.sjp.query.view.converter.results;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

public class EXPENConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.AMOUNT_OF_EXCISE_PENALTY);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
