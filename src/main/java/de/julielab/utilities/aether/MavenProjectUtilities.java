package de.julielab.utilities.aether;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.normalization.DefaultModelNormalizer;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MavenProjectUtilities {

    private MavenProjectUtilities() {
    }


    public static Model getRawPomModel(File pom) throws MavenException {
        final DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        final Result<? extends Model> result = modelBuilder.buildRawModel(pom, 0, false);
        final Model model = result.get();
        return model;
    }


    public static Model getEffectivePomModel(File pom) throws MavenException {
        try {
            final ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
            modelRequest.setPomFile(pom);
            modelRequest.setModelResolver(createModelResolver());
            // This is required to avoid errors about not able to determine the Java version
            modelRequest.setSystemProperties(System.getProperties());

            final DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest);
            return modelBuildingResult.getEffectiveModel();
        } catch (SettingsBuildingException | ModelBuildingException e) {
            throw new MavenException(e);
        }
    }

    public static ModelResolver createModelResolver() throws SettingsBuildingException, MavenException {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        final RepositorySystemSession session = MavenRepositoryUtilities.newSession(MavenRepositoryUtilities.newRepositorySystem(locator), MavenConstants.LOCAL_REPO);
        ModelResolver modelResolver;
        try {
            Constructor<?> constr = Class.forName("org.apache.maven.repository.internal.DefaultModelResolver").getConstructors()[0];
            constr.setAccessible(true);
            modelResolver = (ModelResolver) constr.newInstance(session, null, null,
                    locator.getService(ArtifactResolver.class),
                    locator.getService(VersionRangeResolver.class),
                    locator.getService(RemoteRepositoryManager.class), MavenRepositoryUtilities.getEffectiveRepositories(session));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MavenException(e);
        }
        return modelResolver;
    }
}
