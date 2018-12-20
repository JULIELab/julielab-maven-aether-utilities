package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.MavenException;
import de.julielab.utilities.aether.MavenProjectUtilities;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.FileNotFoundException;

public class GetCoordinatesFromRawPom {

    public static void main(String args[]) throws FileNotFoundException, MavenException {
        if (args.length != 1) {
            System.err.println("Usage: " + GetCoordinatesFromRawPom.class.getSimpleName() + " <path to pom.xml>");
            System.err.println("This program retrieves the artifact coordinates of the given pom.xml file. To do this, " +
                    "the XML is parsed and the respective information is retrieved from the project element. If " +
                    "the artifact does not define a groupId or version itself, the parent element is checked for these " +
                    "information. This program does not attempt to resolve the complete project model including " +
                    "parent and super POM as " + GetCoordinatesFromEffectivePom.class.getSimpleName() + " does.");
            System.exit(1);
        }
        final File pom = new File(args[0]);
        if (!pom.exists())
            throw new FileNotFoundException(args[0] + " does not exist");
        final Model model = MavenProjectUtilities.getRawPomModel(pom);
        String groupId = model.getGroupId();
        String version = model.getVersion();
        if (null == groupId)
            groupId = model.getParent().getGroupId();
        if (null == version)
            version = model.getParent().getVersion();
        System.out.println("GROUPID: " + groupId);
        System.out.println("ARTIFACTID: " + model.getArtifactId());
        System.out.println("VERSION: " + version);
        System.out.println("PACKAGING: " + model.getPackaging());

    }
}
