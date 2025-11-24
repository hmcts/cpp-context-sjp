package uk.gov.moj.cpp.sjp.query.view.util.fakes;

import uk.gov.moj.cpp.sjp.query.view.converter.ResultCode;
import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeCachedReferenceData extends CachedReferenceData {

    private Map<UUID, String> withdrawalReasons = new HashMap<>();

    public FakeCachedReferenceData() {
        super(null, null);
    }

    @Override
    public String getWithdrawalReason(final UUID withdrawalReasonId) {
        return withdrawalReasons.get(withdrawalReasonId);
    }

    @Override
    public UUID getResultId(final String resultCode) {
        final Optional<UUID> result = Arrays.stream(ResultCode.values())
                .filter(rc -> rc.name().equalsIgnoreCase(resultCode))
                .findFirst()
                .map(ResultCode::getResultDefinitionId);

        return result.orElse(null);
    }

    public void addWithdrawalReason(final UUID withdrawReasonId, final String withdrawReason) {
        this.withdrawalReasons.put(withdrawReasonId, withdrawReason);
    }
}
