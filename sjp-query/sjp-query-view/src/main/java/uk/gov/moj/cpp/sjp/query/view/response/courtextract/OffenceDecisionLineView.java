package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import java.util.Objects;

public class OffenceDecisionLineView {
    private final String label;

    private String value;

    public OffenceDecisionLineView(final String label, final String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return label + "->" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OffenceDecisionLineView that = (OffenceDecisionLineView) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, value);
    }
}


