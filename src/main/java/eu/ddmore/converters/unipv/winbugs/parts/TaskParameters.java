/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs.parts;

/**
 *
 * @author cristianalarizza
 */
public enum TaskParameters {

    WINBUGS_GUI("winbugsgui", "false"),
    ODE_SOLVER("odesolver", "RK45"),
    BURN_IN("burnin", "100"),
    THIN_UPDATER("thinupdater","1"),
    THIN_SAMPLES("thinsamples","1"),
    DIC("dic","false"),
    N_ITER("niter", "1"),
    N_CHAINS("nchains", "1"),
    RES_NAME("resname", ""),
    PRED_NAME("predname", ""),
    TIME("time"),
    MODEL_NAME("model", "model"),
    PARAMETERS("parameters", ""),
    N_COV("NUM1", "0"),
    N_GRID("NUM2");

    private final String label;   // in kilograms
    private final String defaultVal; // in meters

    TaskParameters(String label, String defaultVal) {
        this.label = label;
        this.defaultVal = defaultVal;
    }

    TaskParameters(String label) {
        this(label, "");
    }

    public String label() {
        return label;
    }

    public String defaultVal() {
        return defaultVal;
    }

    public static void main(String[] args) {

        String t = "burnin";
        if (t.equals(TaskParameters.BURN_IN.label)) {

        }
    }

}
