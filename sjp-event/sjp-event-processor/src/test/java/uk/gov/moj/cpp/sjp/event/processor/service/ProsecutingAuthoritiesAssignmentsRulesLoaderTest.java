package uk.gov.moj.cpp.sjp.event.processor.service;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.everit.json.schema.ValidationException;
import org.junit.Test;

public class ProsecutingAuthoritiesAssignmentsRulesLoaderTest {

    private ProsecutingAuthoritiesAssignmentsRulesLoader prosecutingAuthoritiesAssignmentsRulesLoader;

    @Test
    public void shouldLoadDefaultConfiguration() throws Exception {
        prosecutingAuthoritiesAssignmentsRulesLoader = new ProsecutingAuthoritiesAssignmentsRulesLoader();
        final ProsecutingAuthoritiesAssignmentsRules prosecutorsAssignmentsRules = prosecutingAuthoritiesAssignmentsRulesLoader.load();
        assertThat(prosecutorsAssignmentsRules, not(nullValue()));
    }

    @Test
    public void shouldLoadCorrectConfiguration() throws Exception {
        prosecutingAuthoritiesAssignmentsRulesLoader = new ProsecutingAuthoritiesAssignmentsRulesLoader();
        final ProsecutingAuthoritiesAssignmentsRules prosecutorsAssignmentsRules = prosecutingAuthoritiesAssignmentsRulesLoader.load("prosecuting_authorities_assignments_correct.json");

        assertThat(prosecutorsAssignmentsRules.getCourtRestrictedProsecutingAuthorities("1"), containsInAnyOrder("TFL"));
        assertThat(prosecutorsAssignmentsRules.getCourtRestrictedProsecutingAuthorities("2"), containsInAnyOrder("TFL"));
        assertThat(prosecutorsAssignmentsRules.getCourtRestrictedProsecutingAuthorities("3"), containsInAnyOrder("TVL"));
    }

    @Test
    public void shouldThrowExceptionWhenConfigurationDoesNotMatchSchema() throws Exception {
        prosecutingAuthoritiesAssignmentsRulesLoader = new ProsecutingAuthoritiesAssignmentsRulesLoader();

        try {
            prosecutingAuthoritiesAssignmentsRulesLoader.load("prosecuting_authorities_assignments_incorrect.json");
            fail("Validation exception expected");
        } catch (final ValidationException exception) {
            assertThat(exception.getViolationCount(), is(1));
        }
    }

}
