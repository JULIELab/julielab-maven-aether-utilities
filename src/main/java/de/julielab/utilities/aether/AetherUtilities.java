package de.julielab.utilities.aether;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.julielab.utilities.aether.metadata.MetadataFile;
import de.julielab.utilities.aether.metadata.SnapshotVersion;
import org.apache.maven.model.building.*;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.julielab.utilities.aether.MavenConstants.LOCAL_REPO;
import static java.util.stream.Collectors.toList;

/**
 * Taken and adapted from:
 * https://stackoverflow.com/questions/48537735/download-artifact-from-maven-repository-in-java-program
 */

public class AetherUtilities {
    private final static Logger log = LoggerFactory.getLogger(AetherUtilities.class);


    private AetherUtilities() {
    }


    public static Optional<List<Checksum>> getRemoteChecksums(MavenArtifact artifact) throws MavenException {
        try {
            RepositorySystemSession session = MavenRepositoryUtilities.newSession(MavenRepositoryUtilities.newRepositorySystem(), LOCAL_REPO);
            if (artifact.getAetherArtifact().isSnapshot()) {
                return getRemoteChecksumsOfSnapshotArtifact(artifact, session);
            } else {
                return getRemoteChecksumsOfReleaseArtifact(artifact, session);
            }
        } catch (SettingsBuildingException | NoRepositoryLayoutException e) {
            throw new MavenException(e);
        }
    }

    private static Optional<List<Checksum>> getRemoteChecksumsOfReleaseArtifact(MavenArtifact artifact, RepositorySystemSession session) throws SettingsBuildingException, NoRepositoryLayoutException, MavenException {
        List<Checksum> ret = new ArrayList<>();
        final Maven2RepositoryLayoutFactory layoutFactory = new Maven2RepositoryLayoutFactory();
        final List<RemoteRepository> repositories = MavenRepositoryUtilities.getEffectiveRepositories(session);
        for (RemoteRepository repository : repositories) {
            final RepositoryLayout layout = layoutFactory.newInstance(session, repository);
            final List<RepositoryLayout.Checksum> checksums = layout.getChecksums(artifact.getAetherArtifact(), false, layout.getLocation(artifact.getAetherArtifact(), false));
            for (RepositoryLayout.Checksum cs : checksums) {
                final URI checksumUri = URI.create(repository.getUrl() + "/" + cs.getLocation().toString());
                // And now finally read the checksum file's contents. It should be a single line with the actual checksum.
                try (BufferedReader br = new BufferedReader(new InputStreamReader(checksumUri.toURL().openStream()))) {
                    final Optional<String> any = br.lines().filter(s -> !s.isEmpty()).findAny();
                    if (!any.isPresent()) {
                        log.warn("Checksum file at {} does not have any content", checksumUri);
                    } else {
                        ret.add(new Checksum(any.get(), cs.getAlgorithm(), repository));
                    }
                } catch (FileNotFoundException e) {
                    log.debug("Checksum file for artifact {} was not found at {}", artifact, checksumUri);
                } catch (IOException e) {
                    if (e.getMessage().contains("code: 401"))
                        log.warn("Access for URI {} was denied when trying to retrieve checksum of artifact {}", checksumUri, artifact);
                    else throw new MavenException(e);
                }
            }
        }
        return ret.isEmpty() ? Optional.empty() : Optional.of(ret);
    }

    private static Optional<List<Checksum>> getRemoteChecksumsOfSnapshotArtifact(MavenArtifact artifact, RepositorySystemSession session) throws MavenException {
        List<Checksum> ret = new ArrayList<>();
        final Maven2RepositoryLayoutFactory layoutFactory = new Maven2RepositoryLayoutFactory();
        try {
            // Request the metadata. It will just be stored in the local repository as XML files.
            final List<MetadataResult> metadataResults = getRemoteArtifactMetadata(artifact);
            final List<MetadataResult> filteredMetadataResults = metadataResults.stream().filter(result -> !result.isMissing()).collect(toList());
            // Not all nexus servers seem to create the meta data. Our intenal nexus doesn't, for example.
            // So this algorithm will only work as long as any server will offer the meta data.
            if (filteredMetadataResults.isEmpty()) {
                try {
                    getArtifactByAether(artifact, LOCAL_REPO, false);
                } catch (MavenException e) {
                    if (e.getCause() instanceof ArtifactResolutionException) {
                        // This artifact does not exist. Return the empty optional.
                        return Optional.empty();
                    } else throw e;
                }
                log.info("No existing meta data could be found for artifact {}, checking if the artifact already existing in any repository", artifact);
                throw new IllegalStateException("The requested snapshot artifact " + artifact + " has no meta data on any server in " + metadataResults.stream().map(MetadataResult::getRequest).map(MetadataRequest::getRepository).map(RemoteRepository::toString).collect(Collectors.joining(", ")) + ". This case is currently not supported by this code. A workaround needs to be added.");
            }

            XmlMapper xmlMapper = new XmlMapper();
            for (MetadataResult result : filteredMetadataResults) {
                // Get the location of the metadata XML file on disc, parse it using the XmlMapper and check
                // that we have found the correct artifact extension type.
                final Metadata metadata = result.getMetadata();
                final RemoteRepository repository = result.getRequest().getRepository();
                final MetadataFile metadataFile = xmlMapper.readValue(metadata.getFile(), MetadataFile.class);
                final Optional<SnapshotVersion> svOpt = metadataFile.getVersioning().getSnapshotVersions().stream().filter(sv -> sv.hasExtension(artifact.getPackaging())).findAny();
                if (!svOpt.isPresent())
                    throw new IllegalArgumentException("Could not find the artifact metadata for artifact " + artifact + " by extension " + artifact.getPackaging());

                // As we have found the meta data we have been looking for, we now use it to construct the
                // URL of the checksum file of the artifact we actually look for.
                final RepositoryLayout layout = layoutFactory.newInstance(session, repository);
                final SnapshotVersion snapshotVersion = svOpt.get();
                final Artifact a = metadataFile.getCorrespondingSnapshotArtifact(snapshotVersion);
                // These "checksums" actually denote the checksum files and then again only the relative path
                // within some repository
                final List<RepositoryLayout.Checksum> checksums = layout.getChecksums(a, false, layout.getLocation(a, false));
                for (RepositoryLayout.Checksum cs : checksums) {
                    // Here we concatenate the repository URL with the relative path of the checksum file
                    URI checksumUri = URI.create(repository.getUrl() + "/" + cs.getLocation().toString());
                    // And now finally read the checksum file's contents. It should be a single line with the actual checksum.
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(checksumUri.toURL().openStream()))) {
                        final Optional<String> any = br.lines().filter(s -> !s.isEmpty()).findAny();
                        if (!any.isPresent()) {
                            log.warn("Checksum file at {} does not have any content", checksumUri);
                        } else {
                            ret.add(new Checksum(any.get(), cs.getAlgorithm(), repository));
                        }
                    }
                }
            }
        } catch (NoRepositoryLayoutException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.of(ret);
    }

    /**
     * <p>
     * Returns the results of metadata requests regarding the request coordinates represented by the passed <tt>MavenArtifact</tt>.
     * </p>
     * <p>
     * Note that the passed artifact really only represents the expected location of the metadata to retrieve.
     * <em>Releases do not have metadata</em>; snapshots have metadata and only the latest one will be returned.
     * To get the meta data about the artifact regardless of its version, leave the <tt>version</tt> and
     * <tt>packaging</tt> fields of the given <tt>MavenArtifact</tt> empty.
     * </p>
     *
     * @param a
     * @return
     * @throws MavenException
     */
    public static List<MetadataResult> getRemoteArtifactMetadata(MavenArtifact a) throws MavenException {
        Metadata requestMetadata;
        RepositorySystem repositorySystem;
        RepositorySystemSession session;
        try {
            repositorySystem = MavenRepositoryUtilities.newRepositorySystem();
            session = MavenRepositoryUtilities.newSession(repositorySystem, LOCAL_REPO);
            requestMetadata = new DefaultMetadata(a.getGroupId(), a.getArtifactId(), a.getVersion(), "maven-metadata.xml", Metadata.Nature.RELEASE_OR_SNAPSHOT);

            List<RemoteRepository> repositories = MavenRepositoryUtilities.getEffectiveRepositories(session);

            List<MetadataRequest> requests = new ArrayList<>();
            for (RemoteRepository rr : repositories) {
                requests.add(new MetadataRequest(requestMetadata, rr, null));
            }

            List<MetadataResult> artifactResult = repositorySystem.resolveMetadata(session, requests);
            return artifactResult;
        } catch (SettingsBuildingException e) {
            throw new MavenException(e);
        }
    }


    public static MavenArtifact getArtifactByAether(MavenArtifact artifact) throws MavenException {
        return getArtifactByAether(artifact,
                LOCAL_REPO);
    }

    public static MavenArtifact getArtifactByAether(MavenArtifact a, File localRepository) throws MavenException {
        return getArtifactByAether(a, localRepository, true);
    }

    public static MavenArtifact getArtifactByAether(MavenArtifact a, File localRepository, boolean searchLocally) throws MavenException {
        Artifact artifact;
        RepositorySystem repositorySystem;
        RepositorySystemSession session;
        ArtifactRequest artifactRequest;
        try {
            repositorySystem = MavenRepositoryUtilities.newRepositorySystem();
            session = MavenRepositoryUtilities.newSession(repositorySystem, localRepository);
            artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getPackaging(), a.getVersion());
            artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);


            List<RemoteRepository> repositories = MavenRepositoryUtilities.getEffectiveRepositories(session);


            artifactRequest.setRepositories(repositories);

            File localArtifactFile = new File(localRepository.getAbsolutePath() + File.separator + session.getLocalRepositoryManager().getPathForLocalArtifact(artifact));

            if (!searchLocally || !localArtifactFile.exists()) {
                ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
                artifact = artifactResult.getArtifact();
            } else {
                MavenArtifact ret = new MavenArtifact(artifact);
                ret.setFile(localArtifactFile);
                return ret;
            }
        } catch (ArtifactResolutionException e) {
            throw new MavenException(e);
        } catch (SettingsBuildingException e) {
            throw new MavenException(e);
        }
        return new MavenArtifact(artifact);
    }

    public static void storeArtifactWithDependencies(MavenArtifact requestedArtifact, File libDir) throws MavenException {
        log.trace("Storing artifact {} with all its dependencies to {}", requestedArtifact, libDir);
        Stream<Artifact> dependencies = getDependencies(requestedArtifact);
        if (!libDir.exists())
            libDir.mkdirs();
        Consumer<Artifact> writer = a -> {
            File destination = new File(libDir.getAbsolutePath() + File.separator + a.getFile().getName());
            try {
                log.trace("Now writing: {} to {}", a, destination);
                Files.copy(a.getFile().toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        };
        dependencies.forEach(writer);
    }

    public static void storeArtifactsWithDependencies(Stream<MavenArtifact> requestedArtifacts, File libDir) throws MavenException {
        log.trace("Storing artifacts {} with all its dependencies to {}", requestedArtifacts, libDir);
        Stream<Artifact> dependencies = getDependencies(requestedArtifacts);
        if (!libDir.exists())
            libDir.mkdirs();
        Consumer<Artifact> writer = a -> {
            File destination = new File(libDir.getAbsolutePath() + File.separator + a.getFile().getName());
            try {
                log.trace("Now writing: {} to {}", a, destination);
                Files.copy(a.getFile().toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        };
        dependencies.forEach(writer);
    }

    /**
     * Retrieves the dependency tree that has <code>requestedArtifact</code> as its root. Thus, the
     * <code>requestedArtifact</code> is resolved itself and included in the returned artifacts.
     *
     * @param requestedArtifact The Maven artifact to retrieve dependencies for.
     * @return The resolved dependencies of <code>requestedArtifact</code>, including <code>requestedArtifact</code>
     * itself.
     * @throws MavenException If an artifact cannot be found or another Maven related error occurs.
     */
    public static Stream<Artifact> getDependencies(MavenArtifact requestedArtifact) throws MavenException {
        return getDependencies(requestedArtifact, false);
    }

    /**
     * Returns all available versions of the given artifact.
     *
     * @param requestedArtifact
     * @return
     * @throws MavenException
     */
    public static Stream<String> getVersions(MavenArtifact requestedArtifact) throws MavenException {
        return getVersions(requestedArtifact, "0", String.valueOf(Integer.MAX_VALUE), true, true);
    }

    /**
     * Retrieves all versions of the given artifact - whose given version is ignored in this method - that are available
     * within the described version range.
     *
     * @param requestedArtifact
     * @param lowerBound
     * @param upperBound
     * @param lowerInclusive
     * @param upperInclusive
     * @return
     * @throws MavenException
     */
    public static Stream<String> getVersions(MavenArtifact requestedArtifact, String lowerBound, String upperBound, boolean lowerInclusive, boolean upperInclusive) throws MavenException {
        RepositorySystem repositorySystem = MavenRepositoryUtilities.newRepositorySystem();
        RepositorySystemSession session;
        try {
            session = MavenRepositoryUtilities.newSession(repositorySystem,
                    LOCAL_REPO);
        } catch (SettingsBuildingException e) {
            throw new MavenException(e);
        }
        String groupId = requestedArtifact.getGroupId();
        String artifactId = requestedArtifact.getArtifactId();
        String classifier = requestedArtifact.getClassifier();
        String version = requestedArtifact.getVersion();
        String packaging = requestedArtifact.getPackaging();
        String lower = lowerInclusive ? "[" : "(";
        String upper = upperInclusive ? "]" : ")";
        String range = lower + lowerBound + ", " + upperBound + upper;
        Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, packaging, range);
        try {
            VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, MavenRepositoryUtilities.getEffectiveRepositories(session), null);
            VersionRangeResult result = repositorySystem.resolveVersionRange(session, versionRangeRequest);
            return result.getVersions().stream().map(Version::toString);
        } catch (SettingsBuildingException e) {
            throw new MavenException(e);
        } catch (VersionRangeResolutionException e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    /**
     * Retrieves all available versions of the given artifact and returns the newest one or null, if no version is available.
     *
     * @param requestedArtifact
     * @return
     * @throws MavenException
     */
    public static String getNewestVersion(MavenArtifact requestedArtifact) throws MavenException {
        List<String> versions = getVersions(requestedArtifact).collect(toList());
        if (!versions.isEmpty())
            return versions.get(versions.size() - 1);
        return null;
    }

    public static Stream<Artifact> getDependencies(Stream<MavenArtifact> requestedArtifacts) throws MavenException {
        RepositorySystem repositorySystem = MavenRepositoryUtilities.newRepositorySystem();
        RepositorySystemSession session;
        try {
            session = MavenRepositoryUtilities.newSession(repositorySystem,
                    LOCAL_REPO);
        } catch (SettingsBuildingException e) {
            throw new MavenException(e);
        }


        final List<Dependency> components = requestedArtifacts
                .map(a -> new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getPackaging(), a.getVersion()))
                .map(a -> new Dependency(a, "compile"))
                .collect(toList());

        try {
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setDependencies(components);
            collectRequest.setRepositories(MavenRepositoryUtilities.getEffectiveRepositories(session));
            CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);
            DependencyNode node = collectResult.getRoot();

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);
            DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
            return dependencyResult.getArtifactResults().stream().map(ArtifactResult::getArtifact);
        } catch (SettingsBuildingException e) {
            e.printStackTrace();
        } catch (DependencyCollectionException e) {
            e.printStackTrace();
        } catch (DependencyResolutionException e) {
            e.printStackTrace();
        }

        return Stream.empty();
    }

    /**
     * Retrieves the dependency tree that has <code>requestedArtifact</code> as its root. Thus, the
     * <code>requestedArtifact</code> is resolved itself and included in the returned artifacts.
     *
     * @param requestedArtifact The Maven artifact to retrieve dependencies for.
     * @return The resolved dependencies of <code>requestedArtifact</code>, including <code>requestedArtifact</code>
     * itself.
     * @throws MavenException If an artifact cannot be found or another Maven related error occurs.
     */
    public static Stream<Artifact> getDependencies(MavenArtifact requestedArtifact, boolean recursiveCall) throws MavenException {
        RepositorySystem repositorySystem = MavenRepositoryUtilities.newRepositorySystem();
        RepositorySystemSession session;
        try {
            session = MavenRepositoryUtilities.newSession(repositorySystem,
                    LOCAL_REPO);
        } catch (SettingsBuildingException e) {
            throw new MavenException(e);
        }

        String groupId = requestedArtifact.getGroupId();
        String artifactId = requestedArtifact.getArtifactId();
        String classifier = requestedArtifact.getClassifier();
        String version = requestedArtifact.getVersion();
        String packaging = requestedArtifact.getPackaging();
        Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, packaging, version);

        Dependency dependency =
                new Dependency(artifact, "compile");

        DependencyResult dependencyResult = null;
        try {
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.setRepositories(MavenRepositoryUtilities.getEffectiveRepositories(session));
            CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);
            DependencyNode node = collectResult.getRoot();

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);
            dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
        } catch (DependencyCollectionException | DependencyResolutionException | SettingsBuildingException e) {
            if (!recursiveCall) {
                // EF 2018/05/13 - it seems to help to just do it again. I have no idea why. Perhaps this is just
                // BS.
                try {
                    return getDependencies(requestedArtifact, true);
                } catch (MavenException e1) {
                    throw new MavenException(e);
                }
            } else {
                throw new MavenException(e);
            }
        }

        return dependencyResult.getArtifactResults().stream().map(ArtifactResult::getArtifact);
    }

    public static class Checksum {
        private String checksum;
        private String algorithm;
        private RemoteRepository repository;

        public Checksum(String checksum, String algorithm, RemoteRepository repository) {
            this.checksum = checksum;
            this.algorithm = algorithm;
            this.repository = repository;

        }

        @Override
        public String toString() {
            return "Checksum{" +
                    "checksum='" + checksum + '\'' +
                    ", algorithm='" + algorithm + '\'' +
                    ", repository=" + repository +
                    '}';
        }

        public RemoteRepository getRepository() {
            return repository;
        }

        public void setRepository(RemoteRepository repository) {
            this.repository = repository;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }
}
