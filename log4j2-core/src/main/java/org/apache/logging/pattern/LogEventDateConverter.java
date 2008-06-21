/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.pattern;

import org.apache.logging.core.LogEvent;
import org.apache.logging.core.*;
import java.text.DateFormat;
import java.util.Locale;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @PatternSpecifier("d", "date")
 */
public final class LogEventDateConverter<T extends Appendable>
        implements Converter<T> {

    private final DateFormat format;

    /**
     * Create new instance.
     */
    public LogEventDateConverter(final DateFormat fmt) {
        if (fmt == null) {
            throw new NullPointerException("fmt");
        }
        format = fmt;
    }

    /**
     * {@inheritDoc}
     */
    public Object extract(final LogEvent record) {
        return record.getMillis();
    }


    /**
     * {@inheritDoc}
     */
    public void render(Object extract, Locale locale, T destination)
        throws IOException {
        destination.append(format.format(
                new Date(((Long) extract).longValue())));
    }

    /**
     * {@inheritDoc}
     */
    public void format(LogEvent record, Locale locale, T destination)
        throws IOException {
        render(extract(record), locale, destination);
    }

}
