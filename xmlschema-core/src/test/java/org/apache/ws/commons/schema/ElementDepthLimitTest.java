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

import org.junit.Assert;
import org.junit.Test;

/**
 * The parser element-depth limit that XmlSchemaCollection derives from maxNestingDepth.
 */
public class ElementDepthLimitTest extends Assert {

    @Test
    public void testLimitIsTwiceTheNestingDepthPlus64() {
        assertEquals(1088L, XmlSchemaCollection.elementDepthLimit(512));
    }

    @Test
    public void testExtremeNestingDepthDoesNotDisableTheLimit() {
        assertEquals(30L, XmlSchemaCollection.elementDepthLimit(-17));
        assertEquals(1L, XmlSchemaCollection.elementDepthLimit(-32));
        assertEquals(1L, XmlSchemaCollection.elementDepthLimit(-33));
        assertEquals(1L, XmlSchemaCollection.elementDepthLimit(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, XmlSchemaCollection.elementDepthLimit(Integer.MAX_VALUE));
    }
}
