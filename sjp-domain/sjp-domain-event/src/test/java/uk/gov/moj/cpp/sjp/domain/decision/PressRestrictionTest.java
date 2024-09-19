package uk.gov.moj.cpp.sjp.domain.decision;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class PressRestrictionTest {

    private static final String CHILDS_NAME = "A Name";

    @Test
    public void shouldMatchEquality() {
        assertThat(new PressRestriction(CHILDS_NAME), equalTo(new PressRestriction(CHILDS_NAME)));
        assertThat(new PressRestriction(), equalTo(new PressRestriction()));
        assertThat(new PressRestriction(CHILDS_NAME).equals(new PressRestriction("Another Name")), is(false));
    }


    @Test
    public void shouldSerializePressRestriction() throws JsonProcessingException {
        final String restrictionWithName = new ObjectMapper().writeValueAsString(new PressRestriction("Robert"));
        assertThat(restrictionWithName, is("{\"name\":\"Robert\",\"requested\":true}"));

        final String restrictionWithoutName = new ObjectMapper().writeValueAsString(new PressRestriction());
        assertThat(restrictionWithoutName, is("{\"name\":null,\"requested\":false}"));
    }

    @Test
    public void isRevokedShouldMatchTheTypeOfRestriction() {
        assertThat(PressRestriction.revoked().isRevoked(), is(true));
        assertThat(PressRestriction.requested(CHILDS_NAME).isRevoked(), is(false));
    }
}
