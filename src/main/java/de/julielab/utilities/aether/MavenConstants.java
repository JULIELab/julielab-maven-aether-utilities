package de.julielab.utilities.aether;

import java.io.File;
import java.util.StringJoiner;

public class MavenConstants {
    public static final File LOCAL_REPO = new File(System.getProperty("user.home") + File.separatorChar + MavenConstants.LOCAL_REPO);
    public static final String SONATYPE_REPO_ID = "sonatype-nexus";
    public static final String SONATYPE_REPO_TYPE = "default";
    public static final String SONATYPE_REPO_URL = "https://oss.sonatype.org/content/repositories/releases/";
    public static final String SONATYPE_SNAPSHOT_REPO_URL = "https://oss.sonatype.org/content/repositories/snapshots/";
    public static final CharSequence POM = "pom.xml";

    private MavenConstants() {
    }
}
