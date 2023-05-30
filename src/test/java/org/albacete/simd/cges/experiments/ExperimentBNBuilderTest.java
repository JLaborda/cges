package org.albacete.simd.cges.experiments;

import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.bnbuilders.CGES;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.RandomClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.framework.BackwardStage;
import org.albacete.simd.cges.framework.ForwardStage;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;


public class ExperimentBNBuilderTest {
    int nThreads = 2;
    int nItInterleaving = 5;
    int seed = 42;
    int maxIterations = 15;
    Clustering clustering = new RandomClustering();
    BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2");
    ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, "cancer", Resources.CANCER_NET_PATH, Resources.CANCER_BBDD_PATH, seed);


    @Before
    public void restartMeans() {
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    @Test
    public void experimentsConstructorTest(){

        //Asserting
        assertNotNull(exp);
    }

    @Test
    public void runExperimentTest(){
        BNBuilder algorithm = new CGES(Resources.CANCER_BBDD_PATH, clustering, 4, 100000, "c2");
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, "cancer", Resources.CANCER_NET_PATH, Resources.CANCER_BBDD_PATH, seed);
        exp.runExperiment();

        assertNotEquals(0.0, exp.getBdeuScore(), 0.000001);
        assertNotNull(exp.getDifferencesOfMalkovsBlanket());
        assertNotEquals(0L,exp.getElapsedTimeMiliseconds());
        assertNotEquals(0,exp.getNumberOfIterations());
        assertNotEquals(Integer.MAX_VALUE,exp.getStructuralHamiltonDistanceValue());
        assertEquals("CGES", exp.getAlgName());
    }
    
    @Test
    public void saveExperimentTest(){
        String savePath = "./src/test/res/testBN.txt";
        File file = new File(savePath);
        Clustering clustering = new RandomClustering(42);
        BNBuilder algorithm = new CGES(Resources.ALARM_BBDD_PATH, clustering, 4, 100000, "c2");
        ExperimentBNBuilder exp = new ExperimentBNBuilder(algorithm, "alarm", Resources.ALARM_NET_PATH, Resources.ALARM_BBDD_PATH, seed);
        try {
            //Arrange: Creating Experiment and deleting previous file
            Files.deleteIfExists(file.toPath());
            exp.runExperiment();

            //Act: Saving Experiment
            exp.saveExperiment(savePath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Assert: Checking if the file has been saved
        File temp = new File(savePath);
        assertTrue(temp.exists());

        // Deleting again
        try {
            Files.deleteIfExists(temp.toPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }




}
