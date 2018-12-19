package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.AetherUtilities;
import de.julielab.utilities.aether.MavenArtifact;
import de.julielab.utilities.aether.MavenException;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.util.List;
import java.util.Optional;

public class GetRemoteChecksums {
    public static void main(String args[]) throws MavenException {
        if (args.length != 1) {
            System.err.println("Usage: " + GetRemoteChecksums.class.getSimpleName() + " <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
            System.exit(1);
        }
        // Use Aether's version string parsing algorithm
        final DefaultArtifact artifact = new DefaultArtifact(args[0]);
        final Optional<List<AetherUtilities.Checksum>> checksumsOpt = AetherUtilities.getRemoteChecksums(new MavenArtifact(artifact));
        if (!checksumsOpt.isPresent())
            System.out.println("<checkums not found>");
        else {
            final List<AetherUtilities.Checksum> checksums = checksumsOpt.get();
            for (AetherUtilities.Checksum checksum : checksums) {
                System.out.println("CHECKSUM:\t" + checksum.getAlgorithm() + "\t" + checksum.getChecksum() + "\t" + checksum.getRepository().getUrl());
            }
        }
    }
}
