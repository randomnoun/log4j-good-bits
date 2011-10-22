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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ListAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RegexReplacementTest {
    private static final String CONFIG = "log4j-replace.xml";
    private static Configuration config;
    private static ListAppender app;
    private static ListAppender app2;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
            }
            if (entry.getKey().equals("List2")) {
                app2 = (ListAppender) entry.getValue();
            }
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
        ThreadContext.clear();
    }

    org.apache.logging.log4j.Logger logger = LogManager.getLogger("LoggerTest");
    org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("ReplacementTest");

    @Test
    public void testReplacement() {
        logger.error(this.getClass().getName());
        List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertTrue("Incorrect number of messages. Should be 1 is " + msgs.size(), msgs.size() == 1);
        assertTrue("Replacement failed", msgs.get(0).endsWith("/RegexReplacementTest\n"));
        app.clear();
        ThreadContext.put("MyKey", "Apache");
        logger.error("This is a test for ${ctx:MyKey}");
        msgs = app.getMessages();
        assertNotNull(msgs);
        assertTrue("Incorrect number of messages. Should be 1 is " + msgs.size(), msgs.size() == 1);
        assertEquals("LoggerTest This is a test for Apache\n", msgs.get(0));
        app.clear();

    }
     @Test
    public void testConverter() {
        logger2.error(this.getClass().getName());
        List<String> msgs = app2.getMessages();
        assertNotNull(msgs);
        assertTrue("Incorrect number of messages. Should be 1 is " + msgs.size(), msgs.size() == 1);
        assertTrue("Replacement failed", msgs.get(0).endsWith("/RegexReplacementTest\n"));
        app2.clear();
    }
}
