package org.albacete.simd.cges.threads;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Test;

import java.util.*;
import static org.albacete.simd.cges.utils.Utils.pdagToDag;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BackwardsHillClimbingThreadTest {

    /**
     * String containing the path to the data used in the test. The data used in these tests is made by sampling the
     * cancer Bayesian Network @see
     * <a href="https://www.bnlearn.com/bnrepository/discrete-small.html">https://www.bnlearn.com/bnrepository/discrete-small.html</a>
     */
    final String path = Resources.CANCER_BBDD_PATH;
    /**
     * Dataset created from the data file
     */
    final DataSet dataset = Utils.readData(path);
    /**
     * Variable X-Ray
     */
    final Node xray = dataset.getVariable("Xray");
    /**
     * Variable Dysponea
     */
    final Node dyspnoea = dataset.getVariable("Dyspnoea");
    /**
     * Variabe Cancer
     */
    final Node cancer = dataset.getVariable("Cancer");
    /**
     * Variable Pollution
     */
    final Node pollution = dataset.getVariable("Pollution");
    /**
     * Variable Smoker
     */
    final Node smoker = dataset.getVariable("Smoker");

    /**
     * Subset1 of pairs of nodes or variables.
     */
    final Set<Edge> subset1 = new HashSet<>();
    /**
     * Subset2 of pairs of nodes or variables.
     */
    final Set<Edge> subset2 = new HashSet<>();

    private Problem problem;


    /**
     * Constructor of the test. It initializes the subsets.
     */
    public BackwardsHillClimbingThreadTest(){
        problem = new Problem(dataset);
        initializeSubsets();
    }

    /**
     * This method initializes the subsets, splitting the nodes in what is expected to happen when the seed is 42
     */
    private void initializeSubsets(){
        // Seed used for arc split is 42

        // Subset 1:
        subset1.add(Edges.directedEdge(dyspnoea, cancer));
        subset1.add(Edges.directedEdge(cancer, dyspnoea));
        subset1.add(Edges.directedEdge(dyspnoea, smoker));
        subset1.add(Edges.directedEdge(smoker, dyspnoea));
        subset1.add(Edges.directedEdge(xray, pollution));
        subset1.add(Edges.directedEdge(pollution, xray));
        subset1.add(Edges.directedEdge(xray , cancer));
        subset1.add(Edges.directedEdge(cancer, xray));
        subset1.add(Edges.directedEdge(cancer, pollution));
        subset1.add(Edges.directedEdge(pollution, cancer));

        //Subset 2:
        subset2.add(Edges.directedEdge(pollution, smoker));
        subset2.add(Edges.directedEdge(smoker, pollution));
        subset2.add(Edges.directedEdge(cancer, smoker));
        subset2.add(Edges.directedEdge(smoker, cancer));
        subset2.add(Edges.directedEdge(dyspnoea, pollution));
        subset2.add(Edges.directedEdge(pollution, dyspnoea));
        subset2.add(Edges.directedEdge(xray, smoker));
        subset2.add(Edges.directedEdge(smoker, xray));
        subset2.add(Edges.directedEdge(xray, dyspnoea));
        subset2.add(Edges.directedEdge(dyspnoea, xray));

    }

    /**
     * Method used to remove inconsistencies in the graph passed as a parameter.
     * @param graph Graph that will have its inconsistencies removed
     * @return The modified graph
     */
    private Graph removeInconsistencies(Graph graph){
        // Transforming the current graph into a DAG
        pdagToDag(graph);

        Node nodeT, nodeH;
        for (Edge e : graph.getEdges()){
            if(!e.isDirected()) continue;
            Endpoint endpoint1 = e.getEndpoint1();
            if (endpoint1.equals(Endpoint.ARROW)){
                nodeT = e.getNode1();
                nodeH = e.getNode2();
            }else{
                nodeT = e.getNode2();
                nodeH = e.getNode1();
            }
            if(graph.existsDirectedPathFromTo(nodeT, nodeH))
                graph.removeEdge(e);
        }
        return graph;
    }

    @Test
    public void constructorTest() throws InterruptedException{
        // Arrange
        BackwardsHillClimbingThread thread1 = new BackwardsHillClimbingThread(problem, subset1);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        BackwardsHillClimbingThread thread2 = new BackwardsHillClimbingThread(problem, graph, subset1);
        // Arrange
        assertNotNull(thread1);
        assertNotNull(thread2);
    }

    /**
     * Checks the first iteration of the Cancer problem for the BHC stage
     * @result Each expected node is in the resulting graph after executing the first iteration of FES stage
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void searchTwoThreadsTest() throws InterruptedException {

        List<Node> nodes = Arrays.asList(cancer, xray, dyspnoea, pollution, smoker);
        Graph fusionGraph = new EdgeListGraph(nodes);
        fusionGraph.addDirectedEdge(cancer, dyspnoea);
        fusionGraph.addDirectedEdge(cancer, xray);
        fusionGraph.addDirectedEdge(pollution, cancer);
        fusionGraph.addDirectedEdge(smoker, cancer);
        fusionGraph.addDirectedEdge(xray, dyspnoea);
        fusionGraph.addDirectedEdge(pollution, smoker);


        //System.out.println("Initial Graph");
        //System.out.println(fusionGraph);

        // Threads objects
        BackwardsHillClimbingThread thread1 = new BackwardsHillClimbingThread(problem, fusionGraph, subset1);

        // Expectation
        List<Edge> expected = new ArrayList<>();
        expected.add(new Edge(cancer, xray, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(pollution, cancer, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(pollution, smoker, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(smoker, cancer, Endpoint.TAIL, Endpoint.ARROW));
        expected.add(new Edge(xray, dyspnoea, Endpoint.TAIL, Endpoint.ARROW));



        //Act
        thread1.run();
        Graph g1 = thread1.getCurrentGraph();

        System.out.println(g1);

        // Getting dags
        Dag_n gdag1 = new Dag_n(removeInconsistencies(g1));


        for(Edge edge : expected){
            assertTrue(gdag1.getEdges().contains(edge));
        }


    }






}
