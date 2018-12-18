package de.julielab.utilities.aether.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

@JsonIgnoreProperties("plugins")
public class MetadataFile {
    private String modelVersion;
    private String groupId;
    private String artifactId;
    private String version;
    private Versioning versioning;

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String toString() {
        return "MetadataFile{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", versioning=" + versioning +
                '}';
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Versioning getVersioning() {
        return versioning;
    }

    public void setVersioning(Versioning versioning) {
        this.versioning = versioning;
    }

    public Artifact getCorrespondingReleaseArtifact(String extension) {
        return new DefaultArtifact(groupId, artifactId, version, extension);
    }

    public Artifact getCorrespondingSnapshotArtifact(SnapshotVersion version) {
        return new DefaultArtifact(groupId, artifactId, version.getExtension(), version.getVersion());
    }
}
