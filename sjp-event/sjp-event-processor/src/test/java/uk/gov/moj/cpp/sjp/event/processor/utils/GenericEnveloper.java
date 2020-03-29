package uk.gov.moj.cpp.sjp.event.processor.utils;

import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

public class GenericEnveloper {

    public GenericEnveloper() {
        /* helper class with no properties  */
    }

    public <T> Envelope<T> envelopeWithNewActionName(final T payload, final Metadata metadata, final String actionName) {
        return Envelope.envelopeFrom(metadataWithNewActionName(metadata, actionName), payload);
    }


}