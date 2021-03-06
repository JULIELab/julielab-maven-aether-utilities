package de.julielab.utilities.aether;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public class MavenArtifact implements Serializable {

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    transient private File file;
    private String packaging = "jar";

    public MavenArtifact() {


    }

    public MavenArtifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public MavenArtifact(Artifact artifact) {
        groupId = artifact.getGroupId();
        artifactId = artifact.getArtifactId();
        version = artifact.getVersion();
        classifier = artifact.getClassifier();
        file = artifact.getFile();
        packaging = artifact.getExtension();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenArtifact that = (MavenArtifact) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(classifier, that.classifier) &&
                Objects.equals(file, that.file) &&
                Objects.equals(packaging, that.packaging);
    }

    @Override
    public int hashCode() {

        return Objects.hash(groupId, artifactId, version, classifier, file, packaging);
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
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

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setCoordinatesFromFile() {
        if (file == null)
            throw new IllegalStateException("The POM file is not set.");
        if (!file.exists())
            throw new IllegalStateException("The POM file " + file.getAbsolutePath() + " should be used to set the MavenArtifact coordinates, but the file does not exist.");
        final Model model = MavenProjectUtilities.getRawPomModel(file);
        setGroupId(model.getGroupId());
        if (getGroupId() == null && model.getParent() != null)
            setGroupId(model.getParent().getGroupId());
        else throw new IllegalArgumentException("The POM file " + file.getAbsolutePath() + " does not specify a groupId nor a parent to retrieve the groupId from.‚");
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setPackaging(model.getPackaging());
    }

    public Artifact asAetherArtifact() {
        final DefaultArtifact ret = new DefaultArtifact(groupId, artifactId, classifier, packaging, version);
        ret.setFile(file);
        return ret;
    }

    @Override
    public String toString() {
        return "MavenArtifact{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                ", file=" + file +
                ", packaging='" + packaging + '\'' +
                '}';
    }
}
