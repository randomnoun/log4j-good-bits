/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Represents a key/value pair in the configuration.
 */
@Plugin(name = "property", type = "Core", printObject = true)
public final class Property {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private String name;
    private String value;

    private Property(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Return the property name.
     * @return the property name.
     */
    public String getName() {
        return name;
    }

    /**
     * Return the property value.
     * @return the value of the property.
     */
    public String getValue() {
        return value;
    }

    /**
     * Create a Property.
     * @param key The key.
     * @param value The value.
     * @return A Property.
     */
    @PluginFactory
    public static Property createProperty(@PluginAttr("name") String key,
                                          @PluginValue("value") String value) {
        if (key == null) {
            LOGGER.error("Property key cannot be null");
        }
        return new Property(key, value);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
