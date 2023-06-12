package org.albacete.simd.cges.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.cges.bnbuilders.CircularDag;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.utils.Utils;

import java.io.IOException;
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

        // 2. Setting Algorithm
        Clustering clustering = new HierarchicalClustering();
        CGES algorithm = new CGES(ds, clustering, 4, 100000, "c2");
        algorithm.setBroadcasting(false);

        //3. Create experiment environment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, net_name, net_path, bbdd_path);

        // 4. Launch Experiment
        System.out.println("Alg Name: " + experiment.getAlgName());
        experiment.runExperiment();
        experiment.printResults();
        String savePath = "results/prueba.txt";

        // 5. Save Experiment
        try {
            experiment.printResults();
            System.out.println("Number of times broadcasting fusion is used: " + CircularDag.fusionWinCounter);
            experiment.saveExperiment(savePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
