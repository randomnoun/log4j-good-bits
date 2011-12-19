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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.Serializable;

/**
 * Base class for Layouts.
 */
public abstract class LayoutBase<T extends Serializable> implements Layout<T> {

    protected byte[] header;
    protected byte[] footer;

    protected static final Logger logger = StatusLogger.getLogger();

    /**
     * Return the header, if one is available.
     * @return A byte array containing the header.
     */
    public byte[] getHeader() {
        return header;
    }

    /**
     * Set the header.
     * @param header The header.
     */
    public void setHeader(byte[] header) {
        this.header = header;
    }

    /**
     * Returns the footer, if one is available.
     * @return A byte array containing the footer.
     */
    public byte[] getFooter() {
        return footer;
    }

    /**
     * Set the footer.
     * @param footer The footer.
     */
    public void setFooter(byte[] footer) {
        this.footer = footer;
    }
}
