/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs;

import eu.ddmore.converters.unipv.winbugs.parts.TaskParameters;
import eu.ddmore.converters.unipv.winbugs.parts.WinBugsParametersName;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.so.unipv.winbugs.parts.IndexElement;
import eu.ddmore.so.unipv.winbugs.parts.OutputParameters;
import eu.ddmore.so.unipv.winbugs.parts.Parameter;
import eu.ddmore.so.unipv.winbugs.parts.Sample;
import eu.ddmore.so.unipv.winbugs.parts.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class CodaLoader {

    public static final double[] perc = {2.5, 5, 25, 50, 75, 95, 97.5};
    public static final String resName = "residual";
    public static final String predName = "pred";

    public static String indexSuffix = "Index";
    public static String codaPrefix = "output";
    private static String codaFilePath;
    private List<File> dataFile;
    private File indexFile;
    private OutputParameters popPar;
    private OutputParameters indivPar;
    private OutputParameters resPar;
    private OutputParameters predPar;
    private Map<String, Map<Integer, Parameter>> indivMap;
    private Map<String, Map<Integer, Map<Integer, Parameter>>> residMap;
    private Map<String, Map<Integer, Map<Integer, Parameter>>> predMap;
    private String modelName;
    private String solver, model;
    private String update, nchains, burnIn;
    private Long time;
    private List<String> residual;
    private List<String> prediction;
    private List<String> popList;

    public CodaLoader(String inP, String name) throws FileNotFoundException, IOException {
        this(inP);
        this.modelName = name;
    }

    public CodaLoader(String inP) throws FileNotFoundException, IOException {
        this.codaFilePath = inP;
        popPar = new OutputParameters();
        resPar = new OutputParameters();
        indivPar = new OutputParameters();
        predPar = new OutputParameters();
        indivMap = new HashMap<>();
        residMap = new HashMap<>();
        predMap = new HashMap<>();
        popList = new ArrayList<>();
        init();
    }

    
    private Integer getIndex(String label) {
        if(label.contains(","))
            return null;
        String s = label.substring(label.indexOf("[") + 1, label.indexOf("]"));
        return new Integer(s);
    }

    public void setPopList(List<String> popList) {
        this.popList = popList;
    }

    private Integer getIndex(String label, char pos) {
        String s = "";
        switch (pos) {
            case 's':
                s = label.substring(label.indexOf("[") + 1, label.indexOf(","));
                break;
            case 't':
                s = label.substring(label.indexOf(",") + 1, label.indexOf("]"));
                break;
        }
        return new Integer(s);
    }

    private void updateIndivMap(Map<String, Map<Integer, Parameter>> m, OutputParameters pars) {
        for (Map.Entry<String, Parameter> p : pars.getPars().entrySet()) {
            Map<Integer, Parameter> map = m.get(p.getValue().getSymbId());
            if (map == null) {
                map = new HashMap<>();
            }
            if (getIndex(p.getKey()) != null) {
                map.put(getIndex(p.getKey()), p.getValue());
                m.put(p.getValue().getSymbId(), map);
            }
        }
    }

    private void updateIndivTimeMap(Map<String, Map<Integer, Map<Integer, Parameter>>> m, OutputParameters pars) {
        for (Map.Entry<String, Parameter> p : pars.getPars().entrySet()) {
            Map<Integer, Map<Integer, Parameter>> map = m.get(p.getValue().getSymbId());
            if (map == null) {
                map = new HashMap<>();
                Map<Integer, Parameter> pList = new HashMap<>();
            }
            m.put(p.getValue().getSymbId(), map);
        }
    }

    public Map<String, Map<Integer, Parameter>> getIndivMap() {
        return indivMap;
    }

    public Map<String, Map<Integer, Map<Integer, Parameter>>> getResidMap() {
        return residMap;
    }

    public Map<String, Map<Integer, Map<Integer, Parameter>>> getPredMap() {
        return predMap;
    }

    private void updateMaps() {
        updateIndivMap(indivMap, indivPar);
        updateIndivTimeMap(predMap, predPar);
    }

    public String getOdeSolver() {
        return solver;
    }

    public String getNChains() {
        return nchains;
    }

    public String getUpdate() {
        return update;
    }

    public OutputParameters getPopPar() {
        return popPar;
    }

    public OutputParameters getPredPar() {
        return predPar;
    }

    public OutputParameters getResPar() {
        return resPar;
    }

    public OutputParameters getIndivPar() {
        return indivPar;
    }

    public int getPopParNum() {
        return popPar.getPars().size();
    }

    public double[] getVals(OutputParameters par, String name) {
        return par.getPars().get(name).getArrayValues();
    }

    public List<String> getCodaNames() {
        List<String> list = new ArrayList<>();
        for (File f : dataFile) {
            if (!f.isDirectory()) {
                list.add(f.getName());
            }
        }
        return list;
    }

    public void loadData() throws FileNotFoundException, IOException {
        BufferedReader fIndex = new BufferedReader(new FileReader(indexFile));
        String line;

        for (File codaF : dataFile) {
            BufferedReader fData = new BufferedReader(new FileReader(codaF));
            int i, n;
            List<Sample> values;
            while (fIndex.ready()) {
                line = fIndex.readLine();
                String[] f = line.split("\t");

                IndexElement el = new IndexElement(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2]));
                n = el.getSamplesNumber();

                el.setType(popList, residual, prediction);
                i = 0;
                if (!el.getType().equals("INDIV")
                        && !el.getType().equals("POP")) {
                    while (i < n && fData.ready()) {
                        line = fData.readLine();
                        i++;
                    }
                    continue;
                }
                Parameter pp = new Parameter();
                pp.setName(el.getName(), popList, residual, prediction);
                values = new ArrayList<>();
                while (i < n && fData.ready()) {
                    line = fData.readLine();
                    f = line.split("\t");
                    values.add(new Sample(Double.parseDouble(f[1])));
                    i++;
                }
                pp.setValues(values);
                if (Util.isInList(this.residual, Util.getName(pp.getName()))) {
                    resPar.putParameter(pp.getName(), pp);
                } else if (Util.isInList(this.prediction, Util.getName(pp.getName()))) {
                    predPar.putParameter(pp.getName(), pp);
                } else if (pp.getcType().equals(ColumnType.POP_PARAMETER)) {
                    popPar.putParameter(el.getName(), pp);
                } else if (pp.getcType().equals(ColumnType.INDIV_PARAMETER)) {
                    indivPar.putParameter(el.getName(), pp);
                }
            }
        }

        updateMaps();
    }

    public void setInPath(String inPath) {
        this.codaFilePath = inPath;
    }

    public void statistics(OutputParameters parameters) {
        for (Map.Entry<String, Parameter> pars : parameters.getPars().entrySet()) {
            Parameter v = pars.getValue();
            System.out.println(v.getName() + "\n" + v.statistics());
        }
    }

    public String getModelName() {
        return modelName;
    }

    private void decriptiveStatistics(String name, double[] v) {
        System.out.println("\n-- " + name + " --");
        System.out.println(" mean: " + StatUtils.mean(v));

        for (double pe : perc) {
            System.out.println(" percentile " + pe + ": " + StatUtils.percentile(v, pe));
        }
    }

    public String getInPath() {
        return codaFilePath;
    }

    public Long getTime() {
        return time;
    }

    private void loadParameters(FileReader in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        this.solver = props.getProperty(WinBugsParametersName.ODE_SOLVER);
        this.update = props.getProperty(WinBugsParametersName.N_ITER);
        this.nchains = props.getProperty(WinBugsParametersName.N_CHAINS);
        this.burnIn = props.getProperty(WinBugsParametersName.BURN_IN);
        this.residual = getParList(props.getProperty(WinBugsParametersName.RES_NAME, ""));
        this.prediction = getParList(props.getProperty(WinBugsParametersName.PRED_NAME, ""));
        if (props.getProperty(WinBugsParametersName.TIME) != null) {
            try {
                this.time = Long.parseLong(props.getProperty("time"));
            } catch (NumberFormatException e) {
                this.time = null;
            }
        }
        this.model = props.getProperty(WinBugsParametersName.MODEL_NAME);
        if (this.model == null) {
            this.model = WinBugsParametersName.MODEL_NAME_DEFAULT;
        }
    }

    private void loadParametersNew(FileReader in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        // parameters from mdl TaskObject
        this.solver = props.getProperty(TaskParameters.ODE_SOLVER.label());
        this.update = props.getProperty(TaskParameters.N_ITER.label());
        this.nchains = props.getProperty(TaskParameters.N_CHAINS.label());
        this.burnIn = props.getProperty(TaskParameters.BURN_IN.label());
        this.model = props.getProperty(TaskParameters.MODEL_NAME.label());

        // parameters from converter
        this.residual = getParList(props.getProperty(TaskParameters.RES_NAME.label(), TaskParameters.RES_NAME.defaultVal()));
        this.prediction = getParList(props.getProperty(TaskParameters.PRED_NAME.label(), TaskParameters.PRED_NAME.defaultVal()));
        if (props.getProperty(TaskParameters.TIME.label()) != null) {
            try {
                this.time = Long.parseLong(props.getProperty(TaskParameters.TIME.label()));
            } catch (NumberFormatException e) {
                this.time = null;
            }
        }
    }

    public String getModel() {
        return model;
    }

    private List<String> getParList(String list) {
        List<String> out = new ArrayList<>();
        String[] el = list.split(",");
        for (String s : el) {
            if (s.trim().length() > 0) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * @param args the command line arguments
     */
    private void init() throws IOException {
        residual = new ArrayList<>();
        prediction = new ArrayList<>();
        loadParameters(new FileReader(new File(codaFilePath + "/SO.properties")));
        File dataFileList[] = null;
        File inDir = new File(codaFilePath);
        if (inDir.exists()) {
            dataFileList = inDir.listFiles();
        }

        FilenameFilter indexFN = getIndexFileFilter();
        File[] a = inDir.listFiles(indexFN);
        if (inDir.listFiles(indexFN).length != 1) {
            throw new InvalidPathException(codaFilePath, "No index or more than one index file in the directory");
        }
        this.indexFile = inDir.listFiles(indexFN)[0];
        System.out.println("index file = " + this.indexFile.getAbsolutePath());
        dataFile = new ArrayList<>();
        for (File n : dataFileList) {
            if (!n.isDirectory()) {
                if (!n.equals(this.indexFile) && n.getName().startsWith(codaPrefix)) {
                    dataFile.add(n);
                    System.out.println("data file " + n.getAbsolutePath());
                }
            }
        }
    }

    public File getIndexFile() {
        return indexFile;
    }

    private static FilenameFilter getIndexFileFilter() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.contains(indexSuffix)) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        return filter;
    }

    private static FilenameFilter getCodaFile() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(codaPrefix) && !name.contains(indexSuffix)) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        return filter;
    }
}
