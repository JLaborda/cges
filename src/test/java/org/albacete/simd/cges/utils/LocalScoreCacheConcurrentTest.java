package org.albacete.simd.cges.utils;

import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LocalScoreCacheConcurrentTest {

    @Test
    public void constructorTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();
        assertNotNull(cache);
    }

    @Test
    public void addingAndGettingTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();

        // Adding new element into cache
        Set<Node> parents = createParents(new int[]{1,2,3,4,5});
        cache.add(0, parents, 5);
        assertEquals(5, cache.get(0, parents),0.000001);

        // Adding the same element again
        cache.add(0, parents, 5);
        assertEquals(5, cache.get(0, parents),0.000001);

        // Changing the score of the previous element and adding it to see if it has been modified
        cache.add(0, parents, 7);
        assertEquals(7, cache.get(0, parents),0.000001);

        // Getting the same element, but with repeated parents
        int [] parentsIndex2 = {1,1,2,3,4,5};
        Set<Node> parents2 = createParents(parentsIndex2);
        assertEquals(7, cache.get(0, parents2),0.000001);

        // Adding new element with repeated parents and getting it with non-repeating parents
        int [] parentsIndex3 = {1,2,3,3};
        int [] parentsIndex4 = {1,2,3};
        Set<Node> parents3 = createParents(parentsIndex3);
        Set<Node> parents4 = createParents(parentsIndex4);
        cache.add(0, parents3, 5);
        assertEquals(5, cache.get(0, parents4),0.000001);

        // Getting an element with a new int[] with the same values.
        int [] parentsIndex5 = {1,2,3};
        Set<Node> parents5 = createParents(parentsIndex5);
        assertEquals(5, cache.get(0, parents5),0.000001);


    }

    @Test
    public void clearTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();
        int [] parentsIndex = {1,2,3,4,5};
        Set<Node> parents = createParents(parentsIndex);
        cache.add(0, parents, 5);

        cache.clear();

        double score = cache.get(0,parents);

        assertEquals(Double.NaN, score, 0.000001);
    }

    @Test
    public void toStringTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();
        int [] parentsIndex = {1,2,3,4,5};
        Set<Node> parents = createParents(parentsIndex);
        cache.add(0, parents, 5);

        String result = cache.toString();
        String expected = "LocalScoreCacheConcurrent{map={(0, [1, 2, 3, 4, 5])=5.0}}";

        assertEquals(expected, result);

    }

    public Set<Node> createParents(int[] indexes){
        Set<Node> parents = new HashSet<>();
        for (int i = 0; i < indexes.length; i++) {
            parents.add(new GraphNode(String.valueOf(indexes[i])));
        }
        return parents;
    }

}
