package com.techstore.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class CriticalMarkerFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getMarkerList() != null
                && event.getMarkerList().stream().anyMatch(m -> "CRITICAL".equals(m.getName()))) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }
}
