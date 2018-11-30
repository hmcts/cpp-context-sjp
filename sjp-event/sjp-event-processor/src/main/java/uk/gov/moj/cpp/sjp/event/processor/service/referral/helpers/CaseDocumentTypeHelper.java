package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CaseDocumentTypeHelper {

    private static final Map<String, String> documentMappingTypes = new HashMap<>();

    static {
        documentMappingTypes.put("PLEA", "Basis of Plea");
        documentMappingTypes.put("CITN", "Pre Cons");
        documentMappingTypes.put("FINANCIAL_MEANS", "Sentence");
        documentMappingTypes.put("SJPN", "Case Summary");
    }

    public String getDocumentType(String sjpDocumentType) {
        return documentMappingTypes.getOrDefault(sjpDocumentType, "Private Section - Court Logs");
    }
}
