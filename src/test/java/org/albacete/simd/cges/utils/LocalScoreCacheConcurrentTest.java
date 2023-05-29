package org.albacete.simd.cges.utils;

public class LocalScoreCacheConcurrentTest {
/*
    @Test
    public void constructorTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();
        assertNotNull(cache);
    }

    @Test
    public void addingAndGettingTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();

        // Adding new element into cache
        int [] parents = {1,2,3,4,5};
        cache.add(0, parents, 5);
        assertEquals(5, cache.get(0, parents),0.000001);

        // Adding the same element again
        cache.add(0, parents, 5);
        assertEquals(5, cache.get(0, parents),0.000001);

        // Changing the score of the previous element and adding it to see if it has been modified
        cache.add(0, parents, 7);
        assertEquals(7, cache.get(0, parents),0.000001);

        // Getting the same element, but with repeated parents
        int [] parents2 = {1,1,2,3,4,5};
        assertEquals(7, cache.get(0, parents2),0.000001);

        // Adding new element with repeated parents and getting it with non-repeating parents
        int [] parents3 = {1,2,3,3};
        int [] parents4 = {1,2,3};
        cache.add(0, parents3, 5);
        assertEquals(5, cache.get(0, parents4),0.000001);

        // Getting an element with a new int[] with the same values.
        int [] parents5 = {1,2,3};
        assertEquals(5, cache.get(0, parents5),0.000001);


    }

    @Test
    public void clearTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();
        int [] parents = {1,2,3,4,5};
        cache.add(0, parents, 5);

        cache.clear();

        double score = cache.get(0,parents);

        assertEquals(Double.NaN, score, 0.000001);
    }

    @Test
    public void toStringTest(){
        LocalScoreCacheConcurrent cache = new LocalScoreCacheConcurrent();
        int [] parents = {1,2,3,4,5};
        cache.add(0, parents, 5);

        String result = cache.toString();
        String expected = "LocalScoreCacheConcurrent{map={(0, [1, 2, 3, 4, 5])=5.0}}";

        assertEquals(expected, result);

    }*/

}
