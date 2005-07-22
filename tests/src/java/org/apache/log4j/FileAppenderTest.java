/*
 * Copyright 1999,2005 The Apache Software Foundation.
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

package org.apache.log4j;
import java.io.File;
import junit.framework.TestCase;


/**
 *
 * FileAppender tests.
 *
 * @author Curt Arnold
 */
public class FileAppenderTest extends TestCase {
  /**
   * Tests that any necessary directories are attempted to
   * be created if they don't exist.  See bug 9150.
   *
   */
  public void testDirectoryCreation() {
      File newFile = new File("output/newdir/temp.log");
      newFile.delete();
      File newDir = new File("output/newdir");
      newDir.delete();

      org.apache.log4j.FileAppender wa = new org.apache.log4j.FileAppender();
      wa.setFile("output/newdir/temp.log");
      wa.setLayout(new PatternLayout("%m%n"));
      wa.activateOptions();

      assertTrue(new File("output/newdir/temp.log").exists());
  }
}
