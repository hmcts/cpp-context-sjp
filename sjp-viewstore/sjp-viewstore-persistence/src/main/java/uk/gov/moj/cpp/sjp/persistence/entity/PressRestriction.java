package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class PressRestriction implements Serializable {

    @Column(name = "press_restriction_name")
    private final String name;

    @Column(name = "press_restriction_requested")
    private final Boolean requested;

    public PressRestriction(final String name, final boolean requested) {
        this.name = name;
        this.requested = requested;
    }
    public PressRestriction(final String name) {
        this.name = name;
        this.requested = Boolean.TRUE;
    }

    public static PressRestriction revoked() {
        return new PressRestriction(null, false);
    }

    public static PressRestriction requested(final String name) {
        return new PressRestriction(name);
    }

    public String getName() {
        return name;
    }

    public Boolean getRequested() {
        return requested;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PressRestriction)) {
            return false;
        }
        final PressRestriction that = (PressRestriction) o;
        return Objects.equals(name, that.name) &&
                requested.equals(that.requested);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, requested);
    }
}
