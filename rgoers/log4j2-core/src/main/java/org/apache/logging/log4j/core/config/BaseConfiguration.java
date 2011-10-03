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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.appender.DefaultErrorHandler;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.Filters;
import org.apache.logging.log4j.core.helpers.NameUtil;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Base Configuration. Many configuration implementations will extend this class.
 */
public class BaseConfiguration extends Filterable implements Configuration {

    protected Node rootNode;

    protected PluginManager pluginManager;

    protected final static Logger logger = StatusLogger.getLogger();

    protected final List<ConfigurationListener> listeners =
        new CopyOnWriteArrayList<ConfigurationListener>();

    protected ConfigurationMonitor monitor = new DefaultConfigurationMonitor();

    private String name;

    private ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<String, Appender>();

    private ConcurrentMap<String, LoggerConfig> loggers = new ConcurrentHashMap<String, LoggerConfig>();

    private StrSubstitutor subst = new StrSubstitutor();

    private LoggerConfig root = new LoggerConfig();

    private boolean started = false;

    /**
     * Constructor.
     */
    protected BaseConfiguration() {
        pluginManager = new PluginManager("Core");
        rootNode = new Node();
    }

    /**
     * Initialize the configuration.
     */
    public void start() {
        pluginManager.collectPlugins();
        setup();
        doConfigure();
        for (Appender appender : appenders.values()) {
            appender.start();
        }

        startFilters();
    }

    /**
     * Tear down the configuration.
     */
    public void stop() {
        for (LoggerConfig logger : loggers.values()) {
            logger.clearAppenders();
        }
        for (Appender appender : appenders.values()) {
            appender.stop();
        }
        stopFilters();
    }

    protected void setup() {
    }

    protected void doConfigure() {
        boolean setRoot = false;
        boolean setLoggers = false;
        for (Node child : rootNode.getChildren()) {
            createConfiguration(child);
            if (child.getObject() == null) {
                continue;
            }
            if (child.getName().equalsIgnoreCase("properties")) {
                subst = (StrSubstitutor) child.getObject();
            } else if (child.getName().equals("appenders")) {
                appenders = (ConcurrentMap<String, Appender>) child.getObject();
            } else if (child.getName().equals("filters")) {
                setFilters((Filters) child.getObject());
            } else if (child.getName().equals("loggers")) {
                Loggers l = (Loggers) child.getObject();
                loggers = l.getMap();
                setLoggers = true;
                if (l.getRoot() != null) {
                    root = l.getRoot();
                    setRoot = true;
                }
            } else {
                logger.error("Unknown object \"" + child.getName() + "\" of type " +
                    child.getObject().getClass().getName() + " is ignored");
            }
        }

        if (!setLoggers) {
            logger.warn("No Loggers were configured, using default");
        } else if (!setRoot) {
            logger.warn("No Root logger was configured, using default");
        }

        root.setConfigurationMonitor(monitor);

        for (Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            LoggerConfig l = entry.getValue();
            l.setConfigurationMonitor(monitor);
            for (String ref : l.getAppenderRefs()) {
                Appender app = appenders.get(ref);
                if (app != null) {
                    l.addAppender(app);
                } else {
                    logger.error("Unable to locate appender " + ref + " for logger " + l.getName());
                }
            }
        }

        setParents();
    }

    protected PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Set the name of the configuration.
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the name of the configuration.
     * @return the name of the configuration.
     */
    public String getName() {
        return name;
    }

    /**
     * Add a listener for changes on the configuration.
     * @param listener The ConfigurationListener to add.
     */
    public void addListener(ConfigurationListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a ConfigurationListener.
     * @param listener The ConfigurationListener to remove.
     */
    public void removeListener(ConfigurationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Return the Appender with the specified name.
     * @param name The name of the Appender.
     * @return the Appender with the specified name or null if the Appender cannot be located.
     */
    public Appender getAppender(String name) {
        return appenders.get(name);
    }

    /**
     * Return a Map containing all the Appenders and their name.
     * @return A Map containing each Appender's naem and the Appender object.
     */
    public Map<String, Appender> getAppenders() {
        return appenders;
    }

    /**
     * Adds an Appender to the configuration.
     * @param appender The Appender to add.
     */
    public void addAppender(Appender appender) {
        appenders.put(appender.getName(), appender);
    }

    /**
     * Associates an Appender with a LoggerConfig. This method is synchronized in case a Logger with the
     * same name is being updated at the same time.
     *
     * Note: This method is not used when configuring via configuration. It is primarily used by
     * unit tests.
     * @param logger The Logger the Appender will be associated with.
     * @param appender The Appender.
     */
    public synchronized void addLoggerAppender(org.apache.logging.log4j.core.Logger logger, Appender appender) {
        String name = logger.getName();
        appenders.putIfAbsent(name, appender);
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.addAppender(appender);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), lc.isAdditive());
            nlc.setConfigurationMonitor(monitor);
            nlc.addAppender(appender);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }
    /**
     * Associates a Filter with a LoggerConfig. This method is synchronized in case a Logger with the
     * same name is being updated at the same time.
     *
     * Note: This method is not used when configuring via configuration. It is primarily used by
     * unit tests.
     * @param logger The Logger the Fo;ter will be associated with.
     * @param filter The Filter.
     */
    public synchronized void addLoggerFilter(org.apache.logging.log4j.core.Logger logger, Filter filter) {
        String name = logger.getName();
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.addFilter(filter);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), lc.isAdditive());
            nlc.setConfigurationMonitor(monitor);
            nlc.addFilter(filter);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }
    /**
     * Marks a LoggerConfig as additive. This method is synchronized in case a Logger with the
     * same name is being updated at the same time.
     *
     * Note: This method is not used when configuring via configuration. It is primarily used by
     * unit tests.
     * @param logger The Logger the Appender will be associated with.
     * @param additive True if the LoggerConfig should be additive, false otherwise.
     */
    public synchronized void setLoggerAdditive(org.apache.logging.log4j.core.Logger logger, boolean additive) {
        String name = logger.getName();
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.setAdditive(additive);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), additive);
            nlc.setConfigurationMonitor(monitor);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }

    /**
     * Remove an Appender. First removes any associations between LoggerContigs and the Appender, removes
     * the Appender from this appender list and then stops the appender. This method is synchronized in
     * case an Appender with the same name is being added during the removal.
     */
    public synchronized void removeAppender(String name) {
        for (LoggerConfig logger : loggers.values()) {
            logger.removeAppender(name);
        }
        Appender app = appenders.remove(name);

        if (app != null) {
            app.stop();
        }
    }

    /**
     * Locates the appropriate LoggerConfig for a Logger name. This will remove tokens from the
     * package name as necessary or return the root LoggerConfig if no other matches were found.
     * @param name The Logger name.
     * @return The located LoggerConfig.
     */
    public LoggerConfig getLoggerConfig(String name) {
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        int i = 0;
        String substr = name;
        while ((substr = NameUtil.getSubName(substr)) != null) {
            if (loggers.containsKey(substr)) {
                return loggers.get(substr);
            }
        }
        return root;
    }

    /**
     * Returns the root Logger.
     * @return the root Logger.
     */
    public LoggerConfig getRootLogger() {
        return root;
    }

    /**
     * Return a Map of all the LoggerConfigs.
     * @return a Map with each entry containing the name of the Logger and the LoggerConfig.
     */
    public Map<String, LoggerConfig> getLoggers() {
        return Collections.unmodifiableMap(loggers);
    }

    /**
     * Returns the LoggerConfig with the specified name.
     * @param name The Logger name.
     * @return The LoggerConfig or null if no match was found.
     */
    public LoggerConfig getLogger(String name) {
        return loggers.get(name);
    }

    /**
     * Adding a logger cannot be done atomically so is not allowed in an active configuration. Adding
     * or removing a Logger requires creating a new configuration and then switching.
     *
     * @param name The name of the Logger.
     * @param loggerConfig The LoggerConfig.
     */
    public void addLogger(String name, LoggerConfig loggerConfig) {
        if (started) {
            String msg = "Cannot add logger " + name + " to an active configuration";
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }
        loggers.put(name, loggerConfig);
        setParents();
    }

    /**
     * Removing a logger cannot be done atomically so is not allowed in an active configuration. Adding
     * or removing a Logger requires creating a new configuration and then switching.
     *
     * @param name The name of the Logger.
     */
    public void removeLogger(String name) {
        if (started) {
            String msg = "Cannot remove logger " + name + " in an active configuration";
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }
        loggers.remove(name);
        setParents();
    }

    private void createConfiguration(Node node) {
        for (Node child : node.getChildren()) {
            createConfiguration(child);
        }
        PluginType type = node.getType();
        if (type == null) {
            if (node.getParent() != null) {
                logger.error("Unable to locate plugin for " + node.getName());
            }
        } else {
            node.setObject(createPluginObject(type, node));
        }
    }
   /*
    * Retrieve a static public 'method to create the desired object. Every parameter
    * will be annotated to identify the appropriate attribute or element to use to
    * set the value of the paraemter.
    * Parameters annotated with PluginAttr will always be set as Strings.
    * Parameters annotated with PluginElement may be Objects or arrays. Collections
    * and Maps are currently not supported, although the factory method that is called
    * can create these from an array.
    *
    * Although the happy path works, more work still needs to be done to log incorrect
    * parameters. These will generally result in unhelpful InvocationTargetExceptions.
    * @param classClass the class.
    * @return the instantiate method or null if there is none by that
    * description.
    */
    public Object createPluginObject(PluginType type, Node node)
    {
        Class clazz = type.getPluginClass();

        if (Map.class.isAssignableFrom(clazz)) {
            try {
                Map map = (Map) clazz.newInstance();
                for (Node child : node.getChildren()) {
                    map.put(child.getName(), child.getObject());
                }
                return map;
            } catch (Exception ex) {

            }
        }

        if (List.class.isAssignableFrom(clazz)) {
            try {
                List list = (List) clazz.newInstance();
                for (Node child : node.getChildren()) {
                    list.add(child.getObject());
                }
                return list;
            } catch (Exception ex) {

            }
        }

        Method factoryMethod = null;

        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(PluginFactory.class)) {
                factoryMethod = method;
                break;
            }
        }
        if (factoryMethod == null) {
            return null;
        }

        Annotation[][] parmArray = factoryMethod.getParameterAnnotations();
        Class[] parmClasses = factoryMethod.getParameterTypes();
        if (parmArray.length != parmClasses.length) {
            logger.error("");
        }
        Object[] parms = new Object[parmClasses.length];

        int index = 0;
        Map<String, String> attrs = node.getAttributes();
        List<Node> children = node.getChildren();
        StringBuilder sb = new StringBuilder();
        List<Node> used = new ArrayList<Node>();

        /*
         * For each parameter:
         * If the parameter is an attribute store the value of the attribute in the parameter array.
         * If the parameter is an element:
         *   Determine if the required parameter is an array.
         *     If so, if a child contains the array, use it,
         *      otherwise create the array from all child nodes of the correct type.
         *     Store the array into the parameter array.
         *   If not an array, store the object in the child node into the parameter array.
         */
        for (Annotation[] parmTypes : parmArray) {
            for (Annotation a : parmTypes) {
                if (sb.length() == 0) {
                    sb.append(" with params(");
                } else {
                    sb.append(", ");
                }
                if (a instanceof PluginValue) {
                    String name = ((PluginValue)a).value();
                    String value = subst.replace(node.getValue());
                    sb.append(name +"=" + "\"" + value + "\"");
                    parms[index] = value;
                }
                if (a instanceof PluginAttr) {
                    String name = ((PluginAttr)a).value();
                    String value = subst.replace(getAttrValue(name, attrs));
                    sb.append(name +"=" + "\"" + value + "\"");
                    parms[index] = value;
                } else if (a instanceof PluginElement) {
                    PluginElement elem = (PluginElement)a;
                    String name = elem.value();
                    if (parmClasses[index].isArray()) {
                        Class parmClass = parmClasses[index].getComponentType();
                        List<Object> list = new ArrayList<Object>();
                        sb.append("{");
                        boolean first = true;
                        for (Node child : children) {
                            PluginType childType = child.getType();
                            if (elem.value().equals(childType.getElementName()) ||
                                parmClass.isAssignableFrom(childType.getPluginClass())) {
                                used.add(child);
                                if (!first) {
                                    sb.append(", ");
                                }
                                first = false;
                                Object obj = child.getObject();
                                if (obj.getClass().isArray()) {
                                    printArray(sb, (Object[])obj);
                                    parms[index] = obj;
                                    break;
                                }
                                sb.append(child.toString());
                                list.add(obj);
                            }
                        }
                        sb.append("}");
                        if (parms[index] != null) {
                            break;
                        }
                        Object[] array = (Object[]) Array.newInstance(parmClass, list.size());
                        int i=0;
                        for (Object obj : list) {
                            array[i] = obj;
                            ++i;
                        }
                        parms[index] = array;
                    } else {
                        Class parmClass = parmClasses[index];
                        boolean present = false;
                        for (Node child : children) {
                            PluginType childType = child.getType();
                            if (elem.value().equals(childType.getElementName()) ||
                                parmClass.isAssignableFrom(childType.getPluginClass())) {
                                sb.append(child.toString());
                                present = true;
                                used.add(child);
                                parms[index] = child.getObject();
                                break;
                            }
                        }
                        if (!present) {
                            sb.append("null");
                        }
                    }
                }
            }
            ++index;
        }
        if (sb.length() > 0) {
            sb.append(")");
        }

        if (attrs.size() > 0) {
            StringBuilder eb = new StringBuilder();
            for (String key : attrs.keySet()) {
                if (eb.length() == 0) {
                    eb.append(node.getName());
                    eb.append(" contains ");
                    if (attrs.size() == 1) {
                        eb.append("an invalid element or attribute ");
                    } else {
                        eb.append("invalid attributes ");
                    }
                } else {
                    eb.append(", ");
                }
                eb.append("\"");
                eb.append(key);
                eb.append("\"");

            }
            logger.error(eb.toString());
        }

        if (used.size() != children.size()) {
            for (Node child : children) {
                if (used.contains(child)) {
                    continue;
                }

                logger.error("node.getName()" + " contains invalid element " + child.getName());
            }
        }

        try
        {
            int mod = factoryMethod.getModifiers();
            if (!Modifier.isStatic(mod))
            {
                logger.error(factoryMethod.getName() + " method is not static on class " +
                    clazz.getName() + " for element " + node.getName());
                return null;
            }
            logger.debug("Calling " + factoryMethod.getName() + " on class " + clazz.getName() + " for element " +
                node.getName() + sb.toString());
            StringBuilder b = new StringBuilder();
            //if (parms.length > 0) {
                return factoryMethod.invoke(null, parms);
            //}
            //return factoryMethod.invoke(null, node);
        }
        catch (Exception e)
        {
            logger.error("Unable to invoke method " + factoryMethod.getName() + " in class " +
                clazz.getName() + " for element " + node.getName(), e);
        }
        return null;
    }

    private void printArray(StringBuilder sb, Object[] array) {
        boolean first = true;
        for (Object obj : array) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(obj.toString());
        }
    }

    private String getAttrValue(String name, Map<String, String>attrs) {
        for (String key : attrs.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                String attr = attrs.get(key);
                attrs.remove(key);
                return attr;
            }
        }
        return null;
    }

    private void setParents() {
         for (Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            LoggerConfig logger = entry.getValue();
            String name = entry.getKey();
            if (!name.equals("")) {
                int i = name.lastIndexOf(".");
                if (i > 0) {
                    name = name.substring(0, i);
                    LoggerConfig parent = getLoggerConfig(name);
                    if (parent == null) {
                        parent = root;
                    }
                    logger.setParent(parent);
                }
            }
        }
    }
}