package org.albacete.simd.cges.experiments;

import edu.cmu.tetrad.data.DataSet;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.utils.Utils;

import java.io.IOException;
import org.albacete.simd.cges.bnbuilders.CGES;

public class SimpleBNExperiment {


    public static void main(String[] args){
        // 1. Configuration
        String networkFolder = "./res/networks/";
        String net_name = "andes";
        String net_path = networkFolder + net_name + ".xbif";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50003_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        String test_path = networkFolder + "BBDD/tests/" + net_name + "_test.csv";

        // 2. Algorithm
        //BNBuilder algorithm = new GES_BNBuilder(bbdd_path);
        Clustering clustering = new HierarchicalClustering();
        //Clustering clustering = new RandomClustering();

        //BNBuilder algorithm = new PGESwithStages(ds, clustering, 4, 30, 10000, false, true, true);
        //BNBuilder algorithm = new GES_BNBuilder(ds, true);
        BNBuilder algorithm = new CGES(ds, clustering, 4, 100000, "c4");
        //BNBuilder algorithm = new Fges_BNBuilder(ds);
        //BNBuilder algorithm = new Empty(ds);
        
        // Experiment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(algorithm, net_name, net_path, bbdd_path, test_path);//new ExperimentBNBuilder(algorithm, net_path, bbdd_path, test_path, 42);
        
        System.out.println("Alg Name: " + experiment.getAlgName());
        experiment.runExperiment();
        experiment.printResults();
        String savePath = "results/prueba.txt";
        
        /*BDeuScore bdeu = new BDeuScore(ds);
        Fges fges = new Fges(bdeu);
        System.out.println("Score FGES: " + fges.scoreDag(experiment.resultingBayesianNetwork));*/
        
        try {
            experiment.saveExperiment(savePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
