package uk.gov.moj.cpp.sjp.query.view.converter.fixedlists;


import static uk.gov.moj.cpp.sjp.query.view.converter.FixedListConverterUtil.mapValue;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CASE_UNSUITABLE_FOR_SJP;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.DEFENCE_REQUEST;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.EQUIVOCAL_PLEA_DEFENDANT_TO_ATTEND_TO_CLARIFY_PLEA;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.EQUIVOCAL_PLEA_FOR_TRIAL;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FOR_A_CASE_MANAGEMENT_HEARING_DEFENDANT_TO_ATTEND;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FOR_A_CASE_MANAGEMENT_HEARING_NO_NEED_FOR_DEFENDANT_TO_ATTEND;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FOR_DISQUALIFICATION_DEFENDANT_TO_ATTEND;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FOR_SENTENCING_HEARING_DEFENDANT_TO_ATTEND;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FOR_TRIAL;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;

public class SUMRCCFixedListConverter implements FixedListConverter {

    @SuppressWarnings("unchecked")
    private static final Map<String, String> dataMap = MapUtils.putAll(new HashMap<String, String>(), new String[]{
            "0f03e10f-d9a5-47f9-92ba-a8e220448451", EQUIVOCAL_PLEA_DEFENDANT_TO_ATTEND_TO_CLARIFY_PLEA,
            "33e0aeae-ef9f-40e3-8032-49d50a5a0904", EQUIVOCAL_PLEA_FOR_TRIAL,
            "798e3d82-44fa-40e6-a93d-4c8f5322fa66", DEFENCE_REQUEST,
            "809f7aac-d285-43a5-9fb1-3a894db71530", FOR_TRIAL,
            "9753b66a-8845-491f-a42b-7fc207ae6b1b", FOR_A_CASE_MANAGEMENT_HEARING_DEFENDANT_TO_ATTEND,
            "23121983-9c84-4e1e-8e5f-9b1d81124204", FOR_A_CASE_MANAGEMENT_HEARING_NO_NEED_FOR_DEFENDANT_TO_ATTEND,
            "bc5c3ce5-6029-489f-b149-bc59efca17d1", FOR_SENTENCING_HEARING_DEFENDANT_TO_ATTEND,
            "cb23156c-fa9d-48d7-bac6-4d900d237ba0", FOR_DISQUALIFICATION_DEFENDANT_TO_ATTEND,
            "d10c5cc4-ec2a-41ac-bd6e-a3659c5cfeb1", CASE_UNSUITABLE_FOR_SJP
    });

    @Override
    public Optional<String> convert(final String value) {
        return mapValue(value, dataMap);
    }
}
