package de.julielab.utilities.aether;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenProjectUtilities {

    private MavenProjectUtilities() {
    }


    public static List<String> getProjectModules(File pom, boolean recursively) {
        List<String> ret = new ArrayList<>();
        final Model model = getRawPomModel(pom);
        final List<String> modules = model.getModules();
        if (modules != null) {
            String basepathString = pom.getParent() != null ? pom.getParent() + File.separator : "";
            ret.addAll(modules.stream().map(m -> basepathString + m).collect(Collectors.toList()));
        }
        if (recursively) {
            ret.addAll(getRecursiveProjectModules(ret));
        }
        return ret;
    }

    public static List<String> getRecursiveProjectModules(List<String> modules) {
        List<String> ret = new ArrayList<>();
        for (String module : modules) {
            ret.addAll(getProjectModules(new File(module + File.separator + "pom.xml"), true));
        }
        return ret;
    }

    public static Model getRawPomModel(File pom) {
        final DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        final Result<? extends Model> result = modelBuilder.buildRawModel(pom, 0, false);
        final Model model = result.get();
        if (model == null)
            throw new IllegalArgumentException("Could not create a model from file " + pom.getAbsolutePath());
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

    public static Model createModelWithDependencies(File basePom, Stream<MavenArtifact> dependencyArtifacts) throws MavenException {
        try {
            final ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
            modelRequest.setPomFile(basePom);
            modelRequest.setModelResolver(createModelResolver());
            // This is required to avoid errors about not able to determine the Java version
            modelRequest.setSystemProperties(System.getProperties());

            final DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest);
            final Model model = modelBuildingResult.getEffectiveModel();

            dependencyArtifacts.map(d -> {
                Dependency dep = new Dependency();
                dep.setArtifactId(d.getArtifactId());
                dep.setGroupId(d.getGroupId());
                dep.setVersion(d.getVersion());
                dep.setType(d.getPackaging());
                dep.setClassifier(dep.getClassifier());
                return dep;
            }).forEach(model::addDependency);
            return model;
        } catch (SettingsBuildingException | ModelBuildingException e) {
            throw new MavenException(e);
        }
    }

    public static void writeModel(File destination, Model model) throws IOException {
        final DefaultModelWriter modelWriter = new DefaultModelWriter();
        modelWriter.write(destination, null, model);
    }
}
