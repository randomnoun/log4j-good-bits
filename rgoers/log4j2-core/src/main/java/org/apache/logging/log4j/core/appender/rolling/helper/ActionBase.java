/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.appender.rolling.helper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;


/**
 * Abstract base class for implementations of Action.
 */
public abstract class ActionBase implements Action {
    /**
     * Is action complete.
     */
    private boolean complete = false;

    /**
     * Is action interrupted.
     */
    private boolean interrupted = false;

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Constructor.
     */
    protected ActionBase() {
    }

    /**
     * Perform action.
     *
     * @return true if successful.
     * @throws IOException if IO error.
     */
    public abstract boolean execute() throws IOException;

    /**
     * {@inheritDoc}
     */
    public synchronized void run() {
        if (!interrupted) {
            try {
                execute();
            } catch (IOException ex) {
                reportException(ex);
            }

            complete = true;
            interrupted = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() {
        interrupted = true;
    }

    /**
     * Tests if the action is complete.
     *
     * @return true if action is complete.
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Capture exception.
     *
     * @param ex exception.
     */
    protected void reportException(final Exception ex) {
    }
}
