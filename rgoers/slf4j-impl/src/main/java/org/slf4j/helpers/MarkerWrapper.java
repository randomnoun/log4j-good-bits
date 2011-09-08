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
package org.slf4j.helpers;

import org.apache.logging.log4j.Marker;

/**
 *
 */
public class MarkerWrapper extends BasicMarker implements Marker {
    private static final long serialVersionUID = 1903952589649545191L;

    private MarkerWrapper parent;

    MarkerWrapper(String name) {
        super(name);
    }

    @Override
    public void add(org.slf4j.Marker reference) {
        super.add(reference);
        ((MarkerWrapper) reference).setParent(this);
    }

    private void setParent(MarkerWrapper marker) {
        parent = marker;
    }

    public org.apache.logging.log4j.Marker getParent() {
        return this.parent;
    }

    public boolean isInstanceOf(org.apache.logging.log4j.Marker marker) {
        if (marker == null) {
            throw new IllegalArgumentException("A marker parameter is required");
        }

        return contains((MarkerWrapper) marker);
    }
}
