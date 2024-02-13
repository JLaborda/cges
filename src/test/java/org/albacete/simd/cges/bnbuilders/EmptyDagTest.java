package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.clustering.RandomClustering;
import org.albacete.simd.cges.threads.GESThread;
import org.junit.Test;
import static org.junit.Assert.*;

public class EmptyDagTest {

    @Test
    public void testSearch() {
        // Create an Empty object and a CGES object to compare
        EmptyDag emptyDag = new EmptyDag(Resources.ALARM_BBDD_PATH);
        CGES cges = new CGES(Resources.ALARM_BBDD_PATH, new RandomClustering(42), 4, CGES.Broadcasting.NO_BROADCASTING);
        // Execute the search method
        Graph resultEmpty = emptyDag.search();
        Graph resultCGES = cges.search();

        // Compare results
        assertNotNull(resultEmpty);
        assertNotNull(resultCGES);
        assertTrue(GESThread.scoreGraph(resultEmpty, emptyDag.getProblem()) <= GESThread.scoreGraph(resultCGES, cges.getProblem()));

    }
}

