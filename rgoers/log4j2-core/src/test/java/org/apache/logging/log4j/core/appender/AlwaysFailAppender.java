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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 *
 */
@Plugin(name="AlwaysFail",type="Core",elementType="appender",printObject=true)
public class AlwaysFailAppender extends AppenderBase {

    private AlwaysFailAppender(String name) {
        super(name, null, null, false);
    }

    public void append(LogEvent event) {
        throw new LoggingException("Always fail");
    }

    @PluginFactory
    public static AlwaysFailAppender createAppender(@PluginAttr("name") String name) {
        if (name == null) {
            logger.error("A name for the Appender must be specified");
            return null;
        }

        return new AlwaysFailAppender(name);
    }

}
