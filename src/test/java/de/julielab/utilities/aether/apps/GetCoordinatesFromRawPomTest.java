package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.MavenException;
import de.julielab.utilities.aether.apps.GetCoordinatesFromRawPom;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
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
