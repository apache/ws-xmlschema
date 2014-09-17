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

import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;

import static org.junit.Assert.*;

/**
 * This is a simpler representation of an XML element to facilitate easier
 * testing.
 */
class ExpectedElement {

    private XmlSchemaTypeInfo typeInfo;

    ExpectedElement(XmlSchemaTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    void validate(XmlSchemaDocumentNode docNode) {
        String qName = docNode.getStateMachineNode().getElement().getQName().toString();

        XmlSchemaTypeInfo actType = docNode.getStateMachineNode().getElementType();

        validate(qName, actType);
    }

    void validate(String qName, XmlSchemaTypeInfo actType) {
        assertEquals(qName, typeInfo.getType(), actType.getType());
        assertEquals(qName, typeInfo.getBaseType(), actType.getBaseType());

        assertEquals(qName, typeInfo.getUserRecognizedType(), actType.getUserRecognizedType());
    }
}
