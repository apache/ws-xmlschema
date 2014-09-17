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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.testutils.UtilsForTests;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;
import org.junit.Test;

public class TestXmlSchemaStateMachineGenerator {

    private static final String TESTSCHEMA_NS = "http://avro.apache.org/AvroTest";
    private static final String COMPLEX_SCHEMA_NS = "urn:avro:complex_schema";

    @Test
    public void testSchema() throws IOException {
        final File schemaFile = UtilsForTests.buildFile("src", "test", "resources", "test_schema.xsd");

        XmlSchemaStateMachineNode stateMachine = buildSchema(schemaFile, new QName(TESTSCHEMA_NS, "root"));

        HashSet<QName> seen = new HashSet<QName>();

        // avro:root
        ExpectedElement rootElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        ExpectedStateMachineNode rootState = new ExpectedStateMachineNode(
                                                                          XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                          new QName(TESTSCHEMA_NS, "root"),
                                                                          rootElem);

        ExpectedStateMachineNode groupSequence = new ExpectedStateMachineNode(
                                                                              XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                              null, null);

        rootState.addNextState(groupSequence);

        ExpectedStateMachineNode groupChoice = new ExpectedStateMachineNode(
                                                                            XmlSchemaStateMachineNode.Type.CHOICE,
                                                                            null, null);

        groupSequence.addNextState(groupChoice);

        // avro:primitive
        ExpectedElement primitiveElem = new ExpectedElement(
                                                            new XmlSchemaTypeInfo(
                                                                                  XmlSchemaBaseSimpleType.STRING));

        QName primitiveQName = new QName(TESTSCHEMA_NS, "primitive");

        ExpectedStateMachineNode primitiveState = new ExpectedStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                               primitiveQName, primitiveElem);

        groupChoice.addNextState(primitiveState);

        // avro:nonNullPrimitive
        ExpectedElement nonNullPrimitiveElem = new ExpectedElement(
                                                                   new XmlSchemaTypeInfo(
                                                                                         XmlSchemaBaseSimpleType.STRING));

        QName nonNullPrimitiveQName = new QName(TESTSCHEMA_NS, "nonNullPrimitive");

        ExpectedStateMachineNode nonNullPrimitiveState = new ExpectedStateMachineNode(
                                                                                      XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                      nonNullPrimitiveQName,
                                                                                      nonNullPrimitiveElem);

        groupChoice.addNextState(nonNullPrimitiveState);

        // avro:record
        ExpectedElement recordElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName recordQName = new QName(TESTSCHEMA_NS, "record");

        ExpectedStateMachineNode recordState = new ExpectedStateMachineNode(
                                                                            XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                            recordQName, recordElem);

        recordState.addNextState(groupSequence);

        // avro:map
        ExpectedElement mapElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName mapQName = new QName(TESTSCHEMA_NS, "map");

        ExpectedStateMachineNode mapState = new ExpectedStateMachineNode(
                                                                         XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                         mapQName, mapElem);

        mapState.addNextState(groupSequence);

        ExpectedStateMachineNode recordSubstGrp = new ExpectedStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP,
                                                                               null, null);

        recordSubstGrp.addNextState(recordState);
        recordSubstGrp.addNextState(mapState);

        groupChoice.addNextState(recordSubstGrp);

        // avro:list
        ExpectedElement listElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName listQName = new QName(TESTSCHEMA_NS, "list");

        ExpectedStateMachineNode listState = new ExpectedStateMachineNode(
                                                                          XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                          listQName, listElem);

        groupChoice.addNextState(listState);

        ExpectedStateMachineNode listChoice = new ExpectedStateMachineNode(
                                                                           XmlSchemaStateMachineNode.Type.CHOICE,
                                                                           null, null);

        listChoice.addNextState(primitiveState);
        listChoice.addNextState(recordSubstGrp);

        listState.addNextState(listChoice);

        // avro:tuple
        ExpectedElement tupleElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName tupleQName = new QName(TESTSCHEMA_NS, "tuple");

        ExpectedStateMachineNode tupleState = new ExpectedStateMachineNode(
                                                                           XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                           tupleQName, tupleElem);

        groupChoice.addNextState(tupleState);

        ExpectedStateMachineNode groupOfAll = new ExpectedStateMachineNode(
                                                                           XmlSchemaStateMachineNode.Type.ALL,
                                                                           null, null);

        groupOfAll.addNextState(primitiveState);
        groupOfAll.addNextState(nonNullPrimitiveState);
        groupOfAll.addNextState(recordSubstGrp);
        groupOfAll.addNextState(listState);

        tupleState.addNextState(groupOfAll);

        validate(rootState, stateMachine, seen);
    }

    @Test
    public void testComplex() throws IOException {
        final File schemaFile = UtilsForTests.buildFile("src", "test", "resources", "complex_schema.xsd");

        XmlSchemaStateMachineNode stateMachine = buildSchema(schemaFile, new QName(COMPLEX_SCHEMA_NS, "root"));

        HashSet<QName> seen = new HashSet<QName>();

        ExpectedStateMachineNode rootSubstGrp = new ExpectedStateMachineNode(
                                                                             XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP,
                                                                             null, null);

        // realRoot
        ExpectedElement realRootElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName realRootQName = new QName(COMPLEX_SCHEMA_NS, "realRoot");

        ExpectedStateMachineNode realRootState = new ExpectedStateMachineNode(
                                                                              XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                              realRootQName, realRootElem);

        ExpectedStateMachineNode realRootSeq = new ExpectedStateMachineNode(
                                                                            XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                            null, null);

        // backtrack
        ExpectedElement backtrackElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName backtrackQName = new QName(COMPLEX_SCHEMA_NS, "backtrack");

        ExpectedStateMachineNode backtrackState = new ExpectedStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                               backtrackQName, backtrackElem);

        ExpectedStateMachineNode backtrackSeq = new ExpectedStateMachineNode(
                                                                             XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                             null, null);

        ExpectedStateMachineNode backtrackChoice = new ExpectedStateMachineNode(
                                                                                XmlSchemaStateMachineNode.Type.CHOICE,
                                                                                null, null);

        ExpectedStateMachineNode backtrackChoiceChoice = new ExpectedStateMachineNode(
                                                                                      XmlSchemaStateMachineNode.Type.CHOICE,
                                                                                      null, null);

        // avro:qName
        ExpectedElement qNameElem = new ExpectedElement(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.QNAME));

        QName qNameQName = new QName(COMPLEX_SCHEMA_NS, "qName");

        ExpectedStateMachineNode qNameState = new ExpectedStateMachineNode(
                                                                           XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                           qNameQName, qNameElem);

        // avro:avroEnum
        ExpectedElement avroEnumElem = new ExpectedElement(
                                                           new XmlSchemaTypeInfo(
                                                                                 XmlSchemaBaseSimpleType.STRING));

        QName avroEnumQName = new QName(COMPLEX_SCHEMA_NS, "avroEnum");

        ExpectedStateMachineNode avroEnumState = new ExpectedStateMachineNode(
                                                                              XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                              avroEnumQName, avroEnumElem);

        // avro:xmlEnum
        ExpectedElement xmlEnumElem = new ExpectedElement(
                                                          new XmlSchemaTypeInfo(
                                                                                XmlSchemaBaseSimpleType.STRING));

        QName xmlEnumQName = new QName(COMPLEX_SCHEMA_NS, "xmlEnum");

        ExpectedStateMachineNode xmlEnumState = new ExpectedStateMachineNode(
                                                                             XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                             xmlEnumQName, xmlEnumElem);

        ExpectedStateMachineNode backtrackChoiceSequence = new ExpectedStateMachineNode(
                                                                                        XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                                        null, null);

        // avro:unsignedLongList
        ExpectedElement unsignedLongListElem = new ExpectedElement(
                                                                   new XmlSchemaTypeInfo(
                                                                                         new XmlSchemaTypeInfo(
                                                                                                               XmlSchemaBaseSimpleType.DECIMAL)));

        QName unsignedLongListQName = new QName(COMPLEX_SCHEMA_NS, "unsignedLongList");

        ExpectedStateMachineNode unsignedLongListState = new ExpectedStateMachineNode(
                                                                                      XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                      unsignedLongListQName,
                                                                                      unsignedLongListElem);

        ExpectedStateMachineNode backtrackSeqSequence = new ExpectedStateMachineNode(
                                                                                     XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                                     null, null);

        // listOfUnion
        ArrayList<XmlSchemaTypeInfo> unionForListOfUnion = new ArrayList<XmlSchemaTypeInfo>();
        unionForListOfUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));
        unionForListOfUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING));
        unionForListOfUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));
        unionForListOfUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        ExpectedElement listOfUnionElem = new ExpectedElement(
                                                              new XmlSchemaTypeInfo(
                                                                                    new XmlSchemaTypeInfo(
                                                                                                          unionForListOfUnion)));

        QName listOfUnionQName = new QName(COMPLEX_SCHEMA_NS, "listOfUnion");

        ExpectedStateMachineNode listOfUnionState = new ExpectedStateMachineNode(
                                                                                 XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                 listOfUnionQName,
                                                                                 listOfUnionElem);

        // avro:allTheThings
        ExpectedElement allTheThingsElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName allTheThingsQName = new QName(COMPLEX_SCHEMA_NS, "allTheThings");

        ExpectedStateMachineNode allTheThingsState = new ExpectedStateMachineNode(
                                                                                  XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                  allTheThingsQName,
                                                                                  allTheThingsElem);

        ExpectedStateMachineNode allTheThingsAll = new ExpectedStateMachineNode(
                                                                                XmlSchemaStateMachineNode.Type.ALL,
                                                                                null, null);

        // firstMap
        ExpectedElement firstMapElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName firstMapQName = new QName(COMPLEX_SCHEMA_NS, "firstMap");

        ExpectedStateMachineNode firstMapState = new ExpectedStateMachineNode(
                                                                              XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                              firstMapQName, firstMapElem);

        ExpectedStateMachineNode firstMapSeq = new ExpectedStateMachineNode(
                                                                            XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                            null, null);

        // value
        ExpectedElement valueElem = new ExpectedElement(
                                                        new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        QName valueQName = new QName(COMPLEX_SCHEMA_NS, "value");

        ExpectedStateMachineNode valueState = new ExpectedStateMachineNode(
                                                                           XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                           valueQName, valueElem);

        // secondMap
        ExpectedElement secondMapElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName secondMapQName = new QName(COMPLEX_SCHEMA_NS, "secondMap");

        ExpectedStateMachineNode secondMapState = new ExpectedStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                               secondMapQName, secondMapElem);

        // prohibit
        ExpectedElement prohibitElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName prohibitQName = new QName(COMPLEX_SCHEMA_NS, "prohibit");

        ExpectedStateMachineNode prohibitState = new ExpectedStateMachineNode(
                                                                              XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                              prohibitQName, prohibitElem);

        ExpectedStateMachineNode prohibitSeq = new ExpectedStateMachineNode(
                                                                            XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                            null, null);

        // fixed
        ExpectedElement fixedElem = new ExpectedElement(
                                                        new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        QName fixedQName = new QName(COMPLEX_SCHEMA_NS, "fixed");

        ExpectedStateMachineNode fixedState = new ExpectedStateMachineNode(
                                                                           XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                           fixedQName, fixedElem);

        // avro:anyAndFriends
        ExpectedElement anyAndFriendsElem = new ExpectedElement(new XmlSchemaTypeInfo(true));

        QName anyAndFriendsQName = new QName(COMPLEX_SCHEMA_NS, "anyAndFriends");

        ExpectedStateMachineNode anyAndFriendsState = new ExpectedStateMachineNode(
                                                                                   XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                   anyAndFriendsQName,
                                                                                   anyAndFriendsElem);

        ExpectedStateMachineNode anyAndFriendsSeq = new ExpectedStateMachineNode(
                                                                                 XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                                 null, null);

        ExpectedStateMachineNode anyState = new ExpectedStateMachineNode(XmlSchemaStateMachineNode.Type.ANY,
                                                                         null, null);

        // avro:simpleExtension
        ArrayList<XmlSchemaTypeInfo> baseUnion = new ArrayList<XmlSchemaTypeInfo>();
        baseUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));
        baseUnion.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        ExpectedElement simpleExtensionElem = new ExpectedElement(new XmlSchemaTypeInfo(baseUnion));

        QName simpleExtensionQName = new QName(COMPLEX_SCHEMA_NS, "simpleExtension");

        ExpectedStateMachineNode simpleExtensionState = new ExpectedStateMachineNode(
                                                                                     XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                     simpleExtensionQName,
                                                                                     simpleExtensionElem);

        // simpleRestriction
        QName simpleRestrictionQName = new QName(COMPLEX_SCHEMA_NS, "simpleRestriction");

        ExpectedStateMachineNode simpleRestrictionState = new ExpectedStateMachineNode(
                                                                                       XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                       simpleRestrictionQName,
                                                                                       simpleExtensionElem);

        // complexExtension
        ExpectedElement complexExtensionElem = new ExpectedElement(new XmlSchemaTypeInfo(false));

        QName complexExtensionQName = new QName(COMPLEX_SCHEMA_NS, "complexExtension");

        ExpectedStateMachineNode complexExtensionState = new ExpectedStateMachineNode(
                                                                                      XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                                      complexExtensionQName,
                                                                                      complexExtensionElem);

        ExpectedStateMachineNode complexExtensionSeq = new ExpectedStateMachineNode(
                                                                                    XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                                    null, null);

        ExpectedStateMachineNode complexExtensionSeqChoice = new ExpectedStateMachineNode(
                                                                                          XmlSchemaStateMachineNode.Type.CHOICE,
                                                                                          null, null);

        ExpectedStateMachineNode complexExtensionSeqSequence = new ExpectedStateMachineNode(
                                                                                            XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                                            null, null);

        // avro:mixedType
        ExpectedElement mixedTypeElem = new ExpectedElement(new XmlSchemaTypeInfo(true));

        QName mixedTypeQName = new QName(COMPLEX_SCHEMA_NS, "mixedType");

        ExpectedStateMachineNode mixedTypeState = new ExpectedStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.ELEMENT,
                                                                               mixedTypeQName, mixedTypeElem);

        ExpectedStateMachineNode mixedTypeSeq = new ExpectedStateMachineNode(
                                                                             XmlSchemaStateMachineNode.Type.SEQUENCE,
                                                                             null, null);

        // Indentation follows schema
        rootSubstGrp.addNextState(realRootState);
        realRootState.addNextState(realRootSeq);
        realRootSeq.addNextState(backtrackState);
        backtrackState.addNextState(backtrackSeq);
        backtrackSeq.addNextState(backtrackChoice);
        backtrackChoice.addNextState(backtrackChoiceChoice);
        backtrackChoiceChoice.addNextState(qNameState);
        backtrackChoiceChoice.addNextState(avroEnumState);
        backtrackChoiceChoice.addNextState(xmlEnumState);
        backtrackChoice.addNextState(backtrackChoiceSequence);
        backtrackChoiceSequence.addNextState(xmlEnumState);
        backtrackChoiceSequence.addNextState(unsignedLongListState);
        backtrackSeq.addNextState(backtrackSeqSequence);
        backtrackSeqSequence.addNextState(qNameState);
        backtrackSeqSequence.addNextState(avroEnumState);
        backtrackSeqSequence.addNextState(xmlEnumState);
        backtrackSeqSequence.addNextState(unsignedLongListState);
        backtrackSeqSequence.addNextState(listOfUnionState);
        realRootSeq.addNextState(allTheThingsState);
        allTheThingsState.addNextState(allTheThingsAll);
        allTheThingsAll.addNextState(firstMapState);
        firstMapState.addNextState(firstMapSeq);
        firstMapSeq.addNextState(valueState);
        allTheThingsAll.addNextState(secondMapState);
        realRootSeq.addNextState(prohibitState);
        prohibitState.addNextState(prohibitSeq);
        prohibitSeq.addNextState(fixedState);
        realRootSeq.addNextState(anyAndFriendsState);
        anyAndFriendsState.addNextState(anyAndFriendsSeq);
        anyAndFriendsSeq.addNextState(anyState);
        anyAndFriendsSeq.addNextState(anyState);
        anyAndFriendsSeq.addNextState(anyState);
        anyAndFriendsSeq.addNextState(anyState);
        anyAndFriendsSeq.addNextState(anyState);
        anyAndFriendsSeq.addNextState(anyState);
        realRootSeq.addNextState(simpleExtensionState);
        realRootSeq.addNextState(simpleRestrictionState);
        realRootSeq.addNextState(complexExtensionState);
        complexExtensionState.addNextState(complexExtensionSeq);
        complexExtensionSeq.addNextState(complexExtensionSeqSequence);
        complexExtensionSeqSequence.addNextState(fixedState);
        complexExtensionSeq.addNextState(complexExtensionSeqChoice);
        complexExtensionSeqChoice.addNextState(listOfUnionState);
        complexExtensionSeqChoice.addNextState(unsignedLongListState);
        realRootSeq.addNextState(mixedTypeState);
        mixedTypeState.addNextState(mixedTypeSeq);
        mixedTypeSeq.addNextState(listOfUnionState);
        mixedTypeSeq.addNextState(unsignedLongListState);

        validate(rootSubstGrp, stateMachine, seen);
    }

    private XmlSchemaStateMachineNode buildSchema(File schemaFile, QName root) throws IOException {

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

        XmlSchemaStateMachineGenerator stateMachineGen = new XmlSchemaStateMachineGenerator();

        XmlSchemaWalker walker = new XmlSchemaWalker(xmlSchemaCollection, stateMachineGen);

        XmlSchemaElement rootElement = xmlSchemaCollection.getElementByQName(root);

        walker.walk(rootElement);

        return stateMachineGen.getStartNode();
    }

    private void validate(ExpectedStateMachineNode exp, XmlSchemaStateMachineNode act, HashSet<QName> seen) {

        exp.validate(act);

        if (exp.expNodeType.equals(XmlSchemaStateMachineNode.Type.ELEMENT)) {
            /*
             * The state machine may fold back onto itself if an element is a
             * child of itself. Likewise, we need to keep track of what we've
             * seen so we do not traverse state machine nodes again.
             */
            seen.add(exp.expElemQName);
        }

        for (int idx = 0; idx < exp.expNextStates.size(); ++idx) {
            ExpectedStateMachineNode expNext = exp.expNextStates.get(idx);
            XmlSchemaStateMachineNode actNext = act.getPossibleNextStates().get(idx);

            if (expNext.expNodeType.equals(XmlSchemaStateMachineNode.Type.ELEMENT)
                && seen.contains(expNext.expElemQName)) {

                // We've seen this one; no need to follow it.
                expNext.validate(actNext);
                continue;
            }

            validate(expNext, actNext, seen);
        }

    }
}
