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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractServer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.XMLConfiguration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class SocketServer extends AbstractServer implements Runnable {

    private static Logger logger;

    private boolean isActive = true;

    private ServerSocket server;

    private ConcurrentMap<Long, SocketHandler> handlers = new ConcurrentHashMap<Long, SocketHandler>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Incorrect number of arguments");
            printUsage();
            return;
        }
        int port = Integer.parseInt(args[0]);
        if (port <= 0 || port > 65535) {
            System.err.println("Invalid port number");
            printUsage();
            return;
        }
        if (args.length == 2 && args[1].length() > 0) {
            ConfigurationFactory.setConfigurationFactory(new ServerConfigurationFactory(args[1]));
        }
        logger = LogManager.getLogger(SocketServer.class.getName());
        SocketServer sserver = new SocketServer(port);
        Thread server = new Thread(sserver);
        server.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = reader.readLine();
            if (line.equalsIgnoreCase("Quit") || line.equalsIgnoreCase("Stop") || line.equalsIgnoreCase("Exit")) {
                sserver.shutdown();
                server.join();
                break;
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: ServerSocket port configFilePath");
    }

    public SocketServer(int port) throws IOException {
        server = new ServerSocket(port);
        if (logger == null) {
            logger = LogManager.getLogger(getClass().getName());
        }
    }

    public void shutdown() {
        this.isActive = false;
        Thread.currentThread().interrupt();
    }

    public void run() {
        while(isActive)
        {
            try
            {
                // Accept incoming connections.
                Socket clientSocket = server.accept();

                // accept() will block until a client connects to the server.
                // If execution reaches this point, then it means that a client
                // socket has been accepted.

                SocketHandler handler = new SocketHandler(clientSocket);
                handlers.put(handler.getId(), handler);
                handler.start();
            }
            catch(IOException ioe)
            {
                System.out.println("Exception encountered on accept. Ignoring. Stack Trace :");
                ioe.printStackTrace();
            }
        }
        for (Map.Entry<Long, SocketHandler> entry : handlers.entrySet()) {
            SocketHandler handler = entry.getValue();
            handler.shutdown();
            try {
                handler.join();
            } catch (InterruptedException ie) {
                // Ignore the exception
            }
        }
    }

    private class SocketHandler extends Thread {
        private final ObjectInputStream ois;

        private boolean shutdown = false;

        public SocketHandler(Socket socket) throws IOException {

            ois = new ObjectInputStream(socket.getInputStream());
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
        }

        public void run() {
            boolean closed = false;
            try {
                try {
                    while(!shutdown) {
                        LogEvent event = (LogEvent) ois.readObject();
                        if (event != null) {
                            log(event);
                        }
                    }
                } catch (EOFException eof) {
                    closed = true;
                } catch (OptionalDataException opt) {
                    logger.error("OptionalDataException eof=" + opt.eof + " length=" + opt.length, opt);
                } catch (ClassNotFoundException cnfe) {
                    logger.error("Unable to locate LogEvent class", cnfe);
                } catch (IOException ioe) {
                    logger.error("IOException encountered while reading from socket", ioe);
                }
                if (!closed) {
                    try {
                        ois.close();
                    } catch (Exception ex) {
                        // Ignore the exception;
                    }
                }
            } finally {
                handlers.remove(getId());
            }
        }
    }

    private static class ServerConfigurationFactory extends XMLConfigurationFactory {

        private final String path;

        public ServerConfigurationFactory(String path) {
            this.path = path;
        }

        @Override
        public Configuration getConfiguration() {
            if (path != null && path.length() > 0) {
                InputSource source = null;
                try {
                    FileInputStream is = new FileInputStream(path);
                    source = new InputSource(is);
                    source.setSystemId(path);
                } catch (FileNotFoundException ex) {
                    // Ignore this error
                }
                if (source == null) {
                    try {
                        URL url = new URL(path);
                        source = new InputSource(url.openStream());
                        source.setSystemId(path);
                    } catch (MalformedURLException mue) {
                        // Ignore this error
                    } catch (IOException ioe) {
                        // Ignore this error
                    }
                }

                try {
                    if (source != null) {
                        return new XMLConfiguration(source);
                    }
                } catch (Exception ex) {
                    // Ignore this error.
                }
                System.err.println("Unable to process configuration at " + path + ", using default.");
            }
            return super.getConfiguration();
        }
    }
}
