package uk.gov.moj.cpp.sjp.persistence.entity.view;

public class ReadyCasesReasonCount {

    private String reason;
    private long count;

    public ReadyCasesReasonCount(final String reason, final long count) {
        this.reason = reason;
        this.count = count;
    }

    public String getReason() {
        return reason;
    }

    public long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadyCasesReasonCount that = (ReadyCasesReasonCount) o;

        if (count != that.count) {
            return false;
        }
        return reason.equals(that.reason);

    }

    @Override
    public int hashCode() {
        int result = reason.hashCode();
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }

}
