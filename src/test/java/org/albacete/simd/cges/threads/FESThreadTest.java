package org.albacete.simd.cges.threads;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.albacete.simd.cges.utils.Utils.pdagToDag;

import static org.junit.Assert.*;

public class FESThreadTest {

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

    private final Problem problem;


    /**
     * Constructor of the test. It initializes the subsets.
     */

    public FESThreadTest(){
        problem = new Problem(dataset);
        initializeSubsets();
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
     * Checks that both constructors work perfectly.
     * @throws InterruptedException Exception caused by thread interruption.
     */
    @Test
    public void constructorTest() throws InterruptedException{
        // Arrange
        FESThread thread1 = new FESThread(problem, subset1, 15, false,true,true);
        thread1.run();
        Graph graph = thread1.getCurrentGraph();
        // Act
        FESThread thread2 = new FESThread(problem, graph, subset1, 15,false, true, true);
        // Arrange
        assertNotNull(thread1);
        assertNotNull(thread2);

    }

    /**
     * Checks the first iteration of the Cancer problem for the FES stage
     * @throws InterruptedException Exception caused by thread interruption
     */
    @Test
    public void searchTwoThreadsTest() throws InterruptedException {
        // thread objects
        FESThread thread1 = new FESThread(problem, subset1, 15, false,true,true);
        FESThread thread2 = new FESThread(problem, subset2, 15, false,true,true);


        //Act
        thread1.run();
        thread2.run();
        Graph g1 = thread1.getCurrentGraph();
        Graph initialGraphg1 = thread1.getInitialGraph();
        Graph g2 = thread2.getCurrentGraph();
        Graph initialGraphg2 = thread2.getInitialGraph();

        // Getting dags
        Dag_n gdag1 = new Dag_n(removeInconsistencies(g1));
        Dag_n gdag2 = new Dag_n(removeInconsistencies(g2));

        assertNotNull(gdag1);
        assertEquals(gdag1.getNodes().size(),5);
        assertNotNull(gdag2);
        assertEquals(gdag2.getNodes().size(),5);

        assertTrue(GESThread.scoreGraph(g1,problem) >= GESThread.scoreGraph(initialGraphg1,problem));
        assertTrue(GESThread.scoreGraph(g2,problem) >= GESThread.scoreGraph(initialGraphg2,problem));


    }

    /**
     * Checking that fes stops when there are no more edges to be added.
     */
    @Test
    public void noMoreEdgesToAddInFESTest(){

        // thread objects
        FESThread thread1 = new FESThread(problem, subset1, 1000, false,true,true);

        //Act
        thread1.run();

        //Assert
        assertNotEquals(thread1.getIterations(), 1000);
    }

    /**
     * Testing that fes stops when the maximum number of edges is reached.
     * @throws InterruptedException Caused by an external interruption.
     */
    @Test
    public void maximumNumberOfEdgesReachedTest() throws InterruptedException {
        // thread objects
        FESThread thread1 = new FESThread(problem, subset1, 1000, false,true,true);
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
     */
    @Test
    public void cancerExecutionTest() throws InterruptedException {
        // thread objects
        String alarmPath = Resources.CANCER_BBDD_PATH;
        DataSet alarmDataset = Utils.readData(alarmPath);
        Set<Edge> setOfArcs = Utils.calculateArcs(alarmDataset);
        Utils.setSeed(42);
        List<Set<Edge>> subsets = Utils.split(setOfArcs, 2);
        Set<Edge> subset1 = subsets.get(0);
        Set<Edge> subset2 = subsets.get(1);

        Problem pAlarm = new Problem(alarmDataset);
        FESThread thread1 = new FESThread(pAlarm, subset1, 100, false,true,true);
        FESThread thread2 = new FESThread(pAlarm, subset2, 100, false,true,true);


        //Act
        thread1.run();
        thread2.run();
        Graph result1 = thread1.getCurrentGraph();
        Graph result2 = thread2.getCurrentGraph();
        //Assert
        assertNotNull(result1);
        assertNotNull(result2);

    }

    /**
     * Tests that if two nodes X and Y are equal in the subset S, then it should not be considered in the fes stage.
     */
    @Test
    public void xAndYAreEqualShouldContinueTest() throws InterruptedException {
        // Arrange
        Edge edge1 = Edges.directedEdge(this.cancer,this.cancer);
        Edge edge2 = Edges.directedEdge(this.cancer, this.smoker);

        Set<Edge> S = new HashSet<>();
        S.add(edge1);
        S.add(edge2);

        FESThread fes = new FESThread(problem, S, 100, false,true,true);
        Thread tFES = new Thread(fes);

        // Act
        tFES.start();
        tFES.join();
        Graph result = fes.getCurrentGraph();
        Set<Edge> edgesResult = result.getEdges();
        // Assert
        Edge badEdge1 = Edges.undirectedEdge(this.cancer, this.cancer);
        Edge badEdge2 = Edges.directedEdge(this.cancer, this.cancer);
        Edge goodEdge1 = Edges.undirectedEdge(this.cancer, this.smoker);
        Edge goodEdge2 = Edges.directedEdge(this.cancer, this.smoker);
        Edge goodEdge3 = Edges.directedEdge(this.smoker, this.cancer);

        System.out.println(edgesResult);

        assertFalse(edgesResult.contains(badEdge1));
        assertFalse(edgesResult.contains(badEdge2));
        assertTrue(edgesResult.contains(goodEdge1) ||
                edgesResult.contains(goodEdge2) ||
                edgesResult.contains(goodEdge3));
    }



    /**
     * Checking that getter works correctly for AggressivelyPreventCycles variable
     */
    @Test
    public void isAggressivelyPreventCyclesTest(){
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        assertFalse(thread.isAggressivelyPreventCycles());
    }
    /**
     * Checking that setter works correctly for AggressivelyPreventCycles variable
     */
    @Test
    public void setAggresivelyPreventCyclesTest(){
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        thread.setAggressivelyPreventCycles(true);
        // Assert
        assertTrue(thread.isAggressivelyPreventCycles());
    }

    /**
     * Checking that getter works correctly for currentGraph variable
     */
    @Test
    public void getCurrentGraphTest() throws InterruptedException {
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        thread.run();
        Graph result = thread.getCurrentGraph();
        // Assert
        assertNotNull(result);
    }
    /**
     * Checking that getter works correctly for flag variable
     */
    @Test
    public void getFlagTest() throws InterruptedException {
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        thread.run();
        boolean result = thread.getFlag();
        // Assert
        assertTrue(result);
    }
    /**
     * Checking that resetting the flag works correctly
     */
    @Test
    public void resetFlagTest() throws InterruptedException {
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        thread.run();
        thread.resetFlag();
        boolean result = thread.getFlag();
        // Assert
        assertFalse(result);
    }

    /**
     * Checking that the bdeu score works correctly
     */
    @Test
    public void getBDeuScoreTest(){
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        thread.run();
        thread.resetFlag();
        double result = thread.getScoreBDeu();

        // Assert
        assertNotEquals(result, Double.NEGATIVE_INFINITY);
    }

    /**
     * Checking that setting the initial graph works correctly
     */
    @Test
    public void setterAndGetterOfInitialGraphTest() throws InterruptedException {
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        thread.run();
        Graph expected = thread.getCurrentGraph();

        thread.setInitialGraph(null);
        Graph result = thread.getInitialGraph();
        // Assert
        assertNotEquals(expected, result);
        assertNull(result);
    }

    /**
     * Checking structurePrior getter and setter
     */
    @Test
    public void setterAndGetterOfStructurePriorTest(){
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        double expected = 2.3;
        thread.setStructurePrior(expected);
        double actual = thread.getStructurePrior();
        // Assert
        assertEquals(expected, actual, 0);
    }

    /**
     * Checking samplePrior getter and setter
     */
    @Test
    public void setterAndGetterOfSamplePriorTest(){
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        double expected = 2.3;
        thread.setSamplePrior(expected);
        double actual = thread.getSamplePrior();
        // Assert
        assertEquals(expected, actual, 0);
    }


    /**
     * Checking elapsed time getter and setter
     */
    @Test
    public void setterAndGetterOfElapsedTimeTest(){
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        long expected = 0;
        long actual = thread.getElapsedTime();
        // Assert
        assertEquals(expected, actual, 0);
    }

    /**
     * Checking maxNumEdges getter and setter
     */
    @Test
    public void setterAndGetterOfMaxNumEdges(){
        // Arrange
        FESThread thread = new FESThread(problem, subset1, 15, false,true,true);
        // Act
        int expected = 23;
        thread.setMaxNumEdges(expected);
        int actual = thread.getMaxNumEdges();
        // Assert
        assertEquals(expected, actual);
    }





}
