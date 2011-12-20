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

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.xml.sax.InputSource;

import java.io.File;

/**
 *
 */
@Plugin(name="JSONConfigurationFactory", type="ConfigurationFactory")
@Order(6)
public class JSONConfigurationFactory extends ConfigurationFactory {

    public static final String[] SUFFIXES = new String[] {".json", ".jsn"};

    private File configFile = null;

    private static String[] dependencies = new String[] {
        "org.codehaus.jackson.JsonNode",
        "org.codehaus.jackson.map.ObjectMapper"
    };

    private boolean isActive;

    public JSONConfigurationFactory() {
        try {
            for (String item : dependencies) {
                Class.forName(item);
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.debug("Missing dependencies for Json support");
            isActive = false;
            return;
        }
        isActive = true;
    }

    @Override
    protected boolean isActive() {
        return isActive;
    }

    public Configuration getConfiguration(InputSource source) {
        if (!isActive) {
            return null;
        }
        return new JSONConfiguration(source, configFile);
    }

    public String[] getSupportedTypes() {
        return SUFFIXES;
    }
}
