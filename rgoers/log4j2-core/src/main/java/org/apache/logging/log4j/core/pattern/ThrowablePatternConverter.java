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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Outputs the Throwable portion of the LoggingEvent as a full stacktrace
 * unless this converter's option is 'short', where it just outputs the first line of the trace, or if
 * the number of lines to print is explicitly specified.
 */
@Plugin(name = "ThrowablePatternConverter", type = "Converter")
@ConverterKeys({"ex", "throwable", "exception" })
public class ThrowablePatternConverter extends LogEventPatternConverter {

    /**
     * Format the whole stack trace.
     */
    protected static final String FULL = "full";
    /**
     * Format only the first line of the throwable.
     */
    protected static final String SHORT = "short";
    /**
     * If "short", only first line of throwable report will be formatted.<br>
     * If "full", the whole stack trace will be formatted.<br>
     * If "numeric" the output will be limited to the specified number of lines.
     */
    protected final String option;

    /**
     * The number of lines to write.
     */
    protected final int lines;

    /**
     * Constructor.
     * @param name Name of converter.
     * @param style CSS style for output.
     * @param options options, may be null.
     */
    protected ThrowablePatternConverter(String name, String style, final String[] options) {
        super(name, style);
        int count = 0;
        if ((options != null) && (options.length > 0)) {
            option = options[0];
            if (option.equalsIgnoreCase(SHORT)) {
                count = 2;
            } else if (!option.equalsIgnoreCase(FULL)) {
                count = Integer.parseInt(option);
            }

        } else {
            option = null;
        }
        lines = count;
    }

    /**
     * Gets an instance of the class.
     *
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static ThrowablePatternConverter newInstance(
        final String[] options) {
        return new ThrowablePatternConverter("Throwable", "throwable", options);
    }

    /**
     * {@inheritDoc}
     */
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        Throwable t = event.getThrown();

        if (t != null) {
            StringWriter w = new StringWriter();
            t.printStackTrace(new PrintWriter(w));
            if (lines > 0) {
                StringBuilder sb = new StringBuilder();
                String[] array = w.toString().split("\n");
                for (int i = 0; i < lines; ++i) {
                    sb.append(array[i]).append("\n");
                }
                toAppendTo.append(sb.toString());

            } else {
                toAppendTo.append(w.toString());
            }
        }
    }

    /**
     * This converter obviously handles throwables.
     *
     * @return true.
     */
    public boolean handlesThrowable() {
        return true;
    }
}
