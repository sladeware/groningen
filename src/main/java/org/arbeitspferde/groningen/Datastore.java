package org.arbeitspferde.groningen;

import org.arbeitspferde.groningen.config.GroningenConfig;

import java.util.List;

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
    private List<PipelineState> conflictingPipelines;
    
    public PipelineConflictsWithRunningPipelines(List<PipelineState> conflictingPipelines) {
      this.conflictingPipelines = conflictingPipelines;
    }
    
    public List<PipelineState> conflictingPipelines() {
      return conflictingPipelines;
    }
  }
  
  List<PipelineId> listPipelinesIds() throws DatastoreException;
  
  List<PipelineState> getPipelines(List<PipelineId> ids) throws DatastoreException;
  
  void createPipeline(PipelineState pipelineState, boolean checkForConflicts)
    throws PipelineAlreadyExists, PipelineConflictsWithRunningPipelines, DatastoreException;
  
  void writePipelines(List<PipelineState> pipelinesStates) throws DatastoreException;
  
  void deletePipelines(List<PipelineId> ids) throws DatastoreException;
  
  List<PipelineState> findConflictingPipelines(GroningenConfig pipelineConfiguration)
    throws DatastoreException;
}

