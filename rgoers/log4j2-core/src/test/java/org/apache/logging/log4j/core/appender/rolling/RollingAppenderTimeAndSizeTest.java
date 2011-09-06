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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RollingAppenderTimeAndSizeTest {

    private static final String CONFIG = "log4j-rolling3.xml";
    private static final String DIR = "target/rolling3";

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(RollingAppenderTimeAndSizeTest.class.getName());

    @BeforeClass
    public static void setupClass() {
        deleteDir();
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        deleteDir();
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testAppender() throws Exception {
        for (int i=0; i < 100; ++i) {
            if (i % 11 == 0) {
                Thread.sleep(1000);
            }
            logger.debug("This is test message number " + i);
        }
        File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        File[] files = dir.listFiles();
        assertTrue("No files created", files.length > 0);
        boolean found = false;
        for (File file : files) {
            if (file.getName().endsWith(".gz")) {
                found = true;
            }
        }
        assertTrue("No compressed files found", found);
    }

    private static void deleteDir() {
        File dir = new File(DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }
}
