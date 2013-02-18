package org.arbeitspferde.groningen;

import org.arbeitspferde.groningen.config.GroningenConfig;

/**
 * Interface for Groningen's storage system.
 */
public interface Datastore {
  /**
   * Base exception class for all Datastore-related exceptions. This exception was intentionally
   * made a checked one, because nearly any datastore operation may fail and usually we have
   * to recover from the failure in the place of a datastore call.
   */
  class DatastoreException extends Exception {
    public DatastoreException() {
      super();
    }
    
    public DatastoreException(final String message) {
      super(message);
    }
    
    public DatastoreException(final Throwable cause) {
      super(cause);
    }
    
    public DatastoreException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
  
  /**
   * Exception thrown when pipeline in question already exists.
   */
  class PipelineAlreadyExists extends DatastoreException {
    private PipelineState existingPipeline;
    
    public PipelineAlreadyExists(PipelineState existingPipeline) {
      this.existingPipeline = existingPipeline;
    }
    
    public PipelineState existingPipeline() {
      return existingPipeline;
    }
  }

  /**
   * Exception thrown when pipeline in question conflicts with already existing pipeline.
   */
  class PipelineConflictsWithRunningPipelines extends DatastoreException {
    private PipelineState[] conflictingPipelines;
    
    public PipelineConflictsWithRunningPipelines(PipelineState[] conflictingPipelines) {
      this.conflictingPipelines = conflictingPipelines;
    }
    
    public PipelineState[] conflictingPipelines() {
      return conflictingPipelines;
    }
  }
  
  PipelineId[] listPipelinesIds() throws DatastoreException;
  PipelineState[] getPipelines(PipelineId[] ids) throws DatastoreException;
  
  void createPipeline(PipelineState pipelineState, boolean checkForConflicts)
    throws PipelineAlreadyExists, PipelineConflictsWithRunningPipelines, DatastoreException;
  void writePipelines(PipelineState[] pipelinesStates) throws DatastoreException;
  void deletePipelines(PipelineId[] ids) throws DatastoreException;
  
  PipelineState[] findConflictingPipelines(GroningenConfig pipelineConfiguration)
    throws DatastoreException;
}
