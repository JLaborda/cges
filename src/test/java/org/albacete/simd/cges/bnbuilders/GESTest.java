package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class GESTest {

    final String path = Resources.CANCER_BBDD_PATH;
    final DataSet dataSet = Utils.readData(path);


    @Test
    public void testConstructor() {
        BNBuilder alg1 = new GES(dataSet, false);
        BNBuilder alg2 = new GES(path, false);

        List<Node> nodes = Arrays.asList(Resources.CANCER, Resources.DYSPNOEA, Resources.POLLUTION, Resources.XRAY, Resources.SMOKER);
        Dag_n initialGraph = new Dag_n(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        BNBuilder alg3 = new GES(initialGraph, Resources.CANCER_BBDD_PATH, false);
        BNBuilder alg4 = new GES(initialGraph, Resources.CANCER_DATASET, false);


        assertNotNull(alg1);
        assertNotNull(alg2);
        assertNotNull(alg3);
        assertNotNull(alg4);

        assertEquals(1, alg1.getNumberOfThreads());
        assertEquals(1, alg2.getNumberOfThreads());
        assertEquals(1, alg3.getNumberOfThreads());
        assertEquals(1, alg4.getNumberOfThreads());
        assertEquals(-1, alg1.getMaxIterations());
        assertEquals(-1, alg2.getMaxIterations());
        assertEquals(-1, alg3.getMaxIterations());
        assertEquals(-1, alg4.getMaxIterations());
        assertEquals(-1, alg1.getItInterleaving());
        assertEquals(-1, alg2.getItInterleaving());
        assertEquals(-1, alg3.getItInterleaving());
        assertEquals(-1, alg4.getItInterleaving());
        assertNull(alg1.getCurrentGraph());
        assertNull(alg2.getCurrentGraph());
        assertNotNull(alg3.getCurrentGraph());
        assertNotNull(alg4.getCurrentGraph());
        assertEquals(initialGraph.getNodes(), alg3.getCurrentGraph().getNodes());
        assertEquals(initialGraph.getEdges(), alg3.getCurrentGraph().getEdges());
        assertEquals(initialGraph.getNodes(), alg4.getCurrentGraph().getNodes());
        assertEquals(initialGraph.getEdges(), alg4.getCurrentGraph().getEdges());


    }

    @Test
    public void searchTest() {
        BNBuilder ges = new GES(Resources.CANCER_BBDD_PATH, false);
        Utils.setSeed(42);

        System.out.println("Searching...");
        ges.search();

        //System.out.println((alg1.getCurrentGraph()));

        assertNotNull(ges.getCurrentGraph());
        assertTrue(ges.getCurrentGraph() instanceof Dag_n);

    }


    @Test
    public void convergenceTest() {
        BNBuilder alg = new GES(Utils.readData(Resources.CANCER_BBDD_PATH), false);
        alg.search();

        assertNotNull(alg.getCurrentGraph());
        assertNotEquals(0, alg.getCurrentGraph().getEdges().size());
        assertTrue(alg.getCurrentGraph() instanceof Dag_n);

    }

    @Test
    public void testSearchWithInitialGraph() {
        List<Node> nodes = Arrays.asList(Resources.XRAY, Resources.DYSPNOEA, Resources.CANCER, Resources.POLLUTION, Resources.SMOKER);
        Dag_n initialGraph = new Dag_n(nodes);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.DYSPNOEA);
        initialGraph.addDirectedEdge(Resources.CANCER, Resources.XRAY);

        BNBuilder alg = new GES(initialGraph, Resources.CANCER_DATASET, false);

        Dag_n result = alg.getCurrentDag();
        // Equals is never gonna work. Because tetrad doesn't have a proper equals
        assertEquals(initialGraph.getNodes(), result.getNodes());
        assertEquals(initialGraph.getEdges(), result.getEdges());
        //&assertEquals(initialGraph, result);
        alg.search();
        assertNotNull(alg.getCurrentGraph());
        assertNotNull(alg.getCurrentDag());
        assertNotEquals(initialGraph, alg.getCurrentDag());

    }


}
