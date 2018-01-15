package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ProsecutingAuthoritiesAssignmentsRulesTest {

    @Test
    public void shouldExposeRestrictedProsecutingAuthoritiesRules() {
        final Map<String, Set<String>> allowedCourtCodesForProsecutingAuthorities = new HashMap<>();
        allowedCourtCodesForProsecutingAuthorities.put("TFL", newHashSet("1", "2", "3"));
        allowedCourtCodesForProsecutingAuthorities.put("TVL", newHashSet("1", "4", "5"));
        allowedCourtCodesForProsecutingAuthorities.put("DVLA", newHashSet("6", "7"));

        final ProsecutingAuthoritiesAssignmentsRules prosecutingAuthoritiesAssignmentsRules = new ProsecutingAuthoritiesAssignmentsRules(allowedCourtCodesForProsecutingAuthorities);

        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtRestrictedProsecutingAuthorities("1"), containsInAnyOrder("TFL", "TVL"));
        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("1"), containsInAnyOrder("DVLA"));

        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtRestrictedProsecutingAuthorities("2"), containsInAnyOrder("TFL"));
        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("2"), containsInAnyOrder("TVL", "DVLA"));

        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtRestrictedProsecutingAuthorities("100"), hasSize(0));
        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("100"), containsInAnyOrder("TFL", "TVL", "DVLA"));
    }

    @Test
    public void shouldAllowGlobalExclusionOfProsecutingAuthorities() {
        final Map<String, Set<String>> allowedCourtCodesForProsecutingAuthorities = new HashMap<>();
        allowedCourtCodesForProsecutingAuthorities.put("TFL", Collections.emptySet());

        final ProsecutingAuthoritiesAssignmentsRules prosecutingAuthoritiesAssignmentsRules = new ProsecutingAuthoritiesAssignmentsRules(allowedCourtCodesForProsecutingAuthorities);

        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("1"), containsInAnyOrder("TFL"));
        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("2"), containsInAnyOrder("TFL"));
    }

    @Test
    public void shouldHandleNullMappings() {
        final ProsecutingAuthoritiesAssignmentsRules prosecutingAuthoritiesAssignmentsRules = new ProsecutingAuthoritiesAssignmentsRules(null);

        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtRestrictedProsecutingAuthorities("1"), hasSize(0));
        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("1"), hasSize(0));
    }

    @Test
    public void shouldHandleEmptyMappings() {
        final ProsecutingAuthoritiesAssignmentsRules prosecutingAuthoritiesAssignmentsRules = new ProsecutingAuthoritiesAssignmentsRules(emptyMap());

        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtRestrictedProsecutingAuthorities("1"), hasSize(0));
        assertThat(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities("1"), hasSize(0));
    }
}
