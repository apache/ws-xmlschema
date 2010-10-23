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


/**
 * Values for block and final attributes. Generally, either no value {@link #isNone()} returns true),
 * {@link #isAll()} returns true, or any number of the other booleans return true. 
 */
public class XmlSchemaDerivationMethod {
    public static final XmlSchemaDerivationMethod NONE = new XmlSchemaDerivationMethod();
    
    private boolean all;
    private boolean empty;
    private boolean extension;
    private boolean list;
    private boolean restriction;
    private boolean substitution;
    private boolean union;
    
    
    
    public XmlSchemaDerivationMethod() {
    }
    
    //TODO: not all contexts accept all these possibilities. Enforce here?
    public static XmlSchemaDerivationMethod schemaValueOf(String name) {
        String[] tokens = name.split("\\s");
        XmlSchemaDerivationMethod method = new XmlSchemaDerivationMethod();
        for (String t : tokens) {
            if ("#all".equalsIgnoreCase(t) || "all".equalsIgnoreCase(t)) {
                if (method.notAll()) {
                    throw new XmlSchemaException("Derivation method cannot be #all and something else.");
                } else {
                    method.setAll(true);
                }
            } else {
                if (method.isAll()) {
                    throw new XmlSchemaException("Derivation method cannot be #all and something else.");
                }
                if ("extension".equals(t)) {
                    method.setExtension(true);
                } else if ("list".equals(t)) {
                    method.setList(true);
                } else if ("restriction".equals(t)) {
                    method.setRestriction(true);
                } else if ("substitution".equals(t)) {
                    method.setSubstitution(true);
                } else if ("union".equals(t)) {
                    method.setUnion(true);
                }
            }
        }
        return method;
    }

    @Override
    public String toString() {
        if (isAll()) {
            return "#all";
        } else {
            StringBuilder sb = new StringBuilder();
            if (isExtension()) {
                sb.append("extension ");
            }
            if (isList()) {
                sb.append("list ");
            }
            if (isRestriction()) {
                sb.append("restriction ");
            }
            if (isSubstitution()) {
                sb.append("substitution ");
            }
            if (isUnion()) {
                sb.append("union ");
            }
            return sb.toString().trim();
        }
    }
    
    public boolean notAll() {
        return empty | extension | list | restriction | substitution | union;
    }
    

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
        if (all) {
            empty = false;
            extension = false; 
            list = false;
            restriction = false;
            substitution = false;
            union = false;
        }
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public boolean isExtension() {
        return extension;
    }

    public void setExtension(boolean extension) {
        this.extension = extension;
    }

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public boolean isNone() {
        return !(all || empty || extension || list || restriction || substitution || union);
    }

    public void setNone(boolean none) {
        all = false;
        empty = false;
        extension = false; 
        list = false;
        restriction = false;
        substitution = false;
        union = false;
    }

    public boolean isRestriction() {
        return restriction;
    }

    public void setRestriction(boolean restriction) {
        this.restriction = restriction;
    }

    public boolean isSubstitution() {
        return substitution;
    }

    public void setSubstitution(boolean substitution) {
        this.substitution = substitution;
    }

    public boolean isUnion() {
        return union;
    }

    public void setUnion(boolean union) {
        this.union = union;
    }
}
