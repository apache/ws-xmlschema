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

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.xml.namespace.QName;

/**
 * Describes an expected element or group in the document tree.
 */
public class ExpectedNode {

    XmlSchemaStateMachineNode.Type nodeType;
    long minOccurs;
    long maxOccurs;
    List<SortedMap<Integer, ExpectedNode>> children;
    QName elemQName;

    ExpectedNode(XmlSchemaStateMachineNode.Type nodeType, long minOccurs, long maxOccurs,
                 List<SortedMap<Integer, ExpectedNode>> children) {

        this.nodeType = nodeType;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.children = children;
    }

    void setElemQName(QName elemQName) {
        this.elemQName = elemQName;
    }

    static <U> void validate(String msg, ExpectedNode exp, XmlSchemaDocumentNode<U> docNode,
                         Map<QName, ExpectedElement> expElements) {

        assertEquals(msg, exp.nodeType, docNode.getStateMachineNode().getNodeType());

        assertEquals(msg, exp.minOccurs, docNode.getMinOccurs());
        assertEquals(msg, exp.maxOccurs, docNode.getMaxOccurs());
        assertEquals(msg, exp.children.size(), docNode.getIteration());

        if (docNode.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)) {

            QName actQName = docNode.getStateMachineNode().getElement().getQName();
            assertEquals(msg, exp.elemQName, actQName);

            if (expElements != null) {
                ExpectedElement expElem = expElements.get(actQName);
                assertNotNull(msg, expElem);
                expElem.validate(docNode);
            }
        }

        for (int iteration = 1; iteration <= docNode.getIteration(); ++iteration) {
            SortedMap<Integer, ExpectedNode> expected = exp.children.get(iteration - 1);

            SortedMap<Integer, XmlSchemaDocumentNode<U>> actual = docNode.getChildren(iteration);

            assertEquals(msg + ", iteration=" + iteration + "; " + exp.nodeType, expected.size(),
                         (actual == null) ? 0 : actual.size());

            if (actual != null) {
                for (Map.Entry<Integer, XmlSchemaDocumentNode<U>> actEntry : actual.entrySet()) {
                    ExpectedNode expNode = expected.get(actEntry.getKey());
                    assertNotNull(msg + ", iteration=" + iteration + ", child = " + actEntry.getKey()
                                  + ": entry " + actEntry.getKey() + " is not expected.", expNode);

                    validate(msg + "\titeration=" + iteration + "child=" + actEntry.getKey() + " ("
                             + exp.nodeType + ")", expNode, actEntry.getValue(), expElements);
                }
            }
        }
    }
}
