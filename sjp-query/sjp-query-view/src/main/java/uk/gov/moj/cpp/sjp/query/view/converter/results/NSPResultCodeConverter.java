package uk.gov.moj.cpp.sjp.query.view.converter.results;


import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Collections;
import java.util.List;

public class NSPResultCodeConverter extends ResultCodeConverter{

    @Override
    protected List<Prompt> getPromptList() {
        return Collections.emptyList();
    }
}
