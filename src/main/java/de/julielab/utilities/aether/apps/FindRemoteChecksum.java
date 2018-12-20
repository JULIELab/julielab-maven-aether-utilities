package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.AetherUtilities;
import de.julielab.utilities.aether.MavenArtifact;
import de.julielab.utilities.aether.MavenException;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.util.List;
import java.util.Optional;

public class FindRemoteChecksum {
    public static void main(String args[]) throws MavenException {

        if (args.length != 2) {
            System.err.println("Usage: " + FindRemoteChecksum.class.getSimpleName() + " <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version> <comparison checksum>");
            System.exit(1);
        }
        // Use Aether's version string parsing algorithm
        final DefaultArtifact artifact = new DefaultArtifact(args[0]);
        final Optional<List<AetherUtilities.Checksum>> checksumsOpt = AetherUtilities.getRemoteChecksums(new MavenArtifact(artifact));
        if (!checksumsOpt.isPresent())
            System.out.println("CHECKSUM FOUND: false");
        else {
            final List<AetherUtilities.Checksum> checksums = checksumsOpt.get();
            boolean found = false;
            for (AetherUtilities.Checksum checksum : checksums) {
                found |= checksum.getChecksum().equals(args[1]);
            }
            System.out.println("CHECKSUM FOUND: " + found);
        }
    }
}
