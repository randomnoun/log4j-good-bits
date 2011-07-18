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
package org.apache.logging.log4j.jcl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContext;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class LogFactoryImpl extends LogFactory {

    private final Map<LoggerContext, ConcurrentMap<String, Log>> contextMap =
        Collections.synchronizedMap(new WeakHashMap<LoggerContext, ConcurrentMap<String, Log>>());

    private ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    public Log getInstance(String name) throws LogConfigurationException {
        LoggerContext context = PrivateManager.getContext();
        ConcurrentMap<String, Log> loggers = getLoggersMap();
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        org.apache.logging.log4j.Logger logger = PrivateManager.getLogger(name);
        if (logger instanceof AbstractLogger) {
            loggers.putIfAbsent(name, new Log4JLog((AbstractLogger) logger, name));
            return loggers.get(name);
        }
        throw new LogConfigurationException("SLF4J Adapter requires base logging system to extend Log4J AbstractLogger");
    }

    private ConcurrentMap<String, Log> getLoggersMap() {
        LoggerContext context = PrivateManager.getContext();
        synchronized (contextMap) {
            ConcurrentMap<String, Log> map = contextMap.get(context);
            if (map == null) {
                map = new ConcurrentHashMap<String, Log>();
                contextMap.put(context, map);
            }
            return map;
        }
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Override
    public Log getInstance(Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    /**
     * This method is supposed to clear all loggers. In this implementation it will clear all the logger
     * wrappers but the loggers managed by the underlying logger context will not be.
     */
    @Override
    public void release() {
        getLoggersMap().clear();
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    private static class PrivateManager extends LogManager {
        private static final String FQCN = LogFactory.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(String name) {
            return getLogger(FQCN, name);
        }
    }

}
