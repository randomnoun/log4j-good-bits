/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.APL file.  */

package org.log4j;

import java.util.Vector;

class ProvisionNode extends Vector {
    
  ProvisionNode(Category cat) {
    super();
    this.addElement(cat);
  }
}
