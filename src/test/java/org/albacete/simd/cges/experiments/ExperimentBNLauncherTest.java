package org.albacete.simd.cges.experiments;

import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.bnbuilders.CGES;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.clustering.RandomClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.Test;

import weka.experiment.Experiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.*;

public class ExperimentBNLauncherTest {

    public final static String testSaveFolder = "src/test/res/test_results";
    public final static String paramsFile = "src/test/res/params/params.txt";
    
    @Test
    public void testExperimentBNLauncher() throws Exception {
        ExperimentBNLauncher experimentBNLauncher = new ExperimentBNLauncher(0, paramsFile,testSaveFolder);
        
        ExperimentBNLauncher.main(new String[]{paramsFile, "0", testSaveFolder});
        
        assertNotNull(experimentBNLauncher);
        ExperimentBNBuilder exp = experimentBNLauncher.getExperiment();
    }

    @Test
    public void testRandomBroadcastingExperiment() throws Exception {
        ExperimentBNLauncher experimentBNLauncher = new ExperimentBNLauncher(0, paramsFile,testSaveFolder);
        ExperimentBNBuilder exp = experimentBNLauncher.getExperiment();

        Map<String, String> params = exp.getParamsMap();
        // Check params: algName cges netName alarm clusteringName HierarchicalClustering numberOfClusters 16 broadcasting RANDOM_BROADCASTING seed 13 databasePath src/test/res/datasets/alarm/alarm.xbif_.csv netPath src/test/res/networks/alarm/alarm.xbif
        assertEquals(params.get("algName"), "cges");
        assertEquals(params.get("netName"), "alarm");
        assertEquals(params.get("clusteringName"), "HierarchicalClustering");
        assertEquals(params.get("numberOfClusters"), "16");
        assertEquals(params.get("broadcasting"), "RANDOM_BROADCASTING");
        assertEquals(params.get("seed"), "13");
        assertEquals(params.get("databasePath"), "src/test/res/datasets/alarm/alarm.xbif_.csv");
        assertEquals(params.get("netPath"), "src/test/res/networks/alarm/alarm.xbif");

        // Check actual seed
        

    }


}
