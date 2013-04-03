package org.arbeitspferde.groningen.historydatastore;

import org.arbeitspferde.groningen.HistoryDatastore;

/**
 * Test for {@link MemoryHistoryDatastore}.
 */
public class MemoryHistoryDatastoreTest extends HistoryDatastoreTestBase {

  @Override
  protected HistoryDatastore createHistoryDatastore() {
    return new MemoryHistoryDatastore();
  }

  @Override
  protected void destroyHistoryDatastore(HistoryDatastore dataStore) {
  }

}
