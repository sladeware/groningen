package org.arbeitspferde.groningen.datastore;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.Datastore;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineState;
import org.arbeitspferde.groningen.config.GroningenConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * {@link Datastore} implementation that keeps everything in memory only.
 */
@Singleton
public class InMemoryDatastore implements Datastore {
  private static final Logger log = Logger.getLogger(InMemoryDatastore.class.getCanonicalName());

  private final Map<PipelineId, PipelineState> data = Maps.newHashMap();

  @Override
  public synchronized List<PipelineId> listPipelinesIds() {
    log.fine("list pipeline ids");
    return Lists.newArrayList(data.keySet());
  }

  @Override
  public synchronized List<PipelineState> getPipelines(List<PipelineId> ids) {
    log.fine(String.format("get pipelines %s", Joiner.on(",").join(ids)));

    List<PipelineState> states = new ArrayList<>();
    for (PipelineId id : ids) {
      states.add(data.get(id));
    }
    return states;
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
      List<PipelineState> conflictingPipelines =
            findConflictingPipelines(pipelineState.config());
      if (!conflictingPipelines.isEmpty()) {
        throw new PipelineConflictsWithRunningPipelines(conflictingPipelines);
      }
    }

    writePipelines(Lists.newArrayList(pipelineState));
  }

  @Override
  public synchronized void writePipelines(List<PipelineState> pipelinesStates) {
    List<PipelineId> ids = new ArrayList<>();
    for (PipelineState state : pipelinesStates) {
      ids.add(state.pipelineId());
    }
    log.fine(String.format("write pipelines %s", Joiner.on(",").join(ids)));

    for (PipelineState state : pipelinesStates) {
      data.put(state.pipelineId(), state);
    }
  }

  @Override
  public synchronized void deletePipelines(List<PipelineId> ids) {
    log.fine(String.format("delete pipelines %s", Joiner.on(",").join(ids)));

    for (PipelineId id : ids) {
      data.remove(id);
    }
  }

  @Override
  public List<PipelineState> findConflictingPipelines(GroningenConfig pipelineConfiguration) {
    // TODO(mbushkov): implement this method. Returning empty result here is preferable:
    // throwing UnsupportedOperationException would prevent JTune from using this class, as
    // JTune uses findConflictingPipelines() as an additional check when launching pipelines.
    return Lists.newArrayList();
  }

}

