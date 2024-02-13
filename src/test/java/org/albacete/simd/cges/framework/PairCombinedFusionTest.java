package org.albacete.simd.cges.framework;
import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.bnbuilders.CircularProcess;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class PairCombinedFusionTest {

    @Test
    public void emptyGraphsFusionTest() throws InterruptedException {
        Problem problem = new Problem(Resources.ALARM_BBDD_PATH);
        Graph currentGraph = new Dag_n(problem.getVariables());
        ArrayList<Dag_n> graphs = new ArrayList<>();

        PairCombinedFusion fusion = new PairCombinedFusion(problem, currentGraph, graphs);
        Dag_n result = fusion.fusion();

        assertNull(result);
    }


    @Test
    public void normalBehaviorFusionTest() throws InterruptedException {
        Problem problem = new Problem(Resources.ALARM_BBDD_PATH);
        Set<Edge> edges = Utils.calculateArcs(problem.getData());
        List<Edge> edgesList = new ArrayList<>(edges);
        // Setting currentGraph
        Dag_n currentGraph = new Dag_n(problem.getVariables());
        for (int i = 0; i < 20; i+=2) {
            currentGraph.addEdge(new Edge(edgesList.get(i)));
        }

        ArrayList<Dag_n> graphs = new ArrayList<>();
        Dag_n graph1 = new Dag_n(problem.getVariables());
        for (int i = 1; i < 40; i+=2) {
            graph1.addEdge(new Edge(edgesList.get(i)));
        }
        graphs.add(graph1);

        Dag_n graph2 = new Dag_n(problem.getVariables());
        for (int i = 23; i < 70; i+=2) {
            graph2.addEdge(new Edge(edgesList.get(i)));
        }
        graphs.add(graph2);

        ArrayList<Dag_n> graphsCheck = new ArrayList<>();
        graphsCheck.add(graph1);
        graphsCheck.add(currentGraph);
        ConsensusUnion fuseCheck = new ConsensusUnion(graphsCheck);
        Dag_n fusion1 = fuseCheck.union();
        double complexity1 = fuseCheck.getNumberOfInsertedEdges();

        graphsCheck = new ArrayList<>();
        graphsCheck.add(graph2);
        graphsCheck.add(currentGraph);
        fuseCheck = new ConsensusUnion(graphsCheck);
        Dag_n fusion2 = fuseCheck.union();
        double complexity2 = fuseCheck.getNumberOfInsertedEdges();

        Dag_n expectedFusion;
        if(complexity1 < complexity2)
            expectedFusion = fusion1;
        else
            expectedFusion = fusion2;

        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, expectedFusion, expectedFusion.getEdges());
        bes.run();
        expectedFusion = CircularProcess.transformPDAGtoDAG(bes.getCurrentGraph());

        // Act
        PairCombinedFusion fusion = new PairCombinedFusion(problem, currentGraph, graphs);
        Dag_n result = fusion.fusion();

        assertNotNull(result);
        assertEquals(expectedFusion, result);
    }

    @Test
    public void currentGraphNotFused() throws InterruptedException {
        Problem problem = new Problem(Resources.ALARM_BBDD_PATH);
        Set<Edge> edges = Utils.calculateArcs(problem.getData());
        List<Edge> edgesList = new ArrayList<>(edges);
        // Setting currentGraph
        Dag_n currentGraph = new Dag_n(problem.getVariables());
        for (int i = 0; i < 20; i+=2) {
            currentGraph.addEdge(new Edge(edgesList.get(i)));
        }
        ArrayList<Dag_n> graphs = new ArrayList<>();
        graphs.add(currentGraph);

        PairCombinedFusion fusion = new PairCombinedFusion(problem, currentGraph, graphs);
        Dag_n result = fusion.fusion();

        assertNull(result);
    }


}
