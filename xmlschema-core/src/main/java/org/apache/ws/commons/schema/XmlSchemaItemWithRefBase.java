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

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.utils.XmlSchemaRefBase;

/**
 * Common interface for all objects that can 'ref=' to a global item of the same type.
 * Note that this interface does not return the XmlSchemaRef, since that type is generic.
 * It is convenient to have a non-generic interface in common for the ref= items since
 * there are cases where code wants to treat them independent of the type.
 */
public interface XmlSchemaItemWithRefBase {
    /**
     * @return true if this object has a non-null ref.
     */
    boolean isRef();

    /**
     * @return the Qualified Name of the target of the ref.
     */
    QName getTargetQName();

    /**
     * @return the non-generic reference object.
     */
    XmlSchemaRefBase getRefBase();

}
