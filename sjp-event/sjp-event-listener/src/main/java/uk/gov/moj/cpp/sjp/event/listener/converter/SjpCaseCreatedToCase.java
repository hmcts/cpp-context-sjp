package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.stream.Collectors.toSet;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.util.UUID;

/**
 * @deprecated Replaced by {@link CaseReceivedToCase}
 */
public class SjpCaseCreatedToCase implements Converter<SjpCaseCreated, CaseDetail> {

    private OffenceToOffenceDetail offenceToOffenceDetailConverter = new OffenceToOffenceDetail();

    @Override
    public CaseDetail convert(SjpCaseCreated event) {
        return new CaseDetail(UUID.fromString(event.getId()),
                event.getUrn(),
                event.getProsecutingAuthority().toString(),
                event.getInitiationCode(),
                false,
                false,
                event.getCreatedOn(), createDefendantDetail(event), event.getCosts(), event.getPostingDate());
    }

    private DefendantDetail createDefendantDetail(SjpCaseCreated sjpCaseCreated) {
        return new DefendantDetail(
                sjpCaseCreated.getDefendantId(),
                null,
                sjpCaseCreated.getOffences().stream().map(offenceToOffenceDetailConverter::convert).collect(toSet()),
                sjpCaseCreated.getNumPreviousConvictions());
    }

}
