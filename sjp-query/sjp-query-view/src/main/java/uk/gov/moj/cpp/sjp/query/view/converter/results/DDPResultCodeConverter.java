package uk.gov.moj.cpp.sjp.query.view.converter.results;


import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class DDPResultCodeConverter extends ResultCodeConverter {

    @Override
    public List<Prompt> getPromptList() {
        return Arrays.asList(Prompt.DDP_DISQUALIFICATION_PERIOD, Prompt.DDP_NOTIONAL_PENALTY_POINTS);
    }
}
