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

import javax.xml.namespace.QName;

class ExpectedStateMachineNode {
    XmlSchemaStateMachineNode.Type expNodeType;
    ExpectedElement expElem;
    QName expElemQName;
    List<ExpectedStateMachineNode> expNextStates;

    ExpectedStateMachineNode(XmlSchemaStateMachineNode.Type expNodeType, QName expElemQName,
                             ExpectedElement expElem) {
        this.expNodeType = expNodeType;
        this.expElem = expElem;
        this.expElemQName = expElemQName;
        this.expNextStates = new java.util.ArrayList<ExpectedStateMachineNode>();
    }

    void addNextState(ExpectedStateMachineNode expNextState) {
        expNextStates.add(expNextState);
    }

    void validate(XmlSchemaStateMachineNode actualNode) {
        assertEquals("Expected Type: " + expNodeType + "; actual: " + actualNode.getNodeType(), expNodeType,
                     actualNode.getNodeType());

        if (actualNode.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)) {
            assertEquals("Expected QName: " + expElemQName + "; actual: "
                         + actualNode.getElement().getQName(), expElemQName, actualNode.getElement()
                .getQName());

            expElem.validate(expElemQName.toString(), actualNode.getElementType());
        }

        assertEquals(expNodeType.name() + " number of children", expNextStates.size(), actualNode
            .getPossibleNextStates().size());
    }
}
