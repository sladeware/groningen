package org.arbeitspferde.groningen.datastore;

import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.Datastore;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineState;
import org.arbeitspferde.groningen.config.GroningenConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * {@link Datastore} implementation that keeps everything in memory only.
 */
@Singleton
public class MemoryDatastore implements Datastore {
  private static final Logger log = Logger.getLogger(MemoryDatastore.class.getCanonicalName());

  private final Map<PipelineId, PipelineState> data = new HashMap<PipelineId, PipelineState>();
  
  @Override
  public synchronized PipelineId[] listPipelinesIds() {
    log.fine("list pipeline ids");
    return data.keySet().toArray(new PipelineId[] {});
  }

  @Override
  public synchronized PipelineState[] getPipelines(PipelineId[] ids) {
    log.fine(String.format("get pipelines %s", Joiner.on(",").join(ids)));
    
    List<PipelineState> states = new ArrayList<PipelineState>();
    for (PipelineId id : ids) {
      states.add(data.get(id));
    }
    return states.toArray(new PipelineState[] {});
  }

  @Override
  public synchronized void createPipeline(PipelineState pipelineState, boolean checkForConflicts) 
      throws PipelineAlreadyExists, PipelineConflictsWithRunningPipelines {
    log.fine(String.format("createPipeline %s", pipelineState.pipelineId()));
    
    PipelineState existingPipeline = data.get(pipelineState.pipelineId());
    if (existingPipeline != null) {
      throw new PipelineAlreadyExists(existingPipeline);
    }
    
    if (checkForConflicts) {
      PipelineState[] conflictingPipelines =
            findConflictingPipelines(pipelineState.config());
      if (conflictingPipelines.length > 0) {
        throw new PipelineConflictsWithRunningPipelines(conflictingPipelines);
      }
    }
    
    writePipelines(new PipelineState[] {pipelineState});
  }

  @Override
  public synchronized void writePipelines(PipelineState[] pipelinesStates) {
    List<PipelineId> ids = new ArrayList<PipelineId>();
    for (PipelineState state : pipelinesStates) {
      ids.add(state.pipelineId());
    }
    log.fine(String.format("write pipelines %s", Joiner.on(",").join(ids)));
    
    for (PipelineState state : pipelinesStates) {
      data.put(state.pipelineId(), state);
    }
  }

  @Override
  public synchronized void deletePipelines(PipelineId[] ids) {
    log.fine(String.format("delete pipelines %s", Joiner.on(",").join(ids)));
    
    for (PipelineId id : ids) {
      data.remove(id);
    }
  }

  @Override
  public PipelineState[] findConflictingPipelines(GroningenConfig pipelineConfiguration) {
    // TODO(mbushkov): implement findConflictingPipelines logic.
    return new PipelineState[] {};
  }

}
