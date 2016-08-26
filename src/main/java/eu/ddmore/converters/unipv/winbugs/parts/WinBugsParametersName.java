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
public class WinBugsParametersName {

    public static final String ODE_SOLVER = "odesolver";
    public static final String ODE_SOLVER_DEFAULT = "RK45";

    public static final String N_ITER = "niter";
    public static final String N_ITER_DEFAULT = "5000";

    public static final String N_CHAINS  = "nchains";
    public static final String N_CHAINS_DEFAULT  = "1";

    public static final String BURN_IN= "burnin";
    public static final String BURN_IN_DEFAULT="1000";
    
    public static final String RES_NAME=  "residual";// dovrebbe essere resname
    public static final String PRED_NAME= "pred";// dovrebbe essere predname
    public static final String TIME= "time";
    public static final String MODEL_NAME= "model";
    public static final String MODEL_NAME_DEFAULT= "model";
    public static final String PARAMETERS="parameters";
    
    public static final String THIN_UPDATER="thinupdater";
    public static final String THIN_UPDATER_DEFAULT ="1"; 
    
    public static final String THIN_SAMPLES= "thinsamples";
    public static final String THIN_SAMPLES_DEFAULT= "1";
    
    public static final String DIC= "dic";
    public static final String DIC_DEFAULT="false";
    
    public static final String WINBUGSGUI= "winbugsgui";
    public static final String WINBUGSGUI_DEFAULT= "false";
    
    public static final String N_COV="NUM1";
    
    public static final String N_COV_DEFAULT="0";
    public static final String N_GRID= "NUM2";

}
