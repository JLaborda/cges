package org.albacete.simd.cges.sampling;

import weka.classifiers.bayes.net.BIFReader;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.CSVSaver;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Random;

import org.albacete.simd.cges.utils.Utils;

/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * BayesNet.java
 * Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */


public class SamplingBNGenerator {


    public BIFReader bf = null;
    int nSamples = 1000;



  /** the seed value */
  int m_nSeed = 1;

  /** the random number generator */
  Random random;

  /**
   * Constructor for BayesNetGenerator.
 * @throws Exception from reading wrong file.
   */
  public SamplingBNGenerator(String fileName, int nSamples, int seed) throws Exception {

      this.bf = new BIFReader();
      bf.processFile(fileName);
      this.nSamples = nSamples;
      random = new Random(seed);


  } // c'tor


  /**
   * GenerateInstances generates random instances sampling from the distribution
   * represented by the Bayes network structure. It assumes a Bayes network
   * structure has been initialized
   *
   * @throws Exception if something goes wrong
   */
  public void generateInstances() throws Exception {
    int[] order = getOrder();
    for (int iInstance = 0; iInstance < nSamples; iInstance++) {
      int nNrOfAtts = bf.m_Instances.numAttributes();
      double[] instance = new double[nNrOfAtts];
      for (int iAtt2 = 0; iAtt2 < nNrOfAtts; iAtt2++) {
        int iAtt = order[iAtt2];
        double iCPT = 0;
        for (int iParent = 0; iParent <  bf.getNrOfParents(iAtt); iParent++) {
          int nParent = bf.getParent(iAtt, iParent); //m_ParentSets[iAtt].getParent(iParent);
          iCPT = iCPT *  bf.m_Instances.attribute(nParent).numValues() + instance[nParent];
        }
        double fRandom = random.nextInt(1000) / 1000.0f;
        int iValue = 0;
        while (fRandom > bf.m_Distributions[iAtt][(int) iCPT]
          .getProbability(iValue)) {
          fRandom = fRandom
            - bf.m_Distributions[iAtt][(int) iCPT].getProbability(iValue);
          iValue++;

        }
        instance[iAtt] = iValue;
      }
      bf.m_Instances.add(new DenseInstance(1.0, instance));
    }
  } // GenerateInstances

  /**
   * @throws Exception if there's a cycle in the graph
   */
  int[] getOrder() throws Exception {
    int nNrOfAtts = bf.m_Instances.numAttributes();
    int[] order = new int[nNrOfAtts];
    boolean[] bDone = new boolean[nNrOfAtts];
    for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
//	      Utils.println("at: "+bf.getNodeName(iAtt));
      int iAtt2 = 0;
      boolean allParentsDone = false;
      while (!allParentsDone && iAtt2 < nNrOfAtts) {
//	   	  Utils.println("con at: "+bf.getNodeName(iAtt2));
        if (!bDone[iAtt2]) {
          allParentsDone = true;
          int iParent = 0;
          int nrParents = bf.getNrOfParents(iAtt2);
          while (allParentsDone && iParent < nrParents) {
            int indexParent = bf.getParent(iAtt2,iParent++);
            allParentsDone = bDone[indexParent];
          }
          nrParents = bf.getNrOfParents(iAtt2);
          if ((allParentsDone && iParent == nrParents)){
            order[iAtt] = iAtt2;
            bDone[iAtt2] = true;
          } else {
            iAtt2++;
          }
        } else {
          iAtt2++;
        }
      }
      if (allParentsDone && iAtt2 == nNrOfAtts) {
        throw new Exception("There appears to be a cycle in the graph");
      }
    }
    return order;
  } // getOrder

  /**
   * Returns either the net (if BIF format) or the generated instances
   *
   * @return either the net or the generated instances
   */
  @Override
  public String toString() {
    return bf.m_Instances.toString();
  } // toString

  boolean m_bGenerateNet = false;


  void setNrOfInstances(int nNrOfInstances) {
    this.nSamples = nNrOfInstances;
  }

  void setSeed(int nSeed) {
    m_nSeed = nSeed;
  }

  /**
   * Main method
   *
   * @param args the commandline parameters
 * @throws Exception from Sampling
 * @throws NumberFormatException from reading data
   */
  static public void main(String[] args) throws NumberFormatException, Exception {

    // Definiendo los argumentos
    // networks: pathfinder, andes, diabetes, pigs, link y munin
    String [] inpuXbifPathStrings = {
      "/home/jorlabs/projects/cges/res/networks/pathfinder/pathfinder.xbif",
      "/home/jorlabs/projects/cges/res/networks/andes/andes.xbif",
      "/home/jorlabs/projects/cges/res/networks/diabetes/diabetes.xbif",
      "/home/jorlabs/projects/cges/res/networks/pigs/pigs.xbif",
      "/home/jorlabs/projects/cges/res/networks/link/link.xbif",
      "/home/jorlabs/projects/cges/res/networks/munin/munin.xbif"
    };

    // Numero de instancias
    int [] instances = {1000, 2000, 5000, 10000, 20000, 50000, 100000};

    // Semillas: 10 primeros numeros primos
    int seed = 42;

    // Carpeta de salida
    String outputFolder = "/home/jorlabs/projects/cges/res/large_datasets/";
    String [] outputHeaderPaths = {
      outputFolder + "pathfinder/pathfinder",
      outputFolder + "andes/andes",
      outputFolder + "diabetes/diabetes",
      outputFolder + "pigs/pigs",
      outputFolder + "link/link",
      outputFolder + "munin/munin"
      };
   for(int i = 0; i<inpuXbifPathStrings.length; i++){
      for(int j = 0; j<instances.length; j++){
        String outputPath = outputHeaderPaths[i] + "_" + instances[j] + ".csv";
        sampleBN(inpuXbifPathStrings[i], instances[j], seed, outputPath);
        System.out.println("Sampled  " + inpuXbifPathStrings[i] + " with " + instances[j] + " instances and seed " + seed + " to " + outputPath);
      }
    }
    

    //Utils.println("Length of args: " + args.length);

      // // Lectura de argumentos
      // String folderExperimentsString = args[0]; // Carpeta de experimentos
      // String nInstances = args[1]; // Numero de instancias
      // String initialSeed = args[2];// Semilla

      // // Impresion de argumentos
      // Utils.println(folderExperimentsString); 
      // Utils.println(nInstances); 
      // Utils.println(initialSeed); 

      // File carpeta_experimentos = new File(folderExperimentsString);
      // ArrayList<String> experimentos = listarFicherosdeCarpeta(carpeta_experimentos);
      // for(String exp: experimentos){
      //     Utils.println(exp);
      // }
      // for (int red = 0; red<experimentos.size(); red++){
      //     for(int ndatos = 0; ndatos< 10; ndatos++) {
      //         if(experimentos.get(red).contains("xbif")){
      //             int seed = Integer.parseInt(initialSeed)+(12*ndatos);
      //             SamplingBNGenerator b = new SamplingBNGenerator(carpeta_experimentos+"/"+experimentos.get(red),Integer.parseInt(nInstances), seed);
      //             b.generateInstances();
      //             CSVSaver csv = new CSVSaver();
      //             Instances data = b.bf.m_Instances;
      //             csv.setInstances(data);
      //             String outpath = carpeta_experimentos + "/BBDD/" + experimentos.get(red)+Integer.parseInt(nInstances)+ndatos+".csv";
      //             Utils.println("Saving to: " + outpath);
      //             File fcsv = new File(outpath);
      //             FileOutputStream output = new FileOutputStream(fcsv);
      //             csv.setDestination(output);
      //             csv.setNoHeaderRow(false);
      //             csv.writeBatch();
      //         }
      //     }

      // }
       } // main

  public static void sampleBN(String inputXbifPath, int nInstances, int seed, String outputPath){
    try {

      // Generating sample of nInstances from the inputXbifPath with random seed
      SamplingBNGenerator sampler = new SamplingBNGenerator(inputXbifPath, nInstances, seed);
      sampler.generateInstances();
      Instances data = sampler.bf.m_Instances;
      
      // Saving sampled data to outputPath
      CSVSaver csv = new CSVSaver();
      csv.setInstances(data);
      Utils.println("Saving to: " + outputPath);
      File fcsv = new File(outputPath);
      FileOutputStream output = new FileOutputStream(fcsv);
      csv.setDestination(output);
      csv.setNoHeaderRow(false);
      csv.writeBatch();

    } catch (Exception e) {
      System.out.println("Error sampling BN");
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


   public static ArrayList<String> listarFicherosPorCarpeta(final File carpeta) {
        ArrayList<String> carpetas = new ArrayList<String>();

        for (final File ficheroEntrada : carpeta.listFiles()) {
            if (ficheroEntrada.isDirectory()) {
                carpetas.add(ficheroEntrada.getName());
            } else {
                Utils.println("Se espera un subdirectorio");
            }
        }
        return carpetas;
    }

   public static ArrayList<String> listarFicherosdeCarpeta(File carpeta) {
        ArrayList<String> ficheros = new ArrayList<String>();

        //File[] ficherosF = carpeta.listFiles();

        for (final File ficheroEntrada : carpeta.listFiles()) {
            if (ficheroEntrada.isDirectory()) {
                Utils.println("Se esperan ficheros");
            } else {
                ficheros.add(ficheroEntrada.getName());
            }
        }
        return ficheros;
    }


} // class BayesNetGenerator
