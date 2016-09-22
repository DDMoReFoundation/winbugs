/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.StringValue;
import eu.ddmore.libpharmml.dom.commontypes.SymbolType;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.DataSetTable;
import eu.ddmore.libpharmml.dom.dataset.DatasetRow;
import eu.ddmore.libpharmml.dom.dataset.ExternalFile;
import eu.ddmore.libpharmml.dom.dataset.ExternalFile.Delimiter;
import eu.ddmore.libpharmml.dom.dataset.HeaderColumnsDefinition;
import eu.ddmore.libpharmml.dom.probonto.DistributionName;
import eu.ddmore.libpharmml.dom.probonto.ProbOnto;
import eu.ddmore.libpharmml.so.SOFactory;
import eu.ddmore.libpharmml.so.StandardisedOutputResource;
import eu.ddmore.libpharmml.so.dom.Distribution;
import eu.ddmore.libpharmml.so.dom.Estimation;
import eu.ddmore.libpharmml.so.dom.IndividualEstimates;
import eu.ddmore.libpharmml.so.dom.Message;
import eu.ddmore.libpharmml.so.dom.OFMeasures;
import eu.ddmore.libpharmml.so.dom.PopulationEstimates;
import eu.ddmore.libpharmml.so.dom.PrecisionIndividualEstimates;
import eu.ddmore.libpharmml.so.dom.PrecisionPopulationEstimates;
import eu.ddmore.libpharmml.so.dom.Residual;
import eu.ddmore.libpharmml.so.dom.SOBlock;
import eu.ddmore.libpharmml.so.dom.SOTableDistrib;
import eu.ddmore.libpharmml.so.dom.StandardisedOutput;
import eu.ddmore.libpharmml.so.dom.TaskInformation;
import eu.ddmore.libpharmml.so.impl.LibSO;
import eu.ddmore.libpharmml.so.impl.SOVersion;
import eu.ddmore.so.unipv.winbugs.CodaLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * @author cristiana
 */
public class SOGenerator {

    private StandardisedOutput so;
    private static final String HEADER = "UNIPV WinBUGS Converter";
    private static final String TOOL = "WINBUGS";
    public static final String indivIdPrefix = "ind";
    public static final String SEP = ",";
    public static final Delimiter DELIM = ExternalFile.Delimiter.COMMA;

    private CodaLoader dl;
    private LibSO libSO;
    private SOBlock block;
    private StandardisedOutputResource soResource;
    private static SOVersion version = SOVersion.v0_3_1;
    private Object outPar;

    public SOGenerator(String id, CodaLoader dl) {
        SOFactory soFactory = SOFactory.getInstance();
        libSO = soFactory.createLibSO();
        soResource = libSO.createDom(version);
        this.so = soResource.getDom();
        so.setId(id);
        so.setImplementedBy("WinBUGS converter");
        so.createPharmMLRef("pharmml_model.xml");
        this.dl = dl;
    }

    public SOBlock createSOBlock(String bId) {
        block = so.createSoBlock();
        block.setBlkId(bId);
        return block;
    }

    public void createRawResults(String codaIndexName, List<String> codaNames) throws IOException {
        if (block.getRawResults() == null) {
            block.createRawResults();
        }
        ExternalFile fI = Util.createEF("codaIndex", "outIndex", DELIM);
        DataSet dsI = block.getRawResults().createDataFile();
        dsI.setExternalFile(fI);

        int i = 1;
        for (String n : codaNames) {
            ExternalFile f = Util.createEF(n, "output" + i++, DELIM);
            DataSet ds = block.getRawResults().createDataFile();
            ds.setExternalFile(f);
        }
    }

    public void createRawResults(String codaRootName) throws IOException {
        block.createRawResults();
        ExternalFile fI = Util.createEF(dl.getIndexFile().getName(), codaRootName + CodaLoader.indexSuffix, ExternalFile.Delimiter.TAB);
        DataSet dsI = block.getRawResults().createDataFile();
        dsI.setExternalFile(fI);

        int i = 1;
        for (String n : dl.getCodaNames()) {
            ExternalFile f = Util.createEF(n, "output" + i++, ExternalFile.Delimiter.TAB);
            DataSet ds = block.getRawResults().createDataFile();
            ds.setExternalFile(f);
        }
    }

    public TaskInformation createTaskInformation(String message) {
        block.createTaskInformation();
        TaskInformation ti = block.getTaskInformation();
        Message msg = new Message();
        msg.setToolname(new StringValue(TOOL));
        msg.setName(new StringValue("winbugs_message"));
        msg.setType("INFORMATION");
        ti.setNumberChains(new IntValue(Integer.parseInt(dl.getNChains())));
        ti.setRunTime(new RealValue((System.currentTimeMillis() - dl.getTime()) / 6000));
        ti.setNumberIterations(new IntValue(Integer.parseInt(dl.getUpdate())));
        ti.setOutputFilePath(new ExternalFile());
        ti.getOutputFilePath().setOid("oidFP");
        ti.createMessage(TOOL, "winbugs_message", message, 1, "INFORMATION");
        return block.getTaskInformation();
    }

    public SOBlock getBlock() {
        return block;
    }

    public Estimation createEstimation() {
        block.createEstimation();
        return block.getEstimation();
    }

    public Residual createResiduals(String name) throws IOException {

        DataSet ds = createDSDef(dl.getPopPar().getPars(), ValueType.RESIDUAL, name);
        block.getEstimation().createResiduals().createResidualTable();
        return block.getEstimation().getResiduals();
    }

    public OFMeasures createLikelihood(String name) {
        block.getEstimation().createOFMeasures().createLikelihood(Double.NaN);
        return block.getEstimation().getOFMeasures();
    }

    public IndividualEstimates createIndividualEstimates() throws IOException {
        if (block.getEstimation() == null) {
            block.createEstimation();
        }
        IndividualEstimates ie = block.getEstimation().createIndividualEstimates();
        IndividualEstimates.Estimates iee = ie.createEstimates();

        DataSet ieeMean = iee.createMean();
        ieeMean.setTable(createIndivDataTable(ValueType.MEAN));
        ieeMean.setDefinition(headersIndivDef(true, dl.getIndivMap()));

        DataSet ieeMedian = iee.createMedian();
        ieeMedian.setTable(createIndivDataTable(ValueType.MEDIAN));
        ieeMedian.setDefinition(headersIndivDef(true, dl.getIndivMap()));

        return ie;
    }

    public DataSet createPredictions(String name) throws IOException {
        if (block.getEstimation() == null) {
            block.createEstimation();
        }

        DataSet ds = createDSDef(dl.getPopPar().getPars(), ValueType.POSTERIORMEDIAN, name);
        block.getEstimation().setPredictions(ds);
        return block.getEstimation().getPredictions();
    }

    public PopulationEstimates createPopulationEstimates(String name) throws IOException {
        if (block.getEstimation() == null) {
            block.createEstimation();
        }

        Map<String, Parameter> pars = dl.getPopPar().getPars();
        PopulationEstimates pe = block.getEstimation().createPopulationEstimates();

        List<String[]> vals;
        DataSet ds = new DataSet();
        ds.setDefinition(headersDef(false, pars));
        vals = Util.getMean(pars);
        ds = Util.addRows(vals, ds);
        pe.createBayesian().setPosteriorMean(ds);
        ds = new DataSet();
        ds.setDefinition(headersDef(false, pars));
        vals = Util.getMedian(pars);
        ds = Util.addRows(vals, ds);
        pe.getBayesian().setPosteriorMedian(ds);
        return block.getEstimation().getPopulationEstimates();
    }

    public PopulationEstimates createPrecisionPopulationEstimates(String name) throws IOException {
        if (block.getEstimation() == null) {
            block.createEstimation();
        }

        Map<String, Parameter> pars = dl.getPopPar().getPars();
        if (pars.isEmpty()) {
            return null;
        }

        PrecisionPopulationEstimates ppe = block.getEstimation().createPrecisionPopulationEstimates();
        ppe.createBayesian().setStandardDeviation(createDSDef(pars, ValueType.SD, null, "SDP"));

        SOTableDistrib td = new SOTableDistrib();
        Distribution dist = new Distribution();
        td.setDistribution(dist);
        ProbOnto distP = td.getDistribution().createProbOnto(DistributionName.RANDOM_SAMPLE);
        distP.setDataSet(createDSDef(pars, ValueType.VALUE, name));
        ppe.getBayesian().setPosteriorDistribution(td);
        ppe.getBayesian().setPercentilesCI(createDSDef(pars, ValueType.PERC, null, "SDP"));
        return block.getEstimation().getPopulationEstimates();
    }

    ColumnType getColumnType(String type) {
        ColumnType ret = ColumnType.valueOf(ColumnType.class, type);
        return ret;
    }

    HeaderColumnsDefinition headersDef(boolean id, Map<String, Parameter> pars) {
        int num = 1;
        HeaderColumnsDefinition def = new HeaderColumnsDefinition();
        if (id) {
            def.createColumnDefinition("ID", ColumnType.ID, SymbolType.REAL, num++);
        }
        for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
            def.createColumnDefinition(outPar.getKey(), outPar.getValue().getcType(), SymbolType.REAL, num++);
        }
        return def;
    }

    HeaderColumnsDefinition headersIndivPrecisionDef() {
        int num = 1;
        HeaderColumnsDefinition def = new HeaderColumnsDefinition();

        def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, num++);
        def.createColumnDefinition("Parameter", ColumnType.INDIV_PARAMETER, SymbolType.STRING, num++);
        def.createColumnDefinition("SD", ColumnType.STAT_PRECISION, SymbolType.REAL, num++);

        return def;
    }

    HeaderColumnsDefinition headersIndivPrecisionDistDef() {
        int num = 1;
        HeaderColumnsDefinition def = new HeaderColumnsDefinition();

        def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, num++);
        def.createColumnDefinition("Parameter", ColumnType.INDIV_PARAMETER, SymbolType.STRING, num++);
        def.createColumnDefinition("SD", ColumnType.STAT_PRECISION, SymbolType.REAL, num++);

        return def;
    }

    HeaderColumnsDefinition headersIndivPrecisionPercDef() {
        int num = 1;
        HeaderColumnsDefinition def = new HeaderColumnsDefinition();

        def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, num++);
        def.createColumnDefinition("Percentile", ColumnType.INDIV_PARAMETER, SymbolType.STRING, num++);
        for (String n : getIndivParList()) {
            def.createColumnDefinition(n, ColumnType.INDIV_PARAMETER, SymbolType.REAL, num++);
        }
        return def;
    }

    HeaderColumnsDefinition headersIndivDef(boolean id, Map<String, Map<Integer, Parameter>> pars) {
        int num = 1;
        HeaderColumnsDefinition def = new HeaderColumnsDefinition();
        if (id) {
            def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, num++);
        }
        for (String n : getIndivParList()) {
            def.createColumnDefinition(n, ColumnType.INDIV_PARAMETER, SymbolType.REAL, num++);
        }
        return def;
    }

    private DataSetTable createDataTable(Map<String, Parameter> pars, ValueType type) throws IOException {
        int nRows = 0;
        DataSet ds = new DataSet();
        DataSetTable table = new DataSetTable();
        ds.setTable(table);

        List<String[]> vals = new ArrayList<>();
        int size = 0;
        String[] val = new String[size];
        int k = 0;
        switch (type.getType()) {
            case "VALUE":
                size = pars.size();
                for (int i = 0; i < nRows; i++) {
                    k = 0;
                    val = new String[size];
                    for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                        val[k++] = ("" + outPar.getValue().getArrayValues()[i]);
                    }
                    vals.add(val);
                }
                break;
            case "MEAN":
            case "POSTERIORMEAN":
                size = 2;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[size];
                    val[0] = outPar.getKey();
                    val[1] = ("" + StatUtils.mean(outPar.getValue().getArrayValues()));
                    vals.add(val);
                }
                break;

            case "MEDIAN":
            case "POSTERIORMEDIAN":
                size = 2;
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[size];
                    val[0] = outPar.getKey();
                    val[1] = ("" + StatUtils.percentile(outPar.getValue().getArrayValues(), 50));
                    vals.add(val);
                }

                break;

            case "VAR":
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[size];
                    val[0] = "variance" + k++;
                    val[1] = outPar.getKey();
                    val[2] = ("" + StatUtils.variance(outPar.getValue().getArrayValues()));
                    vals.add(val);
                }

                break;
            case "SD":
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[size];
                    val[0] = outPar.getKey();
                    val[1] = ("" + Math.sqrt(StatUtils.variance(outPar.getValue().getArrayValues())));
                    vals.add(val);
                }

                break;
            case "PERC":
                k = 2;
                for (double pe : CodaLoader.perc) {
                    val = new String[size];
                    val[0] = "" + pe;
                    int i = 1;
                    for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {

                        val[i++] = ("" + StatUtils.percentile(outPar.getValue().getArrayValues(), pe));
                    }
                    vals.add(val);
                }
                break;
            default:
                throw new RuntimeException("output type " + type.getType() + " not allowed");
        }

        DatasetRow dr = new DatasetRow();
        int i = 0;
        for (String[] v : vals) {
            i = 0;
            for (String vv : v) {
                if (i == 0) {
                    dr.setId(vv);
                } else {
                    Scalar sc = new RealValue(Double.parseDouble(vv));
                    dr.getListOfValue().add(sc);
                }
                i++;
            }
            ds.getTable().getListOfRow().add(dr);
        }

        return table;
    }

    private DataSet createDSDef(Map<String, Parameter> pars, ValueType type, String extFileName) throws IOException {
        return createDSDef(pars, type, extFileName, "");
    }

    private DataSet createDSDef(Map<String, Parameter> pars, ValueType type, String extFileName, String sdId) throws IOException {
        DataSet ds = new DataSet();
        int nRows = 0;
        HeaderColumnsDefinition def = ds.createDefinition();

        List<String[]> vals = new ArrayList<>();
        String[] val;
        int k = 1;
        switch (type.getType()) {
            case "VALUE":
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    nRows = outPar.getValue().getValues().size();
                    def.createColumnDefinition(outPar.getKey(), outPar.getValue().getcType(), SymbolType.REAL, k++);
                }
                for (int i = 0; i < nRows; i++) {
                    k = 0;
                    val = new String[pars.size()];
                    for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                        val[k++] = ("" + outPar.getValue().getArrayValues()[i]);
                    }
                    vals.add(val);
                }
                break;
            case "MEAN":
                def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, 1);
                def.createColumnDefinition("Parameter", ColumnType.POP_PARAMETER, SymbolType.STRING, 2);
                def.createColumnDefinition("MEAN", ColumnType.UNDEFINED, SymbolType.REAL, 3);
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = "mean" + k++;
                    val[1] = outPar.getKey();
                    val[2] = ("" + StatUtils.mean(outPar.getValue().getArrayValues()));
                    vals.add(val);
                }
                break;

            case "POSTERIORMEAN":
                def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, 1);
                def.createColumnDefinition("Parameter", ColumnType.POP_PARAMETER, SymbolType.STRING, 2);
                def.createColumnDefinition("MEAN", ColumnType.UNDEFINED, SymbolType.REAL, 3);
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = "mean" + k++;
                    val[1] = outPar.getKey();
                    val[2] = ("" + StatUtils.mean(outPar.getValue().getArrayValues()));
                    vals.add(val);
                }
                break;

            case "MEDIAN":
                def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, 1);
                def.createColumnDefinition("Parameter", ColumnType.POP_PARAMETER, SymbolType.STRING, 2);
                def.createColumnDefinition("MEDIAN", ColumnType.UNDEFINED, SymbolType.REAL, 3);
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = "median" + k++;
                    val[1] = outPar.getKey();
                    val[2] = ("" + StatUtils.percentile(outPar.getValue().getArrayValues(), 50));
                    vals.add(val);
                }

                break;

            case "POSTERIORMEDIAN":
                def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, 1);
                def.createColumnDefinition("Parameter", ColumnType.POP_PARAMETER, SymbolType.STRING, 2);
                def.createColumnDefinition("MEDIAN", ColumnType.UNDEFINED, SymbolType.REAL, 3);
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = "median" + k++;
                    val[1] = outPar.getKey();
                    val[2] = ("" + StatUtils.percentile(outPar.getValue().getArrayValues(), 50));
                    vals.add(val);
                }

                break;
            case "VAR":
                def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, 1);
                def.createColumnDefinition("Parameter", ColumnType.POP_PARAMETER, SymbolType.STRING, 2);
                def.createColumnDefinition("VARIANCE", ColumnType.VARIANCE, SymbolType.REAL, 3);
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = "variance" + k++;
                    val[1] = outPar.getKey();
                    val[2] = ("" + StatUtils.variance(outPar.getValue().getArrayValues()));
                    vals.add(val);
                }

                break;
            case "SD":
                def.createColumnDefinition("Parameter", ColumnType.POP_PARAMETER, SymbolType.STRING, 1);
                def.createColumnDefinition(sdId, ColumnType.STAT_PRECISION, SymbolType.REAL, 2);
                k = 0;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = outPar.getKey();
                    val[1] = ("" + Math.sqrt(StatUtils.variance(outPar.getValue().getArrayValues())));
                    vals.add(val);
                }

                break;
            case "PERC":
                def.createColumnDefinition("Percentile", ColumnType.STAT_PRECISION, SymbolType.REAL, 1);
                k = 2;
                for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                    def.createColumnDefinition(outPar.getKey(), outPar.getValue().getcType(), SymbolType.REAL, k++);
                }
                for (double pe : CodaLoader.perc) {
                    val = new String[def.getListOfColumn().size()];
                    val[0] = "" + pe;
                    int i = 1;
                    for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {

                        val[i++] = ("" + StatUtils.percentile(outPar.getValue().getArrayValues(), pe));
                    }
                    vals.add(val);
                }
                break;
            default:
                throw new RuntimeException("output type " + type.getType() + " not allowed");
        }
        if (extFileName == null) {
            for (String[] v : vals) {
                ds.createRow(v);
            }
        } else {
            String name = extFileName;
            String simpleName = Util.getSimpleFileName(extFileName);
            ExternalFile f = Util.createEF(simpleName, "population estimates", DELIM);
            generateCSV(extFileName, pars, vals);
            ds.setExternalFile(f);
        }
        return ds;
    }

    private DataSet createIndivPrecDistDef(String extFileName) throws IOException {
        DataSet ds = new DataSet();
        HeaderColumnsDefinition def = ds.createDefinition();

        List<String[]> vals = new ArrayList<>();
        String[] val;
        def.createColumnDefinition("ID", ColumnType.ID, SymbolType.STRING, 1);
        int k = 2;
        for (String n : getIndivParList()) {
            def.createColumnDefinition(n, ColumnType.INDIV_PARAMETER, SymbolType.REAL, k++);
        }
        for (Integer subj : getSubjectsList()) {
            int maxT = dl.getIndivMap().get(getIndivParList()[0]).get(subj).getArrayValues().length;
            val = new String[getIndivParList().length + 1];
            for (int t = 0; t < maxT; t++) {
                val = new String[getIndivParList().length + 1];

                k = 0;
                val[0] = indivIdPrefix + subj;
                k = 1;
                for (String n : getIndivParList()) {
                    val[k++] = ("" + dl.getIndivMap().get(n).get(subj).getArrayValues()[t]);
                }
                vals.add(val);
            }
        }

        if (extFileName == null) {
            for (String[] v : vals) {
                ds.createRow(v);
            }
        } else {
            String name = extFileName;
            String simpleName = Util.getSimpleFileName(extFileName);
            ExternalFile f = Util.createEF(simpleName, "precision individual estimates", DELIM);
            generateIndivCSV(extFileName, vals);
            ds.setExternalFile(f);
        }
        return ds;
    }

    private Integer[] getSubjectsList() {
        Set<Integer> sub = new TreeSet<Integer>();

        for (Map.Entry<String, Map<Integer, Parameter>> indPar : dl.getIndivMap().entrySet()) {
            sub = indPar.getValue().keySet();
            break;

        }
        return sub.toArray(new Integer[sub.size()]);

    }

    private String[] getIndivParList() {
        Set<String> sub = new TreeSet<String>();

        for (Map.Entry<String, Map<Integer, Parameter>> indPar : dl.getIndivMap().entrySet()) {
            sub.add(indPar.getKey());

        }

        
        return sub.toArray(new String[sub.size()]);

    }

    String[][] getTable(ValueType type) {

        Integer[] subjects = getSubjectsList();
        String mat[][] = null;
        switch (type) {
            case MEAN:
            case MEDIAN:
                mat = new String[subjects.length][dl.getIndivMap().size() + 1];
                for (int j = 0; j < getIndivParList().length; j++) {
                    for (int i = 0; i < subjects.length; i++) {
                        mat[i][0] = indivIdPrefix + (i + 1);
                        double[] values;
                        switch (type) {
                            case MEAN:
                                values = dl.getIndivMap().get(getIndivParList()[j]).get(subjects[i]).getArrayValues();
                                mat[i][j + 1] = "" + StatUtils.mean(values);
                                break;
                            case MEDIAN:
                                values = dl.getIndivMap().get(getIndivParList()[j]).get(subjects[i]).getArrayValues();
                                mat[i][j + 1] = "" + StatUtils.percentile(values, 50);
                                break;
                        }
                    }
                }
                break;
            case VALUE:
                Map<String, Map<Integer, Parameter>> map = dl.getIndivMap();
                mat = new String[subjects.length * map.size()][3];
                int row = 0;
                for (int j = 0; j < getIndivParList().length; j++) {
                    for (int i = 0; i < subjects.length; i++) {
                        mat[row][0] = indivIdPrefix + (i + 1);
                        mat[row][1] = getIndivParList()[j];
                        Map<Integer, Parameter> a = map.get(getIndivParList()[j]);
                        Parameter b = a.get(i + 1);
                        double[] c = b.getArrayValues();
                        mat[row][2] = "" + Math.sqrt(StatUtils.variance(map.get(getIndivParList()[j]).get(i + 1).getArrayValues()));
                        row++;
                    }

                }
                break;
        }

        return mat;
    }

    private DataSetTable createIndivDataTable(ValueType type) throws IOException {
        Integer[] subjects = getSubjectsList();
        String mat[][] = new String[subjects.length][dl.getIndivMap().size()];
        switch (type.getType()) {
            case "MEAN":
                mat = getTable(ValueType.MEAN);
                break;
            case "MEDIAN":
                mat = getTable(ValueType.MEDIAN);
                break;
            default:
                throw new RuntimeException("output type " + type.getType() + " not allowed");
        }

        DatasetRow dr = new DatasetRow();
        DataSetTable table = new DataSetTable();
        DataSet ds = new DataSet();
        table = new DataSetTable();
        for (int i = 0; i < subjects.length; i++) {
            ds.setTable(table);
            dr = new DatasetRow();

            table.getListOfRow().add(dr);
            dr.getListOfValue().add(new StringValue(mat[i][0]));
            for (int j = 0; j < dl.getIndivMap().size(); j++) {
                Scalar sc = new RealValue(Double.parseDouble(mat[i][j + 1]));
                dr.getListOfValue().add(sc);
            }
        }
        return table;
    }

    private DataSetTable createIndivPrecisionDataTable(ValueType type) throws IOException {
        int nRows = 0;
        int size = 0;
        Integer[] subjects = getSubjectsList();
        String mat[][] = new String[subjects.length * dl.getIndivMap().size()][3];
        mat = getTable(ValueType.VALUE);

        DatasetRow dr = new DatasetRow();
        DataSetTable table = new DataSetTable();
        DataSet ds = new DataSet();
        table = new DataSetTable();
        ds.setTable(table);
        for (int i = 0; i < subjects.length * dl.getIndivMap().size(); i++) {
            dr = new DatasetRow();

            table.getListOfRow().add(dr);
            dr.getListOfValue().add(new StringValue(mat[i][0]));
            dr.getListOfValue().add(new StringValue(mat[i][1]));
            dr.getListOfValue().add(new RealValue(Double.parseDouble(mat[i][2])));

        }
        return table;
    }

    private DataSetTable createIndivPrecisionDistDataTable(boolean infile) throws IOException {
        int nRows = 0;
        int size = 0;
        Integer[] subjects = getSubjectsList();
        String mat[][] = new String[subjects.length * dl.getIndivMap().size()][3];
        mat = getTable(ValueType.VALUE);

        DatasetRow dr = new DatasetRow();
        DataSetTable table = new DataSetTable();
        DataSet ds = new DataSet();
        table = new DataSetTable();
        ds.setTable(table);
        for (int i = 0; i < subjects.length * dl.getIndivMap().size(); i++) {
            dr = new DatasetRow();

            table.getListOfRow().add(dr);
            dr.getListOfValue().add(new StringValue(mat[i][0]));
            dr.getListOfValue().add(new StringValue(mat[i][1]));
            dr.getListOfValue().add(new RealValue(Double.parseDouble(mat[i][2])));

        }
        return table;
    }

    private DataSetTable createIndivPrecisionPercDataTable() throws IOException {
        DatasetRow dr = new DatasetRow();
        DataSetTable table = new DataSetTable();
        DataSet ds = new DataSet();
        table = new DataSetTable();
        ds.setTable(table);
        for (int i = 0; i < getSubjectsList().length; i++) {
            for (double pe : dl.perc) {
                dr = new DatasetRow();

                table.getListOfRow().add(dr);
                dr.getListOfValue().add(new StringValue(indivIdPrefix + (i + 1)));
                dr.getListOfValue().add(new RealValue(pe));
                for (String n : getIndivParList()) {
                    Map<String, Map<Integer, Parameter>> im;
                    dr.getListOfValue().add(new RealValue(dl.getIndivMap().get(n).get(i + 1).getPerc(pe)));
                }
            }
        }
        return table;
    }

    public DataSet outputCSVRawResults(String name) throws IOException {
        return createDSDef(dl.getPopPar().getPars(), ValueType.VALUE, name);
    }

    public void save(String fName) throws FileNotFoundException {
        libSO.save(new FileOutputStream(fName), soResource);
    }

    private void generateCSV(String name, Map<String, Parameter> pars) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(new File(name)));
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("###.###", sym);
        int nSpamples = 0;
        for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
            nSpamples = outPar.getValue().getValues().size();
            out.print(outPar.getKey() + "\t");
        }
        out.println();
        for (int i = 0; i < nSpamples; i++) {
            String[] val = new String[pars.size()];
            int k = 0;
            for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                out.print(df.format(outPar.getValue().getArrayValues()[i]) + "\t");
            }
            out.println();
        }
        out.close();
    }

    private void generateCSV(String name, Map<String, Parameter> pars, List<String[]> vals) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(new File(name)));
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("###.###", sym);
        out.println(Util.createList(pars.keySet().toArray(new String[1]), SEP));
        for (String[] outPar : vals) {
            out.println(Util.createList(outPar, SEP));
        }
        out.close();
    }

    private void generateIndivCSV(String name, List<String[]> vals) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(new File(name)));
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("###.###", sym);
        out.println(Util.createList(true, getIndivParList(), SEP));

        for (String[] outPar : vals) {
            out.println(Util.createList(outPar, SEP));

        }
        out.close();
    }

    public PrecisionIndividualEstimates createPrecisionIndividualEstimates(String fileName) throws IOException {
        if (block.getEstimation() == null) {
            block.createEstimation();
        }
        if (dl.getIndivMap().entrySet().isEmpty()) {
            return null;
        }
        Set<Map.Entry<String, Map<Integer, Parameter>>> indMap = dl.getIndivMap().entrySet();

        PrecisionIndividualEstimates iPrecEst = block.getEstimation().createPrecisionIndividualEstimates();
        DataSet ds = new DataSet();
        ds.setDefinition(headersIndivPrecisionDef());
        ds.setTable(createIndivPrecisionDataTable(ValueType.VALUE));
        iPrecEst.setStandardDeviation(ds);
        SOTableDistrib td = new SOTableDistrib();
        Distribution dist = new Distribution();
        td.setDistribution(dist);
        ProbOnto distP = td.getDistribution().createProbOnto(DistributionName.RANDOM_SAMPLE);
        distP.setDataSet(createIndivPrecDistDef(fileName));
        iPrecEst.setEstimatesDistribution(td);

        DataSet percCI = iPrecEst.createPercentilesCI();
        percCI.setDefinition(headersIndivPrecisionPercDef());
        percCI.setTable(createIndivPrecisionPercDataTable());
        return iPrecEst;
    }

}
