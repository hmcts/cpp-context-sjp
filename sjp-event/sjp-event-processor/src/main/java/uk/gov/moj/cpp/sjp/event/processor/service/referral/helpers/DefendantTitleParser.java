package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class DefendantTitleParser {

  private static final List<String> VALID_TITLES = asList("MR",  "MS", "MISS", "MRS");
  private static final String DEFAULT_TITLE = "MR";

  private DefendantTitleParser(){

  }

  public static String parse(final String value) {

    if(isBlank(value)) {
      return DEFAULT_TITLE;
    }

    final String title = value.trim().toUpperCase();
    return VALID_TITLES.contains(title) ? title : DEFAULT_TITLE;
  }
}
