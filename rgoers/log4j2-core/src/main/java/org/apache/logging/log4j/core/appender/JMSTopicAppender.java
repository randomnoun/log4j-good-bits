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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.JMSTopicManager;

/**
 * Appender to write to a JMS Topic.
 */
@Plugin(name = "JMSTopic", type = "Core", elementType = "appender", printObject = true)
public final class JMSTopicAppender extends AppenderBase {

    private final JMSTopicManager manager;

    private JMSTopicAppender(String name, Filter filter, Layout layout, JMSTopicManager manager,
                            boolean handleExceptions) {
        super(name, filter, layout, handleExceptions);
        this.manager = manager;
    }

    /**
     * Actual writing occurs here.
     * <p/>
     * @param event The LogEvent.
     */
    public void append(LogEvent event) {
        try {
            manager.send(getLayout().formatAs(event));
        } catch (Exception ex) {
            throw new AppenderRuntimeException(ex);
        }
    }

    /**
     * Create a JMSTopicAppender.
     * @param factoryName The fully qualified class name of the InitialContextFactory.
     * @param providerURL The URL of the provider to use.
     * @param urlPkgPrefixes A colon-separated list of package prefixes for the class name of the factory class that
     * will create a URL context factory
     * @param securityPrincipalName The name of the identity of the Principal.
     * @param securityCredentials The security credentials of the Principal.
     * @param factoryBindingName The name to locate in the Context that provides the TopicConnectionFactory.
     * @param topicBindingName The name to use to locate the Topic.
     * @param userName The userid to use to create the Topic Connection.
     * @param password The password to use to create the Topic Connection.
     * @param layout The layout to use (defaults to SerlializedLayout).
     * @param filter The Filter or null.
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @return The JMSTopicAppender.
     */
    @PluginFactory
    public static JMSTopicAppender createAppender(@PluginAttr("factoryName") String factoryName,
                                                  @PluginAttr("providerURL") String providerURL,
                                                  @PluginAttr("urlPkgPrefixes") String urlPkgPrefixes,
                                                  @PluginAttr("securityPrincipalName") String securityPrincipalName,
                                                  @PluginAttr("securityCredentials") String securityCredentials,
                                                  @PluginAttr("factoryBindingName") String factoryBindingName,
                                                  @PluginAttr("topicBindingName") String topicBindingName,
                                                  @PluginAttr("userName") String userName,
                                                  @PluginAttr("password") String password,
                                                  @PluginElement("layout") Layout layout,
                                                  @PluginElement("filters") Filter filter,
                                                  @PluginAttr("suppressExceptions") String suppress) {

        String name = "JMSTopic" + factoryBindingName + "." + topicBindingName;
        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        JMSTopicManager manager = JMSTopicManager.getJMSTopicManager(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, topicBindingName, userName, password);
        if (manager == null) {
            return null;
        }
        if (layout == null) {
            layout = SerializedLayout.createLayout();
        }
        return new JMSTopicAppender(name, filter, layout, manager, handleExceptions);
    }
}
