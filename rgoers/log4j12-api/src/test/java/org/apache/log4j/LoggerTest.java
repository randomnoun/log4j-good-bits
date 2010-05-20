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

package org.apache.log4j;

import org.apache.logging.log4j.core.Appender;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AppenderBase;
import org.apache.logging.log4j.core.appender.ListAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import org.junit.Before;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

/**
 * Used for internal unit testing the Logger class.
 */
public class LoggerTest {

    Logger logger;
    Appender a1;
    Appender a2;

    static ResourceBundle rbUS;
    static ResourceBundle rbFR;
    static ResourceBundle rbCH;

    // A short message.
    static String MSG = "M";

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @BeforeClass
    public static void setUpClass() {
        rbUS = ResourceBundle.getBundle("L7D", new Locale("en", "US"));
        assertNotNull(rbUS);

        rbFR = ResourceBundle.getBundle("L7D", new Locale("fr", "FR"));
        assertNotNull("Got a null resource bundle.", rbFR);

        rbCH = ResourceBundle.getBundle("L7D", new Locale("fr", "CH"));
        assertNotNull("Got a null resource bundle.", rbCH);

        ConfigurationFactory.setConfigurationFactory(cf);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @After
    public void tearDown() {
        LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager.getContext();
        ctx.reconfigure();
        a1 = null;
        a2 = null;
    }

    /**
     * Add an appender and see if it can be retrieved.
     *  Skipping this test as the Appender interface isn't compatible with legacy Log4j.
    public void testAppender1() {
        logger = Logger.getLogger("test");
        a1 = new ListAppender("testAppender1");
        logger.addAppender(a1);

        Enumeration enumeration = logger.getAllAppenders();
        Appender aHat = (Appender) enumeration.nextElement();
        assertEquals(a1, aHat);
    } */

    /**
     * Add an appender X, Y, remove X and check if Y is the only
     * remaining appender.
     * Skipping this test as the Appender interface isn't compatible with legacy Log4j.
    public void testAppender2() {
        a1 = new FileAppender();
        a1.setName("testAppender2.1");
        a2 = new FileAppender();
        a2.setName("testAppender2.2");

        logger = Logger.getLogger("test");
        logger.addAppender(a1);
        logger.addAppender(a2);
        logger.removeAppender("testAppender2.1");
        Enumeration enumeration = logger.getAllAppenders();
        Appender aHat = (Appender) enumeration.nextElement();
        assertEquals(a2, aHat);
        assertTrue(!enumeration.hasMoreElements());
    }  */

    /**
     * Test if logger a.b inherits its appender from a.
     */
    @Test
    public void testAdditivity1() {
        Logger a = Logger.getLogger("a");
        Logger ab = Logger.getLogger("a.b");
        CountingAppender ca = new CountingAppender();
        a.getLogger().addAppender(ca);

        assertEquals(ca.counter, 0);
        ab.debug(MSG);
        assertEquals(ca.counter, 1);
        ab.info(MSG);
        assertEquals(ca.counter, 2);
        ab.warn(MSG);
        assertEquals(ca.counter, 3);
        ab.error(MSG);
        assertEquals(ca.counter, 4);
    }

    /**
     * Test multiple additivity.
     */
    @Test
    public void testAdditivity2() {

        Logger a = Logger.getLogger("a");
        Logger ab = Logger.getLogger("a.b");
        Logger abc = Logger.getLogger("a.b.c");
        Logger x = Logger.getLogger("x");

        CountingAppender ca1 = new CountingAppender();
        CountingAppender ca2 = new CountingAppender();

        a.getLogger().addAppender(ca1);
        abc.getLogger().addAppender(ca2);

        assertEquals(ca1.counter, 0);
        assertEquals(ca2.counter, 0);

        ab.debug(MSG);
        assertEquals(ca1.counter, 1);
        assertEquals(ca2.counter, 0);

        abc.debug(MSG);
        assertEquals(ca1.counter, 2);
        assertEquals(ca2.counter, 1);

        x.debug(MSG);
        assertEquals(ca1.counter, 2);
        assertEquals(ca2.counter, 1);
    }

    /**
     * Test additivity flag.
     */
    @Test
    public void testAdditivity3() {

        Logger root = Logger.getRootLogger();
        Logger a = Logger.getLogger("a");
        Logger ab = Logger.getLogger("a.b");
        Logger abc = Logger.getLogger("a.b.c");
        Logger x = Logger.getLogger("x");

        CountingAppender caRoot = new CountingAppender();
        CountingAppender caA = new CountingAppender();
        CountingAppender caABC = new CountingAppender();

        root.getLogger().addAppender(caRoot);
        a.getLogger().addAppender(caA);
        abc.getLogger().addAppender(caABC);

        assertEquals(caRoot.counter, 0);
        assertEquals(caA.counter, 0);
        assertEquals(caABC.counter, 0);

        ab.setAdditivity(false);


        a.debug(MSG);
        assertEquals(caRoot.counter, 1);
        assertEquals(caA.counter, 1);
        assertEquals(caABC.counter, 0);

        ab.debug(MSG);
        assertEquals(caRoot.counter, 1);
        assertEquals(caA.counter, 1);
        assertEquals(caABC.counter, 0);

        abc.debug(MSG);
        assertEquals(caRoot.counter, 1);
        assertEquals(caA.counter, 1);
        assertEquals(caABC.counter, 1);
    }

    /* Don't support getLoggerRepository
    public void testDisable1() {
        CountingAppender caRoot = new CountingAppender();
        Logger root = Logger.getRootLogger();
        root.getLogger().addAppender(caRoot);

        LoggerRepository h = LogManager.getLoggerRepository();
        //h.disableDebug();
        h.setThreshold((Level) Level.INFO);
        assertEquals(caRoot.counter, 0);

        root.debug(MSG);
        assertEquals(caRoot.counter, 0);
        root.info(MSG);
        assertEquals(caRoot.counter, 1);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 2);
        root.warn(MSG);
        assertEquals(caRoot.counter, 3);

        //h.disableInfo();
        h.setThreshold((Level) Level.WARN);
        root.debug(MSG);
        assertEquals(caRoot.counter, 3);
        root.info(MSG);
        assertEquals(caRoot.counter, 3);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 4);
        root.error(MSG);
        assertEquals(caRoot.counter, 5);
        root.log(Level.ERROR, MSG);
        assertEquals(caRoot.counter, 6);

        //h.disableAll();
        h.setThreshold(Level.OFF);
        root.debug(MSG);
        assertEquals(caRoot.counter, 6);
        root.info(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 6);
        root.error(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.FATAL, MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.FATAL, MSG);
        assertEquals(caRoot.counter, 6);

        //h.disable(Level.FATAL);
        h.setThreshold(Level.OFF);
        root.debug(MSG);
        assertEquals(caRoot.counter, 6);
        root.info(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.WARN, MSG);
        assertEquals(caRoot.counter, 6);
        root.error(MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.ERROR, MSG);
        assertEquals(caRoot.counter, 6);
        root.log(Level.FATAL, MSG);
        assertEquals(caRoot.counter, 6);
    }  */

    @Test
    public void testRB1() {
        Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertSame(t, rbUS);

        Logger x = Logger.getLogger("x");
        Logger x_y = Logger.getLogger("x.y");
        Logger x_y_z = Logger.getLogger("x.y.z");

        t = x.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y_z.getResourceBundle();
        assertSame(t, rbUS);
    }

    @Test
    public void testRB2() {
        Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertTrue(t == rbUS);

        Logger x = Logger.getLogger("x");
        Logger x_y = Logger.getLogger("x.y");
        Logger x_y_z = Logger.getLogger("x.y.z");

        x_y.setResourceBundle(rbFR);
        t = x.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y.getResourceBundle();
        assertSame(t, rbFR);
        t = x_y_z.getResourceBundle();
        assertSame(t, rbFR);
    }

    @Test
    public void testRB3() {
        Logger root = Logger.getRootLogger();
        root.setResourceBundle(rbUS);
        ResourceBundle t = root.getResourceBundle();
        assertTrue(t == rbUS);

        Logger x = Logger.getLogger("x");
        Logger x_y = Logger.getLogger("x.y");
        Logger x_y_z = Logger.getLogger("x.y.z");

        x_y.setResourceBundle(rbFR);
        x_y_z.setResourceBundle(rbCH);
        t = x.getResourceBundle();
        assertSame(t, rbUS);
        t = x_y.getResourceBundle();
        assertSame(t, rbFR);
        t = x_y_z.getResourceBundle();
        assertSame(t, rbCH);
    }

    @Test
    public void testExists() {
        Logger a = Logger.getLogger("a");
        Logger a_b = Logger.getLogger("a.b");
        Logger a_b_c = Logger.getLogger("a.b.c");

        Logger t;
        t = LogManager.exists("xx");
        assertNull(t);
        t = LogManager.exists("a");
        assertSame(a, t);
        t = LogManager.exists("a.b");
        assertSame(a_b, t);
        t = LogManager.exists("a.b.c");
        assertSame(a_b_c, t);
    }
    /* Don't support hierarchy
    public void testHierarchy1() {
        Hierarchy h = new Hierarchy(new RootLogger((Level) Level.ERROR));
        Logger a0 = h.getLogger("a");
        assertEquals("a", a0.getName());
        assertNull(a0.getLevel());
        assertSame(Level.ERROR, a0.getEffectiveLevel());

        Logger a1 = h.getLogger("a");
        assertSame(a0, a1);
    } */

    /**
     * Tests logger.trace(Object).
     */
    @Test
    public void testTrace() {
        ListAppender appender = new ListAppender("List");
        appender.start();
        Logger root = Logger.getRootLogger();
        root.getLogger().addAppender(appender);
        root.setLevel(Level.INFO);

        Logger tracer = Logger.getLogger("com.example.Tracer");
        tracer.setLevel(Level.TRACE);

        tracer.trace("Message 1");
        root.trace("Discarded Message");
        root.trace("Discarded Message");

        List<LogEvent> msgs = appender.getEvents();
        assertEquals(1, msgs.size());
        LogEvent event = (LogEvent) msgs.get(0);
        assertEquals(org.apache.logging.log4j.Level.TRACE, event.getLevel());
        assertEquals("Message 1", event.getMessage().getMessageFormat());
    }

    /**
     * Tests logger.trace(Object, Exception).
     */
    @Test
    public void testTraceWithException() {
        ListAppender appender = new ListAppender("List");
        Logger root = Logger.getRootLogger();
        root.getLogger().addAppender(appender);
        root.setLevel(Level.INFO);

        Logger tracer = Logger.getLogger("com.example.Tracer");
        tracer.setLevel(Level.TRACE);
        NullPointerException ex = new NullPointerException();

        tracer.trace("Message 1", ex);
        root.trace("Discarded Message", ex);
        root.trace("Discarded Message", ex);

        List<LogEvent> msgs = appender.getEvents();
        assertEquals(1, msgs.size());
        LogEvent event = msgs.get(0);
        assertEquals(org.apache.logging.log4j.Level.TRACE, event.getLevel());
        assertEquals("Message 1", event.getMessage().getFormattedMessage());
    }

    /**
     * Tests isTraceEnabled.
     */
    @Test
    public void testIsTraceEnabled() {
        ListAppender appender = new ListAppender("List");
        Logger root = Logger.getRootLogger();
        root.getLogger().addAppender(appender);
        root.setLevel(Level.INFO);

        Logger tracer = Logger.getLogger("com.example.Tracer");
        tracer.setLevel(Level.TRACE);

        assertTrue(tracer.isTraceEnabled());
        assertFalse(root.isTraceEnabled());
    }

    private static class CountingAppender extends AppenderBase {

        int counter;

        CountingAppender() {
            super("Counter", null);
            counter = 0;
        }

        public void close() {
        }

        public void append(LogEvent event) {
            counter++;
        }

        public boolean requiresLayout() {
            return true;
        }
    }
}

