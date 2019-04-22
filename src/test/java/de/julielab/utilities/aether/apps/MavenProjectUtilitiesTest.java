package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.MavenArtifact;
import de.julielab.utilities.aether.MavenException;
import de.julielab.utilities.aether.MavenProjectUtilities;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertTrue;
public class MavenProjectUtilitiesTest {
    @Test
    public void testWriteModel() throws Exception {
        final Model model = MavenProjectUtilities.getRawPomModel(Path.of("src", "test", "resources", "testpoms", "simplepom.xml").toFile());
        final File output = Path.of("src", "test", "resources").toFile();
        if (!output.exists())
            output.mkdir();
        final File outputPom = Path.of(output.getAbsolutePath(), "output", "writtenpom.xml").toFile();
        if (outputPom.exists())
            assertThatCode(() -> outputPom.delete()).doesNotThrowAnyException();
        MavenProjectUtilities.writeModel(outputPom, model);
        assertTrue(outputPom.exists());
    }

    @Test
    public void testWriteModelWithAddedDependency() throws Exception, MavenException {
        final File output = Path.of("src", "test", "resources").toFile();
        if (!output.exists())
            output.mkdir();
        final File outputPom = Path.of(output.getAbsolutePath(), "output", "writtenpomwithextradependency.xml").toFile();
        if (outputPom.exists())
            assertThatCode(() -> outputPom.delete()).doesNotThrowAnyException();
        final MavenArtifact a = new MavenArtifact();
        a.setArtifactId("commons-io");
        a.setGroupId("commons-io");
        a.setVersion("2.6");
        final Model model = MavenProjectUtilities.addDependenciesToModel(Path.of("src", "test", "resources", "testpoms", "simple_pom_without_parent.xml").toFile(), Stream.of(a));
        MavenProjectUtilities.writeModel(outputPom, model);
        assertTrue(outputPom.exists());
        final Model loadedModel = MavenProjectUtilities.getRawPomModel(outputPom);
        assertThat(loadedModel.getDependencies()).hasSize(1);
        final Dependency dep = loadedModel.getDependencies().get(0);
        assertThat(dep.getArtifactId()).isEqualTo("commons-io");
        assertThat(dep.getGroupId()).isEqualTo("commons-io");
        assertThat(dep.getVersion()).isEqualTo("2.6");
    }
}
