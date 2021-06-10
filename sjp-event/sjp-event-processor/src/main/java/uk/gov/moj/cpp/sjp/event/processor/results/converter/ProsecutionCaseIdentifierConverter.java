package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.UUID.fromString;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ProsecutionCaseIdentifierConverter {

    private final ReferenceDataService referenceDataService;

    @Inject
    public ProsecutionCaseIdentifierConverter(final ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    public ProsecutionCaseIdentifier getProsecutionCaseIdentifier(final String prosecutingAuthority,
                                                                  final String caseUrn) {
        final ProsecutionCaseIdentifier.Builder prosecutionCaseIdentifierBuilder = ProsecutionCaseIdentifier.prosecutionCaseIdentifier();
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataBuilder().
                withName("referencedata.query.prosecutors")
                .withId(UUID.randomUUID())
                .build(), NULL);

        final JsonObject prosecutor = referenceDataService
                .getProsecutor(prosecutingAuthority, emptyEnvelope)
                .getJsonArray("prosecutors")
                .getJsonObject(0);

        if (prosecutor.getBoolean("policeFlag", false)) {
            prosecutionCaseIdentifierBuilder.withCaseURN(caseUrn);
        } else {
            prosecutionCaseIdentifierBuilder.withProsecutionAuthorityReference(caseUrn);
        }

        prosecutionCaseIdentifierBuilder.withProsecutionAuthorityId(fromString(prosecutor.getString("id")));
        prosecutionCaseIdentifierBuilder.withProsecutionAuthorityCode(prosecutor.getString("shortName", null));
        prosecutionCaseIdentifierBuilder.withProsecutionAuthorityName(prosecutor.getString("fullName", null));
        prosecutionCaseIdentifierBuilder.withProsecutionAuthorityOUCode(prosecutor.getString("oucode", null));

        return prosecutionCaseIdentifierBuilder.build();
    }
}
