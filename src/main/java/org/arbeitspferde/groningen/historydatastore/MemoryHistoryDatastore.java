package org.arbeitspferde.groningen.historydatastore;

import org.arbeitspferde.groningen.HistoryDatastore;
import org.arbeitspferde.groningen.PipelineHistoryState;
import org.arbeitspferde.groningen.PipelineId;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memory-only implementation of {@link HistoryDatastore}.
 */
public class MemoryHistoryDatastore implements HistoryDatastore {

  private Map<PipelineId, List<PipelineHistoryState>> data =
      new HashMap<PipelineId, List<PipelineHistoryState>>();
  
  @Override
  public void writeState(PipelineHistoryState state) {
    List<PipelineHistoryState> states = data.get(state.pipelineId());
    if (states == null) {
      states = new ArrayList<PipelineHistoryState>();
      data.put(state.pipelineId(), states);
    }
    states.add(state);
    Collections.sort(states, new Comparator<PipelineHistoryState>() {
      @Override
      public int compare(PipelineHistoryState s1, PipelineHistoryState s2) {
        return s1.endTimestamp().compareTo(s2.endTimestamp());
      }
    });
  }

  @Override
  public PipelineId[] listPipelinesIds() {
    return data.keySet().toArray(new PipelineId[] {});
  }

  @Override
  public PipelineHistoryState[] getStatesForPipelineId(PipelineId pipelineId) {
    List<PipelineHistoryState> states = data.get(pipelineId);
    if (states == null) {
      return null;
    } else {
      return states.toArray(new PipelineHistoryState[] {});
    }
  }

  @Override
  public PipelineHistoryState[] getStatesForPipelineId(
      PipelineId pipelineId, Instant afterTimestamp) {
    List<PipelineHistoryState> states = data.get(pipelineId);
    if (states == null) {
      return null;
    } else {
      List<PipelineHistoryState> results = new ArrayList<PipelineHistoryState>();
      for (PipelineHistoryState s : states) {
        if (s.endTimestamp().isAfter(afterTimestamp)) {
          results.add(s);
        }
      }
      return results.toArray(new PipelineHistoryState[] {});
    }
  }
}
