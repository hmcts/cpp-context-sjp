package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class ABDCResultCodeConverter extends ResultCodeConverter {

    private static List<Prompt> promptList = Arrays.asList(Prompt.REASON_OPTIONAL);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
