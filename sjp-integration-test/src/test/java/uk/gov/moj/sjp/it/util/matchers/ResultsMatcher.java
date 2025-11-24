package uk.gov.moj.sjp.it.util.matchers;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;

import uk.gov.moj.cpp.sjp.domain.resulting.Prompt;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;

import java.util.UUID;

import org.hamcrest.Matcher;

/**
 * Refer to the @enum uk.gov.moj.cpp.sjp.query.view.converter.ResultCode for id references
 */
public class ResultsMatcher {

    private static final UUID LEN = fromString("b0aeb4fc-df63-4e2f-af88-97e3f23e847f");
    private static final UUID NSP = fromString("49939c7c-750f-403e-9ce1-f82e3e568065");

    private static final UUID LEP = fromString("cee54856-4450-4f28-a8a9-72b688726201");
    private static final UUID LEP_PENALTY_POINTS = fromString("a8719de4-7783-448a-b792-e3f94e670ad0");

    private static final UUID FO = fromString("969f150c-cd05-46b0-9dd9-30891efcc766");
    private static final UUID AMOUNT_OF_FINE = fromString("7cd1472f-2379-4f5b-9e67-98a43d86e122");

    private static final UUID NCR = fromString("29e02fa1-42ce-4eec-914e-e62508397a16");
    private static final UUID REASON_FOR_NO_COMPENSATION = fromString("e263de82-47ca-433a-bb41-cad2e1c5bb72");

    private static final UUID LEA = fromString("3fa139cc-efe0-422b-93d6-190a5be50953");
    private static final UUID REASON_FOR_PENALTY_POINTS = fromString("bbbb47bb-3418-463c-bfc3-43c6f72bb7c9");

    public static Matcher<Result> NSP() {
        return containsResult(NSP);
    }

    public static Matcher<Result> LEN() {
        return containsResult(LEN);
    }

    public static Matcher<Result> LEA(final int pointsImposed) {
        Prompt prompt = new Prompt(REASON_FOR_PENALTY_POINTS, String.valueOf(pointsImposed));
        return containsResult(LEA, prompt);
    }

    public static Matcher<Result> LEP(final int pointsImposed) {
        Prompt prompt = new Prompt(LEP_PENALTY_POINTS, String.valueOf(pointsImposed));
        return containsResult(LEP, prompt);
    }

    public static Matcher<Result> FO(final String amountOfFine) {
        Prompt prompt = new Prompt(AMOUNT_OF_FINE, amountOfFine);
        return containsResult(FO, prompt);
    }

    public static Matcher<Result> NCR(final String reasonForNoCompensation) {
        Prompt prompt = new Prompt(REASON_FOR_NO_COMPENSATION, reasonForNoCompensation);
        return containsResult(NCR, prompt);
    }

    private static Matcher<Result> containsResult(final UUID resultDefinitionId) {
        return containsResult(resultDefinitionId, null);
    }

    private static Matcher<Result> containsResult(final UUID resultDefinitionId, final Prompt prompts) {
        return allOf(
                hasProperty("resultDefinitionId", equalTo(resultDefinitionId)),
                hasProperty("prompts", isNull(prompts) ? empty() : containsInAnyOrder(prompts))
        );
    }
}
