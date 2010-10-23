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
import java.util.Collections;
import java.util.List;

/**
 * Class for the identity constraints: key, keyref, and unique elements.
 */

public class XmlSchemaIdentityConstraint extends XmlSchemaAnnotated {

    private List<XmlSchemaXPath> fields;

    private String name;

    private XmlSchemaXPath selector;

    /**
     * Creates new XmlSchemaIdentityConstraint
     */
    public XmlSchemaIdentityConstraint() {
        fields = Collections.synchronizedList(new ArrayList<XmlSchemaXPath>());
    }

    public List<XmlSchemaXPath> getFields() {
        return fields;
    }

    public String getName() {
        return name;
    }

    public XmlSchemaXPath getSelector() {
        return selector;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSelector(XmlSchemaXPath selector) {
        this.selector = selector;
    }

    void setFields(List<XmlSchemaXPath> fields) {
        this.fields = fields;
    }

}
