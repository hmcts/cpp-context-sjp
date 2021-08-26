package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import uk.gov.justice.core.courts.JudicialResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JudicialResultsAggregate {

    private final Map<UUID, List<JudicialResult>> results = new HashMap<>();

    public void put(final UUID key, final List<JudicialResult> value) {
        results.put(key, value);
    }

    public List<JudicialResult> get(final UUID key) {
        return results.get(key);
    }

}
