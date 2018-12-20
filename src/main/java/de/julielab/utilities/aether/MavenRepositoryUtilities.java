package de.julielab.utilities.aether;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class MavenRepositoryUtilities {

    public static final RemoteRepository CENTRAL = new RemoteRepository.Builder("CENTRAL", "default", "https://oss.sonatype.org/content/repositories/public/").setSnapshotPolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN)).build();
    public static final RemoteRepository LOCAL = new RemoteRepository.Builder("LOCAL", "default", MavenConstants.LOCAL_REPO.toURI().toString()).build();


    private MavenRepositoryUtilities() {
    }

    public static RepositorySystem newRepositorySystem() {
        return newRepositorySystem(null);
    }

    public static RepositorySystem newRepositorySystem(DefaultServiceLocator serviceLocator) {
        DefaultServiceLocator locator = serviceLocator != null ? serviceLocator : MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);

    }

    public static RepositorySystemSession newSession(RepositorySystem system, File localRepositoryPath) throws SettingsBuildingException {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        // The following line switches off the creation of the non-timestamp snapshot artifact file
        session.setConfigProperty("aether.artifactResolver.snapshotNormalization", false);
        LocalRepository localRepo = new LocalRepository(localRepositoryPath.getAbsolutePath());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        Settings mavenSettings = MavenSettingsUtilities.getMavenSettings();
        List<Mirror> mirrors = mavenSettings.getMirrors();
        for (Mirror mirror : mirrors) {
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), null, false, mirror.getMirrorOf(), "");
        }
        session.setMirrorSelector(mirrorSelector);
        return session;
    }

    public static List<RemoteRepository> getEffectiveRepositories(RepositorySystemSession session) throws SettingsBuildingException {
        Map<String, Authentication> authenticationMap = MavenSettingsUtilities.getRepositoryAuthenticationsFromMavenSettings();
        DefaultRemoteRepositoryManager remoteRepositoryManager = new DefaultRemoteRepositoryManager();
        List<RemoteRepository> repositories = remoteRepositoryManager.aggregateRepositories(session, Arrays.asList(CENTRAL), MavenSettingsUtilities.getRemoteRepositoriesFromSettings(), true);
        repositories = repositories.stream().map(repo -> {
            if (authenticationMap.containsKey(repo.getId())) {
                return new RemoteRepository.Builder(repo).setAuthentication(authenticationMap.get(repo.getId())).build();
            }
            return repo;
        }).collect(toList());
        return repositories;
    }
}
