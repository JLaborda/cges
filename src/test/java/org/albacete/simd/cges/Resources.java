package org.albacete.simd.cges;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeEqualityMode;
import org.albacete.simd.cges.utils.Utils;

import java.util.Set;

public class Resources {
    // EARTHQUAKE
    public static final String EARTHQUAKE_BBDD_PATH = "./src/test/res/BBDD/earthquake.xbif_.csv";
    public static final String EARTHQUAKE_NET_PATH = "./src/test/res/networks/earthquake.xbif";
    public static final String EARTHQUAKE_TEST_PATH = "./src/test/res/BBDD/tests/earthquake_test.csv";
    public static final DataSet EARTHQUAKE_DATASET = Utils.readData(EARTHQUAKE_BBDD_PATH);

    public static final String ALARM_BBDD_PATH = "./src/test/res/BBDD/alarm.xbif_.csv";
    public static final String ALARM_NET_PATH = "./src/test/res/networks/alarm.xbif";

    // Variables of Earthquake's dataset
    public static final Node ALARM = EARTHQUAKE_DATASET.getVariable("Alarm");
    public static final Node MARYCALLS = EARTHQUAKE_DATASET.getVariable("MaryCalls");
    public static final Node BURGLARY = EARTHQUAKE_DATASET.getVariable("Burglary");
    public static final Node EARTHQUAKE = EARTHQUAKE_DATASET.getVariable("Earthquake");
    public static final Node JOHNCALLS = EARTHQUAKE_DATASET.getVariable("JohnCalls");

    // CANCER
    public static final String CANCER_BBDD_PATH = "./src/test/res/BBDD/cancer.xbif_.csv";
    public static final String CANCER_NET_PATH = "./src/test/res/networks/cancer.xbif";
    public static final String CANCER_TEST_PATH = "./src/test/res/BBDD/tests/cancer_test.csv";
    public static final DataSet CANCER_DATASET = Utils.readData(CANCER_BBDD_PATH);

    // Variables of Cancer's dataset
    public static final Node XRAY = CANCER_DATASET.getVariable("Xray");
    public static final Node DYSPNOEA = CANCER_DATASET.getVariable("Dyspnoea");
    public static final Node CANCER = CANCER_DATASET.getVariable("Cancer");
    public static final Node POLLUTION = CANCER_DATASET.getVariable("Pollution");
    public static final Node SMOKER = CANCER_DATASET.getVariable("Smoker");

    public static boolean equalsEdges(Set<Edge> expected, Set<Edge> result) {
        boolean assertion = false;
        NodeEqualityMode.setEqualityMode(NodeEqualityMode.Type.NAME);
        for (Edge resEdge : result) {
            for (Edge expEdge : expected) {
                if (expEdge.equals(resEdge)) {
                    assertion = true;
                    break;
                }
            }
            if (!assertion)
                return false;
        }
        return assertion;
    }

}
