package org.arbeitspferde.groningen.config;

import org.arbeitspferde.groningen.Datastore;
import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineState;

/**
 * {@link ConfigManager } implementation that gets configuration from {@link Datastore}.
 */
public class DatastoreConfigManager implements ConfigManager {
  private Datastore dataStore;
  private PipelineId pipelineId;

  public DatastoreConfigManager(Datastore dataStore, PipelineId pipelineId) {
    this.dataStore = dataStore;
    this.pipelineId = pipelineId;
  }

  @Override
  public void initialize() {
  }

  @Override
  public void shutdown() {
  }

  @Override
  public GroningenConfig queryConfig() {
    PipelineState[] states;
    try {
      states = dataStore.getPipelines(new PipelineId[] { pipelineId });
    } catch (DatastoreException e) {
      throw new RuntimeException(e);
    }
    
    if (states.length == 0) {
      throw new RuntimeException("No state found.");
    } else {
      return states[0].config();
    }
  }
}
