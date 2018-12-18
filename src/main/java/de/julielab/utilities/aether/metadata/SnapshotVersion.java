package de.julielab.utilities.aether.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SnapshotVersion {
    private String classifier = "";
    private String extension;
    @JsonProperty("value")
    private String version;
    private String updated;

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getExtension() {
        return extension;
    }

    public boolean hasExtension(String extension) {
        return this.extension.equalsIgnoreCase(extension);
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return "SnapshotVersion{" +
                "classifier='" + classifier + '\'' +
                ", extension='" + extension + '\'' +
                ", version='" + version + '\'' +
                ", updated='" + updated + '\'' +
                '}';
    }
}
