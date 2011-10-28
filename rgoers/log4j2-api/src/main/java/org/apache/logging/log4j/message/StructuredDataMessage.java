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
import java.util.Map;

/**
 * Represents a Message that conforms to RFC 5424 (http://tools.ietf.org/html/rfc5424).
 */
public class StructuredDataMessage extends MapMessage implements FormattedMessage, Serializable {
    /**
     * Full message format includes the type and message.
     */
    public static final String FULL = "full";

    private static final long serialVersionUID = 1703221292892071920L;
    private static final int MAX_LENGTH = 32;
    private static final int HASHVAL = 31;

    private StructuredDataId id;

    private String message;

    private String type;

    /**
     * Constructor based on a String id.
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     */
    public StructuredDataMessage(final String id, final String msg, final String type) {
        this.id = new StructuredDataId(id, null, null);
        this.message = msg;
        this.type = type;
    }
    /**
     * Constructor based on a String id.
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     */
    public StructuredDataMessage(final String id, final String msg, final String type,
                                 Map<String, String> data) {
        super(data);
        this.id = new StructuredDataId(id, null, null);
        this.message = msg;
        this.type = type;
    }

    /**
     * Constructor based on a StructuredDataId.
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     */
    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type) {
        this.id = id;
        this.message = msg;
        this.type = type;
    }

    /**
     * Constructor based on a StructuredDataId.
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     */
    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type,
                                 Map<String, String> data) {
        super(data);
        this.id = id;
        this.message = msg;
        this.type = type;
    }


    /**
     * Constructor based on a StructuredDataMessage.
     * @param msg The StructuredDataMessage.
     * @param map The StructuredData map.
     */
    private StructuredDataMessage(StructuredDataMessage msg, Map<String, String> map) {
        super(map);
        this.id = msg.id;
        this.message = msg.message;
        this.type = msg.type;
    }


    /**
     * Basic constructor.
     */
    protected StructuredDataMessage() {

    }

    /**
     * Return the id.
     * @return the StructuredDataId.
     */
    public StructuredDataId getId() {
        return id;
    }

    /**
     * Set the id from a String.
     * @param id The String id.
     */
    protected void setId(String id) {
        this.id = new StructuredDataId(id, null, null);
    }

    /**
     * Set the id.
     * @param id The StructuredDataId.
     */
    protected void setId(StructuredDataId id) {
        this.id = id;
    }

    /**
     * Set the type.
     * @return the type.
     */
    public String getType() {
        return type;
    }

    protected void setType(String type) {
        if (type.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Structured data type exceeds maximum length of 32 characters: " + type);
        }
        this.type = type;
    }
    /**
     * Return the message.
     * @return the message.
     */
    public String getMessageFormat() {
        return message;
    }

    protected void setMessageFormat(String msg) {
        this.message = msg;
    }


    @Override
    protected void validate(String key, String value) {
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Structured data values are limited to 32 characters. key: " + key +
                " value: " + value);
        }
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @return The formatted String.
     */
    @Override
    public String asString() {
        return asString(FULL, null);
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format The format identifier. Ignored in this implementation.
     * @return The formatted String.
     */

    public String asString(String format) {
        return asString(format, null);
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format           "full" will include the type and message. null will return only the STRUCTURED-DATA as
     *                         described in RFC 5424
     * @param structuredDataId The SD-ID as described in RFC 5424. If null the value in the StructuredData
     *                         will be used.
     * @return The formatted String.
     */
    public final String asString(String format, StructuredDataId structuredDataId) {
        StringBuilder sb = new StringBuilder();
        boolean full = FULL.equals(format);
        if (full) {
            String type = getType();
            if (type == null) {
                return sb.toString();
            }
            sb.append(getType()).append(" ");
        }
        StructuredDataId id = getId();
        if (id != null) {
            id = id.makeId(structuredDataId);
        } else {
            id = structuredDataId;
        }
        if (id == null || id.getName() == null) {
            return sb.toString();
        }
        sb.append("[");
        sb.append(id);
        sb.append(" ");
        appendMap(sb);
        sb.append("]");
        if (full) {
            String msg = getMessageFormat();
            if (msg != null) {
                sb.append(" ").append(msg);
            }
        }
        return sb.toString();
    }

    /**
     * Format the message and return it.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        return asString(FULL, null);
    }

    @Override
    public String toString() {
        return asString(null);
    }


    public MapMessage newInstance(Map<String, String> map) {
        return new StructuredDataMessage(this, map);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StructuredDataMessage that = (StructuredDataMessage) o;

        if (!super.equals(o)) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = HASHVAL * result + (type != null ? type.hashCode() : 0);
        result = HASHVAL * result + (id != null ? id.hashCode() : 0);
        result = HASHVAL * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
