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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ConsoleAppender;

/**
 *
 */
public class DefaultConfiguration extends BaseConfiguration {

    private static final String CONSOLE = "CONSOLE";;
    private static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";
    private static final String EMPTY_STRING = "";
    public static final String DEFAULT_NAME = "Default";

    public DefaultConfiguration() {

        setName(DEFAULT_NAME);
        Appender appender = new ConsoleAppender("Console", new BasicLayout());
        addAppender(appender);
        LoggerConfig root = getRootLogger();
        root.addAppender(appender);
        String l = System.getProperty(DEFAULT_LEVEL);
        Level level = (l != null && Level.valueOf(l) != null) ? Level.valueOf(l) : Level.ERROR;
        root.setLevel(level);
    }

    public class BasicLayout implements Layout<String> {
        public byte[] format(LogEvent event) {
            return formatAs(event).getBytes();
        }

        public String formatAs(LogEvent event) {
            return event.getMessage().getFormattedMessage() + "\n";
        }

        public byte[] getHeader() {
            return EMPTY_STRING.getBytes();
        }

        public byte[] getFooter() {
            return EMPTY_STRING.getBytes();
        }
    }
}
