package org.albacete.simd.cges.experiments;

import org.albacete.simd.cges.utils.Utils;

public class SimpleBNExperiment {

    public static void main(String[] args) throws Exception{
        /*String net_name = "andes";
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
        */
        
        // 1. Configuration
        System.out.println("Starting experiment...");
        String algParmString = "algName cges";
        String netNameParamString = "netName andes";
        String clusteringNameString = "clusteringName HierarchicalClustering";
        String numberOfClustersString = "numberOfClusters 8";
        String convergenceString = "convergence c2";
        String broadcastingString = "broadcasting BEST_BROADCASTING";
        //String randomParamString = "seed 103";
        String databasePathString = "databasePath /home/jorlabs/projects/cges/res/datasets/andes/andes8.csv";
        String netPathString = "netPath /home/jorlabs/projects/cges/res/networks/andes/andes.xbif";

        //String paramString = "algName cges netName andes clusteringName HierarchicalClustering numberOfClusters 8 convergence c2 broadcasting PAIR_BROADCASTING seed 103 databasePath /home/jorlabs/projects/cges/res/datasets/andes/andes08.csv netPath /home/jorlabs/projects/cges/res/networks/andes/andes.xbif";
        String paramString = algParmString + " " + netNameParamString + " " + clusteringNameString + " " + numberOfClustersString + " " + convergenceString + " " + broadcastingString  + " " + databasePathString + " " + netPathString; //+ " " + randomParamString;
        String[] parameters = paramString.split(" ");

        //2. Create experiment environment
        ExperimentBNBuilder experiment = new ExperimentBNBuilder(parameters);

        // 3. Launch Experiment
        System.out.println("Setting verbose");
        Utils.setVerbose(true);
        System.out.println("Running experiment...");
        experiment.runExperiment();
        experiment.printResults();
        String savePath = "results/pruebas/" + ExperimentBNBuilder.getSaveFileName(0);//String savePath = "results/prueba.txt";

        // 4. Save Experiment
        //System.out.println("Number of times broadcasting fusion is used: " + CircularProcess.fusionWinCounter);
        System.out.println("Saving at: " + savePath);
        ExperimentBNBuilder.saveExperiment(savePath);

    }
}
