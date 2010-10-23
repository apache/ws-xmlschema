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
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
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

    public XmlSchema loadSchema(File f) throws Exception {
        XmlSchemaCollection col = new XmlSchemaCollection();
        col.setBaseUri(f.getPath());
        return col.read(new FileReader(f));
    }

    @Test
    public void testRoundTrip() throws Exception {

        XmlSchema schema = null;
        DetailedDiff detaileddiffs = null;

        try {
            schema = loadSchema(currentTest.getTestCaseFile());

            // TODO: if we get here and the input was meant to be invalid perhaps
            // should fail. Depends on whether XmlSchema is doing validation. For
            // now we're ignoring invalid tests.

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            schema.write(baos);
            Diff diff = new Diff(new FileReader(currentTest.getTestCaseFile()),
                                 new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            detaileddiffs = new DetailedDiff(diff);
            detaileddiffs.overrideDifferenceListener(new SchemaAttrDiff());
            boolean result = detaileddiffs.similar();
            if (!result) {
                printFailureDetail(schema, detaileddiffs);
            }
            assertTrue("Serialized out schema not similar to original", result);
        } catch (Exception e) {
            if (currentTest.isValid()) {
                printFailureDetail(schema, detaileddiffs);
            }
            throw new Exception(currentTest.getTestCaseFile().getPath(), e);
        }
    }

    private void printFailureDetail(XmlSchema schema, DetailedDiff detaileddiffs) throws Exception {
        System.err.println("Failure detail");
        System.err.println("-----");
        schema.write(System.err);
        if (detaileddiffs != null) {
            ListIterator li = detaileddiffs.getAllDifferences().listIterator();

            while (li.hasNext()) {
                System.err.println(li.next());
            }
        }
    }


}
