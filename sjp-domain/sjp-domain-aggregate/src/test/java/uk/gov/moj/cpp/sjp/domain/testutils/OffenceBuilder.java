package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.moj.cpp.sjp.domain.Offence;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

public class OffenceBuilder {
    private static BigDecimal COMPENSATION = BigDecimal.valueOf(11.11);

    public static List<Offence> createDefaultOffences(final UUID... ids) {
        return Arrays.stream(ids)
                .map(id -> new Offence(id, 0, null, null,
                        0, null, null, null, null, COMPENSATION))
                .collect(Collectors.toList());
    }
}
