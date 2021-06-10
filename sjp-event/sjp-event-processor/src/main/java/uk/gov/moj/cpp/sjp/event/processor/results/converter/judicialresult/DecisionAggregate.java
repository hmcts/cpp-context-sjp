package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DecisionAggregate {

    private final Map<UUID, List<JudicialResult>> results = new HashMap<>();

    private final Map<UUID, ConvictionInfo> convictionInfoMap = new HashMap<>();

    public void putResults(final UUID key, final List<JudicialResult> value) {
        results.put(key, value);
    }

    public List<JudicialResult> getResults(final UUID key) {
        return results.get(key);
    }

    public void putConvictionInfo(final UUID offenceId, final ConvictionInfo convictionInfo) {
        convictionInfoMap.put(offenceId, convictionInfo);
    }

    public ConvictionInfo getConvictionInfo(final UUID offenceId) {
        return convictionInfoMap.get(offenceId);
    }

}
