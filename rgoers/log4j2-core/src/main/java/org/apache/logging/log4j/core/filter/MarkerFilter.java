/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

/**
 * This filter returns the onMatch result if the marker in the LogEvent is the same as or has the
 * configured marker as a parent.
 *
 */
@Plugin(name = "MarkerFilter", type = "Core", elementType = "filter", printObject = true)
public final class MarkerFilter extends FilterBase {

    private final Marker marker;

    private MarkerFilter(Marker marker, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.marker = marker;
    }

    public Result filter(Logger logger, Level level, Marker marker, String msg, Object[] params) {
        return filter(marker);
    }

    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(marker);
    }

    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(marker);
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getMarker());
    }

    private Result filter(Marker marker) {
        return marker != null && marker.isInstanceOf(this.marker) ? onMatch : onMismatch;
    }

    @Override
    public String toString() {
        return marker.getName();
    }

    /**
     * Create the MarkerFilter.
     * @param marker The Marker to match.
     * @param match The action to take if a match occurs.
     * @param mismatch The action to take if no match occurs.
     * @return A MarkerFilter.
     */
    @PluginFactory
    public static MarkerFilter createFilter(@PluginAttr("marker") String marker,
                                            @PluginAttr("onMatch") String match,
                                            @PluginAttr("onMismatch") String mismatch) {

        if (marker == null) {
            LOGGER.error("A marker must be provided for MarkerFilter");
            return null;
        }
        Marker m = MarkerManager.getMarker(marker);
        Result onMatch = match == null ? null : Result.valueOf(match);
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch);

        return new MarkerFilter(m, onMatch, onMismatch);
    }

}
