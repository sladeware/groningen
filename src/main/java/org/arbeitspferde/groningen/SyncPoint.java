package org.arbeitspferde.groningen;

/**
 * A capability list for PipelineSyncPoint implemtations to expose those implemented
 * synchronization points without having to make calls into the individual methods such that one
 * can confirm all necessary steps are implemented before starting that chain of calls.
 */
public enum SyncPoint {
  BLOCK_ITERATION_START(1 << 0),
  SIGNAL_ITERATION_START(1 << 1),
  BLOCK_EXP_ARGS_PUSHED(1 << 2),
  SIGNAL_EXP_ARGS_PUSHED(1 << 3),
  BLOCK_RESTART_WITH_EXP_ARGS(1 << 4),
  SIGNAL_RESTART_EITH_EXP_ARGS(1 << 5),
  FLAG_END_OF_ITERATION(1 << 6),
  BLOCK_ITERATION_FINALIZATION(1 << 7),
  SIGNAL_ITERATION_FINALIZATION(1 << 8);

  // allow ComposedSyncPoints access to the bit value of the SyncPoint
  protected int value;
  SyncPoint(int v) { value = v; }

  /**
   * Composed lists of SyncPoints allow for easy and compact verification of a set of sync points.
   */
  static class ComposedSyncPoints {
    private int composedValue;

    /**
     * Build a set of points against which one can test other points for inclusion in the set.
     * 
     * @param points list of SyncPoints to include in the set
     */
    public ComposedSyncPoints(SyncPoint ... points) {
      composedValue = 0;
      for (SyncPoint s : points) {
        composedValue |= s.value;
      }
    }

    /**
     * Verify that the list of {@link SyncPoint SyncPoints} are all in the composed list this
     * object is maintaining.
     * 
     * @param points a list of SyncPoints to check for inclusion in the list maintained within.
     * @return true if all requested points are included in this object.
     */
    public boolean supportsSyncPoints(SyncPoint ... points) {
      for (SyncPoint s : points) {
        if ((composedValue & s.value) == 0) {
          return false;
        }
      }
      return true;
    }
  } 
}
