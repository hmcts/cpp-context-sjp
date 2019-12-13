package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import javax.inject.Inject;

public class DefendantToDefendantDetails implements Converter<Defendant, DefendantDetail> {

    @Inject
    private PersonToPersonalDetailsEntity<Defendant> personToPersonalDetailsEntity;

    @Inject
    private OffenceToOffenceDetail offenceToOffenceDetailConverter;

    @Inject
    private SpeaksWelshConverter speaksWelshConverter;

    @Override
    public DefendantDetail convert(final Defendant defendant) {
        final PersonalDetails personalDetails = personToPersonalDetailsEntity.convert(defendant);

        return new DefendantDetail(
                defendant.getId(),
                personalDetails,
                defendant.getOffences().stream().map(offenceToOffenceDetailConverter::convert).collect(toList()),
                defendant.getNumPreviousConvictions(),
                speaksWelshConverter.convert(defendant.getHearingLanguage()));
    }

}