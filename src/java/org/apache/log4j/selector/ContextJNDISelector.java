/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.selector;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.helpers.IntializationUtil;
import org.apache.log4j.helpers.JNDIUtil;
import org.apache.log4j.helpers.Loader;

import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;


/**
 * Log4j JNDI based Repository selector
 *
 * <p>based primarily on Ceki G&uuml;lc&uuml;'s article <h3>Supporting the Log4j
 * <code>RepositorySelector</code> in Servlet Containers</h3> at:
 * http://qos.ch/logging/sc.html</p>
 *
 * <p>By default, the class static <code>RepositorySelector</code> variable
 * of the <code>LogManager</code> class is set to a trivial
 * <code>{@link org.apache.log4j.spi.RepositorySelector RepositorySelector}</code>
 * implementation which always returns the same logger repository. a.k.a.
 * hierarchy. In other words, by default log4j will use one hierarchy, the
 * default hierarchy. This behavior can be overridden via
 * the <code>LogManager</code>'s
 * <code>setRepositorySelector(RepositorySelector, Object)</code> method.</p>
 *
 * <p>That is where this class enters the picture.  It can be used to define a
 * custom logger repository.  It makes use of the fact that in J2EE
 * environments, each web-application is guaranteed to have its own JNDI
 * context relative to the <code>java:comp/env</code> context. In EJBs, each
 * enterprise bean (albeit not each application) has its own context relative
 * to the <code>java:comp/env</code> context.  An <code>env-entry</code> in a
 * deployment descriptor provides the information to the JNDI context.  Once the
 * <code>env-entry</code> is set, a repository selector can query the JNDI
 * application context to look up the value of the entry. The logging context of
 * the web-application will depend on the value the env-entry.  The JNDI context
 *  which is looked up by this class is
 * <code>java:comp/env/log4j/context-name</code>.
 *
 * <p>Here is an example of an <code>env-entry<code>:
 * <blockquote>
 * <pre>
 * &lt;env-entry&gt;
 *   &lt;description&gt;JNDI logging context name for this app&lt;/description&gt;
 *   &lt;env-entry-name&gt;log4j/context-name&lt;/env-entry-name&gt;
 *   &lt;env-entry-value&gt;aDistinctiveLoggingContextName&lt;/env-entry-value&gt;
 *   &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 * &lt;/env-entry&gt;
 * </pre>
 * </blockquote>
 * </p>
 *
 * <p> If multiple applications have the same logging context name, then they 
 * will share the same logging context. 
 * </p>
 * 
 *<p>You can also specify the URL for this context's configuration resource.
 * This repository selector (ContextJNDISelector) will use the specified resource
 * to automatically configure the log4j repository.
 *</p>
 ** <blockquote>
 * <pre>
 * &lt;env-entry&gt;
 *   &lt;description&gt;URL for configuring log4j context&lt;/description&gt;
 *   &lt;env-entry-name&gt;log4j/configuration-resource&lt;/env-entry-name&gt;
 *   &lt;env-entry-value&gt;urlOfConfigrationResource&lt;/env-entry-value&gt;
 *   &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 * &lt;/env-entry&gt;
 * </pre>
 * </blockquote>
 * 
 * <p>In case no configuration resource is specified, this repository selector
 * will attempt to find the files <em>log4j.xml</em> and 
 * <em>log4j.properties</em> from the resources available to the application.
 * </p>
 * 
 * <p>It follows that bundling a <em>log4j.xml</em> file in your web-application
 * and setting context name will be enough to ensure a separate logging 
 * environment for your applicaiton.
 *  
 * <p>Given that JNDI is part of the J2EE specification, the JNDI selector
 * is the recommended context selector. 
 * </p>
 * 
 * @author <a href="mailto:hoju@visi.com">Jacob Kjome</a>
 * @author Ceki G&uuml;lc&uuml;
 * @since  1.3
 */
public class ContextJNDISelector implements RepositorySelector {

  static String JNDI_CONFIGURATION_RESOURCE =
    "java:comp/env/log4j/configuration-resource";
  static String JNDI_CONFIGURATOR_CLASS =
    "java:comp/env/log4j/configurator-class";

  /**
   * key: name of logging context,
   * value: Hierarchy instance
   */
  private final Map hierMap;

  /**
   * default hierarchy used in case the JNDI lookup
   * fails to return a non-null value
   */
  private LoggerRepository defaultRepository;

  /**
   * public no-args constructor
   */
  public ContextJNDISelector() {
    hierMap = Collections.synchronizedMap(new HashMap());
  }

  public void setDefaultRepository(LoggerRepository dh) {
    if (defaultRepository == null) {
      defaultRepository = dh;
    } else {
      throw new IllegalStateException(
        "default hierarchy has been already set.");
    }
  }

  /**
   * Return the repoistory selector based on the current JNDI environment.
   * 
   * If the respository is retreived for the first time, then also configure 
   * the repository using a user specified resource or if no such resource
   * is specified, using the resource names "log4j.xml" or "log4j.properties".
   *
   * @return the appropriate JNDI-keyed context name/LoggerRepository
   */
  public LoggerRepository getLoggerRepository() {
    String loggingContextName = null;
    Context ctx = null;

    try {
      ctx = JNDIUtil.getInitialContext();
      loggingContextName = (String) JNDIUtil.lookup(ctx, Constants.JNDI_CONTEXT_NAME);
    } catch (NamingException ne) {
      // we can't log here
    }

    if (loggingContextName == null) {
      return defaultRepository;
    } else {
      System.out.println("loggingContextName is ["+loggingContextName+"]");
      Hierarchy hierarchy = (Hierarchy) hierMap.get(loggingContextName);

      if (hierarchy == null) {
        // create new hierarchy
        hierarchy = new Hierarchy(new RootLogger(Level.DEBUG));
        hierarchy.setName(loggingContextName);
        hierMap.put(loggingContextName, hierarchy);

        // configure log4j internal logging
        IntializationUtil.log4jInternalConfiguration(hierarchy);

        // Use automatic configration to configure the default hierarchy
        String configResourceStr = JNDIUtil.lookup(ctx, JNDI_CONFIGURATION_RESOURCE);
        String configuratorClassName = JNDIUtil.lookup(ctx, JNDI_CONFIGURATOR_CLASS);

        if (configResourceStr == null) {
          if (
            Loader.getResource(Constants.DEFAULT_XML_CONFIGURATION_FILE) != null) {
            configResourceStr = Constants.DEFAULT_XML_CONFIGURATION_FILE;
          } else if (
            Loader.getResource(Constants.DEFAULT_CONFIGURATION_FILE) != null) {
            configResourceStr = Constants.DEFAULT_CONFIGURATION_FILE;
          }
        }

        IntializationUtil.initialConfiguration(
          hierarchy, configResourceStr, configuratorClassName);
      }

      return hierarchy;
    }
  }


  
  /** 
   * Remove the repository with the given context name from the list of
   * known repositories.
   * 
   * @return the detached repository
   */
  public LoggerRepository detachRepository(String contextName) {
    return (LoggerRepository) hierMap.remove(contextName);  
  }
}
