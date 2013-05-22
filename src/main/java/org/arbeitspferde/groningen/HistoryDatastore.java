package org.arbeitspferde.groningen;

import org.joda.time.Instant;

import java.util.List;

/**
 * Datastore for historical data.
 */
public interface HistoryDatastore {
  /**
   * Base exception class for all HistoryDatastoreException-related exceptions. This exception 
   * was intentionally made a checked one, because nearly any datastore operation may fail and 
   * usually we have to recover from the failure in the place of a datastore call.
   */
  class HistoryDatastoreException extends Exception {
    public HistoryDatastoreException() {
      super();
    }
    
    public HistoryDatastoreException(final String message) {
      super(message);
    }
    
    public HistoryDatastoreException(final Throwable cause) {
      super(cause);
    }
    
    public HistoryDatastoreException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
  
  void writeState(PipelineHistoryState state) throws HistoryDatastoreException;
  
  List<PipelineId> listPipelinesIds() throws HistoryDatastoreException;
  
  List<PipelineHistoryState> getStatesForPipelineId(PipelineId pipelineId)
    throws HistoryDatastoreException;
  
  List<PipelineHistoryState> getStatesForPipelineId(PipelineId pipelineId, Instant afterTimestamp)
    throws HistoryDatastoreException;
}
