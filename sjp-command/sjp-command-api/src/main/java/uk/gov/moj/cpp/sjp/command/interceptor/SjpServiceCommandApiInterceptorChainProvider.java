package uk.gov.moj.cpp.sjp.command.interceptor;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;

import java.util.ArrayList;
import java.util.List;

public class SjpServiceCommandApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    @Override
    public String component() {
        return COMMAND_API;
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        final List<InterceptorChainEntry> interceptorChainEntries = new ArrayList<>();
        interceptorChainEntries.add(new InterceptorChainEntry(6000, SjpServiceFileInterceptor.class));
        return interceptorChainEntries;
    }
}