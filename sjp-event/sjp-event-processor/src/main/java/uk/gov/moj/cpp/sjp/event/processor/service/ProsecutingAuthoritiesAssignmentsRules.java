package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

public class ProsecutingAuthoritiesAssignmentsRules {

    private final Set<String> allProsecutingAuthorities;
    private final Map<String, Set<String>> restrictedProsecutingAuthorities;
    private final Map<String, Set<String>> prosecutingAuthoritiesByCourts;
    private final Map<String, Set<String>> excludedProsecutingAuthoritiesByCourts;

    @JsonCreator
    public ProsecutingAuthoritiesAssignmentsRules(final Map<String, Set<String>> courtsByProsecutor) {
        restrictedProsecutingAuthorities = Optional.ofNullable(courtsByProsecutor).orElse(Collections.emptyMap());
        allProsecutingAuthorities = restrictedProsecutingAuthorities.keySet();

        prosecutingAuthoritiesByCourts = restrictedProsecutingAuthorities
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream().map(v -> Pair.of(v, e.getKey())))
                .collect(groupingBy(p -> p.getKey(), mapping(p -> p.getValue(), collectingAndThen(toSet(), Collections::unmodifiableSet))));

        excludedProsecutingAuthoritiesByCourts = prosecutingAuthoritiesByCourts
                .entrySet()
                .stream()
                .collect(toMap(e -> e.getKey(), e -> unmodifiableSet(new HashSet(CollectionUtils.subtract(allProsecutingAuthorities, e.getValue())))));
    }

    public Set<String> getCourtExcludedProsecutingAuthorities(final String court) {
        return excludedProsecutingAuthoritiesByCourts.getOrDefault(court, allProsecutingAuthorities);
    }

    public Set<String> getCourtRestrictedProsecutingAuthorities(final String court) {
        return prosecutingAuthoritiesByCourts.getOrDefault(court, emptySet());
    }

    @Override
    public String toString() {
        return String.valueOf(restrictedProsecutingAuthorities);
    }
}
