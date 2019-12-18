package uk.gov.moj.cpp.sjp.domain;

import static java.util.Objects.nonNull;

import org.apache.commons.lang3.StringUtils;

public final class DomainUtils {

    private DomainUtils() {
    }

    @SuppressWarnings("squid:S4973")
    public static boolean equals(String a, String b) {
        return (a == b) ||
                bothStringsAreBlank(a, b) ||
                (nonNull(a) && a.equals(b));
    }

    public static int hashCode(String... values) {
        if (values == null) {
            return 0;
        }
        int result = 1;
        for (final String s : values) {
            result = 31 * result + (StringUtils.isBlank(s) ? 0 : s.hashCode());
        }
        return result;
    }

    public static int hash(Object... values) {
        if (values == null) {
            return 0;
        }
        int result = 1;

        for (final Object o : values) {
            if (o instanceof String) {
                result = 31 * result + (StringUtils.isBlank((String) o) ? 0 : o.hashCode());
            } else {
                result = 31 * result + (o == null ? 0 : o.hashCode());
            }
        }
        return result;

    }

    private static boolean bothStringsAreBlank(final String a, final String b) {
        return StringUtils.isBlank(a) && StringUtils.isBlank(b);
    }

}
