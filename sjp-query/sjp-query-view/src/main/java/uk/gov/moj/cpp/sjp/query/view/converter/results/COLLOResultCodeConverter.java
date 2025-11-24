package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class COLLOResultCodeConverter extends ResultCodeConverter{

    private static List<Prompt> promptList = Arrays.asList(Prompt.COLLECTION_ORDER_TYPE, Prompt.REASON_NOT_ABD_OR_AEO);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
