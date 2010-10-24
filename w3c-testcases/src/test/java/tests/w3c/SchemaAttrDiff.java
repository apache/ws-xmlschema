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

package tests.w3c;

import org.w3c.dom.Element;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.IgnoreTextAndAttributeValuesDifferenceListener;

class SchemaAttrDiff extends IgnoreTextAndAttributeValuesDifferenceListener {

    public int differenceFound(Difference difference) {

        if (difference.getId() == DifferenceConstants.ELEMENT_NUM_ATTRIBUTES.getId()) {
            // control and test have to be elements
            // check if they are schema elements .. they only
            // seem to have the added attributeFormDefault and
            // elementFormDefault attributes
            // so shldnt have more than 2 attributes difference
            Element actualEl = (Element)difference.getControlNodeDetail().getNode();

            if (actualEl.getLocalName().equals("schema")) {

                int expectedAttrs = Integer.parseInt(difference.getControlNodeDetail().getValue());
                int actualAttrs = Integer.parseInt(difference.getTestNodeDetail().getValue());
                if (Math.abs(actualAttrs - expectedAttrs) <= 2) {
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }
            }
        } else if (difference.getId() == DifferenceConstants.ATTR_NAME_NOT_FOUND_ID) {
            // sometimes the serializer throws in a few extra attributes...
            Element actualEl = (Element)difference.getControlNodeDetail().getNode();

            if (actualEl.getLocalName().equals("schema")) {
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            }
        }

        return super.differenceFound(difference);
    }
}