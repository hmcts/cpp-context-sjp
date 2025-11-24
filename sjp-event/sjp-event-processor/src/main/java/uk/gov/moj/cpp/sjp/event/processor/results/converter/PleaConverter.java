package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.json.schemas.domains.sjp.results.Plea;

import java.util.UUID;

public class PleaConverter {

    public uk.gov.justice.core.courts.Plea getPlea(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence offence,
                                                   final UUID sessionId ) {
            return uk.gov.justice.core.courts.Plea.plea()
                    .withOffenceId(offence.getId())
                    .withPleaDate(offence.getPleaDate().format(DATE_FORMAT))
                    .withPleaValue(offence.getPlea().toString())
                    .withOriginatingHearingId(sessionId)
                    .build();
    }

    public uk.gov.justice.core.courts.Plea getApplicationPlea(final UUID applicationId,
                                                   final Plea plea,
                                                   final UUID sessionId ) {
        if (plea != null) {
            return uk.gov.justice.core.courts.Plea.plea()
                    .withApplicationId(applicationId)//Manadatory

                    .withPleaDate(plea.getPleaDate() != null ? plea.getPleaDate().toString() : null)//Manadatory
                    .withPleaValue(plea.getPleaType() != null ? plea.getPleaType().toString() : null)//Manadatory
                    .withOriginatingHearingId(sessionId)
                    .build();
        }
        return null;
    }
}
