package org.albacete.simd.cges.clustering;


import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class HierarchicalClusteringTest {

    Problem p = new Problem(Resources.CANCER_BBDD_PATH);
    Problem alarmProblem = new Problem(Resources.ALARM_BBDD_PATH);

    @Before
    public void setRandomSeed() {
        Utils.setSeed(42);
    }

    @Test
    public void constructorTest() {
        HierarchicalClustering hc1 = new HierarchicalClustering(p);
        HierarchicalClustering hc2 = new HierarchicalClustering(p, false);

        assertNotNull(hc1);
        assertNotNull(hc2);
    }

    @Test
    public void clusterizeTest() {
        HierarchicalClustering clustering = new HierarchicalClustering(p);
        HierarchicalClustering clustering2 = new HierarchicalClustering(p, true);

        Set<String> names1 = new HashSet<>(Arrays.asList("MINVOLSET", "EXPCO2", "SAO2", "PULMEMBOLUS", "PVSAT", "INTUBATION", "MINVOL", "DISCONNECT", "VENTLUNG", "SHUNT", "VENTTUBE", "KINKEDTUBE", "VENTMACH", "ARTCO2", "BP", "VENTALV", "TPR", "PRESS"));
        Set<Node> cluster1 = p.getVariables().stream().filter(node -> names1.contains(node.getName())).collect(Collectors.toSet());
        Set<String> names2 = new HashSet<>(Arrays.asList("ERRCAUTER", "LVEDVOLUME", "HYPOVOLEMIA", "HRBP", "INSUFFANESTH", "STROKEVOLUME", "HRSAT", "CATECHOL", "PCWP", "LVFAILURE", "HR", "FIO2", "ERRLOWOUTPUT", "HISTORY", "PAP", "HREKG", "CVP", "ANAPHYLAXIS", "CO"));
        Set<Node> cluster2 = p.getVariables().stream().filter(node -> names2.contains(node.getName())).collect(Collectors.toSet());


        List<Set<Node>> clusters = clustering.generateNodeClusters(2);
        List<Set<Node>> clustersParallel = clustering2.generateNodeClusters(2);

        assertEquals(clusters.size(), clustersParallel.size());
        assertEquals(clusters.get(0).size(), clustersParallel.get(0).size());
        assertEquals(clusters.get(1).size(), clustersParallel.get(1).size());

        for (Node node : cluster1) {
            assertTrue(clusters.get(0).contains(node));
            assertTrue(clustersParallel.get(0).contains(node));
        }
        for (Node node : cluster2) {
            assertTrue(clusters.get(1).contains(node));
            assertTrue(clustersParallel.get(1).contains(node));
        }
    }

    @Test
    public void edgeDistributionTest() {
        HierarchicalClustering clustering = new HierarchicalClustering(p);

        //List<Set<Node>> clusters = clustering.clusterize(2);
        List<Set<Edge>> edgeDistribution = clustering.generateEdgeDistribution(2);

        System.out.println("edgeDistribution0: " + edgeDistribution.get(0).size());
        System.out.println("edgeDistribution1: " + edgeDistribution.get(1).size());

        // Checking that there is the same distribution in both clusters
        assertEquals(2, edgeDistribution.size());
        assertTrue(edgeDistribution.get(0).size() >= 3 && edgeDistribution.get(0).size() <= 20);
        assertTrue(edgeDistribution.get(1).size() >= 3 && edgeDistribution.get(1).size() <= 20);

    }

    @Test
    public void edgeDistributionDuplicateTest() {

        HierarchicalClustering clustering = new HierarchicalClustering(true, true);
        clustering.setProblem(alarmProblem);

        List<Set<Edge>> edgeDistribution = clustering.generateEdgeDistribution(2);

        assertEquals(2, edgeDistribution.size());


        Set<Edge> edgeCluster1 = edgeDistribution.get(0);
        Set<Edge> edgeCluster2 = edgeDistribution.get(1);

        boolean duplicate = false;
        for (Edge edge : edgeCluster1) {
            if(edgeCluster2.contains(edge)) {
                duplicate = true;
                break;
            }
        }
        assertTrue(duplicate);

        System.out.println("edgeDistribution0: " + edgeDistribution.get(0).size());
        System.out.println("edgeDistribution1: " + edgeDistribution.get(1).size());

        assertTrue(Math.abs(edgeDistribution.get(0).size() - edgeDistribution.get(1).size()) <= 30);

    }

    @Test
    public void paralellAndSequentialClusteringProduceTheSameResults() {
        HierarchicalClustering clusteringSeq = new HierarchicalClustering(p, false);
        HierarchicalClustering clusteringPar = new HierarchicalClustering(p, true);

        List<Set<Node>> clustersSeq = clusteringSeq.generateNodeClusters(2);
        List<Set<Node>> clustersPar = clusteringPar.generateNodeClusters(2);

        assertEquals(clustersSeq.size(), clustersPar.size());

        for (int i = 0; i < clustersSeq.size(); i++) {
            assertEquals(clustersSeq.get(i).size(), clustersPar.get(i).size());
        }

        for (int i = 0; i < clustersSeq.size(); i++) {
            for (Node node : clustersSeq.get(i)) {
                assertTrue(clustersPar.get(i).contains(node));
            }
        }
    }

    @Test
    public void checkingThatTheJointClusteringIsDeterministic(){
        HierarchicalClustering clustering1 = new HierarchicalClustering(true, true);
        clustering1.setProblem(alarmProblem);
        HierarchicalClustering clustering2 = new HierarchicalClustering(true, true);
        clustering2.setProblem(alarmProblem);

        List<Set<Node>> clusters1 = clustering1.generateNodeClusters(4);
        List<Set<Node>> clusters2 = clustering2.generateNodeClusters(4);

        assertEquals(clusters1.size(), clusters2.size());

        for (int i = 0; i < clusters1.size(); i++) {
            assertEquals(clusters1.get(i).size(), clusters2.get(i).size());
        }

        for (int i = 0; i < clusters1.size(); i++) {
            for (Node node : clusters1.get(i)) {
                assertTrue(clusters2.get(i).contains(node));
            }
        }

        List<Set<Edge>> edgeDistribution1 = clustering1.generateEdgeDistribution(4);
        List<Set<Edge>> edgeDistribution2 = clustering2.generateEdgeDistribution(4);

        assertEquals(edgeDistribution1.size(), edgeDistribution2.size());

        //Checking that the clusters have the same edges
        for (int i = 0; i < edgeDistribution1.size(); i++) {
            assertEquals(edgeDistribution1.get(i).size(), edgeDistribution2.get(i).size());
        }

        //Checking that the edge distribution is the same
        for (int i = 0; i < edgeDistribution1.size(); i++) {
            for (Edge edge : edgeDistribution1.get(i)) {
                assertTrue(edgeDistribution2.get(i).contains(edge));
            }
        }

    }

}
