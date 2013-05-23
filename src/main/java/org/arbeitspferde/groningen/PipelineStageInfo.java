package org.arbeitspferde.groningen;

import com.google.inject.Inject;

/**
 * Ties together the iteration count and pipeline state into a value object in which the count
 * and state can be operated/retrieved atomically. Encapsulating them allows for state to be
 * updated locally within the {@link Pipeline}, the {@link PipelineIteration}, and the
 * {@link PipelineSynchronizer} as well.
 */
public class PipelineStageInfo {
  private int iterationNumber;
  private PipelineStageState state;
  
  @Inject
  public PipelineStageInfo() {
    this(0, PipelineStageState.INITIALIZED);
  }
  
  public PipelineStageInfo(int iterationNumber, PipelineStageState state) {
    this.iterationNumber = iterationNumber;
    this.state = state;
  }
  
  /** Set only the pipeline state. */
  public synchronized void set(PipelineStageState state) {
    this.state = state;
  }

  /** Increment the iteration count and set the stage atomically. */
  public synchronized void incrementIterationAndSetState(PipelineStageState state) {
    iterationNumber++;
    this.state = state;
  }

  /**
   * Get a tuple of the iteration count and state atomically.
   * 
   * @return the iteration count and state
   */
  public synchronized ImmutablePipelineStageInfo getImmutableValueCopy() {
    return new ImmutablePipelineStageInfo(iterationNumber, state);
  }

  /**
   * An immutable value object that pairs the iteration count and state together.
   */
  public static class ImmutablePipelineStageInfo {
    public final int iterationNumber;
    public final PipelineStageState state;
    
    private ImmutablePipelineStageInfo(int iterationNumber, PipelineStageState state) {
      this.iterationNumber = iterationNumber;
      this.state = state;
    }
  }
}
