package org.arbeitspferde.groningen.config;

import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.Datastore;
import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineState;

import java.util.List;

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
    List<PipelineState> states;
    try {
      states = dataStore.getPipelines(Lists.newArrayList(pipelineId));
    } catch (DatastoreException e) {
      throw new RuntimeException(e);
    }
    
    if (states.isEmpty()) {
      throw new RuntimeException("No state found.");
    } else {
      return states.get(0).config();
    }
  }
}
