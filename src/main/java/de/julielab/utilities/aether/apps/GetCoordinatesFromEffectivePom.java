package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.MavenException;
import de.julielab.utilities.aether.MavenProjectUtilities;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.FileNotFoundException;

public class GetCoordinatesFromEffectivePom {

    public static void main(String args[]) throws FileNotFoundException, MavenException {
        if (args.length != 1) {
            System.err.println("Usage: " + GetCoordinatesFromEffectivePom.class.getSimpleName() + " <path to pom.xml>");
            System.err.println("This program retrieves the coordinates of the artifact denoted by the given pom.xml " +
                    "by building the effective model, including parent POM, superpom, properties etc, and retrieving " +
                    "the coordinates from the resulting model. A quicker approach is " + GetCoordinatesFromRawPom.class.getSimpleName() +
                    " which does not attempt to build the full model but relies on simple rules to retrieve missing " +
                    "information (e.g. the version and the groupId might only be defined for the parent POM)."
            );
            System.exit(1);
        }
        final File pom = new File(args[0]);
        if (!pom.exists())
            throw new FileNotFoundException(args[0] + " does not exist");
        final Model model = MavenProjectUtilities.getEffectivePomModel(pom);
        System.out.println("GROUPID: " + model.getGroupId());
        System.out.println("ARTIFACTID: " + model.getArtifactId());
        System.out.println("VERSION: " + model.getVersion());
        System.out.println("PACKAGING: " + model.getPackaging());

    }
}
