package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class SUMRCCResultCodeConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
