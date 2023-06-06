package org.albacete.simd.cges.experiments;

import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.bnbuilders.CGES;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.RandomClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ExperimentBNBuilderTest {


    @Test
    public void experimentsConstructorTest() {

        Clustering clustering = new RandomClustering();
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2");
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH);

        assertNotNull(exp);
        assertEquals(algorithm, exp.getAlgorithm());
        assertEquals("alarm", exp.getNetName());
        assertEquals(Resources.ALARM_NET_PATH, exp.getNetPath());
        assertEquals(Resources.ALARM_BBDD_PATH, exp.getDatabasePath());
    }

    @Test
    public void runExperimentTest() {
        Clustering clustering = new RandomClustering();
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2");
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH);

        exp.runExperiment();

        assertNotEquals(0.0, exp.getBdeuScore(), 0.000001);
        assertNotNull(exp.getDifferencesOfMalkovsBlanket());
        assertNotEquals(0L, exp.getElapsedTimeMiliseconds());
        assertNotEquals(0, exp.getNumberOfIterations());
        assertNotEquals(Integer.MAX_VALUE, exp.getStructuralHamiltonDistanceValue());
        assertEquals("CGES", exp.getAlgName());
    }


    @Test
    public void saveExperimentTest() {
        Clustering clustering = new RandomClustering();
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2");
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH);


        String savePath = "./src/test/res/testBN.txt";
        File file = new File(savePath);

        try {
            // Arrange: Creating Experiment and deleting previous file
            Files.deleteIfExists(file.toPath());

            // Act: Running and saving the experiment
            exp.runExperiment();
            exp.saveExperiment(savePath);

            // Assert: Checking if the file has been saved
            File savedFile = new File(savePath);
            assertTrue(savedFile.exists());
        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        } finally {
            // Cleanup: Deleting the saved file
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                fail("IOException occurred while deleting the saved file: " + e.getMessage());
            }
        }
    }
}
