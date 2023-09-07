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
package org.apache.ws.commons.schema.walker;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.testutils.UtilsForTests;
import org.junit.Test;

public class TestRecursiveSchemaWalk {

    @Test
    public void testRecursiveDefinition() throws Exception {
        List<String> expected = new ArrayList<String>();
        expected.add("{http://avro.apache.org/AvroTest}Recursive");
	    expected.add("{http://avro.apache.org/AvroTest}HoleInTheGround");
	    expected.add("{http://avro.apache.org/AvroTest}level");
	    expected.add("{http://avro.apache.org/AvroTest}RabbitHole (duplicate)"); // Same type as HoleInTheGround
	    expected.add("{http://avro.apache.org/AvroTest}Recursive (duplicate)"); // Same element as root element
	    expected.add("{http://avro.apache.org/AvroTest}WrappedStringArray");
	    expected.add("{http://avro.apache.org/AvroTest}Array");
	    expected.add("{http://avro.apache.org/AvroTest}StringElement");
	    expected.add("{http://avro.apache.org/AvroTest}WrappedNumber");
	    expected.add("{http://avro.apache.org/AvroTest}Array"); // Not a duplicate: different type
	    expected.add("{http://avro.apache.org/AvroTest}NumberElement");
	    expected.add("{http://avro.apache.org/AvroTest}StringElement (duplicate)"); // Repeated

        MyVisitor v = new MyVisitor(expected);

        XmlSchemaCollection collection = null;
        FileReader fileReader = null;
        try {
            File file = UtilsForTests.buildFile("src", "test", "resources", "recursive_schema.xsd");
            fileReader = new FileReader(file);

            collection = new XmlSchemaCollection();
            collection.read(new StreamSource(fileReader, file.getAbsolutePath()));

        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        XmlSchemaElement elem = getElementOf(collection, "Recursive");
        XmlSchemaWalker walker = new XmlSchemaWalker(collection, v);
        try {
            walker.walk(elem);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }


        int pos = v.getNextExpectedPosition();
        if (pos != expected.size()) {
            throw new IllegalStateException("expected a further element");
        }
    }

    static class MyVisitor implements XmlSchemaVisitor {
        List<String> mExpectedElements = null;
        int mListPos = 0;

        public MyVisitor(List<String> aExpectedElements) {
            mExpectedElements = aExpectedElements;
        }

        public int getNextExpectedPosition () {
            return mListPos;
        }

        @Override
        public void onEnterElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo, boolean previouslyVisited) {
            // TODO Auto-generated method stub
            if (mListPos >= mExpectedElements.size()) {
                throw new IllegalStateException("unexpected element");
            }
            String expected = mExpectedElements.get(mListPos++);
            if (!expected.equals(element.getQName().toString() + (previouslyVisited ? " (duplicate)" : ""))) {
                throw new IllegalStateException("incorrect element");
            }
        }

        @Override
        public void onExitElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo, boolean previouslyVisited) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onVisitAttribute(XmlSchemaElement element, XmlSchemaAttrInfo attrInfo) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEndAttributes(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEnterSubstitutionGroup(XmlSchemaElement base) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onExitSubstitutionGroup(XmlSchemaElement base) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEnterAllGroup(XmlSchemaAll all) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onExitAllGroup(XmlSchemaAll all) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEnterChoiceGroup(XmlSchemaChoice choice) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onExitChoiceGroup(XmlSchemaChoice choice) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEnterSequenceGroup(XmlSchemaSequence seq) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onExitSequenceGroup(XmlSchemaSequence seq) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onVisitAny(XmlSchemaAny any) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onVisitAnyAttribute(XmlSchemaElement element, XmlSchemaAnyAttribute anyAttr) {
            // TODO Auto-generated method stub

        }

    }

    private static XmlSchemaElement getElementOf(XmlSchemaCollection collection, String name) {

        XmlSchemaElement elem = null;
        XmlSchema[] schemas = collection.getXmlSchemas();
        for (XmlSchema schema : schemas) {
            elem = schema.getElementByName(name);
            if (elem != null) {
                break;
            }
        }
        return elem;
    }
}

