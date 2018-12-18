package de.julielab.utilities.aether.metadata;

public class Snapshot {
    private String timestamp;
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

    @Override
    public String toString() {
        return "Snapshot{" +
                "timestamp='" + timestamp + '\'' +
                ", buildNumber=" + buildNumber +
                '}';
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }
}
