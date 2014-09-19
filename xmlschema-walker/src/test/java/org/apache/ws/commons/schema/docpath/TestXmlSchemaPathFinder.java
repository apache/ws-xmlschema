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

package org.apache.ws.commons.schema.docpath;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.testutils.UtilsForTests;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestXmlSchemaPathFinder {

    private static final String TESTSCHEMA_NS = "http://avro.apache.org/AvroTest";
    private static final String COMPLEX_SCHEMA_NS = "urn:avro:complex_schema";

    private static DocumentBuilderFactory dbf;

    private DocumentBuilder docBuilder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    }

    @Before
    public void setUp() throws Exception {
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void testRoot() throws Exception {
        final QName root = new QName("http://avro.apache.org/AvroTest", "root");

        final File schemaFile = UtilsForTests.buildFile("src", "test", "resources", "test_schema.xsd");

        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "test1_root.xml");

        XmlSchemaPathNode<Void, Void> traversal = runTest(schemaFile, xmlFile, root);

        Map<QName, ExpectedElement> expectedElements = new HashMap<QName, ExpectedElement>();

        expectedElements.put(root, new ExpectedElement(new XmlSchemaTypeInfo(false)));

        ExpectedNode node = new ExpectedNode(
                                             XmlSchemaStateMachineNode.Type.ELEMENT,
                                             1,
                                             1,
                                             Collections
                                                 .<SortedMap<Integer, ExpectedNode>> singletonList(new TreeMap<Integer, ExpectedNode>()));

        node.setElemQName(new QName(TESTSCHEMA_NS, "root"));

        ExpectedNode.validate(root.toString(), node, traversal.getDocumentNode(), expectedElements);

        ArrayList<ExpectedPathNode> expPath = new ArrayList<ExpectedPathNode>();
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, node, 1));

        validate(expPath, traversal);
    }

    @Test
    public void testChildren() throws Exception {
        final QName root = new QName("http://avro.apache.org/AvroTest", "root");

        final File schemaFile = UtilsForTests.buildFile("src", "test", "resources", "test_schema.xsd");

        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "test2_children.xml");

        XmlSchemaPathNode<Void, Void> traversal = runTest(schemaFile, xmlFile, root);

        Map<QName, ExpectedElement> expectedElements = new HashMap<QName, ExpectedElement>();

        expectedElements.put(root, new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(TESTSCHEMA_NS, "primitive"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)));

        expectedElements.put(new QName(TESTSCHEMA_NS, "nonNullPrimitive"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)));

        ExpectedNode primitive = new ExpectedNode(
                                                  XmlSchemaStateMachineNode.Type.ELEMENT,
                                                  1,
                                                  1,
                                                  Collections
                                                      .<SortedMap<Integer, ExpectedNode>> singletonList(new TreeMap<Integer, ExpectedNode>()));
        primitive.setElemQName(new QName(TESTSCHEMA_NS, "primitive"));

        ExpectedNode nonNullPrimitive = new ExpectedNode(
                                                         XmlSchemaStateMachineNode.Type.ELEMENT,
                                                         1,
                                                         1,
                                                         Collections
                                                             .<SortedMap<Integer, ExpectedNode>> singletonList(new TreeMap<Integer, ExpectedNode>()));
        nonNullPrimitive.setElemQName(new QName(TESTSCHEMA_NS, "nonNullPrimitive"));

        // avro:primitive is the first choice; its index is 0
        TreeMap<Integer, ExpectedNode> choicePrimitiveChild = new TreeMap<Integer, ExpectedNode>();
        choicePrimitiveChild.put(0, primitive);

        // avro:nonNullPrimitive is the second choice; its index is 1
        TreeMap<Integer, ExpectedNode> choiceNonNullPrimitiveChild = new TreeMap<Integer, ExpectedNode>();
        choiceNonNullPrimitiveChild.put(1, nonNullPrimitive);

        ArrayList<SortedMap<Integer, ExpectedNode>> choiceChildren = new ArrayList<SortedMap<Integer, ExpectedNode>>();

        for (int i = 0; i < 9; ++i) {
            choiceChildren.add(choicePrimitiveChild);
        }
        for (int i = 0; i < 8; ++i) {
            choiceChildren.add(choiceNonNullPrimitiveChild);
        }

        ExpectedNode choiceNode = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0L, Long.MAX_VALUE,
                                                   choiceChildren);

        TreeMap<Integer, ExpectedNode> sequenceChild = new TreeMap<Integer, ExpectedNode>();
        sequenceChild.put(0, choiceNode);

        ExpectedNode sequenceNode = new ExpectedNode(
                                                     XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                     1,
                                                     1,
                                                     Collections
                                                         .<SortedMap<Integer, ExpectedNode>> singletonList(sequenceChild));

        TreeMap<Integer, ExpectedNode> rootChild = new TreeMap<Integer, ExpectedNode>();
        rootChild.put(0, sequenceNode);

        ExpectedNode rootNode = new ExpectedNode(
                                                 XmlSchemaStateMachineNode.Type.ELEMENT,
                                                 1,
                                                 1,
                                                 Collections
                                                     .<SortedMap<Integer, ExpectedNode>> singletonList(rootChild));
        rootNode.setElemQName(new QName(TESTSCHEMA_NS, "root"));

        ExpectedNode.validate(root.toString(), rootNode, traversal.getDocumentNode(), expectedElements);

        ArrayList<ExpectedPathNode> expPath = new ArrayList<ExpectedPathNode>();

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, rootNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, sequenceNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, choiceNode, 1));

        int choiceIter = 1;

        for (int primIter = 0; primIter < 9; ++primIter) {
            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, primitive, 1));

            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, primitive, 1));

            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, choiceIter));

            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, ++choiceIter));
        }

        for (int nonNullPrimIter = 0; nonNullPrimIter < 7; ++nonNullPrimIter) {
            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, choiceIter));

            expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, ++choiceIter));
        }

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, choiceIter));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, sequenceNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, rootNode, 1));

        validate(expPath, traversal);
    }

    @Test
    public void testGrandchildren() throws Exception {
        final QName root = new QName("http://avro.apache.org/AvroTest", "root");

        final File schemaFile = UtilsForTests.buildFile("src", "test", "resources", "test_schema.xsd");

        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "test3_grandchildren.xml");

        XmlSchemaPathNode<Void, Void> traversal = runTest(schemaFile, xmlFile, root);

        Map<QName, ExpectedElement> expectedElements = new HashMap<QName, ExpectedElement>();

        expectedElements.put(root, new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(TESTSCHEMA_NS, "primitive"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)));

        expectedElements.put(new QName(TESTSCHEMA_NS, "nonNullPrimitive"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)));

        expectedElements.put(new QName(TESTSCHEMA_NS, "map"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(TESTSCHEMA_NS, "record"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        ExpectedNode primitive = new ExpectedNode(
                                                  XmlSchemaStateMachineNode.Type.ELEMENT,
                                                  1,
                                                  1,
                                                  Collections
                                                      .<SortedMap<Integer, ExpectedNode>> singletonList(new TreeMap<Integer, ExpectedNode>()));
        primitive.setElemQName(new QName(TESTSCHEMA_NS, "primitive"));

        ExpectedNode nonNullPrimitive = new ExpectedNode(
                                                         XmlSchemaStateMachineNode.Type.ELEMENT,
                                                         1,
                                                         1,
                                                         Collections
                                                             .<SortedMap<Integer, ExpectedNode>> singletonList(new TreeMap<Integer, ExpectedNode>()));
        nonNullPrimitive.setElemQName(new QName(TESTSCHEMA_NS, "nonNullPrimitive"));

        // avro:primitive is the first choice; its index is 0
        TreeMap<Integer, ExpectedNode> choicePrimitiveChild = new TreeMap<Integer, ExpectedNode>();
        choicePrimitiveChild.put(0, primitive);

        // avro:nonNullPrimitive is the second choice; its index is 1
        TreeMap<Integer, ExpectedNode> choiceNonNullPrimitiveChild = new TreeMap<Integer, ExpectedNode>();
        choiceNonNullPrimitiveChild.put(1, nonNullPrimitive);

        // map 1
        ArrayList<SortedMap<Integer, ExpectedNode>> map1ChoiceChildren = new ArrayList<SortedMap<Integer, ExpectedNode>>();
        map1ChoiceChildren.add(choicePrimitiveChild);
        map1ChoiceChildren.add(choiceNonNullPrimitiveChild);

        ExpectedNode map1Choice = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0, Long.MAX_VALUE,
                                                   map1ChoiceChildren);

        SortedMap<Integer, ExpectedNode> map1SeqChildren = new TreeMap<Integer, ExpectedNode>();
        map1SeqChildren.put(0, map1Choice);

        ExpectedNode map1Seq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                Collections.singletonList(map1SeqChildren));

        SortedMap<Integer, ExpectedNode> map1Children = new TreeMap<Integer, ExpectedNode>();
        map1Children.put(0, map1Seq);

        ExpectedNode map1 = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                             Collections.singletonList(map1Children));
        map1.setElemQName(new QName(TESTSCHEMA_NS, "map"));

        // avro:map is a substitution of avro:record, its index is 2
        SortedMap<Integer, ExpectedNode> map1SubsGrpChild = new TreeMap<Integer, ExpectedNode>();
        map1SubsGrpChild.put(1, map1);

        ExpectedNode map1SubstGroup = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1,
                                                       1, Collections.singletonList(map1SubsGrpChild));

        SortedMap<Integer, ExpectedNode> choiceMap1Child = new TreeMap<Integer, ExpectedNode>();
        choiceMap1Child.put(2, map1SubstGroup);

        // map 2
        SortedMap<Integer, ExpectedNode> map2ChoiceChildren = new TreeMap<Integer, ExpectedNode>();
        map2ChoiceChildren.put(0, primitive);

        ExpectedNode map2Choice = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0, Long.MAX_VALUE,
                                                   Collections.singletonList(map2ChoiceChildren));

        SortedMap<Integer, ExpectedNode> map2SequenceChildren = new TreeMap<Integer, ExpectedNode>();
        map2SequenceChildren.put(0, map2Choice);

        ExpectedNode map2Seq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                Collections.singletonList(map2SequenceChildren));

        SortedMap<Integer, ExpectedNode> map2Children = new TreeMap<Integer, ExpectedNode>();
        map2Children.put(0, map2Seq);

        ExpectedNode map2 = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                             Collections.singletonList(map2Children));
        map2.setElemQName(new QName(TESTSCHEMA_NS, "map"));

        SortedMap<Integer, ExpectedNode> map2SubstGrpChild = new TreeMap<Integer, ExpectedNode>();
        map2SubstGrpChild.put(1, map2);

        ExpectedNode map2SubstGrp = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1, 1,
                                                     Collections.singletonList(map2SubstGrpChild));

        SortedMap<Integer, ExpectedNode> choiceMap2Child = new TreeMap<Integer, ExpectedNode>();
        choiceMap2Child.put(2, map2SubstGrp);

        // map 4, which is owned by map 3
        SortedMap<Integer, ExpectedNode> map4ChoiceChildren = new TreeMap<Integer, ExpectedNode>();
        map4ChoiceChildren.put(1, nonNullPrimitive);

        ExpectedNode map4Choice = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0, Long.MAX_VALUE,
                                                   Collections.singletonList(map4ChoiceChildren));

        SortedMap<Integer, ExpectedNode> map4SeqChildren = new TreeMap<Integer, ExpectedNode>();
        map4SeqChildren.put(0, map4Choice);

        ExpectedNode map4Seq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                Collections.singletonList(map4SeqChildren));

        SortedMap<Integer, ExpectedNode> map4Children = new TreeMap<Integer, ExpectedNode>();
        map4Children.put(0, map4Seq);

        ExpectedNode map4 = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                             Collections.singletonList(map4Children));
        map4.setElemQName(new QName(TESTSCHEMA_NS, "map"));

        SortedMap<Integer, ExpectedNode> map4SubstGrpChildren = new TreeMap<Integer, ExpectedNode>();
        map4SubstGrpChildren.put(1, map4);

        ExpectedNode map4SubstGrp = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1, 1,
                                                     Collections.singletonList(map4SubstGrpChildren));

        // map 5, which is owned by map 3
        SortedMap<Integer, ExpectedNode> map5ChoiceChildren = new TreeMap<Integer, ExpectedNode>();
        map5ChoiceChildren.put(0, primitive);

        ExpectedNode map5Choice = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0, Long.MAX_VALUE,
                                                   Collections.singletonList(map5ChoiceChildren));

        SortedMap<Integer, ExpectedNode> map5SeqChildren = new TreeMap<Integer, ExpectedNode>();
        map5SeqChildren.put(0, map5Choice);

        ExpectedNode map5Seq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                Collections.singletonList(map5SeqChildren));

        SortedMap<Integer, ExpectedNode> map5Children = new TreeMap<Integer, ExpectedNode>();
        map5Children.put(0, map5Seq);

        ExpectedNode map5 = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                             Collections.singletonList(map5Children));
        map5.setElemQName(new QName(TESTSCHEMA_NS, "map"));

        SortedMap<Integer, ExpectedNode> map5SubstGrpChildren = new TreeMap<Integer, ExpectedNode>();
        map5SubstGrpChildren.put(1, map5);

        ExpectedNode map5SubstGrp = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1, 1,
                                                     Collections.singletonList(map5SubstGrpChildren));

        // map 3
        ArrayList<SortedMap<Integer, ExpectedNode>> map3ChoiceChildren = new ArrayList<SortedMap<Integer, ExpectedNode>>();

        SortedMap<Integer, ExpectedNode> map3Child1 = new TreeMap<Integer, ExpectedNode>();
        map3Child1.put(0, primitive);

        SortedMap<Integer, ExpectedNode> map3Child2 = new TreeMap<Integer, ExpectedNode>();
        map3Child2.put(2, map4SubstGrp);

        SortedMap<Integer, ExpectedNode> map3Child3 = new TreeMap<Integer, ExpectedNode>();
        map3Child3.put(2, map5SubstGrp);

        map3ChoiceChildren.add(map3Child1);
        map3ChoiceChildren.add(map3Child2);
        map3ChoiceChildren.add(map3Child3);

        ExpectedNode map3Choice = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0, Long.MAX_VALUE,
                                                   map3ChoiceChildren);

        SortedMap<Integer, ExpectedNode> map3SeqChildren = new TreeMap<Integer, ExpectedNode>();
        map3SeqChildren.put(0, map3Choice);

        ExpectedNode map3Seq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                Collections.singletonList(map3SeqChildren));

        SortedMap<Integer, ExpectedNode> map3Children = new TreeMap<Integer, ExpectedNode>();
        map3Children.put(0, map3Seq);

        ExpectedNode map3 = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                             Collections.singletonList(map3Children));
        map3.setElemQName(new QName(TESTSCHEMA_NS, "map"));

        SortedMap<Integer, ExpectedNode> map3SubstGrpChildren = new TreeMap<Integer, ExpectedNode>();
        map3SubstGrpChildren.put(1, map3);

        ExpectedNode map3SubstGrp = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1, 1,
                                                     Collections.singletonList(map3SubstGrpChildren));

        SortedMap<Integer, ExpectedNode> choiceMap3Child = new TreeMap<Integer, ExpectedNode>();
        choiceMap3Child.put(2, map3SubstGrp);

        // avro:record
        SortedMap<Integer, ExpectedNode> recordChoiceChildren = new TreeMap<Integer, ExpectedNode>();
        recordChoiceChildren.put(1, nonNullPrimitive);

        ExpectedNode recordChoice = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0,
                                                     Long.MAX_VALUE,
                                                     Collections.singletonList(recordChoiceChildren));

        SortedMap<Integer, ExpectedNode> recordSeqChildren = new TreeMap<Integer, ExpectedNode>();
        recordSeqChildren.put(0, recordChoice);

        ExpectedNode recordSeq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                  Collections.singletonList(recordSeqChildren));

        SortedMap<Integer, ExpectedNode> recordChildren = new TreeMap<Integer, ExpectedNode>();
        recordChildren.put(0, recordSeq);

        ExpectedNode record = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                               Collections.singletonList(recordChildren));
        record.setElemQName(new QName(TESTSCHEMA_NS, "record"));

        SortedMap<Integer, ExpectedNode> recordSubstGrpChildren = new TreeMap<Integer, ExpectedNode>();
        recordSubstGrpChildren.put(0, record);

        ExpectedNode recordSubstGrp = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1,
                                                       1, Collections.singletonList(recordSubstGrpChildren));

        SortedMap<Integer, ExpectedNode> choiceRecordChild = new TreeMap<Integer, ExpectedNode>();
        choiceRecordChild.put(2, recordSubstGrp);

        // root
        ArrayList<SortedMap<Integer, ExpectedNode>> choiceChildren = new ArrayList<SortedMap<Integer, ExpectedNode>>();

        choiceChildren.add(choicePrimitiveChild);
        choiceChildren.add(choiceNonNullPrimitiveChild);
        choiceChildren.add(choiceMap1Child);
        choiceChildren.add(choiceMap2Child);
        choiceChildren.add(choiceRecordChild);
        choiceChildren.add(choiceMap3Child);
        choiceChildren.add(choiceNonNullPrimitiveChild);
        choiceChildren.add(choiceNonNullPrimitiveChild);

        ExpectedNode choiceNode = new ExpectedNode(XmlSchemaStateMachineNode.Type.CHOICE, 0L, Long.MAX_VALUE,
                                                   choiceChildren);

        TreeMap<Integer, ExpectedNode> sequenceChild = new TreeMap<Integer, ExpectedNode>();
        sequenceChild.put(0, choiceNode);

        ExpectedNode sequenceNode = new ExpectedNode(
                                                     XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                     1,
                                                     1,
                                                     Collections
                                                         .<SortedMap<Integer, ExpectedNode>> singletonList(sequenceChild));

        TreeMap<Integer, ExpectedNode> rootChild = new TreeMap<Integer, ExpectedNode>();
        rootChild.put(0, sequenceNode);

        ExpectedNode rootNode = new ExpectedNode(
                                                 XmlSchemaStateMachineNode.Type.ELEMENT,
                                                 1,
                                                 1,
                                                 Collections
                                                     .<SortedMap<Integer, ExpectedNode>> singletonList(rootChild));
        rootNode.setElemQName(new QName(TESTSCHEMA_NS, "root"));

        ExpectedNode.validate(root.toString(), rootNode, traversal.getDocumentNode(), expectedElements);

        ArrayList<ExpectedPathNode> expPath = new ArrayList<ExpectedPathNode>();

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, rootNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, sequenceNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, choiceNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 2));

        // Path Index 10
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 3));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map1SubstGroup, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map1, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map1Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map1Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map1Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, map1Choice, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        // Path Index 20
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map1Choice, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map1Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map1, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map1SubstGroup, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 3));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 4));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map2SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map2, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map2Seq, 1));

        // Path Index 30
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map2Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map2Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map2Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map2, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map2SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 4));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 5));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, recordSubstGrp, 1));

        // Path Index 40
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, record, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, recordSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, recordChoice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, recordChoice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, recordSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, record, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, recordSubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 5));

        // Path Index 50
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 6));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map3SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map3, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map3Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map3Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map3Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, map3Choice, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map4SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map4, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map4Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map4Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map4Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map4Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map4, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map4SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map3Choice, 2));

        // Path Index 70
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, map3Choice, 3));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map5SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map5, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map5Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, map5Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, primitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map5Choice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map5Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map5, 1));

        // Path Index 80
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map5SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map3Choice, 3));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map3Seq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map3, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, map3SubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 6));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 7));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 7));

        // Path Index 90
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, choiceNode, 8));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, nonNullPrimitive, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, choiceNode, 8));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, sequenceNode, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, rootNode, 1));

        validate(expPath, traversal);
    }

    @Test
    public void testComplex() throws Exception {
        final QName root = new QName("urn:avro:complex_schema", "root");

        final File complexSchemaFile = UtilsForTests.buildFile("src", "test", "resources",
                                                               "complex_schema.xsd");

        final File testSchemaFile = UtilsForTests.buildFile("src", "test", "resources", "test_schema.xsd");

        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "complex_test1.xml");

        XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();

        FileReader schemaFileReader = null;
        try {
            schemaFileReader = new FileReader(complexSchemaFile);
            xmlSchemaCollection.read(new StreamSource(schemaFileReader));
        } finally {
            if (schemaFileReader != null) {
                schemaFileReader.close();
            }
        }

        schemaFileReader = null;
        try {
            schemaFileReader = new FileReader(testSchemaFile);
            xmlSchemaCollection.read(new StreamSource(schemaFileReader));
        } finally {
            if (schemaFileReader != null) {
                schemaFileReader.close();
            }
        }

        XmlSchemaPathNode<Void, Void> traversal = runTest(xmlSchemaCollection, xmlFile, root);

        Map<QName, ExpectedElement> expectedElements = new HashMap<QName, ExpectedElement>();

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "realRoot"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "backtrack"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "allTheThings"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "prohibit"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "anyAndFriends"),
                             new ExpectedElement(new XmlSchemaTypeInfo(true)));

        ArrayList<XmlSchemaTypeInfo> simpleExtensionUnion = new ArrayList<XmlSchemaTypeInfo>();
        simpleExtensionUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));
        simpleExtensionUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "simpleExtension"),
                             new ExpectedElement(new XmlSchemaTypeInfo(simpleExtensionUnion)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "simpleRestriction"),
                             new ExpectedElement(new XmlSchemaTypeInfo(simpleExtensionUnion)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "complexExtension"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "mixedType"),
                             new ExpectedElement(new XmlSchemaTypeInfo(true)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "qName"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.QNAME)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "avroEnum"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "xmlEnum"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)));

        expectedElements
            .put(new QName(COMPLEX_SCHEMA_NS, "unsignedLongList"),
                 new ExpectedElement(
                                     new XmlSchemaTypeInfo(
                                                           new XmlSchemaTypeInfo(
                                                                                 XmlSchemaBaseSimpleType.DECIMAL))));

        ArrayList<XmlSchemaTypeInfo> listOfUnionTypes = new ArrayList<XmlSchemaTypeInfo>();
        listOfUnionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));
        listOfUnionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING));
        listOfUnionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));
        listOfUnionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        expectedElements
            .put(new QName(COMPLEX_SCHEMA_NS, "listOfUnion"),
                 new ExpectedElement(new XmlSchemaTypeInfo(new XmlSchemaTypeInfo(listOfUnionTypes))));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "firstMap"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "value"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "secondMap"),
                             new ExpectedElement(new XmlSchemaTypeInfo(false)));

        expectedElements.put(new QName(COMPLEX_SCHEMA_NS, "fixed"),
                             new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL)));

        // No Children
        SortedMap<Integer, ExpectedNode> noChildren = new TreeMap<Integer, ExpectedNode>();

        // Leaf Nodes
        ExpectedNode qName = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                              Collections.singletonList(noChildren));
        qName.setElemQName(new QName(COMPLEX_SCHEMA_NS, "qName"));

        ExpectedNode avroEnum = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                 Collections.singletonList(noChildren));
        avroEnum.setElemQName(new QName(COMPLEX_SCHEMA_NS, "avroEnum"));

        ExpectedNode xmlEnum = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                Collections.singletonList(noChildren));
        xmlEnum.setElemQName(new QName(COMPLEX_SCHEMA_NS, "xmlEnum"));

        ArrayList<SortedMap<Integer, ExpectedNode>> xmlEnumMaxOccurs2Children = new ArrayList<SortedMap<Integer, ExpectedNode>>();
        xmlEnumMaxOccurs2Children.add(noChildren);
        xmlEnumMaxOccurs2Children.add(noChildren);

        ExpectedNode xmlEnumMaxOccurs2 = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 2,
                                                          xmlEnumMaxOccurs2Children);
        xmlEnumMaxOccurs2.setElemQName(new QName(COMPLEX_SCHEMA_NS, "xmlEnum"));

        ExpectedNode unsignedLongList = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                         Collections.singletonList(noChildren));
        unsignedLongList.setElemQName(new QName(COMPLEX_SCHEMA_NS, "unsignedLongList"));

        ExpectedNode listOfUnion = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                    Collections.singletonList(noChildren));
        listOfUnion.setElemQName(new QName(COMPLEX_SCHEMA_NS, "listOfUnion"));

        ArrayList<SortedMap<Integer, ExpectedNode>> valueChildren = new ArrayList<SortedMap<Integer, ExpectedNode>>();
        valueChildren.add(noChildren);
        valueChildren.add(noChildren);

        ExpectedNode value = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 2, valueChildren);
        value.setElemQName(new QName(COMPLEX_SCHEMA_NS, "value"));

        ExpectedNode fixed = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                              Collections.singletonList(noChildren));
        fixed.setElemQName(new QName(COMPLEX_SCHEMA_NS, "fixed"));

        ExpectedNode any = new ExpectedNode(XmlSchemaStateMachineNode.Type.ANY, 1, 1,
                                            Collections.singletonList(noChildren));

        // backtrack
        SortedMap<Integer, ExpectedNode> backtrackSubSequenceChildren = new TreeMap<Integer, ExpectedNode>();
        backtrackSubSequenceChildren.put(0, qName);
        backtrackSubSequenceChildren.put(1, avroEnum);
        backtrackSubSequenceChildren.put(2, xmlEnumMaxOccurs2);
        backtrackSubSequenceChildren.put(3, unsignedLongList);
        backtrackSubSequenceChildren.put(4, listOfUnion);

        ExpectedNode backtrackSubSequence = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                             Collections
                                                                 .singletonList(backtrackSubSequenceChildren));

        SortedMap<Integer, ExpectedNode> backtrackSequenceChildren = new TreeMap<Integer, ExpectedNode>();
        backtrackSequenceChildren.put(1, backtrackSubSequence);

        ExpectedNode backtrackSequence = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                          Collections
                                                              .singletonList(backtrackSequenceChildren));

        SortedMap<Integer, ExpectedNode> backtrackChildren = new TreeMap<Integer, ExpectedNode>();
        backtrackChildren.put(0, backtrackSequence);

        ExpectedNode backtrack = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                  Collections.singletonList(backtrackChildren));
        backtrack.setElemQName(new QName(COMPLEX_SCHEMA_NS, "backtrack"));

        // allTheThings
        SortedMap<Integer, ExpectedNode> firstMapSeqChildren = new TreeMap<Integer, ExpectedNode>();
        firstMapSeqChildren.put(0, value);

        ExpectedNode firstMapSequence = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                         Collections.singletonList(firstMapSeqChildren));

        SortedMap<Integer, ExpectedNode> firstMapChildren = new TreeMap<Integer, ExpectedNode>();
        firstMapChildren.put(0, firstMapSequence);

        ExpectedNode firstMap = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                 Collections.singletonList(firstMapChildren));
        firstMap.setElemQName(new QName(COMPLEX_SCHEMA_NS, "firstMap"));

        ExpectedNode secondMap = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                  Collections.singletonList(noChildren));
        secondMap.setElemQName(new QName(COMPLEX_SCHEMA_NS, "secondMap"));

        SortedMap<Integer, ExpectedNode> allGroupChildren = new TreeMap<Integer, ExpectedNode>();

        allGroupChildren.put(0, firstMap);
        allGroupChildren.put(1, secondMap);

        ExpectedNode allGroup = new ExpectedNode(XmlSchemaStateMachineNode.Type.ALL, 1, 1,
                                                 Collections.singletonList(allGroupChildren));

        SortedMap<Integer, ExpectedNode> allTheThingsChildren = new TreeMap<Integer, ExpectedNode>();
        allTheThingsChildren.put(0, allGroup);

        ExpectedNode allTheThings = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                     Collections.singletonList(allTheThingsChildren));
        allTheThings.setElemQName(new QName(COMPLEX_SCHEMA_NS, "allTheThings"));

        // prohibit
        SortedMap<Integer, ExpectedNode> prohibitSeqChildren = new TreeMap<Integer, ExpectedNode>();
        prohibitSeqChildren.put(0, fixed);

        ExpectedNode prohibitSequence = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                         Collections.singletonList(prohibitSeqChildren));

        SortedMap<Integer, ExpectedNode> prohibitChildren = new TreeMap<Integer, ExpectedNode>();
        prohibitChildren.put(0, prohibitSequence);

        ExpectedNode prohibit = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                 Collections.singletonList(prohibitChildren));
        prohibit.setElemQName(new QName(COMPLEX_SCHEMA_NS, "prohibit"));

        // anyAndFriends
        SortedMap<Integer, ExpectedNode> anyAndFriendsSeqChildren = new TreeMap<Integer, ExpectedNode>();
        anyAndFriendsSeqChildren.put(0, any);
        anyAndFriendsSeqChildren.put(1, any);
        anyAndFriendsSeqChildren.put(2, any);
        anyAndFriendsSeqChildren.put(3, any);
        anyAndFriendsSeqChildren.put(4, any);
        anyAndFriendsSeqChildren.put(5, any);

        ExpectedNode anyAndFriendsSeq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                         Collections.singletonList(anyAndFriendsSeqChildren));

        SortedMap<Integer, ExpectedNode> anyAndFriendsChildren = new TreeMap<Integer, ExpectedNode>();
        anyAndFriendsChildren.put(0, anyAndFriendsSeq);

        ExpectedNode anyAndFriends = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                      Collections.singletonList(anyAndFriendsChildren));
        anyAndFriends.setElemQName(new QName(COMPLEX_SCHEMA_NS, "anyAndFriends"));

        // simpleExtension
        ExpectedNode simpleExtension = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 0, 1,
                                                        Collections.singletonList(noChildren));
        simpleExtension.setElemQName(new QName(COMPLEX_SCHEMA_NS, "simpleExtension"));

        // simpleRestriction
        ExpectedNode simpleRestriction = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 0, 1,
                                                          Collections.singletonList(noChildren));
        simpleRestriction.setElemQName(new QName(COMPLEX_SCHEMA_NS, "simpleRestriction"));

        // complexExtension
        SortedMap<Integer, ExpectedNode> complexExtensionSubSequenceChildren = new TreeMap<Integer, ExpectedNode>();
        complexExtensionSubSequenceChildren.put(0, fixed);

        ExpectedNode complexExtensionSubSequence = new ExpectedNode(
                                                                    XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                    1,
                                                                    1,
                                                                    Collections
                                                                        .singletonList(complexExtensionSubSequenceChildren));

        SortedMap<Integer, ExpectedNode> complexExtensionSubChoiceChildren = new TreeMap<Integer, ExpectedNode>();
        complexExtensionSubChoiceChildren.put(1, unsignedLongList);

        ExpectedNode complexExtensionSubChoice = new ExpectedNode(
                                                                  XmlSchemaStateMachineNode.Type.CHOICE,
                                                                  1,
                                                                  1,
                                                                  Collections
                                                                      .singletonList(complexExtensionSubChoiceChildren));

        SortedMap<Integer, ExpectedNode> complexExtensionSequenceChildren = new TreeMap<Integer, ExpectedNode>();
        complexExtensionSequenceChildren.put(0, complexExtensionSubSequence);
        complexExtensionSequenceChildren.put(1, complexExtensionSubChoice);

        ExpectedNode complexExtensionSequence = new ExpectedNode(
                                                                 XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                 1,
                                                                 1,
                                                                 Collections
                                                                     .singletonList(complexExtensionSequenceChildren));

        SortedMap<Integer, ExpectedNode> complexExtensionChildren = new TreeMap<Integer, ExpectedNode>();
        complexExtensionChildren.put(0, complexExtensionSequence);

        ExpectedNode complexExtension = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 0, 1,
                                                         Collections.singletonList(complexExtensionChildren));
        complexExtension.setElemQName(new QName(COMPLEX_SCHEMA_NS, "complexExtension"));

        // mixedType
        SortedMap<Integer, ExpectedNode> mixedTypeSeqChildren = new TreeMap<Integer, ExpectedNode>();
        mixedTypeSeqChildren.put(0, listOfUnion);
        mixedTypeSeqChildren.put(1, unsignedLongList);

        ExpectedNode mixedTypeSeq = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                     Collections.singletonList(mixedTypeSeqChildren));

        SortedMap<Integer, ExpectedNode> mixedTypeChildren = new TreeMap<Integer, ExpectedNode>();
        mixedTypeChildren.put(0, mixedTypeSeq);

        ExpectedNode mixedType = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 0, 1,
                                                  Collections.singletonList(mixedTypeChildren));
        mixedType.setElemQName(new QName(COMPLEX_SCHEMA_NS, "mixedType"));

        // realRoot
        SortedMap<Integer, ExpectedNode> realRootSequenceChildren = new TreeMap<Integer, ExpectedNode>();

        realRootSequenceChildren.put(0, backtrack);
        realRootSequenceChildren.put(1, allTheThings);
        realRootSequenceChildren.put(2, prohibit);
        realRootSequenceChildren.put(3, anyAndFriends);
        realRootSequenceChildren.put(4, simpleExtension);
        realRootSequenceChildren.put(5, simpleRestriction);
        realRootSequenceChildren.put(6, complexExtension);
        realRootSequenceChildren.put(7, mixedType);

        ExpectedNode realRootSequence = new ExpectedNode(XmlSchemaStateMachineNode.Type.SEQUENCE, 1, 1,
                                                         Collections.singletonList(realRootSequenceChildren));

        SortedMap<Integer, ExpectedNode> realRootChildren = new TreeMap<Integer, ExpectedNode>();
        realRootChildren.put(0, realRootSequence);

        ExpectedNode realRoot = new ExpectedNode(XmlSchemaStateMachineNode.Type.ELEMENT, 1, 1,
                                                 Collections.singletonList(realRootChildren));
        realRoot.setElemQName(new QName(COMPLEX_SCHEMA_NS, "realRoot"));

        SortedMap<Integer, ExpectedNode> rootSubstGrpChildren = new TreeMap<Integer, ExpectedNode>();
        rootSubstGrpChildren.put(0, realRoot);

        ExpectedNode rootSubstGrp = new ExpectedNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, 1, 1,
                                                     Collections.singletonList(rootSubstGrpChildren));

        ExpectedNode.validate(COMPLEX_SCHEMA_NS, rootSubstGrp, traversal.getDocumentNode(), expectedElements);

        ArrayList<ExpectedPathNode> expPath = new ArrayList<ExpectedPathNode>();

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, rootSubstGrp, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, realRoot, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, backtrack, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, backtrackSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, backtrackSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, qName, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, qName, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrackSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, avroEnum, 1));

        // Path Index 10
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, avroEnum, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrackSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, xmlEnumMaxOccurs2, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, xmlEnumMaxOccurs2, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, xmlEnumMaxOccurs2, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, xmlEnumMaxOccurs2, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrackSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, unsignedLongList, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, unsignedLongList, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrackSubSequence, 1));

        // Path Index 20
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, listOfUnion, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, listOfUnion, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrackSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrackSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, backtrack, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, allTheThings, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, allGroup, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, secondMap, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, allGroup, 1));

        // Path Index 30
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, firstMap, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, firstMapSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, value, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.SIBLING, value, 2));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, firstMapSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, firstMap, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, allGroup, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, allTheThings, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, prohibit, 1));

        // Path Index 40
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, prohibitSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, fixed, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, prohibitSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, prohibit, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, anyAndFriends, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriends, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, any, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriendsSeq, 1));

        // Path Index 50
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, any, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, any, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, any, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriendsSeq, 1));

        // Path Index 60
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, any, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, any, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriendsSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, anyAndFriends, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, anyAndFriends, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, simpleExtension, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, simpleExtension, 1));

        // Path Index 70
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, simpleRestriction, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, simpleRestriction, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, complexExtension, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, complexExtensionSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, complexExtensionSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, fixed, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, complexExtensionSubSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, complexExtensionSequence, 1));

        // Path Index 80
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, complexExtensionSubChoice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, unsignedLongList, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, unsignedLongList, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, complexExtensionSubChoice, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, complexExtensionSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, complexExtension, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, mixedType, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, mixedType, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, mixedTypeSeq, 1));

        // Path Index 90
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, listOfUnion, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, listOfUnion, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, mixedTypeSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, mixedTypeSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CHILD, unsignedLongList, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, unsignedLongList, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, mixedTypeSeq, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, mixedType, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.CONTENT, mixedType, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRootSequence, 1));

        // Path Index 100
        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, realRoot, 1));

        expPath.add(new ExpectedPathNode(XmlSchemaPathNode.Direction.PARENT, rootSubstGrp, 1));

        validate(expPath, traversal);
    }

    private <U, V> XmlSchemaPathNode<U, V> runTest(File schemaFile, File xmlFile, QName root) throws Exception {

        XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();

        FileReader schemaFileReader = null;
        try {
            schemaFileReader = new FileReader(schemaFile);
            xmlSchemaCollection.read(new StreamSource(schemaFileReader));
        } finally {
            if (schemaFileReader != null) {
                schemaFileReader.close();
            }
        }

        return runTest(xmlSchemaCollection, xmlFile, root);
    }

    private <U, V> XmlSchemaPathNode<U, V> runTest(XmlSchemaCollection xmlSchemaCollection, File xmlFile, QName root)
        throws Exception {

        XmlSchemaStateMachineGenerator stateMachineGen = new XmlSchemaStateMachineGenerator();

        XmlSchemaWalker walker = new XmlSchemaWalker(xmlSchemaCollection, stateMachineGen);

        XmlSchemaElement rootElement = xmlSchemaCollection.getElementByQName(root);

        walker.walk(rootElement);

        XmlSchemaStateMachineNode stateMachine = stateMachineGen.getStartNode();

        XmlSchemaPathFinder<U, V> pathFinder = new XmlSchemaPathFinder<U, V>(stateMachine);

        Document xmlDoc = docBuilder.parse(xmlFile);

        SaxWalkerOverDom saxWalker = new SaxWalkerOverDom(pathFinder);

        saxWalker.walk(xmlDoc);

        return pathFinder.getXmlSchemaTraversal();
    }

    <U, V> void validate(ArrayList<ExpectedPathNode> expPath, XmlSchemaPathNode<U, V> start) {
        XmlSchemaPathNode<U, V> prev = null;
        XmlSchemaPathNode<U, V> curr = start;
        int position = 0;

        do {
            assertTrue("Expected Path Too Short (position " + position + " >= expected " + expPath.size()
                       + ")", expPath.size() > position);

            assertEquals("Path Index " + position + "; prev != curr.getPrevious()", prev, curr.getPrevious());

            expPath.get(position).validate(position, curr);

            prev = curr;
            curr = curr.getNext();
            ++position;
        } while (curr != null);

        assertEquals(expPath.size(), position);
    }
}
