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
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.After;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class FileAppenderTest {

    private static final String FILENAME = "target/fileAppenderTest.log";
    private static final int THREADS = 2;

    @BeforeClass
    public static void setupClass() {
        deleteFile();
    }

    @AfterClass
    public static void cleanupClass() {
        deleteFile();
        assertTrue("Manager for " + FILENAME + " not removed", !OutputStreamManager.hasManager(FILENAME));
    }

    @After
    public void teardown() {
        deleteFile();
    }

    @Test
    public void testAppender() throws Exception {
        writer(false, 1, "test");
        verifyFile(1);
    }

    @Test
    public void testLockingAppender() throws Exception {
        writer(true, 1, "test");
        verifyFile(1);
    }

    @Test
    public void testMultipleAppenders() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        int count = 10;
        Runnable runnable = new FileWriterRunnable(false, count);
        for (int i=0; i < THREADS; ++i) {
            pool.execute(runnable);
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        verifyFile(THREADS * count);
    }


    @Test
    public void testMultipleLockedAppenders() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        int count = 10;
        Runnable runnable = new FileWriterRunnable(true, count);
        for (int i=0; i < THREADS; ++i) {
            pool.execute(runnable);
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        verifyFile(THREADS * count);
    }


    //@Test
    public void testMultipleVMs() throws Exception {

        String classPath = System.getProperty("java.class.path");
        Integer count = 10;
        int processes = 3;
        Process[] process = new Process[processes];
        ProcessBuilder[] builders = new ProcessBuilder[processes];
        for (int index=0; index < processes; ++index) {
            builders[index] = new ProcessBuilder("java","-cp", classPath, ProcessTest.class.getName(),
                "Process " + index, count.toString(), "true");
        }
        for (int index=0; index < processes; ++index) {
            process[index] = builders[index].start();
        }
        for (int index=0; index < processes; ++index) {
            Process p = process[index];
            //System.out.println("Process " + index + " exited with " + p.waitFor());
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            p.destroy();
        }
        verifyFile(count * processes);
    }

    private static void writer(boolean lock, int count, String name) throws Exception {
        Layout layout = new PatternLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN);
        FileManager manager = FileManager.getFileManager(FILENAME, true, lock, false);
        FileAppender app = new FileAppender("test", layout, null, manager, FILENAME, false, false);
        Thread t = Thread.currentThread();
        app.start();
        assertTrue("Appender did not start", app.isStarted());
        for (int i=0; i < count; ++i) {
            LogEvent event = new Log4jLogEvent("TestLogger", null, FileAppenderTest.class.getName(), Level.INFO,
                new SimpleMessage("Test"), null, null, null, name, null, System.currentTimeMillis());
            try {
                app.append(event);
                t.sleep(25);  // Give up control long enough for another thread/process to occasionally do something.
            } catch (Exception ex) {
                throw ex;
            }
        }
        app.stop();
        assertFalse("Appender did not stop", app.isStarted());
    }

    private void verifyFile(int count) throws Exception {
        //String expected = "[\\w]* \\[\\s*\\] INFO TestLogger - Test$";
        String expected = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\[[^\\]]*\\] INFO TestLogger - Test";
        Pattern pattern = Pattern.compile(expected);
        DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(FILENAME)));
        int counter = 0;
        String str = "";
        while (is.available() != 0) {
            str = is.readLine();
            //System.out.println(str);
            ++counter;
            Matcher matcher = pattern.matcher(str);
            assertTrue("Bad data: " + str, matcher.matches());
        }
        assertTrue("Incorrect count: was " + counter + " should be " + count, count == counter);

    }


    private static void deleteFile() {
        File file = new File(FILENAME);
        if (file.exists()) {
            file.delete();
        }
    }

    public class FileWriterRunnable implements Runnable {
        private final boolean lock;
        private final int count;

        public FileWriterRunnable(boolean lock, int count)  {
            this.lock = lock;
            this.count = count;
        }
        public void run() {
            Thread thread = Thread.currentThread();

            try {
                writer(lock, count, thread.getName());

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class ProcessTest {

        public static void main(String[] args) {

            if (args.length != 3) {
                System.out.println("Required arguments 'id', 'count' and 'lock' not provided");
                System.exit(-1);
            }
            String id = args[0];

            int count = Integer.parseInt(args[1]);

            if (count <= 0) {
                System.out.println("Invalid count value: " + args[1]);
                System.exit(-1);
            }
            boolean lock = Boolean.parseBoolean(args[2]);

            //System.out.println("Got arguments " + id + ", " + count + ", " + lock);

            try {
                writer(lock, count, id);
                //thread.sleep(50);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }


        }
    }
}
