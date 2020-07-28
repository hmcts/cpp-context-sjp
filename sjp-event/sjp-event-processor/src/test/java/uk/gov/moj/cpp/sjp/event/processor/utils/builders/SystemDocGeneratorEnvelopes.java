package uk.gov.moj.cpp.sjp.event.processor.utils.builders;

public class SystemDocGeneratorEnvelopes {
    public static GenerationFailedEventEnvelopeBuilder generationFailedEvent() {
        return new GenerationFailedEventEnvelopeBuilder();
    }
}
