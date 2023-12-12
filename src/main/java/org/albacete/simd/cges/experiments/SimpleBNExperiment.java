package org.albacete.simd.cges.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.cges.bnbuilders.CircularProcess;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.albacete.simd.cges.bnbuilders.CGES;

public class SimpleBNExperiment {


    public static void main(String[] args){
        // 1. Configuration
        String net_name = "andes";
        String networkFolder = "./res/networks/" + net_name + "/";
        String datasetFolder = "./res/datasets/" + net_name + "/";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = datasetFolder  + net_name + "00.csv";
        DataSet ds = Utils.readData(bbdd_path);
        Map<String,String> paramsMap = new HashMap<>();
        paramsMap.put(ExperimentBNBuilder.KEYS[0], "cges");
        paramsMap.put(ExperimentBNBuilder.KEYS[1], net_name);
        paramsMap.put(ExperimentBNBuilder.KEYS[2], net_path);
        paramsMap.put(ExperimentBNBuilder.KEYS[3], bbdd_path);
        paramsMap.put(ExperimentBNBuilder.KEYS[4], "HierarchicalClustering");
        paramsMap.put(ExperimentBNBuilder.KEYS[5], "4");
        paramsMap.put(ExperimentBNBuilder.KEYS[6], "c2");
        paramsMap.put(ExperimentBNBuilder.KEYS[7], "BEST_BROADCASTING");
        

        // 2. Setting Algorithm
        Clustering clustering = new HierarchicalClustering();
        CGES algorithm = new CGES(ds, clustering, 4, 100000, "c2", CGES.Broadcasting.PAIR_BROADCASTING);

        //2. Create experiment environment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, paramsMap);

        // 4. Launch Experiment
        System.out.println("Alg Name: " + experiment.getAlgName());
        experiment.runExperiment();
        experiment.printResults();
        String savePath = "results/pruebas/" + experiment.getSaveFileName();//String savePath = "results/prueba.txt";

        // 5. Save Experiment
        experiment.printResults();
        System.out.println("Number of times broadcasting fusion is used: " + CircularProcess.fusionWinCounter);
        
        System.out.println("Saving at: " + savePath);
        experiment.saveExperiment(savePath);

    }
}
