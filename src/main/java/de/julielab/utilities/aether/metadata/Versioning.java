package de.julielab.utilities.aether.metadata;


import java.util.List;

public class Versioning {
    private String latest;
    private String release;
    private Snapshot snapshot;
    private String lastUpdated;

    @Override
    public String toString() {
        return "Versioning{" +
                "latest='" + latest + '\'' +
                ", release='" + release + '\'' +
                ", snapshot=" + snapshot +
                ", lastUpdated='" + lastUpdated + '\'' +
                ", snapshotVersions=" + snapshotVersions +
                ", versions=" + versions +
                '}';
    }

    private List<SnapshotVersion> snapshotVersions;
    private List<String> versions;

    public String getLatest() {
        return latest;
    }

    public void setLatest(String latest) {
        this.latest = latest;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<SnapshotVersion> getSnapshotVersions() {
        return snapshotVersions;
    }

    public void setSnapshotVersions(List<SnapshotVersion> snapshotVersions) {
        this.snapshotVersions = snapshotVersions;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }
}
