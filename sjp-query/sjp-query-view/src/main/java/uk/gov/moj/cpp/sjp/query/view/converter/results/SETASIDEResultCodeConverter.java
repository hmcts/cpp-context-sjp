package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static java.util.Collections.emptyList;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.List;

public class SETASIDEResultCodeConverter extends ResultCodeConverter {

    @Override
    public List<Prompt> getPromptList() {
        return emptyList();
    }
}
