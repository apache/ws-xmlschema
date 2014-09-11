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

package tests.w3c;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 *
 */
@RunWith(Parameterized.class)
public class ParameterizedW3CTestsTest extends XMLAssert {
    private static final Charset UTF8 = Charset.forName("utf-8");
    private SchemaCase currentTest;

    public ParameterizedW3CTestsTest(SchemaCase test) {
        currentTest = test;
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        List<SchemaCase> tests = W3CTestCaseCollector.getSchemaTests();
        List<Object[]> results = new ArrayList<Object[]>();
        for (SchemaCase st : tests) {
            results.add(new Object[] {st});
        }
        return results;
    }

    public XmlSchema loadSchema(InputStream caseStream, String basePath) throws Exception {
        XmlSchemaCollection col = new XmlSchemaCollection();
        col.setBaseUri(basePath);
        InputStreamReader inputStreamReader = new InputStreamReader(caseStream, UTF8);
        /*
         * We would expect to need a resolver here, since the input stream is a JAR stream
         * and the base pathname relative to the jar. I'm not sure why we don't. Perhaps none of the
         * cases have any interesting base relative references in them.
         */
        return col.read(inputStreamReader);
    }

    @Test
    public void testRoundTrip() throws Exception {

        XmlSchema schema = null;
        DetailedDiff detaileddiffs = null;

        try {
            schema = loadSchema(currentTest.getTestCase(), currentTest.getBaseFilePathname());

            // TODO: if we get here and the input was meant to be invalid perhaps
            // should fail. Depends on whether XmlSchema is doing validation. For
            // now we're ignoring invalid tests.

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            schema.write(baos);
            InputStreamReader inputStreamReader = new InputStreamReader(currentTest.getTestCase(),
                                                                        UTF8);
            Diff diff = new Diff(inputStreamReader,
                                 new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), UTF8));

            detaileddiffs = new DetailedDiff(diff);
            detaileddiffs.overrideDifferenceListener(new SchemaAttrDiff());
            boolean result = detaileddiffs.similar();
            if (!result) {
                printFailureDetail(schema, detaileddiffs);
            }
            assertTrue("Serialized out schema not similar to original", result);
        } catch (Exception e) {
            System.err.println(currentTest.getSchemaDocumentLink());
            if (currentTest.isValid()) {
                printFailureDetail(schema, detaileddiffs);
            }
            throw e;
        }
    }

    private void printFailureDetail(XmlSchema schema, DetailedDiff detaileddiffs) throws Exception {
        System.err.println("Failure detail");
        System.err.println("-----");
        schema.write(System.err);
        if (detaileddiffs != null) {
            ListIterator<?> li = detaileddiffs.getAllDifferences().listIterator();

            while (li.hasNext()) {
                System.err.println(li.next());
            }
        }
    }


}
