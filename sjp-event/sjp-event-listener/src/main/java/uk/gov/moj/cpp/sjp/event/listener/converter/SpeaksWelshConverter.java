package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.justice.services.common.converter.Converter;

import java.util.Optional;

public class SpeaksWelshConverter implements Converter<Language, Boolean> {

    @Override
    public Boolean convert(Language language) {
        return Optional.ofNullable(language)
                .map(c -> language.equals(Language.W))
                .orElse(null);
    }
}
