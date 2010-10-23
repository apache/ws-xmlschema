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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * There are many collections of XML Schema objects inside XmlSchema. This class provides consistent
 * construction to centralize policy for thread synchronization and the like.
 */
public final class CollectionFactory {

    private static final String PROTECT_READ_ONLY_COLLECTIONS_PROP =
        "org.apache.ws.commons.schema.protectReadOnlyCollections";

    private static final ThreadLocal<Boolean> PROTECT_READ_ONLY_COLLECTIONS = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return Boolean.parseBoolean(System.getProperty(PROTECT_READ_ONLY_COLLECTIONS_PROP));
        }
    };

    private CollectionFactory() {
    }

    public static <T> List<T> getList(Class<T> type) {
        return Collections.synchronizedList(new ArrayList<T>());
    }

    public static <T> Set<T> getSet(Class<T> type) {
        return Collections.synchronizedSet(new HashSet<T>());
    }

    /**
     * Call this to obtain a list to return from a public API where the caller is not supposed to modify the
     * list. If org.apache.ws.commons.schema.protectReadOnlyCollections is 'true', this will return a list
     * that checks at runtime.
     *
     * @param <T> Generic parameter type of the list.
     * @param list the list.
     * @return
     */
    public static <T> List<T> getProtectedList(List<T> list) {
        if (PROTECT_READ_ONLY_COLLECTIONS.get().booleanValue()) {
            return Collections.unmodifiableList(list);
        } else {
            return list;
        }
    }

    /**
     * Call this to obtain a map to return from a public API where the caller is not supposed to modify the
     * map. If org.apache.ws.commons.schema.protectReadOnlyCollections is 'true', this will return a map that
     * checks at runtime.
     *
     * @param <K> key type
     * @param <V> value type
     * @param map the map.
     * @return
     */
    public static <K, V> Map<K, V> getProtectedMap(Map<K, V> map) {
        if (PROTECT_READ_ONLY_COLLECTIONS.get().booleanValue()) {
            return Collections.unmodifiableMap(map);
        } else {
            return map;
        }
    }

    public static void withSchemaModifiable(Runnable action) {
        Boolean saved = PROTECT_READ_ONLY_COLLECTIONS.get();
        try {
            PROTECT_READ_ONLY_COLLECTIONS.set(Boolean.FALSE);
            action.run();
        } finally {
            PROTECT_READ_ONLY_COLLECTIONS.set(saved);
        }
    }
}
