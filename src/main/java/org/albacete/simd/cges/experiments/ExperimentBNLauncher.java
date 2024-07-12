package org.albacete.simd.cges.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.albacete.simd.cges.utils.Utils;

public class ExperimentBNLauncher {

    private final String EXPERIMENTS_FOLDER;
    private final int index;
    private final String paramsFileName;
    private ExperimentBNBuilder experiment;
    private static ExperimentBNLauncher experimentBNLauncher;

    public ExperimentBNLauncher(int index, String paramsFileName, String saveFolder){
        this.index = index;
        this.paramsFileName = paramsFileName;
        if(!saveFolder.endsWith("/")){
            saveFolder = saveFolder + "/";
        }
        this.EXPERIMENTS_FOLDER = saveFolder;
        ExperimentBNBuilder.saveFolder = EXPERIMENTS_FOLDER;
        // Setting index
        ExperimentBNBuilder.index = index;
    }

    public static void main(String[] args) throws Exception {
        // Pre-configuration of the experiment
        Utils.setVerbose(false);
        experimentBNLauncher = getExperimentBNLauncherFromCommandLineArguments(args);

        // Creating experiment
        String[] parameters = experimentBNLauncher.readParameters();
        System.out.println("Creating experiment object...");
        experimentBNLauncher.createExperiment(parameters);
        
        // Checking if experiment has been already run
        checkExperiment(experimentBNLauncher);

        // Configure timer to save experiment every minute
        setAutoSave();
        setSaveExperimentWhenShutdown();

        // Running experiment
        System.out.println("Starting experiment...");
        experimentBNLauncher.runExperiment();
        // Saving final results
        experimentBNLauncher.saveExperiment();
        System.out.println("Experiment finished!");
        
    }

    public static void setAutoSave(){
        // Configurar el temporizador para guardar el experimento cada cierto tiempo
        Timer timer = new Timer(true); // Timer en modo daemon
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if((experimentBNLauncher != null)){
                        System.out.println("Auto-saving experiment...");
                        ExperimentBNBuilder.pauseStopWatch();
                        experimentBNLauncher.getExperiment().calcuateMeasurements();
                        experimentBNLauncher.saveExperiment();
                        ExperimentBNBuilder.resumeStopWatch();
                    } 
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 60000); // Guardar cada 60 segundos (ajusta el intervalo según tus necesidades)

    }

    public static void setSaveExperimentWhenShutdown(){
        // Handling SIGTERM signal from slurm cluster
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                System.out.println("Shutdown hook ran!");
                System.out.println("Saving experiment...");
                //checkExperiment(experimentBNLauncher);  // Checking beforehand if experiment has been already run
                
                experimentBNLauncher.experiment.calcuateMeasurements();
                experimentBNLauncher.saveExperiment();
                System.out.println("Experiment finished!");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private static ExperimentBNLauncher getExperimentBNLauncherFromCommandLineArguments(String[] args) {
        int i = 0;
        Utils.println("Number of args: "  + args.length);
        for (String string : args) {
            Utils.println("Args " + i + ": " + string);
            i++;
        }
        String paramsFileName = args[0];
        int index = Integer.parseInt(args[1]);
        String saveFolder = args.length == 3 ? args[2] : "results/";
        
        return new ExperimentBNLauncher(index, paramsFileName, saveFolder);
    }

    private static void checkExperiment(ExperimentBNLauncher experimentBNLauncher) {
        //Check if experiment has been already run
        String savePath = experimentBNLauncher.EXPERIMENTS_FOLDER + ExperimentBNBuilder.getSaveFileName(experimentBNLauncher.index);
        File file = new File(savePath);
        if(file.exists() && !file.isDirectory()){
            System.out.println("Experiment already executed. Skipping...");
            System.out.println("Skipping Experiment at: " + savePath);
            System.out.println("Skipping experiment with index: " + experimentBNLauncher.index);
            System.out.println("Skipping experiment with paramsFilePath: " +experimentBNLauncher.paramsFileName);
            System.exit(0);
        }
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
            System.out.println("Parameters could not be read");
            System.out.println("File: " + paramsFileName);
            System.out.println("Index: " + index);
            e.printStackTrace();
            System.exit(-1);
        }
        
        Utils.println("Parameters read:");
        for (int i = 0; i < parameterStrings.length; i++) {
            Utils.println("Index: " + i + "\t" + parameterStrings[i]);
        }

        return parameterStrings;
    }

    public void createExperiment(String[] parameters){
        try {
            experiment = new ExperimentBNBuilder(parameters);
        } catch (Exception e) {
            Utils.println("Exception when creating the experiment");
            int i=0;
            for (String string : parameters) {
                Utils.println("Param[" + i + "]: " + string);
                i++;
            }
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    private void runExperiment(){
        experiment.runExperiment();
    }

    private void saveExperiment() {
        String savePath = EXPERIMENTS_FOLDER + ExperimentBNBuilder.getSaveFileName(index);
        ExperimentBNBuilder.saveExperiment(savePath);
    }

    public ExperimentBNBuilder getExperiment() {
        return experiment;
    }
    
}
