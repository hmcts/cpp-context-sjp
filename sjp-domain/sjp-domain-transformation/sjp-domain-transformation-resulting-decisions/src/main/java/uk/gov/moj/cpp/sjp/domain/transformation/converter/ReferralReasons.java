package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;


public class ReferralReasons {

    private static final Map<String, String> REFERRAL_REASONS = new TreeMap<>(new FuzzyMatchComparator());

    static {
        REFERRAL_REASONS.put("Equivocal plea (Defendant to attend to clarify plea)", "0f03e10f-d9a5-47f9-92ba-a8e220448451");
        REFERRAL_REASONS.put("Defendant to attend to clarify", "0f03e10f-d9a5-47f9-92ba-a8e220448451");

        REFERRAL_REASONS.put("Equivocal plea (For Trial)", "33e0aeae-ef9f-40e3-8032-49d50a5a0904");
        REFERRAL_REASONS.put("Equivocal plea – for trial", "33e0aeae-ef9f-40e3-8032-49d50a5a0904");

        REFERRAL_REASONS.put("Defence request", "798e3d82-44fa-40e6-a93d-4c8f5322fa66");

        REFERRAL_REASONS.put("For trial", "809f7aac-d285-43a5-9fb1-3a894db71530");

        REFERRAL_REASONS.put("For a case management hearing (Defendant to attend)", "9753b66a-8845-491f-a42b-7fc207ae6b1b");
        REFERRAL_REASONS.put("For a case management hearing (deft to attend)", "9753b66a-8845-491f-a42b-7fc207ae6b1b");

        REFERRAL_REASONS.put("For a case management hearing (No need for defendant to attend)", "23121983-9c84-4e1e-8e5f-9b1d81124204");
        REFERRAL_REASONS.put("For a case management hearing (no appearance)", "23121983-9c84-4e1e-8e5f-9b1d81124204");

        REFERRAL_REASONS.put("For sentencing hearing - defendant to attend", "bc5c3ce5-6029-489f-b149-bc59efca17d1");
        REFERRAL_REASONS.put("To attend for sentence", "bc5c3ce5-6029-489f-b149-bc59efca17d1");

        REFERRAL_REASONS.put("For disqualification - defendant to attend", "cb23156c-fa9d-48d7-bac6-4d900d237ba0");
        REFERRAL_REASONS.put("To attend re disqualification", "cb23156c-fa9d-48d7-bac6-4d900d237ba0");

        REFERRAL_REASONS.put("Case unsuitable for SJP", "d10c5cc4-ec2a-41ac-bd6e-a3659c5cfeb1");
        REFERRAL_REASONS.put("Case inappropriate for the single justice procedure", "d10c5cc4-ec2a-41ac-bd6e-a3659c5cfeb1");
    }

    private ReferralReasons() {
    }

    public static String getReferralReasonId(final String referralReason) {
        return REFERRAL_REASONS.get(referralReason);
    }

    private static class FuzzyMatchComparator implements Comparator<String> {

        private static String normalize(final String string) {
            return normalizeSpace(defaultString(string)).toLowerCase();
        }

        @Override
        public int compare(String o1, String o2) {
            return normalize(o1).compareTo(normalize(o2));
        }
    }
}
