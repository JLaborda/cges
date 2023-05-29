package org.albacete.simd.cges.threads;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.framework.BackwardStage;
import org.albacete.simd.cges.framework.ForwardStage;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.albacete.simd.cges.utils.Utils.pdagToDag;

import static org.junit.Assert.*;

public class ForwardHillClimbingThreadTest {

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


    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }


    /**
     * Constructor of the test. It initializes the subsets.
     */
    public ForwardHillClimbingThreadTest(){
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
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread (problem, subset1, 15);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        ForwardHillClimbingThread thread2 = new ForwardHillClimbingThread(problem, graph, subset1, 15);
        // Arrange
        assertNotNull(thread1);
        assertNotNull(thread2);
    }

    /**
     * Checks the first iteration of the Cancer problem for the FES stage
     * @result Each expected node is in the resulting graph after executing the first iteration of FES stage
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void searchTwoThreadsTest() throws InterruptedException {

        // ThFES objects
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread(problem, subset1, 15);
        ForwardHillClimbingThread thread2 = new ForwardHillClimbingThread(problem, subset2, 15);

        // Expectation
        List<Edge> expected1 = new ArrayList<>();
        expected1.add(new Edge(xray, cancer, Endpoint.TAIL, Endpoint.ARROW));

        List<Edge> expected2 = new ArrayList<>();
        expected2.add(new Edge(cancer, smoker, Endpoint.TAIL, Endpoint.ARROW));


        //Act
        thread1.run();
        thread2.run();
        Graph g1 = thread1.getCurrentGraph();
        Graph g2 = thread2.getCurrentGraph();

        // Getting dags
        Dag_n gdag1 = new Dag_n(removeInconsistencies(g1));
        Dag_n gdag2 = new Dag_n(removeInconsistencies(g2));


        for(Edge edge : expected1){
            assertTrue(gdag1.getEdges().contains(edge));
        }

        for(Edge edge : expected2){
            assertTrue(gdag2.getEdges().contains(edge));
        }

    }


    /**
     * Checking that FHC stops when there are no more edges to be added.
     * @result The number of iterations is less than the maximum iterations set
     */
    @Test
    public void noMoreEdgesToAddInFESTest(){

        // ThFES objects
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread(problem, subset1, 1000);

        //Act
        thread1.run();

        //Assert
        assertNotEquals(thread1.getIterations(), 1000);
    }


    /**
     * Testing that fhc stops when the maximum number of edges is reached.
     * @result The resulting graph has the same number of edges as the set maximum number of edges.
     * @throws InterruptedException Caused by an external interruption.
     */
    @Test
    public void maximumNumberOfEdgesReachedTest() throws InterruptedException {
        // ThFES objects
        FESThread thread1 = new FESThread(problem, subset1, 1000, false, true, true);
        thread1.setMaxNumEdges(2);

        //Act
        thread1.run();
        Graph result = thread1.getCurrentGraph();
        //Assert
        assertEquals(2, result.getEdges().size());

    }

    /**
     * Tests that the algorithm works correct with the Alarm network.
     *
     * @throws InterruptedException Caused by an external interruption.
     * @result The resulting graph has the same number of edges as the set maximum number of edges.
     */
    @Test
    public void cancerExecutionTest() throws InterruptedException {
        // ThFES objects
        String alarmPath = Resources.CANCER_BBDD_PATH;
        DataSet alarmDataset = Utils.readData(alarmPath);
        Set<Edge> setOfArcs = Utils.calculateArcs(alarmDataset);
        Utils.setSeed(42);
        List<Set<Edge>> subsets = Utils.split(setOfArcs, 2);
        Set<Edge> subset1 = subsets.get(0);
        Set<Edge> subset2 = subsets.get(1);

        Problem pAlarm = new Problem(alarmDataset);
        ForwardHillClimbingThread thread1 = new ForwardHillClimbingThread(pAlarm, subset1, 100);
        ForwardHillClimbingThread thread2 = new ForwardHillClimbingThread(pAlarm, subset2, 100);


        //Act
        thread1.run();
        thread2.run();
        Graph result1 = thread1.getCurrentGraph();
        Graph result2 = thread2.getCurrentGraph();
        //Assert
        assertNotNull(result1);
        assertNotNull(result2);

    }



}
