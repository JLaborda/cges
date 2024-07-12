package org.albacete.simd.cges.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.GraphNode;
import org.junit.Test;
import static org.junit.Assert.*;


public class EdgeSearchTest {

    @Test
    public void constructorTest(){
        EdgeSearch e = new EdgeSearch(0, new SubSet(), null);
        assertNotNull(e);
    }

    @Test
    public void compareTest(){
        EdgeSearch e1 =new EdgeSearch(1, null, null);
        EdgeSearch e2 = new EdgeSearch(2, null, null);
        EdgeSearch e3 =new EdgeSearch(1, null, null);

        assertEquals(-1, e1.compareTo(e2));
        assertEquals(1, e2.compareTo(e1));
        assertEquals(0, e1.compareTo(e3));
    }

    @Test
    public void equalTest(){
        EdgeSearch e1 =new EdgeSearch(1, null, null);
        Edge edge = new Edge(new GraphNode("A"), new GraphNode("B"), Endpoint.TAIL, Endpoint.ARROW);
        EdgeSearch e2 = new EdgeSearch(2, null, edge);
        EdgeSearch e4 = new EdgeSearch(4, null, edge);

        assertNotEquals(e1,e2);
        assertEquals(e1,e1);
        assertEquals(e2,e4);
        assertNotEquals(e1, new Object());
    }

    @Test
    public void hashCodeTest(){
        Edge edge = new Edge(new GraphNode("A"), new GraphNode("B"), Endpoint.TAIL, Endpoint.ARROW);
        EdgeSearch e1 =new EdgeSearch(1, null, edge);
        assertEquals(edge.hashCode(),e1.hashCode());
    }

}
