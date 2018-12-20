package de.julielab.utilities.aether.apps;

import de.julielab.utilities.aether.MavenException;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class GetCoordinatesFromEffectivePomTest {

    @BeforeClass
    public static void checkInternetConnection() throws MalformedURLException {
        try (InputStream ignored = new URL("https://oss.sonatype.org/content/repositories/public/").openStream()) {
            // nothing, test can be executed
        } catch (IOException e) {
            // Cannot reach sonatype, assuming no internet connection available, skip this test
            Assume.assumeTrue("There is no internet connection, skipping test", false);
        }
    }
    @Test
    public void testMain() throws FileNotFoundException, MavenException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        GetCoordinatesFromEffectivePom.main(new String[]{"src/test/resources/testpoms/simplepom.xml"});
        String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertThat(output).contains("GROUPID: the.group");
        assertThat(output).contains("ARTIFACTID: the-artifact");
        assertThat(output).contains("VERSION: 1.2.3");
        assertThat(output).contains("PACKAGING: jar");
    }
}
