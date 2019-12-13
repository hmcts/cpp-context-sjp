package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class SUMRTOResultCodeConverter extends ResultCodeConverter{

    private static final List<Prompt> promptList = Arrays.asList(Prompt.SUMRTO_REASONS_FOR_REFERRING, Prompt.SUMRTO_DATE_OF_HEARING, Prompt.SUMRTO_TIME_OF_HEARING, Prompt.SUMRTO_MAGISTRATES_COURT);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
