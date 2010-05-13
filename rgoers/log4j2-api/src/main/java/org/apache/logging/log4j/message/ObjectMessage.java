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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Handles messages that contain an Object.
 */
public class ObjectMessage implements Message, Serializable {

    private transient Object obj;

    public ObjectMessage(Object obj) {
        this.obj = obj;
    }

    public String getFormattedMessage() {
        return obj.toString();
    }

    public String getMessageFormat() {
        return obj.toString();
    }

    public Object[] getParameters() {
        return new Object[]{obj};
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectMessage that = (ObjectMessage) o;

        return !(obj != null ? !obj.equals(that.obj) : that.obj != null);
    }

    public int hashCode() {
        return obj != null ? obj.hashCode() : 0;
    }

    public String toString() {
        return "ObjectMessage[obj=" + obj.toString() + "]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (obj instanceof Serializable) {
            out.writeObject(obj);
        } else {
            out.writeObject(obj.toString());
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        obj = in.readObject();
    }
}
