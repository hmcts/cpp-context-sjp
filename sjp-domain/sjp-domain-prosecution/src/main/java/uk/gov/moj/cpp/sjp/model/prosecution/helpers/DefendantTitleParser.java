package uk.gov.moj.cpp.sjp.model.prosecution.helpers;

import static org.apache.commons.lang3.StringUtils.isBlank;

public final class DefendantTitleParser {

  private DefendantTitleParser(){

  }

  public static String parse(final String value) {

    if(isBlank(value)) {
      return null;
    }

    return value.trim().toUpperCase();
  }
}
