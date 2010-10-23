/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ws.commons.schema;

import java.lang.ref.SoftReference;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import tests.Resources;

/**
 * Test the resolved Schema cache.
 */
public class SchemaBuilderCacheTest extends Assert {

    // Amount of time the testcase should wait on the test threads before timing
    // out
    private static final int THREAD_TIMEOUT = 90000;

    /**
     * return resolved schema map.
     */
    static ThreadLocal<Map<String, SoftReference<XmlSchema>>> getResolvedSchemasHashtable() {
        return SchemaBuilder.resolvedSchemas;
    }

    /**
     * Return the HashMap for the current thread or null if there is not one.
     * 
     * @return
     */
    static Map<String, SoftReference<XmlSchema>> getThreadResolvedSchemaHashtable() {
        return getResolvedSchemasHashtable().get();
    }

    // ==============================================================================================
    // Utility Methods
    // ==============================================================================================

    /**
     * Set the resolvedSchemas collection to null. This should be done in a
     * finally block of any tests that cause an SchemaBuilder.initCache to be
     * done to cleanup for the next test.
     */
    static void resetResolvedSchemasHashtable() {
        SchemaBuilder.resolvedSchemas = new ThreadLocal<Map<String, SoftReference<XmlSchema>>>();
    }

    static Document setupDocument() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document doc;
        try {
            doc = documentBuilderFactory.newDocumentBuilder().parse(
                    Resources.asURI("importBase.xsd"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return doc;
    }

    static XmlSchemaCollection setupXmlSchemaCollection() {
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.setBaseUri(Resources.TEST_RESOURCES);
        return schemaCol;
    }

    /**
     * Test that threads can not affect the cache for other threads.
     */
    @Test
    public void testMultithreadCache() {
        try {
            MultithreadUpdateLockMonitor testMonitor = new MultithreadUpdateLockMonitor();
            startupTestThreads(testMonitor);

            if (testMonitor.t1Exception != null) {
                fail("Thread T1 encountred an error: "
                        + testMonitor.t1Exception.toString());
            }
            if (testMonitor.t2Exception != null) {
                fail("Thread T2 encountred an error: "
                        + testMonitor.t2Exception.toString());
            }
            if (testMonitor.t3Exception != null) {
                fail("Thread T3 encountred an error: "
                        + testMonitor.t3Exception.toString());
            }
        } finally {
            resetResolvedSchemasHashtable();
        }
    }

    /**
     * Test if the cache is initialized it will be populated when a schema is
     * read and it will be cleared when the clearCache method is called.
     * 
     * @throws Exception
     */
    @Test
    public void testResolveCacheInitialized() throws Exception {
        try {
            SchemaBuilder.initCache();
            Document doc = setupDocument();
            XmlSchemaCollection schemaCol = setupXmlSchemaCollection();
            XmlSchema schema = schemaCol.read(doc, null);
            assertNotNull(schema);

            // If the cache is in use, it should not be null and there should
            // be an entry for this thread ID
            assertNotNull(getResolvedSchemasHashtable());
            Map<String, SoftReference<XmlSchema>> threadHT = getThreadResolvedSchemaHashtable();
            assertNotNull(threadHT);
            assertFalse(threadHT.isEmpty());
            assertEquals(1, threadHT.size());

            // After clearing the cache, there should be no entry for this
            // thread ID, and
            // the hashtable should not be null
            SchemaBuilder.clearCache();
            assertNotNull(getResolvedSchemasHashtable());
            assertNull(getThreadResolvedSchemaHashtable());
            System.out.println("Line 13");
        } finally {
            resetResolvedSchemasHashtable();
        }

        // If the cache is enabled, then it should be non-null
    }

    /**
     * Test that if the cache is not initialized, then it should not be used
     * when a schema is read.
     * 
     * @throws Exception
     */
    @Test
    public void testResolveCacheUninitialized() throws Exception {
        Document doc = setupDocument();
        XmlSchemaCollection schemaCol = setupXmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc, null);
        assertNotNull(schema);

        // If the cache is not in use, then it should be null
        // The thread-local cannot be null
        assertNull(getThreadResolvedSchemaHashtable());
    }

    /**
     * Configure and start the test threads for the multi-threaded testing. The
     * threads will perform various tests between themselves such as clearing
     * cache in one thread and making sure the cache used by a different thread
     * is not affected. A monitor is used to control the synchonization between
     * the threads and for communicating faliures back to the test method.
     * 
     * See thed Runnable classes for details on the tests performed.
     * 
     * @param testMonitor
     *            Used to synchronize the tests between the threads
     */
    private void startupTestThreads(MultithreadUpdateLockMonitor testMonitor) {
        TestingRunnable1 testRunnable1 = new TestingRunnable1();
        testRunnable1.testMonitor = testMonitor;

        TestingRunnable2 testRunnable2 = new TestingRunnable2();
        testRunnable2.testMonitor = testMonitor;

        TestingRunnable3 testRunnable3 = new TestingRunnable3();
        testRunnable3.testMonitor = testMonitor;

        Thread thread1 = new Thread(testRunnable1);
        Thread thread2 = new Thread(testRunnable2);
        Thread thread3 = new Thread(testRunnable3);

        thread1.start();
        thread2.start();
        thread3.start();

        // Join the threads to wait for their completion, specifying a timeout
        // to prevent
        // a testcase hang if something goes wrong with the threads.
        try {
            thread1.join(THREAD_TIMEOUT);
            thread2.join(THREAD_TIMEOUT);
            thread3.join(THREAD_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Unable to join to testing threads");
        }
    }
}

/**
 * Monitor used to control synchronization between the testing threads and
 * communicate failures back to the test method.
 */
class MultithreadUpdateLockMonitor {
    Exception t1Exception;
    boolean t1SetupComplete;
    Exception t2Exception;
    boolean t2SetupComplete;
    Exception t3Exception;
}

// =================================================================================================
// Test execution threads
// =================================================================================================
/**
 * Thread 1 will do the following - Initialize the cache and verify it was used
 * during a read - Unblock Thread 2 - Wait until Thread 2 unblocks it - Verify
 * that the clearCache done by Thread 2 did not affect this Thread's cache.
 */
@Ignore
class TestingRunnable1 implements Runnable {

    MultithreadUpdateLockMonitor testMonitor;

    public void run() {
        SchemaBuilder.initCache();
        Document doc = SchemaBuilderCacheTest.setupDocument();
        XmlSchemaCollection schemaCol = SchemaBuilderCacheTest
                .setupXmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc, null);
        if (schema == null) {
            testMonitor.t1Exception = new Exception("Schema was null");
        }

        // If the cache is in use, it should not be null and there should be an
        // entry for this
        // Thread
        if (SchemaBuilderCacheTest.getResolvedSchemasHashtable() == null) {
            testMonitor.t1Exception = new Exception("resolvedSchemas was null");
        }
        Map<String, SoftReference<XmlSchema>> threadHT = SchemaBuilderCacheTest
                .getThreadResolvedSchemaHashtable();
        if (threadHT == null) {
            testMonitor.t1Exception = new Exception(
                    "Thread resolvedSchemas was null");
        }

        if (threadHT.isEmpty()) {
            testMonitor.t1Exception = new Exception(
                    "Thread resolvedSchemas was empty");
        }

        synchronized (testMonitor) {
            testMonitor.t1SetupComplete = true;
            testMonitor.notifyAll();
            while (!testMonitor.t2SetupComplete) {
                try {
                    testMonitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    testMonitor.t1Exception = new RuntimeException(e);
                    throw (RuntimeException) testMonitor.t1Exception;
                }
            }
        }

        // After the other thread does a reset, the cache for this thread should
        // NOT be null
        if (SchemaBuilderCacheTest.getResolvedSchemasHashtable() == null) {
            testMonitor.t1Exception = new Exception(
                    "resolvedSchemas was null after reset");
        }
        threadHT = SchemaBuilderCacheTest.getThreadResolvedSchemaHashtable();
        if (threadHT == null) {
            testMonitor.t1Exception = new Exception(
                    "Thread resolvedSchemas was null after clear");
        }

        if (threadHT.isEmpty()) {
            testMonitor.t1Exception = new Exception(
                    "Thread resolvedSchemas was empty after clear");
        }

        // Issue our a clear on this TID, and now there should be no entires for
        // it
        SchemaBuilder.clearCache();
        if (SchemaBuilderCacheTest.getResolvedSchemasHashtable() == null) {
            testMonitor.t1Exception = new Exception(
                    "resolvedSchemas was null after clear on TID");
        }

        threadHT = SchemaBuilderCacheTest.getThreadResolvedSchemaHashtable();
        if (threadHT != null) {
            testMonitor.t1Exception = new Exception(
                    "Thread resolvedSchemas was not null after clear");
        }
    }
}

/**
 * Thread 2 will: - Block until released by Thread 1 - Initialize the cache then
 * make sure it was used for a resolve on this thread - clear the cache and make
 * sure the entries for this Thread a removed - Unblock Thread 1 to make sure
 * the clear did not affect it
 */
@Ignore
class TestingRunnable2 implements Runnable {

    MultithreadUpdateLockMonitor testMonitor;

    public void run() {
        synchronized (testMonitor) {
            while (!testMonitor.t1SetupComplete) {
                try {
                    testMonitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    testMonitor.t2Exception = new RuntimeException(e);
                    throw (RuntimeException) testMonitor.t2Exception;
                }
            }
        }
        SchemaBuilder.initCache();
        Document doc = SchemaBuilderCacheTest.setupDocument();
        XmlSchemaCollection schemaCol = SchemaBuilderCacheTest
                .setupXmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc, null);
        if (schema == null) {
            testMonitor.t2Exception = new Exception("Schema was null");
        }

        // If the cache is in use, it should not be null.
        if (SchemaBuilderCacheTest.getResolvedSchemasHashtable() == null) {
            testMonitor.t2Exception = new Exception("resolvedSchemas was null");
        }
        Map<String, SoftReference<XmlSchema>> threadHT = SchemaBuilderCacheTest
                .getThreadResolvedSchemaHashtable();
        if (threadHT == null) {
            testMonitor.t2Exception = new Exception(
                    "Thread resolvedSchemas was null");
        }
        if (threadHT.isEmpty()) {
            testMonitor.t2Exception = new Exception(
                    "Thread resolvedSchemas was empty");
        }

        // Issue our a clear on this TID, and now there should be no entires for
        // it
        SchemaBuilder.clearCache();
        if (SchemaBuilderCacheTest.getResolvedSchemasHashtable() == null) {
            testMonitor.t2Exception = new Exception(
                    "resolvedSchemas was null after clear on TID");
        }

        threadHT = SchemaBuilderCacheTest.getThreadResolvedSchemaHashtable();
        if (threadHT != null) {
            testMonitor.t2Exception = new Exception(
                    "Thread resolvedSchemas was not null after clear");
        }

        synchronized (testMonitor) {
            testMonitor.t2SetupComplete = true;
            testMonitor.notifyAll();
        }
    }
}

/**
 * Test that the init done by Thread 1 does NOT cause the cache to be used by
 * this thread which did not do an init.
 */
@Ignore
class TestingRunnable3 implements Runnable {
    MultithreadUpdateLockMonitor testMonitor;

    public void run() {
        synchronized (testMonitor) {
            while (!testMonitor.t1SetupComplete) {
                try {
                    testMonitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    testMonitor.t3Exception = new RuntimeException(e);
                    throw (RuntimeException) testMonitor.t2Exception;
                }
            }
        }
        Document doc = SchemaBuilderCacheTest.setupDocument();
        XmlSchemaCollection schemaCol = SchemaBuilderCacheTest
                .setupXmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc, null);
        if (schema == null) {
            testMonitor.t3Exception = new Exception("Schema was null");
        }
        Map<String, SoftReference<XmlSchema>> threadHT = SchemaBuilderCacheTest
                .getThreadResolvedSchemaHashtable();
        if (threadHT != null) {
            testMonitor.t3Exception = new Exception(
                    "Thread resolvedSchemas was not null");
        }

    }

}
