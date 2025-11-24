package uk.gov.moj.sjp.it.util;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

public class UrnProvider {

    public static String generate(final ProsecutingAuthority prosecutingAuthority) {
        return prosecutingAuthority.name() + RandomGenerator.integer(100000000, 999999999).next();
    }
}
