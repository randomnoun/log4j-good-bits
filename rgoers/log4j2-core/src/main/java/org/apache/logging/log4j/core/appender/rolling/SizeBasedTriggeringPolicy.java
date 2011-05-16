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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.internal.StatusLogger;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
@Plugin(name = "SizeBasedTriggeringPolicy", type = "Core", printObject = true)
public class SizeBasedTriggeringPolicy implements TriggeringPolicy {

    protected static final Logger logger = StatusLogger.getLogger();

    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * MB;

    /**
     * Rollover threshold size in bytes.
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // let 10 MB the default max size


    /**
     * Pattern for string parsing.
     */
    private static final Pattern valuePattern =
        Pattern.compile("([0-9]+([\\.,][0-9]+)?)\\s*(|K|M|G)B?", Pattern.CASE_INSENSITIVE);

    private final long maxFileSize;

    private RollingFileManager manager;

    /**
     * Constructs a new instance.
     */
    public SizeBasedTriggeringPolicy() {
        this.maxFileSize = MAX_FILE_SIZE;
    }

    public void initialize(RollingFileManager manager) {
        this.manager = manager;
    }

    /**
     * Constructs a new instance.
     *
     * @param maxFileSize rollover threshold size in bytes.
     */
    public SizeBasedTriggeringPolicy(final long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isTriggeringEvent(LogEvent event) {
        return manager.getFileSize() > maxFileSize;
    }

    public String toString() {
        return "SizeBasedTriggeringPolicy(size=" + maxFileSize +")";
    }

    @PluginFactory
    public static SizeBasedTriggeringPolicy createPolicy(@PluginAttr("size") String size) {

        long maxSize = size == null ? MAX_FILE_SIZE : valueOf(size);
        return new SizeBasedTriggeringPolicy(maxSize);
    }

    /**
     * Converts a string to a number of bytes. Strings consist of a floating point value followed by
     * K, M, or G for kilobytes, megabytes, gigabytes, respectively. The
     * abbreviations KB, MB, and GB are also accepted. Matching is case insensitive.
     *
     * @param string The string to convert
     * @return The Bytes value for the string
     */
    private static long valueOf(final String string){
        final Matcher matcher = valuePattern.matcher(string);

        // Valid input?
        if (matcher.matches()) {
            try {
                // Get double precision value
                final long value = NumberFormat.getNumberInstance(Locale.getDefault()).parse(
                    matcher.group(1)).longValue();

                // Get units specified
                final String units = matcher.group(3);

                if (units.equalsIgnoreCase("")) {
                    return value;
                } else if (units.equalsIgnoreCase("K")) {
                    return value * KB;
                } else if (units.equalsIgnoreCase("M")) {
                    return value * MB;
                } else if (units.equalsIgnoreCase("G")) {
                    return value * GB;
                } else {
                    logger.error("Units not recognized: " + string);
                    return MAX_FILE_SIZE;
                }
            } catch (ParseException e) {
                logger.error("Unable to parse numeric part: " + string, e);
                return MAX_FILE_SIZE;
            }
        } else {
            logger.error("Unable to parse bytes: " + string);
            return MAX_FILE_SIZE;
        }
    }
}
