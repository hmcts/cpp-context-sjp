package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

public class CaseReceivedToCase implements Converter<CaseReceived, CaseDetail> {

    private DefendantToDefendantDetails defendantToDefendantDetailsConverter = new DefendantToDefendantDetails(null);

    @Override
    public CaseDetail convert(CaseReceived event) {
        return new CaseDetail(event.getCaseId(),
                event.getUrn(),
                event.getProsecutingAuthority(),
                null,
                false,
                null,
                event.getCreatedOn(),
                defendantToDefendantDetailsConverter.convert(event.getDefendant()),
                event.getCosts(),
                event.getPostingDate());
    }

}
