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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @doubt All the isEnabled methods could be pushed into a filter interface.  Not sure of the utility of having
 * isEnabled be able to examine the message pattern and parameters. (RG) Moving the isEnabled methods out of
 * Logger noticeably impacts performance. The message pattern and parameters are required so that they can be
 * used in global filters.
 */
public class Logger extends AbstractLogger {

    private final String name;

    private final LoggerContext context;

    /**
     * config should be consistent across threads.
     */
    protected volatile PrivateConfig config;

    protected Logger(LoggerContext context, String name) {
        this.context = context;
        this.name = name;
        config = new PrivateConfig(context.getConfiguration(), this);
    }

    public String getName() {
        return name;
    }

    /**
     * Return the parent of this Logger. If it doesn't already exist return a temporary Logger.
     * @return The parent Logger.
     */
    public Logger getParent() {
        LoggerConfig lc = config.loggerConfig.getParent();
        if (lc == null) {
            return null;
        }
        if (context.hasLogger(lc.getName())) {
            return context.getLogger(name);
        }
        return new Logger(context, name);
    }

    public LoggerContext getContext() {
        return context;
    }

    public synchronized void setLevel(Level level) {
        if (level != null) {
            config = new PrivateConfig(config, level);
        }
    }

    public Level getLevel() {
        return config.level;
    }

    @Override
    protected void log(Marker marker, String fqcn, Level level, Message data, Throwable t) {
        config.loggerConfig.log(name, marker, fqcn, level, data, t);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg) {
        return config.filter(level, marker, msg);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Throwable t) {
        return config.filter(level, marker, msg, t);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1) {
        return config.filter(level, marker, msg, p1);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2) {
        return config.filter(level, marker, msg, p1, p2);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2, Object p3) {
        return config.filter(level, marker, msg, p1, p2, p3);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2, Object p3,
                                Object... params) {
        return config.filter(level, marker, msg, p1, p2, p3, params);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Object msg, Throwable t) {
        return config.filter(level, marker, msg, t);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Message msg, Throwable t) {
        return config.filter(level, marker, msg, t);
    }

    public void addAppender(Appender appender) {
        config.config.addLoggerAppender(this, appender);
    }

    public void removeAppender(Appender appender) {
        config.loggerConfig.removeAppender(appender.getName());
    }

    public Map<String, Appender> getAppenders() {
         return config.loggerConfig.getAppenders();
    }

    public Iterator<Filter> getFilters() {
        return config.loggerConfig.getFilters();
    }

    public int filterCount() {
        return config.loggerConfig.filterCount();
    }

    public void addFilter(Filter filter) {
        config.config.addLoggerFilter(this, filter);
    }

    public boolean isAdditive() {
        return config.loggerConfig.isAdditive();
    }

    public void setAdditive(boolean additive) {
        config.config.setLoggerAdditive(this, additive);
    }

    /**
     * There are two ways that could be used to guarantee all threads are aware of changes to
     * config. 1. synchronize this method. Accessors don't need to be synchronized as Java wil
     * treat all variables within a synchronized block as volatile. 2. Declare the variable
     * volatile. Option 2 is used here as the performance cost is very low and it does a better
     * job at documenting how it is used.
     *
     * @param config The new Configuration.
     */
    void updateConfiguration(Configuration config) {
        this.config = new PrivateConfig(config, this);
    }

    /**
     * The binding between a Logger and its configuration.
     */
    protected class PrivateConfig {
        private final LoggerConfig loggerConfig;
        private final Configuration config;
        private final Level level;
        private final int intLevel;
        private final Logger logger;

        public PrivateConfig(Configuration config, Logger logger) {
            this.config = config;
            this.loggerConfig = config.getLoggerConfig(name);
            this.level = this.loggerConfig.getLevel();
            this.intLevel = this.level.intLevel();
            this.logger = logger;
        }

        public PrivateConfig(PrivateConfig pc, Level level) {
            this.config = pc.config;
            this.loggerConfig = pc.loggerConfig;
            this.level = level;
            this.intLevel = this.level.intLevel();
            this.logger = pc.logger;
        }

        public PrivateConfig(PrivateConfig pc, LoggerConfig lc) {
            this.config = pc.config;
            this.loggerConfig = lc;
            this.level = lc.getLevel();
            this.intLevel = this.level.intLevel();
            this.logger = pc.logger;
        }

        boolean filter(Level level, Marker marker, String msg) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, String msg, Throwable t) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, t);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, String msg, Object p1) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, String msg, Object p1, Object p2) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1, p2);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, String msg, Object p1, Object p2, Object p3) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1, p2, p3);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, String msg, Object p1, Object p2, Object p3,
                       Object... params) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1, p2, p3, params);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, Object msg, Throwable t) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, t);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }

        boolean filter(Level level, Marker marker, Message msg, Throwable t) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, t);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return intLevel >= level.intLevel();
        }
    }
}
