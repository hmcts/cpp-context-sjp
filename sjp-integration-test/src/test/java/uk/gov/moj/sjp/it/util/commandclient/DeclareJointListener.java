package uk.gov.moj.sjp.it.util.commandclient;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DeclareJointListener {
    String key();

    String[] events();

    String topic();
}
