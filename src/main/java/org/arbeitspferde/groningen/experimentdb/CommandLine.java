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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import org.arbeitspferde.groningen.experimentdb.jvmflags.GcMode;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;

import java.util.logging.Logger;

/*
 *  TODO(team): Migrate these methods that use long method signatures into ones that take some
 *              defined, immutable data structure.
 */

/**
 * CommandLine represents subject command line data as it passes through the
 * {@link ExperimentDb}. Stored on {@link SubjectStateBridge}s.
 */
public class CommandLine {
  private static final Logger logger = Logger.getLogger(CommandLine.class.getCanonicalName());

  private static final long FALSE_AS_LONG = 0L;
  private static final long TRUE_AS_LONG = 1L;

  private static final Joiner spaceSeparator = Joiner.on(" ");

  private final JvmFlagSet jvmFlagSet;

  // TODO(team): Move the regexp. generation to JvmFlag.

  // We assume the user controls and sets the -XX:MaxPermSize and -XX:PermSize outside Groningen.
  // There is no need to search the space, since it the permgen is a straight forward computation.
  //
  // complete list of flags with types and default values can be found via:
  // * java --XX:+PrintFlagsFinal
  // * potentially not all:
  // //depot/vendor_src/java/jdk/hotspot/src/share/vm/runtime/globals.hpp
  // boolean flags have the form -XX:[+-]<flag>
  static final private ImmutableList<String> MANAGED_ARGS_REGEXES = ImmutableList.<String>builder()
      .add(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR.asRegularExpressionString())
      .add(JvmFlag.CMS_EXP_AVG_FACTOR.asRegularExpressionString())
      .add(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE.asRegularExpressionString())
      .add(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN.asRegularExpressionString())
      .add(JvmFlag.CMS_INCREMENTAL_OFFSET.asRegularExpressionString())
      .add(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR.asRegularExpressionString())
      .add(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION.asRegularExpressionString())
      .add(JvmFlag.GC_TIME_RATIO.asRegularExpressionString())
      .add(JvmFlag.MAX_GC_PAUSE_MILLIS.asRegularExpressionString())
      .add(JvmFlag.MAX_HEAP_FREE_RATIO.asRegularExpressionString())
      .add(JvmFlag.MAX_NEW_SIZE.asRegularExpressionString())
      .add(JvmFlag.MIN_HEAP_FREE_RATIO.asRegularExpressionString())
      .add(JvmFlag.NEW_RATIO.asRegularExpressionString())
      .add(JvmFlag.NEW_SIZE.asRegularExpressionString())
      .add(JvmFlag.PARALLEL_GC_THREADS.asRegularExpressionString())
      .add(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB.asRegularExpressionString())
      .add(JvmFlag.SURVIVOR_RATIO.asRegularExpressionString())
      .add(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT.asRegularExpressionString())
      .add(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT.asRegularExpressionString())
      .add(JvmFlag.CMS_INCREMENTAL_MODE.asRegularExpressionString())
      .add(JvmFlag.CMS_INCREMENTAL_PACING.asRegularExpressionString())
      .add(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY.asRegularExpressionString())
      .add(JvmFlag.USE_CONC_MARK_SWEEP_GC.asRegularExpressionString())
      .add(JvmFlag.USE_PARALLEL_GC.asRegularExpressionString())
      .add(JvmFlag.USE_PARALLEL_OLD_GC.asRegularExpressionString())
      .add(JvmFlag.USE_SERIAL_GC.asRegularExpressionString())
      .add("-Xms\\d+[bBkKmMgG]")
      .add("-Xmx\\d+[bBkKmMgG]")
      .build();

  /**
   * Retrieves an {@link ImmutableList} of the arguments that Groningen will manage.
   *
   * TODO(team): should these be regexes? Look deeper into the forms of the
   * flags and overlap in stems
   */
  static public ImmutableList<String> getManagedArgs() {
    return MANAGED_ARGS_REGEXES;
  }

  /** Creation by package only */
  CommandLine(final JvmFlagSet jvmFlagSet) {
    Preconditions.checkNotNull(jvmFlagSet);

    this.jvmFlagSet = jvmFlagSet;
  }

  /**
   * Returns the value for the given command-line argument.
   *
   * @param argument Command-line argument.
   * @return Value for the given {@code argument}.
   * @throws IllegalArgumentException If the {@code argument} is null.
   */
  public long getValue(final JvmFlag argument) {
    Preconditions.checkNotNull(argument, "Command-line argument cannot be null.");

    return jvmFlagSet.getValue(argument);
  }

  public long getHeapSize() {
    return getValue(JvmFlag.HEAP_SIZE);
  }

  public long getAdaptiveSizeDecrementScaleFactor() {
    return getValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR);
  }

  public long getCmsExpAvgFactor() {
    return getValue(JvmFlag.CMS_EXP_AVG_FACTOR);
  }

  public long getCmsIncrementalDutyCycle() {
    return getValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE);
  }

  public long getCmsIncrementalDutyCycleMin() {
    return getValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN);
  }

  public long getCmsIncrementalOffset() {
    return getValue(JvmFlag.CMS_INCREMENTAL_OFFSET);
  }

  public long getCmsIncrementalSafetyFactor() {
    return getValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR);
  }

  public long getCmsInitiatingOccupancyFraction() {
    return getValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION);
  }

  public long getGcTimeRatio() {
    return getValue(JvmFlag.GC_TIME_RATIO);
  }

  public long getMaxGcPauseMillis() {
    return getValue(JvmFlag.MAX_GC_PAUSE_MILLIS);
  }

  public long getMaxHeapFreeRatio() {
    return getValue(JvmFlag.MAX_HEAP_FREE_RATIO);
  }

  public long getMinHeapFreeRatio() {
    return getValue(JvmFlag.MIN_HEAP_FREE_RATIO);
  }

  public long getNewRatio() {
    return getValue(JvmFlag.NEW_RATIO);
  }

  public long getNewSize() {
    return getValue(JvmFlag.NEW_SIZE);
  }

  public long getParallelGCThreads() {
    return getValue(JvmFlag.PARALLEL_GC_THREADS);
  }

  public long getSurvivorRatio() {
    return getValue(JvmFlag.SURVIVOR_RATIO);
  }

  public long getTenuredGenerationSizeIncrement() {
    return getValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT);
  }

  public long getYoungGenerationSizeIncrement() {
    return getValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT);
  }

  public long getSoftRefLruPolicyMsPerMb() {
    return getValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB);
  }

  public boolean getCmsIncrementalMode() {
    return getValue(JvmFlag.CMS_INCREMENTAL_MODE) == 1 ? true : false;
  }

  public boolean getCmsIncrementalPacing() {
    return getValue(JvmFlag.CMS_INCREMENTAL_PACING) == 1 ? true : false;
  }

  public boolean getUseCmsInitiatingOccupancyOnly() {
    return getValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY) == 1 ? true : false;
  }

  public boolean getUseConcMarkSweepGC() {
    return getValue(JvmFlag.USE_CONC_MARK_SWEEP_GC) == 1 ? true : false;
  }

  public boolean getUseParallelGC() {
    return getValue(JvmFlag.USE_PARALLEL_GC) == 1 ? true : false;
  }

  public boolean getUseParallelOldGC() {
    return getValue(JvmFlag.USE_PARALLEL_OLD_GC) == 1 ? true : false;
  }

  public boolean getUseSerialGC() {
    return getValue(JvmFlag.USE_SERIAL_GC) == 1 ? true : false;
  }

  /**
   * Returns the garbage collection mode chosen in this command line.
   *
   * @return GC mode.
   * @throws RuntimeException If no GC mode is chosen.
   */
  public GcMode getGcMode() {
    if (getUseConcMarkSweepGC()) {
      return GcMode.CMS;
    } else if (getUseParallelGC()) {
      return GcMode.PARALLEL;
    } else if (getUseParallelOldGC()) {
      return GcMode.PARALLEL_OLD;
    } else if (getUseSerialGC()) {
      return GcMode.SERIAL;
    } else {
      // TODO(team): more debug info.
      throw new IllegalStateException("No GC mode found.");
    }
  }

  /**
   * Convert this command line object into a JVM settings string.
   *
   * We only generate command line settings for non-zero integer arguments and
   * true valued boolean argmuments that are relevant to the selected GC Mode.
   * All other settings are effectively ignored and should not be present in the
   * generated JVM settings string.
   *
   * This method is intentionally not called {@link #toString()} due to EasyMock's inability
   * to mock it.
   *
   * @return a string representation of this command line object
   * @throws IllegalArgumentException
   */
  public String toArgumentString() throws IllegalArgumentException {
    final Builder<String> argumentsBuilder = ImmutableList.<String>builder();

    // HEAP SIZING PARAMETERS
    // Since collections occur when generations fill up, throughput is inversely proportional to the
    // amount of memory available. Total available memory is the most important factor affecting
    // garbage collection performance.
    //
    // By default, the virtual machine grows or shrinks the heap at each collection to try to keep
    // the proportion of free space to live objects at each collection within a specific range. This
    // target range is set as a percentage by the parameters
    // -XX:MinHeapFreeRatio=<minimum> and -XX:MaxHeapFreeRatio=<maximum>, and the total size is
    // bounded below by -Xms and above by -Xmx
    //
    // We assume that setting either of these to 0 means ignore the setting
    if (getMinHeapFreeRatio() <= getMaxHeapFreeRatio()) {
      if (getMinHeapFreeRatio() > 0) {
        JvmFlag.MIN_HEAP_FREE_RATIO.validate(getMinHeapFreeRatio());
        argumentsBuilder.add(JvmFlag.MIN_HEAP_FREE_RATIO
            .asArgumentString(getMinHeapFreeRatio()));
      }
      if (getMaxHeapFreeRatio() > 0) {
        JvmFlag.MAX_HEAP_FREE_RATIO.validate(getMaxHeapFreeRatio());
        argumentsBuilder.add(JvmFlag.MAX_HEAP_FREE_RATIO
            .asArgumentString(getMaxHeapFreeRatio()));
      }
    }

    // At initialization of the virtual machine, the entire space for the heap is reserved. The size
    // of the space reserved can be specified with the -Xmx option.
    //
    // If the value of the -Xms parameter is smaller than the value of the -Xmx parameter, not all
    // of the space that is reserved is immediately committed to the JVM.
    //
    // Groningen sets Xmx equal to Xms because this is a common setting within Google. The reason
    // is this keeps the JVM from repeatedly allocating and de-allocating memory, thus reducing the
    // memory allocation overhead from program startup until the JVM wants to reclaim unused memory.
    //
    // The different parts of the heap (permanent generation, tenured
    // generation, and young
    // generation) can grow to the limit of the virtual space as needed.
    if (getHeapSize() > 0) {
      JvmFlag.HEAP_SIZE.validate(getHeapSize());
      argumentsBuilder.add(String.format("-Xmx%sm", getHeapSize()));
      argumentsBuilder.add(String.format("-Xms%sm", getHeapSize()));
    }


    // We assume the user controls and sets the -XX:MaxPermSize and -XX:PermSize outside Groningen.
    // There is no need to search the space, since it the permgen is a straight forward
    // computation.
    //
    // TODO(team): We may want to permute PermSize in a future Groningen generation.

    // The bigger the young generation, the less often minor collections occur.  However, for a
    // bounded heap size a larger young generation implies a smaller tenured generation, which will
    // increase the frequency of major collections. The optimal choice depends on the lifetime
    // distribution of the objects allocated by the application.
    //
    // By default, the young generation size is controlled by NewRatio. For example, setting
    // -XX:NewRatio=3 means that the ratio between the young and tenured generation is 1:3. In other
    // words, the combined size of the eden and survivor spaces will be one fourth of the total heap
    // size.
    //
    // We assume that setting newRatio to 0 means ignore the setting
    if (getNewRatio() > 0) {
      JvmFlag.NEW_RATIO.validate(getNewRatio());
      argumentsBuilder.add(JvmFlag.NEW_RATIO.asArgumentString(getNewRatio()));
    } else

    // An eden bigger than half the virtually committed size of the heap is counterproductive. The
    // serial collector degrades in this scenario and major collections would occur. The throughput
    // collector and the concurrent collector will proceed with a young generation collection in
    // this scenario, but can often degrade into a full GC when the tenured generation cannot
    // accommodate all the promotions from the young generation.
    //
    // For this reason, we restrict the NewSize to less than 1/2 the heap size.
    //
    // TODO(team): Evaluate whether we're OK with invalid genes being generated.
    if (getNewSize() < (getHeapSize() / 2)) {
      // The parameters NewSize and MaxNewSize bound the young generation size from below and above.
      // Setting these equal to one another fixes the young generation, just as setting -Xms and
      // -Xmx equal fixes the total heap size.  We assume that setting newSize to 0 means ignore the
      // setting
      //
      // The size is assumed to be in MB
      if (getNewSize() > 0) {
        JvmFlag.NEW_SIZE.validate(getNewSize());
        argumentsBuilder.add(JvmFlag.NEW_SIZE.asArgumentString(getNewSize()));
        // Notice we are setting MaxNewSize to NewSize as this is a common thing
        // to do
        JvmFlag.MAX_NEW_SIZE.validate(getNewSize());
        argumentsBuilder.add(JvmFlag.MAX_NEW_SIZE.asArgumentString(getNewSize()));
      }
    }

    // SurvivorRatio can be used to tune the size of the survivor spaces. We
    // assume 0 means ignore
    // this setting.
    if (getSurvivorRatio() > 0) {
      JvmFlag.SURVIVOR_RATIO.validate(getSurvivorRatio());
      argumentsBuilder.add(JvmFlag.SURVIVOR_RATIO.asArgumentString(getSurvivorRatio()));
    }

    // Soft references are cleared less aggressively in the server virtual machine than the client.
    // The rate of clearing can be slowed by increasing the parameter SoftRefLRUPolicyMSPerMB with
    // the command line flag -XX:SoftRefLRUPolicyMSPerMB=10000.  SoftRefLRUPolicyMSPerMB is a
    // measure of the time that a soft reference survives for a given amount of free space in the
    // heap. The default value is 1000 ms per megabyte. This can be read to mean that a soft
    // reference will survive (after the last strong reference to the object has been collected)
    // for 1 second for each megabyte of free space in the heap. This is very approximate.
    if (getSoftRefLruPolicyMsPerMb() > 0) {
      JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB.validate(getSoftRefLruPolicyMsPerMb());
      argumentsBuilder.add(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB
          .asArgumentString(getSoftRefLruPolicyMsPerMb()));
    }

    // COLLECTOR SPECFIC PARAMETERS
    if (getUseConcMarkSweepGC()) {
      // The Concurrent Low Pause Collector
      //
      // Use the concurrent low pause collector if your application would benefit from shorter
      // garbage collector pauses and can afford to share processor resources with the garbage
      // collector when the application is running. Typically applications which have a relatively
      // large set of long-lived data (a large tenured generation), and run on machines with two
      // or more processors tend to benefit from the use of this collector.
      argumentsBuilder.add(JvmFlag.USE_CONC_MARK_SWEEP_GC.asArgumentString(TRUE_AS_LONG));

      // Normally, the concurrent collector uses one processor for the concurrent work for the
      // entire concurrent mark phase, without (voluntarily) relinquishing it.  Similarly, one
      // processor is used for the entire concurrent sweep phase, again without relinquishing it.
      // This processor utilization can be too much of a disruption for applications with pause
      // time constraints, particularly when run on systems with just one or two processors.
      // i-cms solves this problem by breaking up the concurrent phases into short bursts of
      // activity, which are scheduled to occur mid-way between minor pauses.
      //
      // I-cms uses a "duty cycle" to control the amount of work the concurrent collector is allowed
      // to do before voluntarily giving up the processor. The duty cycle is the percentage of time
      // between young generation collections that the concurrent collector is allowed to run. I-cms
      // can automatically compute the duty cycle based on the behavior of the application (the
      // recommended method), or the duty cycle can be set to a fixed value on the command line.
      if (getCmsIncrementalMode()) {
        argumentsBuilder.add(JvmFlag.CMS_INCREMENTAL_MODE.asArgumentString(TRUE_AS_LONG));

        // This flag enables automatic adjustment of the incremental mode duty cycle based on
        // statistics collected while the JVM is running.
        if (getCmsIncrementalPacing()) {
          argumentsBuilder.add(JvmFlag.CMS_INCREMENTAL_PACING.asArgumentString(TRUE_AS_LONG));
          // This is the percentage (0-100) which is the lower bound on the duty
          // cycle when
          // CMSIncrementalPacing is enabled.
          //
          // We assume that setting this to 0 means ignore the setting
          if (getCmsIncrementalDutyCycleMin() > 0) {
            JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN.validate(getCmsIncrementalDutyCycle());
            argumentsBuilder.add(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN
                .asArgumentString(getCmsIncrementalDutyCycleMin()));
          }
        }

        // This is the percentage (0-100) used to weight the current sample when computing
        // exponential averages for the concurrent collection statistics.
        //
        // We assume that setting this to 0 means ignore the setting
        if (getCmsExpAvgFactor() > 0) {
          JvmFlag.CMS_EXP_AVG_FACTOR.validate(getCmsExpAvgFactor());
          argumentsBuilder.add(JvmFlag.CMS_EXP_AVG_FACTOR
              .asArgumentString(getCmsExpAvgFactor()));
        }

        // This is the percentage (0-100) of time between minor collections that the concurrent
        // collector is allowed to run. If CMSIncrementalPacing is enabled, then this is just the
        // initial value.
        //
        // We assume that setting this to 0 means ignore the setting
        if (getCmsIncrementalDutyCycle() > 0) {
          JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE.validate(getCmsIncrementalDutyCycle());
          argumentsBuilder.add(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE
              .asArgumentString(getCmsIncrementalDutyCycle()));
        }

        // This is the percentage (0-100) by which the incremental mode duty cycle is shifted to the
        // right within the period between minor collections.
        //
        // We assume that setting this to 0 means ignore the setting
        if (getCmsIncrementalOffset() > 0) {
          JvmFlag.CMS_INCREMENTAL_OFFSET.validate(getCmsIncrementalOffset());
          argumentsBuilder.add(JvmFlag.CMS_INCREMENTAL_OFFSET
              .asArgumentString(getCmsIncrementalOffset()));
        }

        // This is the percentage (0-100) used to add conservatism when computing the duty cycle.
        if (getCmsIncrementalSafetyFactor() > 0) {
          JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR.validate(getCmsIncrementalSafetyFactor());
          argumentsBuilder.add(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR
              .asArgumentString(getCmsIncrementalSafetyFactor()));
        }
      }

      // This parameter sets the threshold percentage occupancy of the old generation at which the
      // CMS GC is triggered. Setting this parameter explicitly tells the JVM when to trigger CMS
      // GC in the tenured generation.
      if (getCmsInitiatingOccupancyFraction() > 0) {
        JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION.validate(getCmsInitiatingOccupancyFraction());
        argumentsBuilder.add(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION
            .asArgumentString((getCmsInitiatingOccupancyFraction())));
        // This parameter tells the JVM to use only the value defined by
        // -XX:CMSInitiatingOccupancyFraction, rather than try to also calculate
        // the value at
        // runtime.

        argumentsBuilder.add(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY
            .asArgumentString(getUseCmsInitiatingOccupancyOnly() ? TRUE_AS_LONG : FALSE_AS_LONG));
      }
    } else if (getUseParallelGC() || getUseParallelOldGC()) {
      // The Throughput Collector
      //
      // This is a generational collector similar to the serial collector but with multiple threads
      // used to do the minor collection. The major collections are essentially the same as with the
      // serial collector. By default on a host with N CPUs, the throughput collector uses N garbage
      // collector threads in the minor collection. The number of garbage collector threads can be
      // controlled with a command line option (see below). On a host with 1 CPU the throughput
      // collector will likely not perform as well as the serial collector because of the additional
      // overhead for the parallel execution (e.g., synchronization costs). On a host with 2 CPUs
      // the throughput collector generally performs as well as the serial garbage collector and a
      // reduction in the minor garbage collector pause times can be expected on hosts with more
      // than 2 CPUs.

      // Young Generation GC done in parallel threads
      argumentsBuilder.add(JvmFlag.USE_PARALLEL_GC.asArgumentString(
          getUseParallelGC() ? TRUE_AS_LONG : FALSE_AS_LONG));

      // Certain phases of an ‘Old Generation’ collection can be performed in parallel, speeding
      // up a old generation collection.
      argumentsBuilder.add(JvmFlag.USE_PARALLEL_OLD_GC.asArgumentString(getUseParallelOldGC()
          ? TRUE_AS_LONG : FALSE_AS_LONG));

      // Growing and shrinking the size of a generation is done by increments that are a fixed
      // percentage of the size of the generation. A generation steps up or down toward its
      // desired size. Growing and shrinking are done at different rates. By default a generation
      // grows in increments of 20% and shrinks in increments of 5%. The percentage for growing
      // is controlled by the command line flag -XX:YoungGenerationSizeIncrement=<nnn > for the
      // young generation and -XX:TenuredGenerationSizeIncrement=<nnn> for the tenured generation.
      //
      // We assume 0 means ignore the setting.
      if (getYoungGenerationSizeIncrement() > 0) {
        JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT.validate(getYoungGenerationSizeIncrement());
        argumentsBuilder.add(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT
            .asArgumentString(getYoungGenerationSizeIncrement()));
      }
      if (getTenuredGenerationSizeIncrement() > 0) {
        JvmFlag.TENURED_GENERATION_SIZE_INCREMENT.validate(getTenuredGenerationSizeIncrement());
        argumentsBuilder.add(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT
            .asArgumentString(getTenuredGenerationSizeIncrement()));
      }

      // The percentage by which a generation shrinks is adjusted by the command line flag
      // -XX: AdaptiveSizeDecrementScaleFactor=<nnn >. If the size of an increment for growing
      // is XXX percent, the size of the decrement for shrinking will be XXX / nnn percent.
      //
      // We assume that setting this to 0 means ignore the setting
      if (getAdaptiveSizeDecrementScaleFactor() > 0) {
        JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR
            .validate(getAdaptiveSizeDecrementScaleFactor());
        argumentsBuilder.add(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR
            .asArgumentString(getAdaptiveSizeDecrementScaleFactor()));
      }

      // A hint to the virtual machine that it's desirable that not more than 1 / (1 + nnn) of the
      // application execution time be spent in the collector.
      //
      // We assume that setting this to 0 means ignore the setting
      if (getGcTimeRatio() > 0) {
        JvmFlag.GC_TIME_RATIO.validate(getGcTimeRatio());
        argumentsBuilder.add(JvmFlag.GC_TIME_RATIO.asArgumentString(getGcTimeRatio()));
      }

      // This is interpreted as a hint to the throughput collector that pause times of <nnn>
      // milliseconds or less are desired. By default there is no maximum pause time goal.  The
      // throughput collector will adjust the Java heap size and other garbage collection related
      // parameters in an attempt to keep garbage collection pauses shorter than <nnn> milliseconds.
      // These adjustments may cause the garbage collector to reduce overall throughput of the
      // application and in some cases the desired pause time goal cannot be met.
      //
      // We assume that setting this to 0 means ignore the setting
      if (getMaxHeapFreeRatio() > 0) {
        JvmFlag.MAX_GC_PAUSE_MILLIS.validate(getMaxGcPauseMillis());
        argumentsBuilder.add(JvmFlag.MAX_GC_PAUSE_MILLIS
            .asArgumentString(getMaxGcPauseMillis()));
      }

      // The number of garbage collector threads can be controlled with the ParallelGCThreads
      // command line option. We assume 0 implies do not set this parameter.
      if (getParallelGCThreads() > 0) {
        JvmFlag.PARALLEL_GC_THREADS.validate(getParallelGCThreads());
        argumentsBuilder.add(JvmFlag.PARALLEL_GC_THREADS
            .asArgumentString(getParallelGCThreads()));
      }
    } else if (getUseSerialGC()) {
      // The Serial Collector
      //
      // This is a simple serial generational garbage collector that performs well on a single
      // CPU. It uses resources efficiently, pauses application threads and its performance is
      // inversely proportional to the size of the heap.
      //
      // It is the best choice for a single CPU and a small heap.
      argumentsBuilder.add(JvmFlag.USE_SERIAL_GC.asArgumentString(TRUE_AS_LONG));
    }

    final String emission = spaceSeparator.join(argumentsBuilder.build());

    logger.finer(String.format("Converted %s into %s.", jvmFlagSet, emission));

    return emission;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof CommandLine)) {
      return false;
    } else {
      return jvmFlagSet.toString().equals(((CommandLine) obj).jvmFlagSet.toString());
    }
  }

  @Override
  public int hashCode() {
    return jvmFlagSet.toString().hashCode();
  }
}
