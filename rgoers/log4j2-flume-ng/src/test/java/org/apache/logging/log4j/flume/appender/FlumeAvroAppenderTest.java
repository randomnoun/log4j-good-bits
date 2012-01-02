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
package org.apache.logging.log4j.flume.appender;

import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.Context;
import org.apache.flume.Event;

import org.apache.flume.Transaction;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.conf.Configurables;
import org.apache.flume.lifecycle.LifecycleController;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.source.AvroSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class FlumeAvroAppenderTest {

    private static LoggerContext ctx;

    private static final int testServerPort = 12345;

    private AvroSource eventSource;
    private Channel channel;
    private Logger avroLogger;

    private String testPort;

    @BeforeClass
    public static void setupClass() {
        PluginManager.addPackage("org.apache.logging.log4j.flume");
        ctx = (LoggerContext) LogManager.getContext();
    }

    @AfterClass
    public static void cleanupClass() {

    }

    @Before
    public void setUp() throws Exception {
        eventSource = new AvroSource();
        channel = new MemoryChannel();

        Configurables.configure(channel, new Context());

        eventSource.setChannel(channel);

        avroLogger = (Logger) LogManager.getLogger("avrologger");
        /*
        * Clear out all other appenders associated with this logger to ensure we're
        * only hitting the Avro appender.
        */
        removeAppenders(avroLogger);
        boolean bound = false;

        for (int i = 0; i < 100 && !bound; i++) {
            try {
                Context context = new Context();
                testPort = String.valueOf(testServerPort + i);
                context.put("port", testPort);
                context.put("bind", "0.0.0.0");

                Configurables.configure(eventSource, context);

                eventSource.start();
                bound = true;
            } catch (ChannelException e) {

            }
        }
    	  Assert.assertTrue("Reached start or error", LifecycleController.waitForOneOf(
            eventSource, LifecycleState.START_OR_ERROR));
        Assert.assertEquals("Server is started", LifecycleState.START, eventSource.getLifecycleState());
    }

    @After
    public void teardown() throws Exception {
        removeAppenders(avroLogger);
        eventSource.stop();
	      Assert.assertTrue("Reached stop or error",
	           LifecycleController.waitForOneOf(eventSource, LifecycleState.STOP_OR_ERROR));
	      Assert.assertEquals("Server is stopped", LifecycleState.STOP,
	           eventSource.getLifecycleState());
    }

    @Test
    public void testLog4jAvroAppender() throws InterruptedException, IOException {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", testPort)};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", "1", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        avroLogger.info("Test message");

        Transaction transaction = channel.getTransaction();
        transaction.begin();

        Event event = channel.take();
   	    Assert.assertNotNull(event);
  	    Assert.assertTrue("Channel contained event, but not expected message",
            getBody(event).endsWith("Test message"));
	      transaction.commit();
	      transaction.close();

	      eventSource.stop();
    }


    @Test
    public void testMultiple() throws InterruptedException, IOException {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", testPort)};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", "1", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        for (int i = 0; i < 10; ++i) {
            avroLogger.info("Test message " + i);
        }

        for (int i = 0; i < 10; ++i) {
            Transaction transaction = channel.getTransaction();
            transaction.begin();

            Event event = channel.take();
   	        Assert.assertNotNull(event);
  	        Assert.assertTrue("Channel contained event, but not expected message",
                getBody(event).endsWith("Test message " + i));
	          transaction.commit();
	          transaction.close();
        }

	      eventSource.stop();
    }

     @Test
    public void testBatch() throws InterruptedException, IOException {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", testPort)};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", "10", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        for (int i = 0; i < 10; ++i) {
            avroLogger.info("Test message " + i);
        }

        Transaction transaction = channel.getTransaction();
        transaction.begin();

        for (int i = 0; i < 10; ++i) {
            Event event = channel.take();
   	        Assert.assertNotNull("No event for item " + i, event);
  	        Assert.assertTrue("Channel contained event, but not expected message",
                getBody(event).endsWith("Test message " + i));
        }
	      transaction.commit();
	      transaction.close();

	      eventSource.stop();
    }


    @Test
    public void testConnectionRefused() {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", testPort)};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", "1", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);
        eventSource.stop();

        boolean caughtException = false;

        try {
            avroLogger.info("message 1");
        } catch (Throwable t) {
            //logger.debug("Logging to a non-existant server failed (as expected)", t);

            caughtException = true;
        }

        Assert.assertTrue(caughtException);
    }



    @Test
    public void testReconnect() throws Exception {
        String altPort = Integer.toString(Integer.parseInt(testPort) + 1);
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", testPort),
                                      Agent.createAgent("localhost", altPort)};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", "1", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        avroLogger.info("Test message");

        Transaction transaction = channel.getTransaction();
        transaction.begin();

        Event event = channel.take();
   	    Assert.assertNotNull(event);
  	    Assert.assertTrue("Channel contained event, but not expected message",
            getBody(event).endsWith("Test message"));
	      transaction.commit();
	      transaction.close();

        eventSource.stop();
        try {
            Context context = new Context();
            context.put("port", altPort);
            context.put("bind", "0.0.0.0");

            Configurables.configure(eventSource, context);

            eventSource.start();
        } catch (ChannelException e) {
            Assert.fail("Caught exception while resetting port to " + altPort + " : " + e.getMessage());
        }

        avroLogger.info("Test message 2");

        transaction = channel.getTransaction();
        transaction.begin();

        event = channel.take();
   	    Assert.assertNotNull(event);
  	    Assert.assertTrue("Channel contained event, but not expected message",
            getBody(event).endsWith("Test message 2"));
	      transaction.commit();
	      transaction.close();
    }



    private void removeAppenders(Logger logger) {
        Map<String,Appender> map = logger.getAppenders();
        for (Map.Entry<String, Appender> entry : map.entrySet()) {
            Appender app = entry.getValue();
            avroLogger.removeAppender(app);
            app.stop();
        }
    }

    private Appender getAppender(Logger logger, String name) {
        Map<String,Appender> map = logger.getAppenders();
        return map.get(name);
    }

    private String getBody(Event event) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = new GZIPInputStream(new ByteArrayInputStream(event.getBody()));
            int n = 0;
            while (-1 != (n = is.read())) {
                baos.write(n);
            }
            return new String(baos.toByteArray());

    }
}
