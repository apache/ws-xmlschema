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

import java.util.ArrayList;
import java.util.List;

/**
 * Requires the elements in the group to appear in the specified sequence within the containing element.
 * Represents the World Wide Web Consortium (W3C) sequence (compositor) element.
 * 
 * (element|group|choice|sequence|any)
 */

public class XmlSchemaSequence extends XmlSchemaGroupParticle implements XmlSchemaChoiceMember,
    XmlSchemaSequenceMember {
    
    private List<XmlSchemaSequenceMember> items;

    /**
     * Creates new XmlSchemaSequence
     */
    public XmlSchemaSequence() {
        items = new ArrayList<XmlSchemaSequenceMember>();
    }

    /**
     * The elements contained within the compositor. Collection of XmlSchemaElement, XmlSchemaGroupRef,
     * XmlSchemaChoice, XmlSchemaSequence, or XmlSchemaAny.
     */
    public List<XmlSchemaSequenceMember> getItems() {
        return items;
    }
}
