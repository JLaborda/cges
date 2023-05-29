package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Edge;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.bnbuilders.HillClimbingSearch;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class BNBuilderTest {

    @Before
    public void restartMeans(){
        BackwardStage.meanTimeTotal = 0;
        ForwardStage.meanTimeTotal = 0;
    }

    @Test
    public void settersAndGettersTest(){
        String path = Resources.CANCER_BBDD_PATH;
        BNBuilder algorithm = new HillClimbingSearch(path, 15, 5);
        Problem problem = algorithm.getProblem();
        Set<Edge> arcs = Utils.calculateArcs(problem.getData());

        algorithm.setSeed(30);
        algorithm.setMaxIterations(30);
        algorithm.setnItInterleaving(20);


        assertEquals(30, algorithm.getSeed());
        assertEquals(arcs, algorithm.getSetOfArcs());
        assertTrue(algorithm.getSubSets().isEmpty());
        assertEquals(problem.getData(), algorithm.getData());
        assertEquals(30, algorithm.getMaxIterations());
        assertNull(algorithm.getGraphs());
        assertEquals(20,algorithm.getItInterleaving());
        assertNull(algorithm.getCurrentGraph());
        assertEquals(1, algorithm.getIterations());
        assertEquals(problem, algorithm.getProblem());
        assertEquals(0, algorithm.getnThreads());


    }
}
