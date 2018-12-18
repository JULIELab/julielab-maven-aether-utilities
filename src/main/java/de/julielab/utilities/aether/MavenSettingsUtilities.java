package de.julielab.utilities.aether;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenSettingsUtilities {
    private final static Logger log = LoggerFactory.getLogger(MavenSettingsUtilities.class);
    /**
     * Returns the effective settings after resolving global and user settings.
     *
     * @return
     * @throws SettingsBuildingException
     */
    public static Settings getMavenSettings() throws SettingsBuildingException {
        String userHome = System.getProperty("user.home");
        File userMavenConfigurationHome = new File(userHome, ".m2");
        String envM2Home = System.getenv("M2_HOME");
        File DEFAULT_USER_SETTINGS_FILE = new File(userMavenConfigurationHome, "settings.xml");
        File DEFAULT_GLOBAL_SETTINGS_FILE =
                new File(System.getProperty("maven.home", envM2Home != null ? envM2Home : ""), "conf/settings.xml");


        SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest();
        settingsBuildingRequest.setSystemProperties(System.getProperties());
        settingsBuildingRequest.setUserSettingsFile(DEFAULT_USER_SETTINGS_FILE);
        settingsBuildingRequest.setGlobalSettingsFile(DEFAULT_GLOBAL_SETTINGS_FILE);

        SettingsBuildingResult settingsBuildingResult;
        DefaultSettingsBuilderFactory mvnSettingBuilderFactory = new DefaultSettingsBuilderFactory();
        DefaultSettingsBuilder settingsBuilder = mvnSettingBuilderFactory.newInstance();
        settingsBuildingResult = settingsBuilder.build(settingsBuildingRequest);

        return settingsBuildingResult.getEffectiveSettings();
    }

    public static Map<String, Authentication> getRepositoryAuthenticationsFromMavenSettings() throws SettingsBuildingException {
        Map<String, Authentication> authenticationMap = new HashMap<>();
        List<Server> servers = getMavenSettings().getServers();
        for (Server server : servers) {
            if (server.getUsername() != null && server.getPassword() != null) {
                Authentication auth = new AuthenticationBuilder().addUsername(server.getUsername()).addPassword(server.getPassword()).build();
                authenticationMap.put(server.getId(), auth);
            }
            if (server.getPassphrase() != null && server.getPrivateKey() != null) {
                Authentication auth = new AuthenticationBuilder().addPrivateKey(server.getPrivateKey(), server.getPassphrase()).build();
                authenticationMap.put(server.getId(), auth);
            }
        }
        return authenticationMap;
    }

    public static List<RemoteRepository> getRemoteRepositoriesFromSettings() throws SettingsBuildingException {
        Settings effectiveSettings = getMavenSettings();

        Map<String, Authentication> authenticationMap = getRepositoryAuthenticationsFromMavenSettings();
        Map<String, Profile> profilesMap = effectiveSettings.getProfilesAsMap();
        List<RemoteRepository> remotes = new ArrayList<>(20);
        for (String profileName : effectiveSettings.getActiveProfiles()) {
            Profile profile = profilesMap.get(profileName);
            if (profile != null) {
                List<Repository> repositories = profile.getRepositories();
                for (Repository repo : repositories) {
                    Authentication auth = authenticationMap.get(repo.getId());
                    RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl());
                    if (auth != null)
                        repoBuilder.setAuthentication(auth);
                    repoBuilder.setSnapshotPolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN));
                    RemoteRepository remoteRepo
                            = repoBuilder.build();
                    remotes.add(remoteRepo);
                }
            }
        }
        if (log.isTraceEnabled()) {
            remotes.forEach(r -> log.trace("Getting repository from Maven settings: {}", r));
        }
        return remotes;
    }

    private MavenSettingsUtilities() {
    }
}
