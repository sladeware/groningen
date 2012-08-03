package org.arbeitspferde.groningen;

import com.google.inject.Singleton;

/**
 * {@link Build} is a class which is generated during build time and includes pertinent
 * information such as build timestamp, branch, commit, and user.
 */
@Singleton
public class Build {
  private static final String BUILD_TIMESTAMP = "Fr 3. Aug 21:19:11 UTC 2012";
  private static final String BUILD_BRANCH = "master";
  private static final String BUILD_COMMIT = "b9d2c258d5a41070785ed6d83952c5743592b6d0";
  private static final String BUILD_USER = "mtp";
  private static final String BUILD_UNAME = "Linux tyranny.sfo.corp.google.com 2.6.38.8-gg868 #2 SMP Fri May 18 03:37:30 PDT 2012 x86_64 GNU/Linux";

  public String getTimestamp() {
    return BUILD_TIMESTAMP;
  }

  public String getBranch() {
    return BUILD_BRANCH;
  }

  public String getCommit() {
    return BUILD_COMMIT;
  }

  public String getUser() {
    return BUILD_USER;
  }

  public String getUname() {
    return BUILD_UNAME;
  }

  public String getStamp() {
    return new StringBuilder()
        .append("Friesian built on ")
        .append(BUILD_TIMESTAMP)
        .append(" by ")
        .append(BUILD_USER)
        .append(" from branch ")
        .append(BUILD_BRANCH)
        .append(" at commit ")
        .append(BUILD_COMMIT)
        .append(" on a machine with identity ")
        .append(BUILD_UNAME)
        .append(".")
        .toString();
  }
}
