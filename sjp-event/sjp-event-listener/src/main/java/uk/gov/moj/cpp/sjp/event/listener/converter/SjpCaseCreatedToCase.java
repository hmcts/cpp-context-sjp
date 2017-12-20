package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.stream.Collectors.toSet;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail.OffenceDetailBuilder;

import java.util.Set;
import java.util.UUID;

public class SjpCaseCreatedToCase implements Converter<SjpCaseCreated, CaseDetail> {

    @Override
    public CaseDetail convert(SjpCaseCreated event) {
        final DefendantDetail defendantDetail = createDefendantDetail(event);
        CaseDetail caseDetail = new CaseDetail(UUID.fromString(event.getId()),
                event.getUrn(),
                event.getProsecutingAuthority().toString(),
                event.getInitiationCode(),
                false,
                false,
                event.getCreatedOn());
        caseDetail.addDefendant(defendantDetail);
        defendantDetail.setNumPreviousConvictions(event.getNumPreviousConvictions()); // assuming there is just one defendant for now
        caseDetail.setSummonsCode(event.getSummonsCode());
        caseDetail.setLibraOriginatingOrg(event.getLibraOriginatingOrg());
        caseDetail.setPtiUrn(event.getPtiUrn());
        caseDetail.setCosts(event.getCosts());
        caseDetail.setPostingDate(event.getPostingDate());
        return caseDetail;
    }

    private DefendantDetail createDefendantDetail(SjpCaseCreated event) {
        return new DefendantDetail(event.getDefendantId(),
                UUID.fromString(event.getPersonId()),
                createOffenceDetails(event));
    }

    private Set<OffenceDetail> createOffenceDetails(SjpCaseCreated event) {
        return event.getOffences().stream()
                .map(offence -> new OffenceDetailBuilder()
                        .setId(offence.getId())
                        .setCode(offence.getLibraOffenceCode())
                        .setSequenceNumber(offence.getOffenceSequenceNo())
                        .setWording(offence.getOffenceWording())
                        .setStartDate(offence.getOffenceDate())
                        .setChargeDate(offence.getChargeDate())
                        .withLibraOffenceDateCode(offence.getLibraOffenceDateCode())
                        .withProsecutionFacts(offence.getProsecutionFacts())
                        .withWitnessStatement(offence.getWitnessStatement())
                        .withCompensation(offence.getCompensation())
                        .build())
                .collect(toSet());
    }
}
