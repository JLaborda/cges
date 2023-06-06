package org.albacete.simd.cges.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ExperimentBNLauncher {

    public static final int MAXITERATIONS = 100;

    private final String EXPERIMENTS_FOLDER;
    private final int index;
    private final String paramsFileName;
    private ExperimentBNBuilder experiment;

    public ExperimentBNLauncher(int index, String paramsFileName, String saveFolder){
        this.index = index;
        this.paramsFileName = paramsFileName;
        this.EXPERIMENTS_FOLDER = saveFolder;
    }

    public static void main(String[] args) throws Exception {

        ExperimentBNLauncher experimentBNLauncher = getExperimentBNLauncherFromCommandLineArguments(args);
        String[] parameters = experimentBNLauncher.readParameters();

        System.out.println("Launching experiment");
        experimentBNLauncher.createExperiment(parameters);
        
        if (!experimentBNLauncher.checkExistentFile()){
            System.out.println("Starting experiment");
            experimentBNLauncher.runExperiment();
            experimentBNLauncher.saveExperiment();
            System.out.println("Experiment finished");
        }
        else{
            System.out.println("Experiment has already been done. Therefore, it has not been run again.");
        }
    }

    private static ExperimentBNLauncher getExperimentBNLauncherFromCommandLineArguments(String[] args) {
        int i = 1;
        System.out.println("Number of args: "  + args.length);
        for (String string : args) {
            System.out.println("Args " + i + ": " + string);
            i++;
        }
        String paramsFileName = args[0];
        int index = Integer.parseInt(args[1]);
        String saveFolder = args.length == 3 ? args[2] : "results/";
        
        return new ExperimentBNLauncher(index, paramsFileName, saveFolder);
    }

    public String[] readParameters() throws Exception {
        String[] parameterStrings = null;
        try (BufferedReader br = new BufferedReader(new FileReader(paramsFileName))) {
            String line;
            for (int i = 0; i < index; i++)
                br.readLine();
            line = br.readLine();
            parameterStrings = line.split(" ");
        }
        catch(FileNotFoundException e){
            System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parameterStrings;
    }

    private void createExperiment(String[] parameters){
        try {
            experiment = new ExperimentBNBuilder(parameters);
        } catch (Exception e) {
            System.out.println("Exception when creating the experiment");
            int i=0;
            for (String string : parameters) {
                System.out.println("Param[" + i + "]: " + string);
                i++;
            }
            e.printStackTrace();
        }
    }
    
    private void runExperiment(){
        experiment.runExperiment();
    }

    private boolean checkExistentFile() throws IOException{
        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + experiment.netName + "_" + experiment.algName + "_" + 
                experiment.databaseName + "_t" + experiment.numberOfRealThreads + "_PGESt" + experiment.numberOfRealThreads +
                "_i" + experiment.edgeLimitation + ".csv";
        
        return experiment.checkExistentFile(savePath);
    }

    private void saveExperiment() {
        String results = experiment.getResults();

        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + experiment.netName + "_" + experiment.algName + "_" + 
                experiment.databaseName + "_t" + experiment.numberOfRealThreads + "_PGESt" + experiment.numberOfRealThreads +
                "_i" + experiment.edgeLimitation + ".csv";
        try {
            saveExperiment(savePath, results);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving results at: " + savePath);
        }
    }
    
    public static void saveExperiment(String savePath, String results) throws IOException{
        File file = new File(savePath);
            BufferedWriter csvWriter = new BufferedWriter(new FileWriter(savePath, true));
            //FileWriter csvWriter = new FileWriter(savePath, true);
            if(file.length() == 0) {
                String header = "algorithm,network,bbdd,threads,pges_threads,interleaving,seed,SHD,loglike,bdeu,deltaMM,deltaMM+,deltaMM-,iterations,time(s)\n";
                csvWriter.append(header);
            }
            csvWriter.append(results);

            csvWriter.flush();
            csvWriter.close();
            System.out.println("Results saved at: " + savePath);
    }
}
