package de.julielab.utilities.aether.metadata;

public class Snapshot {
    private String timestamp;
    private String localCopy;

    @Override
    public String toString() {
        return "Snapshot{" +
                "timestamp='" + timestamp + '\'' +
                ", localCopy='" + localCopy + '\'' +
                ", buildNumber=" + buildNumber +
                '}';
    }

    public String getLocalCopy() {
        return localCopy;
    }

    public void setLocalCopy(String localCopy) {
        this.localCopy = localCopy;
    }

    private int buildNumber;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }
}
