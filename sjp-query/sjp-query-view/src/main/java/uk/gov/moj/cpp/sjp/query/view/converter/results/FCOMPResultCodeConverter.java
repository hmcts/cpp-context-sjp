package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static com.google.common.collect.ImmutableList.copyOf;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.FCOMP_MAJOR_CREDITOR;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class FCOMPResultCodeConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.AMOUNT_OF_COMPENSATION, FCOMP_MAJOR_CREDITOR);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }

}
