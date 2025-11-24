package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Objects.isNull;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import javax.inject.Inject;

public class PersonDefendantConverter {

    private static final String DEFAULT_BAIL_STATUS = "A";

    private PersonDetailsConverter personDetailsConverter;

    private JCachedReferenceData jCachedReferenceData;

    @Inject
    public PersonDefendantConverter(final PersonDetailsConverter personDetailsConverter,
                                    final JCachedReferenceData jCachedReferenceData) {
        this.personDetailsConverter = personDetailsConverter;
        this.jCachedReferenceData = jCachedReferenceData;
    }

    public PersonDefendant getPersonDefendant(final Defendant defendant,
                                              final String countryCJSCode,
                                              final Metadata metadata) {
        if (isNull(defendant.getPersonalDetails())) {
            return null;
        }
        final BailStatus bailStatus = jCachedReferenceData.getBailStatus(DEFAULT_BAIL_STATUS, envelopeFrom(metadata, null));

        return personDefendant()
                .withBailStatus(bailStatus)
                .withPersonDetails(personDetailsConverter.getPersonDetails(defendant.getPersonalDetails(), countryCJSCode))
                .build();
    }
}
