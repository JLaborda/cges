package org.albacete.simd.cges.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag_n;
import org.albacete.simd.cges.bnbuilders.GES;
import org.albacete.simd.cges.bnbuilders.CGES;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Utils;
import org.apache.commons.lang3.time.StopWatch;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    protected String netPath;
    protected String databasePath;
    protected String netName;
    protected String databaseName;

    protected int numberOfThreads;
    protected int numberOfRealThreads;
    protected int interleaving;
    protected int maxIterations;
    //protected static HashMap<String, HashMap<String,String>> map;

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


    protected String log = "";
    protected String algName;
    protected long seed = -1;
    private MlBayesIm controlBayesianNetwork;
    public Dag_n resultingBayesianNetwork;


    public ExperimentBNBuilder(String[] parameters, int threads) throws Exception {
        extractParametersForClusterExperiment(parameters);
        this.numberOfThreads = threads;
        createBNBuilder();
    }

    public ExperimentBNBuilder(BNBuilder algorithm, String netName, String netPath, String bbddPath) {
        this.algorithm = algorithm;
        this.netName = netName;
        this.netPath = netPath;
        this.databasePath = bbddPath;
        this.algName = algorithm.getClass().getSimpleName();

        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(this.databasePath);
        if (matcher.find()) {
            databaseName = matcher.group(1);
        }
        this.numberOfThreads = Runtime.getRuntime().availableProcessors();
        this.numberOfRealThreads = algorithm.getnThreads();
        this.maxIterations = algorithm.getMaxIterations();
        this.interleaving = algorithm.getItInterleaving();
    }

    public ExperimentBNBuilder(BNBuilder algorithm, String netName, String netPath, String bbddPath, long partition_seed) {
        this(algorithm, netName, netPath, bbddPath);
        this.seed = partition_seed;
        Utils.setSeed(partition_seed);
    }


    private void extractParametersForClusterExperiment(String[] parameters){
        System.out.println("Extracting parameters...");
        System.out.println("Number of hyperparams: " + parameters.length);
        int i=0;
        for (String string : parameters) {
            System.out.println("Param[" + i + "]: " + string);
            i++;
        }
        algName = parameters[0];
        netName = parameters[1];
        netPath = parameters[2];
        databasePath = parameters[3];
        databaseName = getDatabaseNameFromPattern();

        numberOfRealThreads = Integer.parseInt(parameters[5]);
        interleaving = Integer.parseInt(parameters[6]);
        seed = Integer.parseInt(parameters[7]);
    }
    
    public boolean checkExistentFile(String savePath) throws IOException{
        File file = new File(savePath);

        return file.length() != 0;
    }
    

    private String getDatabaseNameFromPattern(){
        // Matching the end of the csv file to get the name of the database
        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(this.databasePath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void createBNBuilder() throws Exception {
        Clustering clustering;

        switch(algName) {
            case "ges":
                algorithm = new GES(databasePath, true);
                break;
            case "ges-noParallel":
                algorithm = new GES(databasePath, false);
                break;
            case "circular_ges_c1":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, interleaving, "c1");
                break;
            case "circular_ges_c2":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, interleaving, "c2");
                break;
            case "circular_ges_c3":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, interleaving, "c3");
                break;
            case "circular_ges_c4":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfRealThreads, interleaving, "c4");
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
    }



    public void runExperiment()
    {
        try {
            printExperimentInformation();

            controlBayesianNetwork = readOriginalBayesianNetwork();

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
        System.out.println("\tNet Name: " + netName);
        System.out.println("\tBBDD Name: " + databaseName);
        //System.out.println("\tFusion Consensus: " + fusion_consensus);
        System.out.println("\tnThreads: " + numberOfThreads);
        System.out.println("\tnPGESThreads: " + numberOfRealThreads);
        System.out.println("\tnItInterleaving: " + interleaving);
        System.out.println("-----------------------------------------");

        System.out.println("Net_path: " + netPath);
        System.out.println("BBDD_path: " + databasePath);
    }

    private MlBayesIm readOriginalBayesianNetwork() throws Exception {
        BIFReader bayesianReader = new BIFReader();
        bayesianReader.processFile(this.netPath);
        BayesNet bayesianNet = bayesianReader;
        System.out.println("Numero de variables: " + bayesianNet.getNrOfNodes());

        //Transforming the BayesNet into a BayesPm
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bayesianNet);
        MlBayesIm bn2 = new MlBayesIm(bayesPm);

        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        return bn2;
    }

    private void calcuateMeasurements(MlBayesIm controlBayesianNetwork) {
        // Getting time
        this.elapsedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        
        // SHD Tetrad
        //GraphUtils.GraphComparison comparison = SearchGraphUtils.getGraphComparison(controlBayesianNetwork.getDag(), algorithm.getCurrentDag());
        //this.structuralHamiltonDistanceValue = comparison.getShd();
        
        // "SDM": 
        this.structuralHamiltonDistanceValue = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        
        this.differencesOfMalkovsBlanket = Utils.avgMarkovBlanquetdif(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
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
    }

    public void saveExperiment(String savePath) throws IOException{
        File file = new File(savePath);
        BufferedWriter csvWriter = new BufferedWriter(new FileWriter(savePath, true));
        //FileWriter csvWriter = new FileWriter(savePath, true);
        if(file.length() == 0) {
            String header = "algorithm,network,bbdd,threads,pges_threads,interleaving,seed,SHD,loglike,bdeu,deltaMB,deltaMB+,deltaMB-,iterations,time(s)\n";
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

    public int getInterleaving() {
        return interleaving;
    }

    public String getAlgName() {
        return algName;
    }

    public String getResults(){
        return  this.algName + ","
                + this.netName + ","
                + this.databaseName + ","
                + this.numberOfThreads + ","
                + this.numberOfRealThreads + ","
                + this.interleaving + ","
                + this.seed + ","
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
        return netPath;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public String getNetName() {
        return netName;
    }

    public long getSeed() {
        return seed;
    }

    @Override
    public String toString() {
        return "-----------------------\nExperiment " + algName + "\n-----------------------\nNet Name: " + netName + "\tDatabase: " + databaseName + "\tThreads: " + numberOfThreads + "\tInterleaving: " + interleaving + "\tMax. Iterations: " + maxIterations;
    }
}

