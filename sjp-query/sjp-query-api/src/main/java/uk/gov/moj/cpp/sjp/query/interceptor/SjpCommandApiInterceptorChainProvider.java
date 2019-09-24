package uk.gov.moj.cpp.sjp.query.interceptor;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;
import uk.gov.moj.cpp.authorisation.interceptor.SynchronousFeatureControlInterceptor;

import java.util.ArrayList;
import java.util.List;

public class SjpCommandApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    @Override
    public String component() {
        return QUERY_API;
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        final List<InterceptorChainEntry> interceptorChainEntries = new ArrayList<>();
        interceptorChainEntries.add(new InterceptorChainEntry(5900, SynchronousFeatureControlInterceptor.class));
        return interceptorChainEntries;
    }
}