/* Copyright 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arbeitspferde.groningen.experimentdb.jvmflags;


import junit.framework.TestCase;

/**
 * Tests for {@link DataSize}.
 */
public class DataSizeTest extends TestCase {
  public void test_getSuffix_NONE() {
    assertEquals("", DataSize.NONE.getSuffix());
  }

  public void test_getSuffix_BYTE() {
    assertEquals("b", DataSize.BYTE.getSuffix());
  }

  public void test_getSuffix_KILO() {
    assertEquals("k", DataSize.KILO.getSuffix());
  }

  public void test_getSuffix_MEGA() {
    assertEquals("m", DataSize.MEGA.getSuffix());
  }

  public void test_getSuffix_GIGA() {
    assertEquals("g", DataSize.GIGA.getSuffix());
  }

  public void test_unitFamilyAsRegexpString_NONE() {
    assertEquals("", DataSize.NONE.unitFamilyAsRegexpString());
  }

  public void test_unitFamilyAsRegexpString_BYTE() {
    assertEquals("[bBkKmMgG]", DataSize.BYTE.unitFamilyAsRegexpString());
  }

  public void test_unitFamilyAsRegexpString_KILO() {
    assertEquals("[bBkKmMgG]", DataSize.KILO.unitFamilyAsRegexpString());
  }

  public void test_unitFamilyAsRegexpString_MEGA() {
    assertEquals("[bBkKmMgG]", DataSize.MEGA.unitFamilyAsRegexpString());
  }

  public void test_unitFamilyAsRegexpString_GIGA() {
    assertEquals("[bBkKmMgG]", DataSize.GIGA.unitFamilyAsRegexpString());
  }
}
