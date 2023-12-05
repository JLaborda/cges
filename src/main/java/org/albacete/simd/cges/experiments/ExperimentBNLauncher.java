package org.albacete.simd.cges.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.albacete.simd.cges.framework.BNBuilder;

public class ExperimentBNLauncher {

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

        System.out.println("Creating experiment object...");
        experimentBNLauncher.createExperiment(parameters);
        
        System.out.println("Starting experiment...");
        experimentBNLauncher.runExperiment();
        experimentBNLauncher.saveExperiment();
        System.out.println("Experiment finished!");
        
    }

    private static ExperimentBNLauncher getExperimentBNLauncherFromCommandLineArguments(String[] args) {
        int i = 0;
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

    public String[] readParameters(){
        String[] parameterStrings = null;
        try (BufferedReader br = new BufferedReader(new FileReader(paramsFileName))) {
            String line;
            for (int i = 0; i < index; i++)
                br.readLine();
            line = br.readLine();
            parameterStrings = line.split(" ");
        } catch(IOException e){
            e.printStackTrace();
        }
        
        System.out.println("Parameters read:");
        for (int i = 0; i < parameterStrings.length; i++) {
            System.out.println("Index: " + i + "\t" + parameterStrings[i]);
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

    private boolean checkExistentFile() {
        String savePath = EXPERIMENTS_FOLDER  + experiment.getSaveFileName();
        
        return experiment.checkExistentFile(savePath);
    }

    private void saveExperiment() {
        String savePath = EXPERIMENTS_FOLDER  + experiment.getSaveFileName();
        experiment.saveExperiment(savePath);
    }
    
}
