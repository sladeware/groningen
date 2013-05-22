package org.arbeitspferde.groningen;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.config.DatastoreConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PipelineRestorer queries the datastore for the pipelines belonging to the current shard. Then it
 * starts them via {@link PipelineManager}. 
 */
public class PipelineRestorer {
  private static final Logger log = Logger.getLogger(PipelineRestorer.class.getCanonicalName());

  private Integer shardIndex;
  private Datastore dataStore;
  private PipelineManager pipelineManager;
  private PipelineIdGenerator pipelineIdGenerator;

  @Inject
  public PipelineRestorer(@Named("shardIndex") Integer shardIndex, Datastore dataStore,
      PipelineManager pipelineManager, PipelineIdGenerator pipelineIdGenerator) {
        this.shardIndex = shardIndex;
        this.dataStore = dataStore;
        this.pipelineManager = pipelineManager;
        this.pipelineIdGenerator = pipelineIdGenerator;
  }

  public void restorePipelines() {
    List<PipelineId> currentShardIds = new ArrayList<PipelineId>();
    List<PipelineId> allPipelinesIds;
    try {
      allPipelinesIds = dataStore.listPipelinesIds();
    } catch (DatastoreException e) {
      log.log(Level.SEVERE, "can't list pipelines in datastore: " + e.getMessage(), e);
      throw new RuntimeException(e);
    }
    
    for (PipelineId id : allPipelinesIds) {
      if (pipelineIdGenerator.shardIndexForPipelineId(id) == shardIndex) {
        currentShardIds.add(id);
      }
    }
    log.info(String.format("%d out of %d stored pipelines belong to me (shard %d).",
        currentShardIds.size(), allPipelinesIds.size(), shardIndex));
    
    List<PipelineState> states;
    try {
      states = dataStore.getPipelines(currentShardIds);
    } catch (DatastoreException e) {
      log.log(Level.SEVERE, "can't get pipelines in datastore: " + e.getMessage(), e);
      throw new RuntimeException(e);
    }
    
    for (PipelineState state : states) {
      pipelineManager.restorePipeline(state,
          new DatastoreConfigManager(dataStore, state.pipelineId()), false);
      log.fine(String.format("Restored pipeline %s.", state.pipelineId()));
    }
    
    log.info("All pipelines belonging to this shard were restored.");
  }
}

