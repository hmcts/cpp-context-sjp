package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static java.util.Collections.singletonList;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.List;

public class D45ResultCodeConverter extends ResultCodeConverter {

    @Override
    public List<Prompt> getPromptList() {
        return singletonList(Prompt.D45_PRESS_RESTRICTION_APPLIED);
    }
}
