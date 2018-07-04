package uk.gov.moj.cpp.sjp.domain;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    ENGLISH,
    WELSH;

    private static final Map<Character, Language> CODES = Arrays.stream(Language.values())
            .collect(toMap(Language::serialize, Function.identity()));

    @JsonCreator
    private static Language deserialize(char code) {
        return CODES.get(code);
    }

    @JsonValue
    private Character serialize() {
        return this.name().charAt(0);
    }

}
