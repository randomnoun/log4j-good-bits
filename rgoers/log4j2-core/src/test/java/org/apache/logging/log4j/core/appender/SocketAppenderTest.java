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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class SocketAppenderTest {

    private static final String HOST = "localhost";
    private static final String PORT = "8199";
    private static final int PORTNUM = Integer.parseInt(PORT);

    private static BlockingQueue<LogEvent> list = new ArrayBlockingQueue<LogEvent>(10);

    private static TCPSocketServer tcp;
    private static UDPSocketServer udp;

    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("SocketAppenderTest");

    private static int tcpCount = 0;
    private static int udpCount = 0;

    @BeforeClass
    public static void setupClass() throws Exception {
        tcp = new TCPSocketServer();
        tcp.start();
        udp = new UDPSocketServer();
        udp.start();
        ((LoggerContext) LogManager.getContext()).reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        tcp.shutdown();
        udp.shutdown();
    }

    @After
    public void teardown() {
        Map<String,Appender> map = root.getAppenders();
        for (Map.Entry<String, Appender> entry : map.entrySet()) {
            Appender app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
        tcpCount = 0;
        udpCount = 0;
    }

    @Test
    public void testTCPAppender() throws Exception {

        SocketAppender appender = SocketAppender.createAppender("localhost", PORT, "tcp", "-1",
            "Test", null, null, null, null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        LogEvent event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Message not delivered via TCP", tcpCount > 0);
    }


    @Test
    public void testUDPAppender() throws Exception {

        SocketAppender appender = SocketAppender.createAppender("localhost", PORT, "udp", "-1",
            "Test", null, null, null, null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        LogEvent event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Message not delivered via UDP", udpCount > 0);
    }

    public static class UDPSocketServer extends Thread {
        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;

        public UDPSocketServer() throws IOException {
            this.sock = new DatagramSocket(PORTNUM);
        }

        public void shutdown() {
            this.shutdown = true;
            thread.interrupt();
        }

        public void run() {
            this.thread = Thread.currentThread();
            byte[] bytes = new byte[4096];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            try {
                while (!shutdown) {
                    sock.receive(packet);
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
                    ++udpCount;
                    list.add((LogEvent) ois.readObject());
                }
            } catch (Exception ex) {
                if (!shutdown) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public static class TCPSocketServer extends Thread {

        private final ServerSocket sock;
        private boolean shutdown = false;

        public TCPSocketServer() throws IOException {
            this.sock = new ServerSocket(PORTNUM);
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
        }

        public void run() {
            try {
                Socket socket = sock.accept();
                if (socket != null) {
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    while (!shutdown) {
                        list.add((LogEvent) ois.readObject());
                        ++tcpCount;
                    }
                }
            } catch (EOFException eof) {
                // Socket is closed.
            } catch (Exception ex) {
                if (!shutdown) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

}
