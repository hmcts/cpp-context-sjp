package uk.gov.moj.sjp.it.util.matchers;

import static java.util.Objects.isNull;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;

import org.hamcrest.Matcher;

public class OffenceDecisionMatcher {

    public static Matcher match(final OffenceDecision offenceDecision) {
        return allOf(
                hasProperty("type", is(offenceDecision.getType())),
                hasPressRestriction(offenceDecision)
        );
    }

    public static Matcher adjourn(final Matcher matcher) {
        return offenceDecisionHaving(ADJOURN, matcher);
    }

    public static Matcher offenceDecisionHaving(final DecisionType decisionType, final Matcher matcher) {
        return allOf(matcher, hasProperty("type", equalTo(decisionType)));
    }

    public static Matcher hasPressRestriction(final OffenceDecision offenceDecision) {
        return pressRestriction(offenceDecision.getPressRestriction());
    }

    public static Matcher pressRestriction(final PressRestriction pressRestriction) {
        if (isNull(pressRestriction)) {
            return hasProperty("pressRestriction", nullValue());
        } else {
            return hasProperty("pressRestriction", allOf(
                            hasProperty("name", is(pressRestriction.getName())),
                            hasProperty("requested", is(pressRestriction.getRequested()))
                    )
            );
        }
    }
}
