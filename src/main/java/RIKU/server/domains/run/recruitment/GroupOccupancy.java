package RIKU.server.domains.run.recruitment;

/** One row per exact group name, including legacy unassigned participants. */
public interface GroupOccupancy {
    String getName();
    long getUsed();
}
