/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

/**
 *
 * @author cristiana
 */
public class WinBugsConverterTester {

    private String xmlFileName;//, dirName;
    private String propFile;
    private int parserType;
    private String solverType;
    private int totCov, totGrid;
    private String outputDir;
//    private String FunctionsFileName;
//    private String GrammarFileName;

    private static final int[] parserTypeCodes = {ConverterProvider.NOPASCAL, ConverterProvider.PASCAL1, ConverterProvider.PASCAL2};
    private static final int[] odeSolverTypeCodes = {ConverterProvider.ODESOLVER_RK45, ConverterProvider.ODESOLVER_LSODA};

    public WinBugsConverterTester(String propFile) {
        this.propFile = propFile;
    }

    public WinBugsConverterTester() {
        this.parserType = 1;
        this.solverType = "RK45";
        this.xmlFileName = "models-0.7.3/usecase01.xml";
//        this.xmlFileName = "models/Rocchetti_2013_oncology_TGI_antiangiogenic_combo.xml";
        this.outputDir = "./";
        //        this.propFile = propFile;
    }

    public static void main(String[] args) throws Exception {
        PrintWriter out = new PrintWriter("tester.txt");
        out.println("ora: " + System.currentTimeMillis());
        out.close();
//        String classpath = System.getProperty("java.class.path");
//        System.out.println("class path: " + classpath);
        /* 
         System.out.println(args.length + " parametri: " + args);

         if (args.length != 1 && args.length != 3) {
         printHelp();
         return;
         }
         System.out.println("args[0] = " + args[0]);
         */
//        WinBugsConverterTester mt = new WinBugsConverterTester();
        WinBugsConverterTester mt = new WinBugsConverterTester(args[0]);
        System.out.println("propfile " + mt.getPropFile());
        File f = new File(mt.getPropFile());
        FileReader in = new FileReader(f);
        mt.loadProp(in);
        System.out.println(mt);
//        if (args.length == 3) {
//            mt.FunctionsFileName = args[1];
//            mt.GrammarFileName = args[2];
//        }
        File mF = new File(mt.xmlFileName);
        if (!mF.exists()) {
            throw new FileNotFoundException("model file " + mF.getAbsolutePath() + " does not exist.");
        }

        File dirF = new File(mt.outputDir);
        if (!dirF.exists()) {
            throw new FileNotFoundException("output directory " + dirF.getAbsolutePath() + " does not exist.");
        }
        System.out.println("opening model " + mF.getAbsolutePath());
        System.out.println("saving output files in " + dirF.getAbsolutePath());

        mt.runConversion();

    }

    public void loadProp(FileReader in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        this.parserType = Integer.parseInt(props.getProperty("parserType"));
        this.solverType = getSolver(props.getProperty("solverType"));
        this.totCov = Integer.parseInt(props.getProperty("totCov"));
        this.totGrid = Integer.parseInt(props.getProperty("totGrid"));
        this.outputDir = props.getProperty("outputDir");
        this.xmlFileName = props.getProperty("modelName");
        if (this.xmlFileName == null) {
            this.xmlFileName = System.getenv("modelName");
            System.out.println("var ambiente = " + System.getenv("modelName"));
            System.out.println("modello = " + this.xmlFileName);
        }
    }

    private String getSolver(String property) {

        switch (property) {
            case "RK45":
                return "1";
            case "LSODA":
                return "2";
            default:
                System.out.println("Wrong or missing solver type. RK45 is selected as default.");
                break;
        }
        return "1";
    }

    private void runConversion() throws IOException {
        ConverterProvider c;
//        System.out.println("file xml  = " + f.getAbsolutePath());
//        if (FunctionsFileName == null || GrammarFileName == null) {
        c = new ConverterProvider();
        if (parserType > 2) {
            throw new UnsupportedOperationException("parser type " + parserType + " not yet available!");
        }
        c.setParserType(parserType);
        if (parserType == ConverterProvider.PASCAL2) {
            c.setSolverType(Integer.parseInt(getSolver(solverType)));
        }
        c.setTotCov(totCov);

        c.performConvert(new File(this.xmlFileName), new File(this.outputDir));
    }

    public String getPropFile() {
        return propFile;
    }

    @Override
    public String toString() {
        return "Model Conversion" + "\nxmlFileName=" + xmlFileName + "\nparserType=" + parserType + "\nsolverType=" + solverType + "\ntotCov=" + totCov + "\ntotGrid=" + totGrid + "\noutputDir=" + outputDir + "\n}";
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

}
