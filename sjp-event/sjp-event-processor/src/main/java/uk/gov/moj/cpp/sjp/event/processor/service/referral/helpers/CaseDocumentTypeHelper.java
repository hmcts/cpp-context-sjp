package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static org.apache.commons.lang3.StringUtils.trim;

import java.util.HashMap;
import java.util.Map;

public class CaseDocumentTypeHelper {

    private static final Map<String, String> DOCUMENT_MAPPING_TYPES = new HashMap<>();
    private static final String DOCUMENT_MAPPING_TYPES_SENTENCE = "Sentence";


    static {
        DOCUMENT_MAPPING_TYPES.put("PLEA", "Plea");
        DOCUMENT_MAPPING_TYPES.put("CITN", "Pre Cons");
        DOCUMENT_MAPPING_TYPES.put("FINANCIAL_MEANS", DOCUMENT_MAPPING_TYPES_SENTENCE);
        DOCUMENT_MAPPING_TYPES.put("SJPN", "Case Summary");
        DOCUMENT_MAPPING_TYPES.put("INTENTION_TO_DISQUALIFY_NOTICE", DOCUMENT_MAPPING_TYPES_SENTENCE);
        DOCUMENT_MAPPING_TYPES.put("DISQUALIFICATION_REPLY_SLIP", DOCUMENT_MAPPING_TYPES_SENTENCE);
        DOCUMENT_MAPPING_TYPES.put("OTHER", "General correspondence");



    }

    private CaseDocumentTypeHelper() {
    }

    public static String getDocumentType(final String sjpDocumentType) {
        return DOCUMENT_MAPPING_TYPES.getOrDefault(trim(sjpDocumentType), "General correspondence");
    }
}
