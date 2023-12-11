package org.albacete.simd.cges.experiments;

import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.bnbuilders.CGES;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.clustering.RandomClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.*;

public class ExperimentBNBuilderTest {


    @Test
    public void experimentsNormalBehaviorConstructorTest() throws Exception {

        String[] KEYS = {
            "algName", "netName", "netPath", "databasePath",
            "clusteringName", "numberOfRealThreads", "convergence", "broadcasting"
        };
        // cges andes /home/jorlabs/projects/cges/res/networks/andes/andes.xbif /home/jorlabs/projects/cges/res/datasets/andes/andes00.csv HierarchicalClustering 8 c2 NO_BROADCASTING

        Map<String,String> paramsMap = new LinkedMap<>();
        String[] parameters = {"cges", "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH, "HierarchicalClustering", "4", "c2", "NO_BROADCASTING"};
        
        for (int i = 0; i < parameters.length; i++) {
            paramsMap.put(KEYS[i], parameters[i]);
        }

        Clustering clustering = new HierarchicalClustering();
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2", CGES.Broadcasting.NO_BROADCASTING);
        
        ExperimentBNBuilder[] experiments = {
            new ExperimentBNBuilder(parameters),
            new ExperimentBNBuilder(paramsMap),
            new ExperimentBNBuilder(algorithm,parameters),
            new ExperimentBNBuilder(algorithm, paramsMap)
        };
        
        for(ExperimentBNBuilder exp : experiments){
        assertNotNull(exp);
        assertEquals(algorithm.getClass().getSimpleName(), exp.getAlgorithm().getClass().getSimpleName());
        assertEquals("alarm", exp.getNetName());
        assertEquals(Resources.ALARM_NET_PATH, exp.getNetPath());
        assertEquals(Resources.ALARM_BBDD_PATH, exp.getDatabasePath());
        }
    }

    @Test
    public void runExperimentTest() {
        Clustering clustering = new RandomClustering();
        String[] parameters = {"cges", "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH, "RandomClustering", "4", "c2", "NO_BROADCASTING"};
        
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2", CGES.Broadcasting.NO_BROADCASTING);
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, parameters);

        exp.runExperiment();

        assertNotEquals(0.0, exp.getBdeuScore(), 0.000001);
        assertNotNull(exp.getDifferencesOfMalkovsBlanket());
        assertNotEquals(0L, exp.getElapsedTimeMiliseconds());
        assertNotEquals(0, exp.getNumberOfIterations());
        assertNotEquals(Integer.MAX_VALUE, exp.getStructuralHamiltonDistanceValue());
        assertEquals("cges", exp.getAlgName());
    }


    @Test
    public void saveExperimentTest() {
        Clustering clustering = new RandomClustering();
        String[] parameters = {"cges", "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH, "RandomClustering", "4", "c2", "NO_BROADCASTING"};
        
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2", CGES.Broadcasting.NO_BROADCASTING);
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm,parameters);

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
