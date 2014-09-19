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

/**
 * Represents the expected {@link XmlSchemaPathNode} at a particular link in the
 * chain.
 */
class ExpectedPathNode {
    ExpectedNode expDocNode;
    XmlSchemaPathNode.Direction expDirection;
    int expIteration;

    ExpectedPathNode(XmlSchemaPathNode.Direction expectedDirection, ExpectedNode expectedDocNode,
                     int expectedIteration) {
        expDocNode = expectedDocNode;
        expDirection = expectedDirection;
        expIteration = expectedIteration;
    }

    void validate(int pathIndex, XmlSchemaPathNode<?, ?> actualPathNode) {
        assertEquals("Path Index: " + pathIndex, expDirection, actualPathNode.getDirection());

        ExpectedNode.validate("Path Index: " + pathIndex, expDocNode, actualPathNode.getDocumentNode(), null);

        assertEquals("Path Index: " + pathIndex, expIteration, actualPathNode.getIteration());
    }
}
