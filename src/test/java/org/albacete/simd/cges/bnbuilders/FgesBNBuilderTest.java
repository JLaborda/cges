package org.albacete.simd.cges.bnbuilders;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.Resources;
import org.junit.Test;
import static org.junit.Assert.*;

public class FgesBNBuilderTest {

    @Test
    public void testSearchWithFges() {
        // Create an instance of the Fges_BNBuilder class with ges=true
        FGES builder = new FGES(Resources.ALARM_BBDD_PATH, false, true);

        // Execute the search() method
        Graph result = builder.search();

        // Verify if a valid graph has been returned
        assertNotNull(result);

    }

    @Test
    public void testSearchWithFges2() {
        // Create an instance of the Fges_BNBuilder class with ges=false
        FGES builder = new FGES(Resources.ALARM_BBDD_PATH, true, false);

        // Execute the search() method
        Graph result = builder.search();

        // Verify if a valid graph has been returned
        assertNotNull(result);

    }
}

