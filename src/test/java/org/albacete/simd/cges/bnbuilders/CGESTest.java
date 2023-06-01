package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.Resources;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.clustering.HierarchicalClustering;
import org.albacete.simd.cges.utils.Utils;
import org.junit.Test;
import static org.junit.Assert.*;

public class CGESTest {

    @Test
    public void testSearchWithConvergence1() {
        // Create an instance of the CGES class
        Clustering clustering = new HierarchicalClustering();
        int nThreads = 4;
        int nItInterleaving = 100000;
        CGES builder = new CGES(Resources.ALARM_BBDD_PATH, clustering, nThreads, nItInterleaving, "c1");

        // Execute the search() method
        Graph result = builder.search();

        // Verify if a valid graph has been returned
        assertNotNull(result);
    }

    @Test
    public void testSearchWithConvergence2() {
        // Create an instance of the CGES class
        Clustering clustering = new HierarchicalClustering();
        int nThreads = 4;
        int nItInterleaving = 100000;
        CGES builder = new CGES(Resources.ALARM_BBDD_PATH, clustering, nThreads, nItInterleaving, "c2");

        // Execute the search() method
        Graph result = builder.search();

        // Verify if a valid graph has been returned
        assertNotNull(result);
    }



    // Add more test cases for the other methods in the CGES class
}
