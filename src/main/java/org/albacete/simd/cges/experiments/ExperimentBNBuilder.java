package org.albacete.simd.cges.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.graph.Dag_n;
import org.albacete.simd.cges.bnbuilders.GES;
import org.albacete.simd.cges.bnbuilders.CGES.Broadcasting;
import org.albacete.simd.cges.bnbuilders.CGES;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.clustering.RandomClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Utils;
import org.apache.commons.lang3.time.StopWatch;
import weka.classifiers.bayes.net.BIFReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.albacete.simd.cges.bnbuilders.FGES;

/*We are checking the following hyperparameters:
 * Threads: [1, 2, 4, 8, 16]
 * Interleaving: [5,10,15]
 *
 * We are going to experiment over */


public class ExperimentBNBuilder {

    protected BNBuilder algorithm;

    protected Map<String,String> paramsMap = new HashMap<>();
    // Definir las claves como constantes de la clase
    public static final String[] KEYS = {
            "algName", "netName", "netPath", "databasePath",
            "clusteringName", "numberOfRealThreads", "convergence", "broadcasting"
    };

    protected int structuralHamiltonDistanceValue = Integer.MAX_VALUE;
    protected double bdeuScore;
    protected double [] differencesOfMalkovsBlanket;
    /**
     * The stop watch that measures the time of the execution of the algorithm
     */
    private static StopWatch stopWatch;
    /**
     * Time elapsed in milliseconds
     */
    protected long elapsedTime;
    protected int numberOfIterations;


    protected String algName;
    public Dag_n resultingBayesianNetwork;


    public ExperimentBNBuilder(String[] parameters) throws Exception {
        //extractParametersForClusterExperiment(parameters);
        extractParameters(parameters);
        createBNBuilder();
    }
    public ExperimentBNBuilder(Map<String,String> paramsMap) throws Exception{
        if(!checkKeys(paramsMap)){
            System.out.println("Keys and paramsMap do not share the same keys or has a different lenght");
            System.exit(1);
        }
        this.paramsMap = paramsMap;
        createBNBuilder();
    }
    public ExperimentBNBuilder(BNBuilder algorithm, String[] parameters){
        extractParameters(parameters);
    }
    public ExperimentBNBuilder(BNBuilder algorithm, Map<String,String> paramsMap){
        if(!checkKeys(paramsMap)){
            System.out.println("Keys and paramsMap do not share the same keys or has a different lenght");
            System.exit(1);
        }
        this.paramsMap = paramsMap;
        this.algorithm = algorithm;
    }

    private boolean checkKeys(Map<String,String> paramsMap){
        if(paramsMap.keySet().size()!= KEYS.length){
            System.out.println("Map and keys don't kave the same size");
            return false;
        }

        for(String key : KEYS){
            if(!paramsMap.keySet().contains(key))
                return false;
        }
        return true;
    }

    private void extractParameters(String[] parameters) {
        // Comprobar el tamaño de los parámetros
        if(parameters.length != KEYS.length){
            System.out.println("The amount of parameters must be: " + KEYS.length);
            System.exit(1);
        }

        // Asignar los valores a variables específicas
        for(int i = 0; i < parameters.length; i++){
            String key = KEYS[i];
            String value = parameters[i];
            this.paramsMap.put(key,value);
        }

        System.out.println("Extracted Parameters:");
        for(String key : paramsMap.keySet()){
            String value = paramsMap.get(key);
            System.out.println(key + ": " + value);
        }
    }

    private void createBNBuilder() throws Exception {
        String clusteringName = paramsMap.get("clusteringName");        
        Clustering clustering;
        if(clusteringName.equals("HierarchicalClustering"))
            clustering = new HierarchicalClustering();
        else
            clustering = new RandomClustering();
        
        algorithm = new CGES(paramsMap.get("databasePath"),
                        clustering,
                        Integer.parseInt(paramsMap.get("numberOfRealThreads")),
                        paramsMap.get("convergence"),
                        Broadcasting.valueOf(paramsMap.get("broadcasting"))
                        );
        
        /*
        switch(algName) {
            case "ges":
                algorithm = new GES(databasePath, true);
                break;
            case "ges-noParallel":
                algorithm = new GES(databasePath, false);
                break;
            case "circular_ges_c1":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, edgeLimitation, "c1", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "cges":
            case "circular_ges_c2":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, edgeLimitation, "c2", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "circular_ges_c3":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, edgeLimitation, "c3", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "circular_ges_c4":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, edgeLimitation, "c4", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "fges":
                algorithm = new FGES(databasePath, true, false);
                break;
            case "fges-faithfulness":
                algorithm = new FGES(databasePath, false, false);
                break;
            case "ges-tetrad":
                algorithm = new FGES(databasePath, true, true);
                break;
            default:
                throw new Exception("Error... Algoritmo incorrecto: " + algName);
        }
        */
    }

    
    public boolean checkExistentFile(String savePath){
        File file = new File(savePath);
        return file.length() != 0;
    }
    

    public static String getDatabaseNameFromPattern(String databasePath){
        // Matching the end of the csv file to get the name of the database
        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(databasePath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }



    public void runExperiment()
    {
        try {
            printExperimentInformation();

            MlBayesIm controlBayesianNetwork = readOriginalBayesianNetwork();

            // Search is executed
            // Starting startWatch
            stopWatch = StopWatch.createStarted();
            this.algorithm.search();
            resultingBayesianNetwork =  this.algorithm.getCurrentDag();
            stopWatch.stop();
            // Metrics
            calcuateMeasurements(controlBayesianNetwork);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printExperimentInformation() {
        System.out.println("Starting Experiment:");
        System.out.println("-----------------------------------------");
        for (String key : paramsMap.keySet()) {
            String parameter = paramsMap.get(key);
            System.out.println(key + ": " + parameter);
        }
    }

    private MlBayesIm readOriginalBayesianNetwork() throws Exception {
        BIFReader bayesianReader = new BIFReader();
        bayesianReader.processFile(this.paramsMap.get("netPath"));
        System.out.println("Numero de variables: " + bayesianReader.getNrOfNodes());

        //Transforming the BayesNet into a BayesPm
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bayesianReader);
        return new MlBayesIm(bayesPm);
    }

    private void calcuateMeasurements(MlBayesIm controlBayesianNetwork) {
        // Getting time
        this.elapsedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        
        // SHD Tetrad
        //GraphUtils.GraphComparison comparison = SearchGraphUtils.getGraphComparison(controlBayesianNetwork.getDag(), algorithm.getCurrentDag());
        //this.structuralHamiltonDistanceValue = comparison.getShd();
        
        // "SDM": 
        this.structuralHamiltonDistanceValue = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        
        this.differencesOfMalkovsBlanket = Utils.avgMarkovBlanketDelta(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        this.numberOfIterations = algorithm.getIterations();
        this.bdeuScore = GESThread.scoreGraph(algorithm.getCurrentDag(), algorithm.getProblem());
    }

    public void printResults() {
        System.out.println(this);
        System.out.println("Resulting DAG:");
        System.out.println(algorithm.getCurrentGraph());
        System.out.println("Total Nodes of Resulting DAG");
        System.out.println(algorithm.getCurrentGraph().getNodes().size());
        System.out.println("-------------------------\nMetrics: ");
        System.out.println("SHD: "+ structuralHamiltonDistanceValue);
        System.out.println("Final BDeu: " +this.bdeuScore);
        System.out.println("Total execution time (s): " + (double) elapsedTime/1000);
        System.out.println("Total number of Iterations: " + this.numberOfIterations);
        System.out.println("differencesOfMalkovsBlanket avg: "+ differencesOfMalkovsBlanket[0]);
        System.out.println("differencesOfMalkovsBlanket plus: "+ differencesOfMalkovsBlanket[1]);
        System.out.println("differencesOfMalkovsBlanket minus: "+ differencesOfMalkovsBlanket[2]);
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Final BN Result:");
        System.out.println(this.resultingBayesianNetwork);
    }

    public void saveExperiment(String savePath) throws IOException{
        File file = new File(savePath);
        BufferedWriter csvWriter = new BufferedWriter(new FileWriter(savePath, true));
        //FileWriter csvWriter = new FileWriter(savePath, true);
        if(file.length() == 0) {
            String header = "algorithm," + this.algorithm.getHyperParamsHeader() + ",network,dataset,cges_threads,edge_limitation,SHD,bdeu,deltaMB,deltaMB+,deltaMB-,iterations,time(s)\n";
            csvWriter.append(header);
        }
        csvWriter.append(this.getResults());

        csvWriter.flush();
        csvWriter.close();
        System.out.println("Results saved at: " + savePath);
    }

    public double[] getDifferencesOfMalkovsBlanket() {
        return differencesOfMalkovsBlanket;
    }

    public double getBdeuScore() {
        return bdeuScore;
    }

    public int getStructuralHamiltonDistanceValue() {
        return structuralHamiltonDistanceValue;
    }

    public long getElapsedTimeMiliseconds() {
        return elapsedTime;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public String getAlgName() {
        return algName;
    }

    public String getResults(){
        return  this.algName + ","
                + this.algorithm.getHyperParamsBody() + ","
                + this.paramsMap.get("netName") + ","
                + getDatabaseNameFromPattern(paramsMap.get("databasePath")) + ","
                + paramsMap.get("numberOfRealThreads") + ","
                + this.algorithm.getItInterleaving() + ","
                + this.structuralHamiltonDistanceValue + ","
                + this.bdeuScore + ","
                + this.differencesOfMalkovsBlanket[0] + ","
                + this.differencesOfMalkovsBlanket[1] + ","
                + this.differencesOfMalkovsBlanket[2] + ","
                + this.numberOfIterations + ","
                + (double) elapsedTime/1000 + "\n";//this.elapsedTime + "\n";
    }

    public BNBuilder getAlgorithm() {
        return algorithm;
    }

    public String getNetPath() {
        return paramsMap.get("netPath");
    }

    public String getDatabasePath() {
        return paramsMap.get("databasePath");
    }

    public String getNetName() {
        return paramsMap.get("netName");
    }

    public String getSaveFileName(){
        /*    public static final String[] KEYS = {
            "algName", "netName", "netPath", "databasePath",
            "clusteringName", "numberOfRealThreads", "convergence", "broadcasting"
    }; */
        int i=0;
        StringBuilder fileNameBuilder = new StringBuilder();
        fileNameBuilder.append("exp_");
        fileNameBuilder.append(paramsMap.get("algName"));
        fileNameBuilder.append("_");
        fileNameBuilder.append(paramsMap.get("netName"));
        fileNameBuilder.append("_");
        fileNameBuilder.append("T");
        fileNameBuilder.append(paramsMap.get("numberOfRealThreads"));
        fileNameBuilder.append("_");
        fileNameBuilder.append(paramsMap.get("convergence"));
        fileNameBuilder.append("_");
        fileNameBuilder.append(paramsMap.get("broadcasting"));
        
        fileNameBuilder.append(".csv");
        return fileNameBuilder.toString();
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("-----------------------\nExperiment " + algName);
        for (String key : paramsMap.keySet()) {
            String parameter = paramsMap.get(key);
            result.append(key + ": " + parameter);
        }
        return  result.toString();
    }
}

