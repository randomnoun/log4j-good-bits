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
package org.apache.logging.log4j.message;

import java.io.Serializable;

/**
 * The simplest possible implementation of Message. It just returns the String given as the constructor argument.
 */
public class SimpleMessage implements Message, Serializable {
    private static final long serialVersionUID = -8398002534962715992L;

    private final String message;

    /**
     * Basic constructor.
     */
    public SimpleMessage() {
        this(null);
    }

    /**
     * Constructor that includes the message.
     * @param message The String message.
     */
    public SimpleMessage(String message) {
        this.message = message;
    }

    /**
     * Return the message.
     * @return the message.
     */
    public String getFormattedMessage() {
        return message;
    }

    /**
     * Return the message.
     * @return the message.
     */
    public String getMessageFormat() {
        return message;
    }

    /**
     * Returns null since there are no parameters.
     * @return null.
     */
    public Object[] getParameters() {
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleMessage that = (SimpleMessage) o;

        return !(message != null ? !message.equals(that.message) : that.message != null);
    }

    public int hashCode() {
        return message != null ? message.hashCode() : 0;
    }

    public String toString() {
        return "SimpleMessage[message=" + message + "]";
    }
}
