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
package org.apache.logging.log4j.core;

import java.io.Serializable;

/**
 * @doubt There is still a need for a character-based layout for character based event sinks (databases, etc).
 *  Would introduce an EventEncoder, EventRenderer or something similar for the logging event to byte encoding.
 * (RG) A layout can be configured with a Charset and then Strings can be converted to byte arrays. OTOH, it isn't
 * possible to write byte arrays as character streams.
 */
public interface Layout<T extends Serializable> {
    // Note that the line.separator property can be looked up even by
    // applets.
    /**
     * @doubt It is very conceivable that distinct layouts might use distinct line separators.  Should not be on the interface.
     */
    public static final String LINE_SEP = System.getProperty("line.separator");
    public static final int LINE_SEP_LEN = LINE_SEP.length();

    /**
     * Formats the event suitable for display.
     * @param event The Logging Event.
     * @return The formatted event.
     * @doubt Likely better to write to a OutputStream instead of return a byte[]. (RG) That limits how the
     * Appender can use the Layout. For example, it might concatenate information in front or behind the
     * data and then write it all to the OutputStream in one call.
     */
    byte[] format(LogEvent event);

    /**
     * Formats the event as an Object that can be serialized.
     * @param event The Logging Event.
     * @return The formatted event.
     */
    T formatAs(LogEvent event);

    /**
     * Returns the header for the layout format.
     * @return The header.
     * @doubt the concept of header and footer is not universal, should not be on the base interface.
     * (RG) I agree with this.
     */
    byte[] getHeader();

    /**
     * Returns the format for the layout format.
     * @return The footer.
     * @doubt the concept of header and footer is not universal, should not be on the base interface.
     * (RG) I agree with this.
     */
    byte[] getFooter();


}
