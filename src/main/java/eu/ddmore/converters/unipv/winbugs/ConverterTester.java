/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author cristiana
 */
public class ConverterTester {

    private String xmlFileName;//, dirName;
    private String propFile;
    private int parserType;
    private String solverType;
//    private int totCov, totGrid;
    private String outputDir;

//    private static final int[] parserTypeCodes = {ConverterProvider.NOPASCAL, ConverterProvider.PASCAL1, ConverterProvider.PASCAL2};
    private static final int[] odeSolverTypeCodes = {ConverterProvider.ODESOLVER_RK45, ConverterProvider.ODESOLVER_LSODA};

    public ConverterTester(String propFile) {
        this.propFile = propFile;
    }

    public ConverterTester(int parserT, String solverT, String fileN) {
        this.parserType = parserT;
        this.solverType = solverT;
        this.xmlFileName = fileN;
        this.outputDir = "./";
//        this.propFile = propFile;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2){
            printHelp();
            return;
        }

        ConverterTester mt = new ConverterTester(2, args[1], args[0]);
        mt.runConversion();
    }

    private void runConversion() throws IOException {
        ConverterProvider c;
        c = new ConverterProvider();
        
        c.setParserType(2);
//        solverType = "1";
        c.setSolverType(Integer.parseInt(solverType));
//        if (parserType == ConverterProvider.PASCAL2) {
//            c.setSolverType(Integer.parseInt(solverType));
//        }
//        c.setTotCov(totCov);
//        c.setTotGrid(totGrid);
        c.performConvert(new File(this.xmlFileName), new File(this.outputDir));
    }

    public String getPropFile() {
        return propFile;
    }

    @Override
    public String toString() {
        return "Model Conversion" + "\nxmlFileName=" + xmlFileName + "\nparserType=" + parserType + "\nsolverType=" + solverType + "\noutputDir=" + outputDir + "\n}";
//        return "Model Conversion" + "\nxmlFileName=" + xmlFileName + "\nparserType=" + parserType + "\nsolverType=" + solverType + "\ntotCov=" + totCov + "\ntotGrid=" + totGrid + "\noutputDir=" + outputDir + "\n}";
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

    private static void printHelp() {
        System.out.println("usage: java -jar jar_name file_name parserType [solverType]\n\twhere\n ");
        System.out.println("\tparser type can be ");
//        System.out.println("\t\t" + parserTypeCodes[0] + " no Pascal");
//        System.out.println("\t\t" + parserTypeCodes[2] + " Pascal Ver 2");
        System.out.println("\t in case of Pascal Ver 2 you have to specify also the solver type: ");
//        System.out.println("\tthe solver type: " );
        System.out.println("\t\t" + odeSolverTypeCodes[0] + " " + ConverterProvider.RK45SOLVER);
        System.out.println("\t\t" + odeSolverTypeCodes[1] + " " + ConverterProvider.LSODASOLVER);
        return;
    }

}
