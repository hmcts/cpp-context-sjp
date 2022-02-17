package uk.gov.moj.cpp.sjp.model.prosecution;

public class LjaDetailsView {

    private final String ljaCode;
    private final String ljaName;
    private final String welshLjaName;

    public LjaDetailsView(final String ljaCode, final String ljaName, final String welshLjaName) {
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.welshLjaName = welshLjaName;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getWelshLjaName() {
        return welshLjaName;
    }

    public static LjaDetailsView.Builder convictingCourt() {
        return new LjaDetailsView.Builder();
    }

    public static class Builder {
        private String ljaCode;
        private String ljaName;
        private String welshLjaName;

        public LjaDetailsView.Builder withLjaCode(final String ljaCode) {
            this.ljaCode = ljaCode;
            return this;
        }

        public LjaDetailsView.Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public LjaDetailsView.Builder withWelshLjaName(final String welshLjaName) {
            this.welshLjaName = welshLjaName;
            return this;
        }

        public LjaDetailsView build() {
            return new LjaDetailsView(ljaCode, ljaName, welshLjaName);
        }

    }
}
