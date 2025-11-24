package uk.gov.moj.cpp.sjp.query.view.converter.results;


import static com.google.common.collect.ImmutableList.copyOf;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYEE_REFERENCE_NO;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_ADDRESS_LINE_1;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_ADDRESS_LINE_2;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_ADDRESS_LINE_3;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_ADDRESS_LINE_4;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_ADDRESS_LINE_5;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_NAME;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.EMPLOYER_POSTCODE;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.util.Arrays;
import java.util.List;

public class AEOCResultCodeConverter extends ResultCodeConverter {

    private static List<Prompt> promptList = Arrays.asList(Prompt.AEOC_REASON, EMPLOYER_NAME, EMPLOYER_ADDRESS_LINE_1, EMPLOYER_ADDRESS_LINE_2, EMPLOYER_ADDRESS_LINE_3, EMPLOYER_ADDRESS_LINE_4, EMPLOYER_ADDRESS_LINE_5, EMPLOYER_POSTCODE, EMPLOYEE_REFERENCE_NO);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }
}
