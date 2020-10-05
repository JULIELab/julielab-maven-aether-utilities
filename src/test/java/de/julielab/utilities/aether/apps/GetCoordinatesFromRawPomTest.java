package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.MavenException;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
public class GetCoordinatesFromRawPomTest {
    @Test
    public void testMain() throws FileNotFoundException, MavenException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        GetCoordinatesFromRawPom.main(new String[]{"src/test/resources/testpoms/simplepom.xml"});
        String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertThat(output).contains("GROUPID: the.group");
        assertThat(output).contains("ARTIFACTID: the-artifact");
        assertThat(output).contains("VERSION: 1.2.3");
        assertThat(output).contains("PACKAGING: jar");
    }
}
