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

package org.apache.ws.commons.schema.utils;


import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaNotation;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Implementation for ref='QName', a common construct in the schema.
 */
public class XmlSchemaRef<T extends XmlSchemaNamed> extends XmlSchemaRefBase {
    private Class<? extends T> targetClass;
    private T targetObject;

    public XmlSchemaRef(XmlSchema parent, Class<T> targetClass) {
        this.parent = parent;
        this.targetClass = targetClass;
    }

    protected void forgetTargetObject() {
        targetObject = null;
    }


    public T getTarget() {

        if (targetObject == null && targetQName != null) {
            Class<?> cls = targetClass;
            XmlSchemaCollection parentCollection = parent.getParent();
            if (cls == XmlSchemaElement.class) {
                targetObject = targetClass.cast(parentCollection.getElementByQName(targetQName));
            } else if (cls == XmlSchemaAttribute.class) {
                targetObject = targetClass.cast(parentCollection.getAttributeByQName(targetQName));
            } else if (cls == XmlSchemaType.class) {
                targetObject = targetClass.cast(parentCollection.getTypeByQName(targetQName));
            } else if (cls == XmlSchemaAttributeGroup.class) {
                targetObject = targetClass.cast(parentCollection.getAttributeGroupByQName(targetQName));
            } else if (cls == XmlSchemaGroup.class) {
                targetObject = targetClass.cast(parentCollection.getGroupByQName(targetQName));
            } else if (cls == XmlSchemaNotation.class) {
                targetObject = targetClass.cast(parentCollection.getNotationByQName(targetQName));
            }
        }
        return targetObject;
    }

    @Override
    public String toString() {
        return "XmlSchemaRef: " + targetClass.getName() + " " + targetQName;
    }
}
