package org.apache.logging.log4j.core.appender.flume;

import org.apache.hadoop.hdfs.server.common.IncorrectVersionException;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
/**
 *
 */
public class UUIDTest {

    private static final int COUNT = 200;
    private static final int THREADS = 10;

    @Test
    public void testTimeBaseUUID() {
        UUID uuid = UUIDUtil.getTimeBasedUUID();
        UUID uuid2 = UUIDUtil.getTimeBasedUUID();
        long current = (System.currentTimeMillis() * 10000) + UUIDUtil.NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
        long time = uuid.timestamp();
        assertTrue("Incorrect time", current + 10000 - time > 0);
        UUID[] uuids = new UUID[COUNT];
        long start = System.nanoTime();
        for (int i=0; i < COUNT; ++i) {
            uuids[i] = UUIDUtil.getTimeBasedUUID();
        }
        long elapsed = System.nanoTime() - start;
        System.out.println("Elapsed for " + COUNT + " UUIDS = " + elapsed + " Average = " + elapsed / COUNT + " ns");
        int errors = 0;
        for (int i=0; i < COUNT; ++i) {
            for (int j=i+1; j < COUNT; ++j) {
                if (uuids[i].equals(uuids[j])) {
                    ++errors;
                    System.out.println("UUID " + i + " equals UUID " + j);
                }
            }
        }
        assertTrue(errors + " duplicate UUIDS", errors == 0);
        int variant = uuid.variant();
        assertTrue("Incorrect variant. Expected 2 got " + variant, variant == 2);
        int version = uuid.version();
        assertTrue("Incorrect version. Expected 1 got " + version, version == 1);
        long node = uuid.node();
        assertTrue("Invalid node", node != 0);
    }

    @Test
    public void testThreads() throws Exception {
        Thread[] threads = new Thread[THREADS];
        UUID[] uuids = new UUID[COUNT * THREADS];
        long[] elapsed = new long[THREADS];
        for (int i=0; i < THREADS; ++i) {
            threads[i] = new Worker(uuids, elapsed, i, COUNT);
        }
        for (int i=0; i < THREADS; ++i) {
            threads[i].start();
        }
        long elapsedTime = 0;
        for (int i=0; i < THREADS; ++i) {
            threads[i].join();
            elapsedTime += elapsed[i];
        }
        System.out.println("Elapsed for " + COUNT * THREADS + " UUIDS = " + elapsedTime + " Average = " +
                elapsedTime / (COUNT * THREADS) + " ns");
        int errors = 0;
        for (int i=0; i < COUNT * THREADS; ++i) {
            for (int j=i+1; j < COUNT * THREADS; ++j) {
                if (uuids[i].equals(uuids[j])) {
                    ++errors;
                    System.out.println("UUID " + i + " equals UUID " + j);
                }
            }
        }
        assertTrue(errors + " duplicate UUIDS", errors == 0);
    }



    private class Worker extends Thread {

        private UUID[] uuids;
        private long[] elapsed;
        private int index;
        private int count;

        public Worker(UUID[] uuids, long[] elapsed, int index, int count) {
            this.uuids = uuids;
            this.index = index;
            this.count = count;
            this.elapsed = elapsed;
        }

        public void run() {
            int pos = index * count;
            long start = System.nanoTime();
            for (int i=pos; i < pos + count; ++i) {
                uuids[i] = UUIDUtil.getTimeBasedUUID();
            }
            elapsed[index] = System.nanoTime() - start;
        }
    }
}
