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

import junit.framework.TestCase;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RootLogger;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;


/**
   Used for internal unit testing the Logger class.

   @author Ceki G&uuml;lc&uuml;

*/
public class LoggerTest extends TestCase {
  // A short message.
  static String MSG = "M";
  Logger logger;
  Appender a1;
  Appender a2;
  ResourceBundle rbUS;
  ResourceBundle rbFR;
  ResourceBundle rbCH;

  public LoggerTest(String name) {
    super(name);
  }

  public void setUp() {
    rbUS = ResourceBundle.getBundle("L7D", new Locale("en", "US"));
    assertNotNull(rbUS);

    rbFR = ResourceBundle.getBundle("L7D", new Locale("fr", "FR"));
    assertNotNull("Got a null resource bundle.", rbFR);

    rbCH = ResourceBundle.getBundle("L7D", new Locale("fr", "CH"));
    assertNotNull("Got a null resource bundle.", rbCH);
  }

  public void tearDown() {
    // Regular users should not use the clear method lightly!
    //Logger.getDefaultHierarchy().clear();
    BasicConfigurator.resetConfiguration();
    a1 = null;
    a2 = null;
  }

  /**
     Add an appender and see if it can be retrieved.
  */
  public void testAppender1() {
    logger = Logger.getLogger("test");
    a1 = new FileAppender();
    a1.setName("testAppender1");
    logger.addAppender(a1);

    Enumeration enumeration = logger.getAllAppenders();
    Appender aHat = (Appender) enumeration.nextElement();
    assertEquals(a1, aHat);
  }

  /**
     Add an appender X, Y, remove X and check if Y is the only
     remaining appender.
  */
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
  }

  /**
     Test if logger a.b inherits its appender from a.
   */
  public void testAdditivity1() {
    Logger a = Logger.getLogger("a");
    Logger ab = Logger.getLogger("a.b");
    CountingAppender ca = new CountingAppender();
    ca.activateOptions();
    a.addAppender(ca);

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
     Test multiple additivity.

   */
  public void testAdditivity2() {
    Logger a = Logger.getLogger("a");
    Logger ab = Logger.getLogger("a.b");
    Logger abc = Logger.getLogger("a.b.c");
    Logger x = Logger.getLogger("x");

    CountingAppender ca1 = new CountingAppender();
    ca1.activateOptions();
    CountingAppender ca2 = new CountingAppender();
    ca2.activateOptions();
    a.addAppender(ca1);
    abc.addAppender(ca2);

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
     Test additivity flag.

   */
  public void testAdditivity3() {
    Logger root = Logger.getRootLogger();
    Logger a = Logger.getLogger("a");
    Logger ab = Logger.getLogger("a.b");
    Logger abc = Logger.getLogger("a.b.c");
    Logger x = Logger.getLogger("x");

    CountingAppender caRoot = new CountingAppender();
    caRoot.activateOptions();
    CountingAppender caA = new CountingAppender();
    caA.activateOptions();
    CountingAppender caABC = new CountingAppender();
    caABC.activateOptions();
    
    root.addAppender(caRoot);
    a.addAppender(caA);
    abc.addAppender(caABC);

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

  public void testDisable1() {
    CountingAppender caRoot = new CountingAppender();
    caRoot.activateOptions();
    Logger root = Logger.getRootLogger();
    root.addAppender(caRoot);

    LoggerRepository h = LogManager.getLoggerRepository();

    //h.disableDebug();
    h.setThreshold((Level) Level.INFO);
    assertEquals(caRoot.counter, 0);

    root.trace(MSG);
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

    root.trace(MSG);
    assertEquals(caRoot.counter, 3);

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

    root.trace(MSG);
    assertEquals(caRoot.counter, 6);

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
  }

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

  public void testRB2() {
    Logger root = Logger.getRootLogger();
    root.setResourceBundle(rbUS);

    ResourceBundle t = root.getResourceBundle();
    assertSame(t, rbUS);

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

  public void testRB3() {
    Logger root = Logger.getRootLogger();
    root.setResourceBundle(rbUS);

    ResourceBundle t = root.getResourceBundle();
    assertSame(t, rbUS);

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

  public void testHierarchy1() {
    Hierarchy h = new Hierarchy(new RootLogger((Level) Level.ERROR));
    Logger a0 = h.getLogger("a");
    assertEquals("a", a0.getName());
    assertNull(a0.getLevel());
    assertSame(Level.ERROR, a0.getEffectiveLevel());

    Logger a1 = h.getLogger("a");
    assertSame(a0, a1);
  }


    /**
     * Tests logger.trace(Object).
     * @since 1.2.12
     */
    public void testTrace() {
        VectorAppender appender = new VectorAppender();
        appender.activateOptions();
        Logger root = Logger.getRootLogger();
        root.addAppender(appender);
        root.setLevel(Level.INFO);

        Logger tracer = Logger.getLogger("com.example.Tracer");
        tracer.setLevel(Level.TRACE);

        tracer.trace("Message 1");
        root.trace("Discarded Message");
        root.trace("Discarded Message");

        Vector msgs = appender.getVector();
        assertEquals(1, msgs.size());
        LoggingEvent event = (LoggingEvent) msgs.get(0);
        assertEquals(Level.TRACE, event.getLevel());
        assertEquals("Message 1", event.getMessage());
    }

      /**
       * Tests logger.trace(Object, Exception).
       * @since 1.2.12
       */
      public void testTraceWithException() {
          VectorAppender appender = new VectorAppender();
          appender.activateOptions();
          Logger root = Logger.getRootLogger();
          root.addAppender(appender);
          root.setLevel(Level.INFO);

          Logger tracer = Logger.getLogger("com.example.Tracer");
          tracer.setLevel(Level.TRACE);
          NullPointerException ex = new NullPointerException();

          tracer.trace("Message 1", ex);
          root.trace("Discarded Message", ex);
          root.trace("Discarded Message", ex);

          Vector msgs = appender.getVector();
          assertEquals(1, msgs.size());
          LoggingEvent event = (LoggingEvent) msgs.get(0);
          assertEquals(Level.TRACE, event.getLevel());
          assertEquals("Message 1", event.getMessage());
      }

      /**
       * Tests isTraceEnabled.
       * @since 1.2.12
       */
      public void testIsTraceEnabled() {
          VectorAppender appender = new VectorAppender();
          appender.activateOptions();
          Logger root = Logger.getRootLogger();
          root.addAppender(appender);
          root.setLevel(Level.INFO);

          Logger tracer = Logger.getLogger("com.example.Tracer");
          tracer.setLevel(Level.TRACE);

          assertTrue(tracer.isTraceEnabled());
          assertFalse(root.isTraceEnabled());
      }

  private static class CountingAppender extends AppenderSkeleton {
    int counter;

    CountingAppender() {
      super(true);
      counter = 0;
    }

    public void close() {
    }

    public void append(LoggingEvent event) {
      counter++;
    }

      /**
       * Gets whether appender requires a layout.
       * @return false
       */
    public boolean requiresLayout() {
        return false;
    }

  }
}

// End of class: LoggerTestCase.java
