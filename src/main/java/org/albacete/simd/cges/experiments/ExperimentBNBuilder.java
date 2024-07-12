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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
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

    protected static Map<String,String> paramsMap = new LinkedHashMap<>();
    protected static Map<String,Double> measurementsMap = new LinkedHashMap<>();

    // Definir las claves como constantes de la clase
    public static final String[] KEYS = {
            "algName", "netName", "netPath", "databasePath",
            "clusteringName", "numberOfClusters", "convergence", "broadcasting", "seed"
    };
    

    protected static int structuralHamiltonDistanceValue = Integer.MAX_VALUE;
    protected static double bdeuScore;
    protected static double [] differencesOfMalkovsBlanket;
    /**
     * The stop watch that measures the time of the execution of the algorithm
     */
    public static StopWatch stopWatch;
    /**
     * Time elapsed in milliseconds
     */
    protected static long elapsedTime;
    protected static int numberOfIterations;

    public static Dag_n resultingBayesianNetwork;
    
    public static String saveFolder;
    public static int index = -1;


    public ExperimentBNBuilder(String[] parameters) throws Exception {
        //extractParametersForClusterExperiment(parameters);
        extractParameters(parameters);
        createBNBuilder();
    }
    public ExperimentBNBuilder(Map<String,String> paramsMap) throws Exception{
        ExperimentBNBuilder.paramsMap = paramsMap;
        createBNBuilder();
    }
    public ExperimentBNBuilder(BNBuilder algorithm, String[] parameters){
        extractParameters(parameters);
        this.algorithm = algorithm;
    }
    public ExperimentBNBuilder(BNBuilder algorithm, Map<String,String> paramsMap){
        this.algorithm = algorithm;
        ExperimentBNBuilder.paramsMap = paramsMap;
    }


    private void extractParameters(String[] parameters) {
        // Verificar que la cantidad de parámetros sea par
        if(parameters.length % 2 != 0){
            Utils.println("The amount of parameters must be even");
            System.exit(1);
        }

        // Asignar a paramsMap clave:valor procedente de parameters
        for (int i = 0; i < parameters.length; i+=2) {
            String key = parameters[i];
            String value = parameters[i+1];
            paramsMap.put(key, value);
        }

    }

    private void createBNBuilder() throws Exception {

        String algName = paramsMap.get("algName");
        switch (algName) {
            case "cges":
                String clusteringName = paramsMap.get("clusteringName");        
                Clustering clustering;
                if(clusteringName.equals("HierarchicalClustering"))
                    clustering = new HierarchicalClustering();
                else
                    clustering = new RandomClustering();
                
                algorithm = new CGES(paramsMap.get("databasePath"),
                                clustering,
                                Integer.parseInt(paramsMap.get("numberOfClusters")),
                                Broadcasting.valueOf(paramsMap.get("broadcasting"))
                                );
                //Setting seed
                if(paramsMap.containsKey("seed")){
                    algorithm.setSeed(Long.parseLong(paramsMap.get("seed")));
                }
                break;
        
            case "ges":
                algorithm = new GES(paramsMap.get("databasePath"), true);
                break;
            case "fges":
                algorithm = new FGES(paramsMap.get("databasePath"), true, false);
                break;
            case "fges-faithfulness":
                algorithm = new FGES(paramsMap.get("databasePath"), false, false);
                break;

            default:
                break;
        }


        
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
                algorithm = new CGES(databasePath, clustering, numberOfClusters, edgeLimitation, "c1", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "cges":
            case "circular_ges_c2":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfClusters, edgeLimitation, "c2", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "circular_ges_c3":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfClusters, edgeLimitation, "c3", CGES.Broadcasting.NO_BROADCASTING);
                break;
            case "circular_ges_c4":
                clustering = new HierarchicalClustering();
                algorithm = new CGES(databasePath, clustering, numberOfClusters, edgeLimitation, "c4", CGES.Broadcasting.NO_BROADCASTING);
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
            // Printing Experiment parameters
            Utils.println(this.toString());

            // Search is executed
            // Starting startWatch
            stopWatch = StopWatch.createStarted();
            this.algorithm.search();
            resultingBayesianNetwork =  this.algorithm.getCurrentDag();
            stopWatch.stop();
            // Metrics
            calcuateMeasurements(this.algorithm);

        } catch (Exception e) {
            System.out.println("Error when running the experiment");
            System.out.println("Experiment: " + this.toString());
            e.printStackTrace();
            System.exit(-1);
        }

    }


    private static MlBayesIm readOriginalBayesianNetwork() throws Exception {
        BIFReader bayesianReader = new BIFReader();
        bayesianReader.processFile(paramsMap.get("netPath"));
        Utils.println("Numero de variables: " + bayesianReader.getNrOfNodes());

        //Transforming the BayesNet into a BayesPm
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bayesianReader);
        return new MlBayesIm(bayesPm);
    }

    public static void calcuateMeasurements(BNBuilder algorithm) throws Exception {
        MlBayesIm controlBayesianNetwork = readOriginalBayesianNetwork();
        resultingBayesianNetwork = algorithm.getCurrentDag();

        // Getting time
        elapsedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        //stopWatch.resume();
        // SHD Tetrad
        //GraphUtils.GraphComparison comparison = SearchGraphUtils.getGraphComparison(controlBayesianNetwork.getDag(), algorithm.getCurrentDag());
        //this.structuralHamiltonDistanceValue = comparison.getShd();
        
        // DAG scores
        if(resultingBayesianNetwork != null){
            structuralHamiltonDistanceValue = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), resultingBayesianNetwork);
            differencesOfMalkovsBlanket = Utils.avgMarkovBlanketDelta(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), resultingBayesianNetwork);
            bdeuScore = GESThread.scoreGraph(resultingBayesianNetwork, algorithm.getProblem());
        }
        else{
            structuralHamiltonDistanceValue = Integer.MAX_VALUE;
            differencesOfMalkovsBlanket = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            bdeuScore = Double.NEGATIVE_INFINITY;
        }
        // Iterations
        numberOfIterations = algorithm.getIterations();

        measurementsMap.put("elapsedTime(s)", (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000);
        measurementsMap.put("shd", (double)Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()),resultingBayesianNetwork));
        measurementsMap.put("dfMM_avg", differencesOfMalkovsBlanket[0]);
        measurementsMap.put("dfMM_plus", differencesOfMalkovsBlanket[1]);
        measurementsMap.put("dfMM_minus", differencesOfMalkovsBlanket[1]);
        measurementsMap.put("iterations", (double) algorithm.getIterations());
        measurementsMap.put("bdeu", GESThread.scoreGraph(algorithm.getCurrentDag(), algorithm.getProblem()));
        measurementsMap.put("ncpus", (double) Runtime.getRuntime().availableProcessors());

        if(algorithm instanceof CGES){
            measurementsMap.put("cgesScore", ((CGES) algorithm).getCgesScore());
            measurementsMap.put("fineTuningTime(s)", (double)((CGES) algorithm).getTimeFineTuning()/1000);
        }

    }

    public void calcuateMeasurements()throws Exception{
        calcuateMeasurements(this.algorithm);
    }

    public static void pauseStopWatch() {
        if(stopWatch == null)
            return;
        if (stopWatch.isStarted() && !stopWatch.isSuspended()) {
            stopWatch.suspend();
            System.out.println("StopWatch paused.");
        }
    }

    public static void resumeStopWatch() {
        if(stopWatch == null)
            return;
        if (stopWatch.isStarted() && stopWatch.isSuspended()) {
            stopWatch.resume();
            System.out.println("StopWatch resumed.");
        }
    }

    public void printResults() {
        System.out.println("\nPrinting Results: ");
        System.out.println(this.toString());
        System.out.println("-------------------------\nMetrics: ");
        System.out.println("SHD: "+ structuralHamiltonDistanceValue);
        System.out.println("Final BDeu: " + bdeuScore);
        System.out.println("Total execution time (s): " + (double) elapsedTime/1000);
        System.out.println("Total number of Iterations: " + numberOfIterations);
        System.out.println("differencesOfMalkovsBlanket avg: "+ differencesOfMalkovsBlanket[0]);
        System.out.println("differencesOfMalkovsBlanket plus: "+ differencesOfMalkovsBlanket[1]);
        System.out.println("differencesOfMalkovsBlanket minus: "+ differencesOfMalkovsBlanket[2]);
        System.out.println("-----------------------------------------------------------------------");
        //System.out.println("Final BN Result:");
        //System.out.println(this.resultingBayesianNetwork.toString());

    }

    public static void saveExperiment(String savePath) {
        // Verificar si el archivo ya existe
        boolean fileExists = new File(savePath).exists();

        try (FileWriter csvWriter = new FileWriter(savePath, true)) {
            // Si el archivo no existe, escribir el encabezado
            if (!fileExists) {
                // Obtener las claves del mapa (parámetros) y agregar las medidas
                String header = getHeaderForParameters() + "," + getHeaderForMeasurements() + "\n" ;
                csvWriter.append(header);
            }

            // Obtener los valores del mapa y agregar las medidas
            String bodyLine = getBodyParams() + "," + getBodyMeasurements() + "\n";

            csvWriter.append(bodyLine);
        } catch (IOException e) {
            System.out.println("Error when saving the experiment");
            System.out.println("File: " + savePath);
            e.printStackTrace();
            System.exit(-1);
            // Manejar la excepción según tus necesidades
        }
    }

    public static void saveExperiment(){
        ExperimentBNBuilder.saveExperiment(saveFolder + ExperimentBNBuilder.getSaveFileName(index));
    }


    private static String getHeaderForParameters(){
        StringBuilder headerBuilder = new StringBuilder();

        for (String key : paramsMap.keySet()) {
        
            // Verificar si el valor contiene el substring "path"
            if (!key.contains("Path")) {
                headerBuilder.append(key).append(",");
            }
        }
        //Añadir el nombre de la base de datos
        headerBuilder.append("database").append(",");


        // Eliminar la coma final si hay al menos un parámetro en el encabezado
        if (headerBuilder.length() > 0) {
            headerBuilder.deleteCharAt(headerBuilder.length() - 1);
        }

        return headerBuilder.toString();

    }

    private static String getHeaderForMeasurements(){
        StringBuilder headerBuilder = new StringBuilder();

        // Añadiendo las claves al header
        for (String key : measurementsMap.keySet()) {
            headerBuilder.append(key).append(",");
        }

        // Eliminar la coma final si hay al menos un parámetro en el encabezado
        if (headerBuilder.length() > 0) {
            headerBuilder.deleteCharAt(headerBuilder.length() - 1);
        }

        Utils.println("Measurement Header: " + headerBuilder.toString());
        return headerBuilder.toString();
    }

    private static String getBodyParams(){
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String,String> entry : paramsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // Verificar si el valor contiene el substring "path"
            if (!key.contains("Path")) {
                builder.append(value).append(",");
            }
        }
        // Añadir el nombre de la base de datos
        builder.append(getDatabaseNameFromPattern(paramsMap.get("databasePath"))).append(",");

        // Eliminar la coma final si hay al menos un parámetro en el encabezado
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

        private static String getBodyMeasurements(){
            StringBuilder builder = new StringBuilder();

            for (Double value : measurementsMap.values()) {
                builder.append(value).append(",");
            }

            // Eliminar la coma final si hay al menos un parámetro en el encabezado
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }

            return builder.toString();
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
        return paramsMap.get("algName");
    }

    public String getResults(){
        return  paramsMap.get("algName") + ","
                + paramsMap.get("netName") + ","
                + getDatabaseNameFromPattern(paramsMap.get("databasePath")) + ","
                + paramsMap.get("numberOfClusters") + ","
                + algorithm.getItInterleaving() + ","
                + structuralHamiltonDistanceValue + ","
                + bdeuScore + ","
                + differencesOfMalkovsBlanket[0] + ","
                + differencesOfMalkovsBlanket[1] + ","
                + differencesOfMalkovsBlanket[2] + ","
                + numberOfIterations + ","
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

    public static String getSaveFileName(int id){
        StringBuilder fileNameBuilder = new StringBuilder();
        fileNameBuilder.append("exp_");
        fileNameBuilder.append(paramsMap.get("algName"));
        fileNameBuilder.append("_");

        if(paramsMap.containsKey("broadcasting")){
            fileNameBuilder.append(paramsMap.get("broadcasting"));
            fileNameBuilder.append("_");
        }
        fileNameBuilder.append(id);
        /*
        fileNameBuilder.append("_");
        fileNameBuilder.append(paramsMap.get("netName"));
        fileNameBuilder.append("_");
        fileNameBuilder.append("T");
        fileNameBuilder.append(paramsMap.get("numberOfClusters"));
        fileNameBuilder.append("_");
        fileNameBuilder.append(paramsMap.get("convergence"));
        fileNameBuilder.append("_");
        fileNameBuilder.append(paramsMap.get("broadcasting"));

        String datasetPath = paramsMap.get("databasePath");
        String[] pathComponents = datasetPath.split("/");
        String datasetIdentifier = pathComponents[pathComponents.length - 1].replace(".csv", "");
    
        fileNameBuilder.append("_");
        fileNameBuilder.append(datasetIdentifier);
        */
        
        fileNameBuilder.append(".csv");
        return fileNameBuilder.toString();
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("-----------------------\nExperiment " + paramsMap.get("algName") + "\n");
        for (String key : paramsMap.keySet()) {
            String parameter = paramsMap.get(key);
            result.append(key + ": " + parameter);
            result.append("\t");
        }
        return  result.toString();
    }

    public Map<String, String> getParamsMap() {
        return paramsMap;
    }
}

