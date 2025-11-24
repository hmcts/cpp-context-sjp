package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.json.schemas.domains.sjp.events.CaseAssignmentRestrictionAdded.caseAssignmentRestrictionAdded;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseAssignmentRestrictionAdded;

import java.util.List;
import java.util.stream.Stream;


public class CaseAssignmentRestrictionAggregate implements Aggregate {

    public Stream<Object> updateCaseAssignmentRestriction(final String prosecutingAuthority, final List<String> includeOnly, final List<String> exclude, final String dateTimeCreated) {

        return apply(of(caseAssignmentRestrictionAdded()
                .withDateTimeCreated(dateTimeCreated)
                .withExclude(exclude)
                .withIncludeOnly(includeOnly)
                .withProsecutingAuthority(prosecutingAuthority)
                .build()));
    }

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(CaseAssignmentRestrictionAdded.class).apply(e -> doNothing()),
                otherwiseDoNothing());
    }
}
