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
package org.apache.logging.log4j.core.appender.routing;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class JSONRoutingAppender2Test {
    private static final String CONFIG = "log4j-routing2.json";
    private static Configuration config;
    private static ListAppender app;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
                break;
            }
        }
        File file = new File("target/rolling1/rollingtest-Unknown.log");
        file.delete();
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
        File file = new File("target/rolling1/rollingtest-Unknown.log");
        file.delete();
    }

    @Test
    public void routingTest() {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        List<LogEvent> list = app.getEvents();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        msg = new StructuredDataMessage("Test", "This is a test", "Unknown");
        EventLogger.logEvent(msg);
        File file = new File("target/rolling1/rollingtest-Unknown.log");
        assertTrue("File was not created", file.exists());
    }
}
