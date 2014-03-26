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

package org.arbeitspferde.groningen.experimentdb;


import junit.framework.TestCase;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The test for {@link CommandLine}.
 */
public class CommandLineTest extends TestCase {
  private static final long HEAP_SIZE = 1024;
  private static final long ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR = 2;
  private static final long CMS_EXP_AVG_FACTOR = 3;
  private static final long CMS_INCREMENTAL_DUTY_CYCLE = 90;
  private static final long CMS_INCREMENTAL_DUTYCYCLE_MIN = 3;
  private static final long CMS_INCREMENTAL_OFFSET = 2;
  private static final long CMS_INCREMENTAL_SAFETY_FACTOR = 12;
  private static final long CMS_INITIATING_OCCUPANCY_FRACTION = 3;
  private static final long GC_TIME_RATIO = 21;
  private static final long MAX_GC_PAUSE_MILLIS = 6546;
  private static final long MAX_HEAP_FREE_RATIO = 4;
  private static final long MIN_HEAP_FREE_RATIO = 5;
  private static final long NEW_RATIO = 0;
  private static final long NEW_SIZE = 8;
  private static final long MAX_NEW_SIZE = 9;
  private static final long PARALLEL_GC_THREADS = 15;
  private static final long SURVIVOR_RATIO = 52;
  private static final long TENURED_GENERATION_SIZE_INCREMENT = 60;
  private static final long YOUNG_GENERATION_SIZE_INCREMENT = 65;
  private static final long SOFT_REF_LRU_POLICY_MS = 2000;
  private static final long CMS_INCREMENTAL_MODE = 0;
  private static final long CMS_INCREMENTAL_PACING = 0;
  private static final long USE_CMS_INITIATING_OCCUPANCY_ONLY = 0;
  private static final long USE_CONCMARKSWEEP_GC = 1;
  private static final long USE_PARALLEL_GC = 0;
  private static final long USE_PARALLEL_OLD_GC = 0;
  private static final long USE_SERIAL_GC = 0;

  /** The object we are testing */
  private CommandLine p;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    final JvmFlagSet.Builder builder = JvmFlagSet.builder();

    builder.withValue(JvmFlag.HEAP_SIZE, HEAP_SIZE)
      .withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR,
        ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR)
      .withValue(JvmFlag.CMS_EXP_AVG_FACTOR, CMS_EXP_AVG_FACTOR)
      .withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE, CMS_INCREMENTAL_DUTY_CYCLE)
      .withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN, CMS_INCREMENTAL_DUTYCYCLE_MIN)
      .withValue(JvmFlag.CMS_INCREMENTAL_OFFSET, CMS_INCREMENTAL_OFFSET)
      .withValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR, CMS_INCREMENTAL_SAFETY_FACTOR)
      .withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, CMS_INITIATING_OCCUPANCY_FRACTION)
      .withValue(JvmFlag.GC_TIME_RATIO, GC_TIME_RATIO)
      .withValue(JvmFlag.MAX_GC_PAUSE_MILLIS, MAX_GC_PAUSE_MILLIS)
      .withValue(JvmFlag.MAX_HEAP_FREE_RATIO, MAX_HEAP_FREE_RATIO)
      .withValue(JvmFlag.MIN_HEAP_FREE_RATIO, MIN_HEAP_FREE_RATIO)
      .withValue(JvmFlag.NEW_RATIO, NEW_RATIO)
      .withValue(JvmFlag.NEW_SIZE, NEW_SIZE)
      .withValue(JvmFlag.MAX_NEW_SIZE, MAX_NEW_SIZE)
      .withValue(JvmFlag.PARALLEL_GC_THREADS, PARALLEL_GC_THREADS)
      .withValue(JvmFlag.SURVIVOR_RATIO, SURVIVOR_RATIO)
      .withValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT, TENURED_GENERATION_SIZE_INCREMENT)
      .withValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT, YOUNG_GENERATION_SIZE_INCREMENT)
      .withValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB, SOFT_REF_LRU_POLICY_MS)
      .withValue(JvmFlag.CMS_INCREMENTAL_MODE, CMS_INCREMENTAL_MODE)
      .withValue(JvmFlag.CMS_INCREMENTAL_PACING, CMS_INCREMENTAL_PACING)
      .withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, CMS_INITIATING_OCCUPANCY_FRACTION)
      .withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, USE_CONCMARKSWEEP_GC)
      .withValue(JvmFlag.USE_PARALLEL_GC, USE_PARALLEL_GC)
      .withValue(JvmFlag.USE_PARALLEL_OLD_GC, USE_PARALLEL_OLD_GC)
      .withValue(JvmFlag.USE_SERIAL_GC, USE_SERIAL_GC);

    final JvmFlagSet jvmFlagSet = builder.build();

    p = new CommandLine(jvmFlagSet);
  }

  private static boolean longToBoolean(final long value) {
    return value == 1 ? true : false;
  }

  public void testSanityOfCommandLine() {
    assertEquals(HEAP_SIZE, p.getHeapSize());
    assertEquals(p.getAdaptiveSizeDecrementScaleFactor(), ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR);
    assertEquals(p.getCmsExpAvgFactor(), CMS_EXP_AVG_FACTOR);
    assertEquals(p.getCmsIncrementalDutyCycle(), CMS_INCREMENTAL_DUTY_CYCLE);
    assertEquals(p.getCmsIncrementalDutyCycleMin(), CMS_INCREMENTAL_DUTYCYCLE_MIN);
    assertEquals(p.getCmsIncrementalOffset(), CMS_INCREMENTAL_OFFSET);
    assertEquals(p.getCmsIncrementalSafetyFactor(), CMS_INCREMENTAL_SAFETY_FACTOR);
    assertEquals(p.getCmsInitiatingOccupancyFraction(), CMS_INITIATING_OCCUPANCY_FRACTION);
    assertEquals(p.getGcTimeRatio(), GC_TIME_RATIO);
    assertEquals(p.getMaxGcPauseMillis(), MAX_GC_PAUSE_MILLIS);
    assertEquals(p.getMaxHeapFreeRatio(), MAX_HEAP_FREE_RATIO);
    assertEquals(p.getMinHeapFreeRatio(), MIN_HEAP_FREE_RATIO);
    assertEquals(p.getNewRatio(), NEW_RATIO);
    assertEquals(p.getNewSize(), NEW_SIZE);
    assertEquals(p.getParallelGCThreads(), PARALLEL_GC_THREADS);
    assertEquals(p.getSurvivorRatio(), SURVIVOR_RATIO);
    assertEquals(p.getTenuredGenerationSizeIncrement(), TENURED_GENERATION_SIZE_INCREMENT);
    assertEquals(p.getYoungGenerationSizeIncrement(), YOUNG_GENERATION_SIZE_INCREMENT);
    assertEquals(p.getSoftRefLruPolicyMsPerMb(), SOFT_REF_LRU_POLICY_MS);
    assertEquals(p.getCmsIncrementalMode(), longToBoolean(CMS_INCREMENTAL_MODE));
    assertEquals(p.getCmsIncrementalPacing(), longToBoolean(CMS_INCREMENTAL_PACING));
    assertEquals(p.getUseCmsInitiatingOccupancyOnly(),
        longToBoolean(USE_CMS_INITIATING_OCCUPANCY_ONLY));
    assertEquals(p.getUseConcMarkSweepGC(), longToBoolean(USE_CONCMARKSWEEP_GC));
    assertEquals(p.getUseParallelGC(), longToBoolean(USE_PARALLEL_GC));
    assertEquals(p.getUseParallelOldGC(), longToBoolean(USE_PARALLEL_OLD_GC));
    assertEquals(p.getUseSerialGC(), longToBoolean(USE_SERIAL_GC));
  }


  /**
   * Tests {@link CommandLine#getValue(JvmFlag)} method with invalid argument.
   */
  public void testGetValueWithInvalidArguments() {
    try {
      p.getValue(null);
      fail("Expected NullPointerException");
    } catch (final NullPointerException expectedSoIgnore) {}
  }

  /**
   * Tests {@link CommandLine#getValue(JvmFlag)} method with valid argument.
   */
  public void testGetValue() {
    assertEquals(HEAP_SIZE, p.getValue(JvmFlag.HEAP_SIZE));
    assertEquals(ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR,
      p.getValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR));
    assertEquals(CMS_EXP_AVG_FACTOR, p.getValue(JvmFlag.CMS_EXP_AVG_FACTOR));
    assertEquals(
        CMS_INCREMENTAL_DUTY_CYCLE, p.getValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE));
    assertEquals(CMS_INCREMENTAL_DUTYCYCLE_MIN,
      p.getValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN));
    assertEquals(CMS_INCREMENTAL_OFFSET, p.getValue(JvmFlag.CMS_INCREMENTAL_OFFSET));
    assertEquals(CMS_INCREMENTAL_SAFETY_FACTOR,
      p.getValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR));
    assertEquals(CMS_INITIATING_OCCUPANCY_FRACTION,
      p.getValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION));
    assertEquals(GC_TIME_RATIO, p.getValue(JvmFlag.GC_TIME_RATIO));
    assertEquals(MAX_GC_PAUSE_MILLIS, p.getValue(JvmFlag.MAX_GC_PAUSE_MILLIS));
    assertEquals(MAX_HEAP_FREE_RATIO, p.getValue(JvmFlag.MAX_HEAP_FREE_RATIO));
    assertEquals(MIN_HEAP_FREE_RATIO, p.getValue(JvmFlag.MIN_HEAP_FREE_RATIO));
    assertEquals(NEW_RATIO, p.getValue(JvmFlag.NEW_RATIO));
    assertEquals(NEW_SIZE, p.getValue(JvmFlag.NEW_SIZE));
    assertEquals(PARALLEL_GC_THREADS, p.getValue(JvmFlag.PARALLEL_GC_THREADS));
    assertEquals(SURVIVOR_RATIO, p.getValue(JvmFlag.SURVIVOR_RATIO));
    assertEquals(TENURED_GENERATION_SIZE_INCREMENT,
      p.getValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT));
    assertEquals(YOUNG_GENERATION_SIZE_INCREMENT,
      p.getValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT));
    assertEquals(SOFT_REF_LRU_POLICY_MS,
      p.getValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB));
    assertEquals(CMS_INCREMENTAL_MODE,
      p.getValue(JvmFlag.CMS_INCREMENTAL_MODE));
    assertEquals(CMS_INCREMENTAL_PACING,
      p.getValue(JvmFlag.CMS_INCREMENTAL_PACING));
    assertEquals(USE_CMS_INITIATING_OCCUPANCY_ONLY,
      p.getValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY));
    assertEquals(USE_CONCMARKSWEEP_GC,
      p.getValue(JvmFlag.USE_CONC_MARK_SWEEP_GC));
    assertEquals(
      USE_PARALLEL_GC, p.getValue(JvmFlag.USE_PARALLEL_GC));
    assertEquals(USE_PARALLEL_OLD_GC,
      p.getValue(JvmFlag.USE_PARALLEL_OLD_GC));
    assertEquals(USE_SERIAL_GC, p.getValue(JvmFlag.USE_SERIAL_GC));
  }

  /** Tests the sanity of {@link CommandLine#toArgumentString()} */
  public void testToArgumentStringSanity() {
    final String result = p.toArgumentString();

    assertEquals("-Xmx1024m -Xms1024m -XX:NewSize=8m -XX:MaxNewSize=8m -XX:SurvivorRatio=52 "
        + "-XX:SoftRefLRUPolicyMSPerMB=2000 -XX:+UseConcMarkSweepGC "
        + "-XX:CMSInitiatingOccupancyFraction=3 -XX:-UseCMSInitiatingOccupancyOnly", result);
  }

  public void testRegularExpressionsCompile() {
    for (final String regexp : p.getManagedArgs()) {
      try {
        Pattern.compile(regexp);
      } catch (final PatternSyntaxException e) {
        fail(String.format("Failed to compile '''s''' into Pattern due to '''%s'''.", regexp, e));
      }
    }
  }

  public void testRegularExpressionsMatch_ms() {
    assertMatchesOnce("-Xms1");

    assertMatchesOnce("-Xms1k");
    assertMatchesOnce("-Xms1m");
    assertMatchesOnce("-Xms1g");
    assertMatchesOnce("-Xms1K");
    assertMatchesOnce("-Xms1M");
    assertMatchesOnce("-Xms1G");
    assertMatchesOnce("-Xms11k");
    assertMatchesOnce("-Xms11m");
    assertMatchesOnce("-Xms11g");
    assertMatchesOnce("-Xms11K");
    assertMatchesOnce("-Xms11M");
    assertMatchesOnce("-Xms11G");
  }

  public void testRegularExpressionsMatch_mx() {
    assertMatchesOnce("-Xmx1");

    assertMatchesOnce("-Xmx1k");
    assertMatchesOnce("-Xmx1m");
    assertMatchesOnce("-Xmx1g");
    assertMatchesOnce("-Xmx1K");
    assertMatchesOnce("-Xmx1M");
    assertMatchesOnce("-Xmx1G");
    assertMatchesOnce("-Xmx11k");
    assertMatchesOnce("-Xmx11m");
    assertMatchesOnce("-Xmx11g");
    assertMatchesOnce("-Xmx11K");
    assertMatchesOnce("-Xmx11M");
    assertMatchesOnce("-Xmx11G");
  }

  public void testRegularExpressionsMatch_AdaptiveSizeDecrementScaleFactor() {
    assertMatchesOnce("-XX:AdaptiveSizeDecrementScaleFactor=1");
    assertMatchesOnce("-XX:AdaptiveSizeDecrementScaleFactor=11");

    assertMatchesNever("-XX:AdaptiveSizeDecrementScaleFactor=1k");
    assertMatchesNever("-XX:AdaptiveSizeDecrementScaleFactor=1m");
    assertMatchesNever("-XX:AdaptiveSizeDecrementScaleFactor=1g");
    assertMatchesNever("-XX:AdaptiveSizeDecrementScaleFactor=1K");
    assertMatchesNever("-XX:AdaptiveSizeDecrementScaleFactor=1M");
    assertMatchesNever("-XX:AdaptiveSizeDecrementScaleFactor=1G");
  }

  public void testRegularExpressionsMatch_CMSExpAvgFactor() {
    assertMatchesOnce("-XX:CMSExpAvgFactor=1");
    assertMatchesOnce("-XX:CMSExpAvgFactor=11");

    assertMatchesNever("-XX:CMSExpAvgFactor=1k");
    assertMatchesNever("-XX:CMSExpAvgFactor=1m");
    assertMatchesNever("-XX:CMSExpAvgFactor=1g");
    assertMatchesNever("-XX:CMSExpAvgFactor=1K");
    assertMatchesNever("-XX:CMSExpAvgFactor=1M");
    assertMatchesNever("-XX:CMSExpAvgFactor=1G");
  }

  public void testRegularExpressionsMatch_CMSIncrementalDutyCycle() {
    assertMatchesOnce("-XX:CMSIncrementalDutyCycle=1");
    assertMatchesOnce("-XX:CMSIncrementalDutyCycle=11");

    assertMatchesNever("-XX:CMSIncrementalDutyCycle=1k");
    assertMatchesNever("-XX:CMSIncrementalDutyCycle=1m");
    assertMatchesNever("-XX:CMSIncrementalDutyCycle=1g");
    assertMatchesNever("-XX:CMSIncrementalDutyCycle=1K");
    assertMatchesNever("-XX:CMSIncrementalDutyCycle=1M");
    assertMatchesNever("-XX:CMSIncrementalDutyCycle=1G");
  }

  public void testRegularExpressionsMatch_CMSIncrementalDutyCycleMin() {
    assertMatchesOnce("-XX:CMSIncrementalDutyCycleMin=1");
    assertMatchesOnce("-XX:CMSIncrementalDutyCycleMin=11");

    assertMatchesNever("-XX:CMSIncrementalDutyCycleMin=1k");
    assertMatchesNever("-XX:CMSIncrementalDutyCycleMin=1m");
    assertMatchesNever("-XX:CMSIncrementalDutyCycleMin=1g");
    assertMatchesNever("-XX:CMSIncrementalDutyCycleMin=1K");
    assertMatchesNever("-XX:CMSIncrementalDutyCycleMin=1M");
    assertMatchesNever("-XX:CMSIncrementalDutyCycleMin=1G");
  }

  public void testRegularExpressionsMatch_CMSIncrementalOffset() {
    assertMatchesOnce("-XX:CMSIncrementalOffset=1");
    assertMatchesOnce("-XX:CMSIncrementalOffset=11");

    assertMatchesNever("-XX:CMSIncrementalOffset=1k");
    assertMatchesNever("-XX:CMSIncrementalOffset=1m");
    assertMatchesNever("-XX:CMSIncrementalOffset=1g");
    assertMatchesNever("-XX:CMSIncrementalOffset=1K");
    assertMatchesNever("-XX:CMSIncrementalOffset=1M");
    assertMatchesNever("-XX:CMSIncrementalOffset=1G");
  }

  public void testRegularExpressionsMatch_CMSIncrementalSafetyFactor() {
    assertMatchesOnce("-XX:CMSIncrementalSafetyFactor=1");
    assertMatchesOnce("-XX:CMSIncrementalSafetyFactor=11");

    assertMatchesNever("-XX:CMSIncrementalSafetyFactor=1k");
    assertMatchesNever("-XX:CMSIncrementalSafetyFactor=1m");
    assertMatchesNever("-XX:CMSIncrementalSafetyFactor=1g");
    assertMatchesNever("-XX:CMSIncrementalSafetyFactor=1K");
    assertMatchesNever("-XX:CMSIncrementalSafetyFactor=1M");
    assertMatchesNever("-XX:CMSIncrementalSafetyFactor=1G");
  }

  public void testRegularExpressionsMatch_CMSInitiatingOccupancyFraction() {
    assertMatchesOnce("-XX:CMSInitiatingOccupancyFraction=1");
    assertMatchesOnce("-XX:CMSInitiatingOccupancyFraction=11");

    assertMatchesNever("-XX:CMSInitiatingOccupancyFraction=1k");
    assertMatchesNever("-XX:CMSInitiatingOccupancyFraction=1m");
    assertMatchesNever("-XX:CMSInitiatingOccupancyFraction=1g");
    assertMatchesNever("-XX:CMSInitiatingOccupancyFraction=1K");
    assertMatchesNever("-XX:CMSInitiatingOccupancyFraction=1M");
    assertMatchesNever("-XX:CMSInitiatingOccupancyFraction=1G");
  }

  public void testRegularExpressionsMatch_GCTimeRatio() {
    assertMatchesOnce("-XX:GCTimeRatio=1");
    assertMatchesOnce("-XX:GCTimeRatio=11");

    assertMatchesNever("-XX:GCTimeRatio=1k");
    assertMatchesNever("-XX:GCTimeRatio=1m");
    assertMatchesNever("-XX:GCTimeRatio=1g");
    assertMatchesNever("-XX:GCTimeRatio=1K");
    assertMatchesNever("-XX:GCTimeRatio=1M");
    assertMatchesNever("-XX:GCTimeRatio=1G");
  }

  public void testRegularExpressionsMatch_MaxGCPauseMillis() {
    assertMatchesOnce("-XX:MaxGCPauseMillis=1");
    assertMatchesOnce("-XX:MaxGCPauseMillis=11");

    assertMatchesNever("-XX:MaxGCPauseMillis=1k");
    assertMatchesNever("-XX:MaxGCPauseMillis=1m");
    assertMatchesNever("-XX:MaxGCPauseMillis=1g");
    assertMatchesNever("-XX:MaxGCPauseMillis=1K");
    assertMatchesNever("-XX:MaxGCPauseMillis=1M");
    assertMatchesNever("-XX:MaxGCPauseMillis=1G");
  }

  public void testRegularExpressionsMatch_MaxHeapFreeRatio() {
    assertMatchesOnce("-XX:MaxHeapFreeRatio=1");
    assertMatchesOnce("-XX:MaxHeapFreeRatio=11");

    assertMatchesNever("-XX:MaxHeapFreeRatio=1k");
    assertMatchesNever("-XX:MaxHeapFreeRatio=1m");
    assertMatchesNever("-XX:MaxHeapFreeRatio=1g");
    assertMatchesNever("-XX:MaxHeapFreeRatio=1K");
    assertMatchesNever("-XX:MaxHeapFreeRatio=1M");
    assertMatchesNever("-XX:MaxHeapFreeRatio=1G");
  }

  public void testRegularExpressionsMatch_MaxNewSize() {
    assertMatchesNever("-XX:MaxNewSize=1");
    assertMatchesNever("-XX:MaxNewSize=11");

    assertMatchesOnce("-XX:MaxNewSize=1k");
    assertMatchesOnce("-XX:MaxNewSize=1m");
    assertMatchesOnce("-XX:MaxNewSize=1g");
    assertMatchesOnce("-XX:MaxNewSize=1K");
    assertMatchesOnce("-XX:MaxNewSize=1M");
    assertMatchesOnce("-XX:MaxNewSize=1G");
    assertMatchesOnce("-XX:MaxNewSize=11k");
    assertMatchesOnce("-XX:MaxNewSize=11m");
    assertMatchesOnce("-XX:MaxNewSize=11g");
    assertMatchesOnce("-XX:MaxNewSize=11K");
    assertMatchesOnce("-XX:MaxNewSize=11M");
    assertMatchesOnce("-XX:MaxNewSize=11G");
  }

  public void testRegularExpressionsMatch_MinHeapFreeRatio() {
    assertMatchesOnce("-XX:MinHeapFreeRatio=1");
    assertMatchesOnce("-XX:MinHeapFreeRatio=11");

    assertMatchesNever("-XX:MinHeapFreeRatio=1k");
    assertMatchesNever("-XX:MinHeapFreeRatio=1m");
    assertMatchesNever("-XX:MinHeapFreeRatio=1g");
    assertMatchesNever("-XX:MinHeapFreeRatio=1K");
    assertMatchesNever("-XX:MinHeapFreeRatio=1M");
    assertMatchesNever("-XX:MinHeapFreeRatio=1G");
  }

  public void testRegularExpressionsMatch_NewRatio() {
    assertMatchesOnce("-XX:NewRatio=1");
    assertMatchesOnce("-XX:NewRatio=11");

    assertMatchesNever("-XX:NewRatio=1k");
    assertMatchesNever("-XX:NewRatio=1m");
    assertMatchesNever("-XX:NewRatio=1g");
    assertMatchesNever("-XX:NewRatio=1K");
    assertMatchesNever("-XX:NewRatio=1M");
    assertMatchesNever("-XX:NewRatio=1G");
  }

  public void testRegularExpressionsMatch_NewSize() {
    assertMatchesNever("-XX:NewSize=1");
    assertMatchesNever("-XX:NewSize=11");

    assertMatchesOnce("-XX:NewSize=1k");
    assertMatchesOnce("-XX:NewSize=1m");
    assertMatchesOnce("-XX:NewSize=1g");
    assertMatchesOnce("-XX:NewSize=1K");
    assertMatchesOnce("-XX:NewSize=1M");
    assertMatchesOnce("-XX:NewSize=1G");
    assertMatchesOnce("-XX:NewSize=11k");
    assertMatchesOnce("-XX:NewSize=11m");
    assertMatchesOnce("-XX:NewSize=11g");
    assertMatchesOnce("-XX:NewSize=11K");
    assertMatchesOnce("-XX:NewSize=11M");
    assertMatchesOnce("-XX:NewSize=11G");
  }

  public void testRegularExpressionsMatch_ParallelGCThreads() {
    assertMatchesOnce("-XX:ParallelGCThreads=1");
    assertMatchesOnce("-XX:ParallelGCThreads=11");

    assertMatchesNever("-XX:ParallelGCThreads=1k");
    assertMatchesNever("-XX:ParallelGCThreads=1m");
    assertMatchesNever("-XX:ParallelGCThreads=1g");
    assertMatchesNever("-XX:ParallelGCThreads=1K");
    assertMatchesNever("-XX:ParallelGCThreads=1M");
    assertMatchesNever("-XX:ParallelGCThreads=1G");
  }

  public void testRegularExpressionsMatch_SurvivorRatio() {
    assertMatchesOnce("-XX:SurvivorRatio=1");
    assertMatchesOnce("-XX:SurvivorRatio=11");

    assertMatchesNever("-XX:SurvivorRatio=1k");
    assertMatchesNever("-XX:SurvivorRatio=1m");
    assertMatchesNever("-XX:SurvivorRatio=1g");
    assertMatchesNever("-XX:SurvivorRatio=1K");
    assertMatchesNever("-XX:SurvivorRatio=1M");
    assertMatchesNever("-XX:SurvivorRatio=1G");
  }

  public void testRegularExpressionsMatch_SoftRefLRUPolicyMSPerMB() {
    assertMatchesOnce("-XX:SoftRefLRUPolicyMSPerMB=1");
    assertMatchesOnce("-XX:SoftRefLRUPolicyMSPerMB=11");

    assertMatchesNever("-XX:SoftRefLRUPolicyMSPerMB=1k");
    assertMatchesNever("-XX:SoftRefLRUPolicyMSPerMB=1m");
    assertMatchesNever("-XX:SoftRefLRUPolicyMSPerMB=1g");
    assertMatchesNever("-XX:SoftRefLRUPolicyMSPerMB=1K");
    assertMatchesNever("-XX:SoftRefLRUPolicyMSPerMB=1M");
    assertMatchesNever("-XX:SoftRefLRUPolicyMSPerMB=1G");
  }

  public void testRegularExpressionsMatch_TenuredGenerationSizeIncrement() {
    assertMatchesOnce("-XX:TenuredGenerationSizeIncrement=1");
    assertMatchesOnce("-XX:TenuredGenerationSizeIncrement=11");

    assertMatchesNever("-XX:TenuredGenerationSizeIncrement=1k");
    assertMatchesNever("-XX:TenuredGenerationSizeIncrement=1m");
    assertMatchesNever("-XX:TenuredGenerationSizeIncrement=1g");
    assertMatchesNever("-XX:TenuredGenerationSizeIncrement=1K");
    assertMatchesNever("-XX:TenuredGenerationSizeIncrement=1M");
    assertMatchesNever("-XX:TenuredGenerationSizeIncrement=1G");
  }

  public void testRegularExpressionsMatch_YoungGenerationSizeIncrement() {
    assertMatchesOnce("-XX:YoungGenerationSizeIncrement=1");
    assertMatchesOnce("-XX:YoungGenerationSizeIncrement=11");

    assertMatchesNever("-XX:YoungGenerationSizeIncrement=1k");
    assertMatchesNever("-XX:YoungGenerationSizeIncrement=1m");
    assertMatchesNever("-XX:YoungGenerationSizeIncrement=1g");
    assertMatchesNever("-XX:YoungGenerationSizeIncrement=1K");
    assertMatchesNever("-XX:YoungGenerationSizeIncrement=1M");
    assertMatchesNever("-XX:YoungGenerationSizeIncrement=1G");
  }

  public void testRegularExpressionsMatch_CMSIncrementalMode() {
    assertMatchesOnce("-XX:-CMSIncrementalMode");
    assertMatchesOnce("-XX:+CMSIncrementalMode");
  }

  public void testRegularExpressionsMatch_CMSIncrementalPacing() {
    assertMatchesOnce("-XX:-CMSIncrementalPacing");
    assertMatchesOnce("-XX:+CMSIncrementalPacing");
  }

  public void testRegularExpressionsMatch_UseCMSInitiatingOccupancyOnly() {
    assertMatchesOnce("-XX:-UseCMSInitiatingOccupancyOnly");
    assertMatchesOnce("-XX:+UseCMSInitiatingOccupancyOnly");
  }

  public void testRegularExpressionsMatch_UseConcMarkSweepGC() {
    assertMatchesOnce("-XX:-UseConcMarkSweepGC");
    assertMatchesOnce("-XX:+UseConcMarkSweepGC");
  }

  public void testRegularExpressionsMatch_UseParallelGC() {
    assertMatchesOnce("-XX:-UseParallelGC");
    assertMatchesOnce("-XX:+UseParallelGC");
  }

  public void testRegularExpressionsMatch_UseSerialGC() {
    assertMatchesOnce("-XX:-UseSerialGC");
    assertMatchesOnce("-XX:+UseSerialGC");
  }

  private int matchCount(final String candidate) {
    int matchCount = 0;

    for (final String regexp : p.getManagedArgs()) {
      if (Pattern.matches(regexp, candidate)) {
        matchCount++;
      }
    }

    return matchCount;
  }

  private void assertMatchesNever(final String candidate) {
    assertMatchesExactly(candidate, 0);
  }

  private void assertMatchesOnce(final String candidate) {
    assertMatchesExactly(candidate, 1);
  }

  private void assertMatchesExactly(final String candidate, final int expectedTimes) {
    final int matches = matchCount(candidate);

    if (expectedTimes != matches) {
      final StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Candidate string '''");
      errorMessage.append(candidate);
      errorMessage.append("''' was expected to be found ");
      errorMessage.append(expectedTimes);
      errorMessage.append(" times but was only found ");
      errorMessage.append(matches);
      errorMessage.append(" times.");

      fail(errorMessage.toString());
    }
  }
}
