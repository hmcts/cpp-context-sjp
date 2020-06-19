package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static org.apache.commons.lang3.StringUtils.trim;

import java.util.HashMap;
import java.util.Map;

public class CaseDocumentTypeHelper {

    private static final Map<String, String> DOCUMENT_MAPPING_TYPES = new HashMap<>();

    static {
        DOCUMENT_MAPPING_TYPES.put("PLEA", "Plea");
        DOCUMENT_MAPPING_TYPES.put("CITN", "Pre Cons");
        DOCUMENT_MAPPING_TYPES.put("FINANCIAL_MEANS", "Sentence");
        DOCUMENT_MAPPING_TYPES.put("SJPN", "Case Summary");
        DOCUMENT_MAPPING_TYPES.put("INTENTION_TO_DISQUALIFY_NOTICE", "Sentence");
    }

    private CaseDocumentTypeHelper() {
    }

    public static String getDocumentType(final String sjpDocumentType) {
        return DOCUMENT_MAPPING_TYPES.getOrDefault(trim(sjpDocumentType), "General correspondence");
    }
}
