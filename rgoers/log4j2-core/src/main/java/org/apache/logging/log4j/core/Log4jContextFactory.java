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

import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.spi.LoggerContextFactory;

/**
 *
 */
public class Log4jContextFactory implements LoggerContextFactory {

    private ContextSelector selector;

    private StatusLogger logger = StatusLogger.getLogger();

    public Log4jContextFactory() {
        String sel = System.getProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        if (sel != null) {
            try {
                Class clazz = Loader.loadClass(sel);
                if (clazz != null && ContextSelector.class.isAssignableFrom(clazz)) {
                    selector = (ContextSelector) clazz.newInstance();
                    return;
                }
            } catch (Exception ex) {
                logger.error("Unable to create context " + sel, ex);
            }

        }
        selector = new ClassLoaderContextSelector();
    }

    public ContextSelector getSelector() {
        return selector;
    }

    public LoggerContext getContext(String fqcn, boolean currentContext) {
        return selector.getContext(fqcn, currentContext);

    }
}
