/**
 * *****************************************************************************
 * Copyright (C) 2014 * Univeristy of Pavia - All rights reserved.
 * ******************************************************************************
 */
package eu.ddmore.converters.unipv.winbugs;

import crx.converter.engine.Accessor;
import crx.converter.engine.ConversionDetail_;
import crx.converter.engine.Manager;
import crx.converter.engine.ParameterContext;
import crx.converter.engine.Part;
import static crx.converter.engine.PharmMLTypeChecker.isColumnDefinition;
import static crx.converter.engine.PharmMLTypeChecker.isColumnMapping;
import static crx.converter.engine.PharmMLTypeChecker.isCommonParameter;
import static crx.converter.engine.PharmMLTypeChecker.isContinuousCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isCovariate;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isEstimation;
import static crx.converter.engine.PharmMLTypeChecker.isFunction;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionCall;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionParameter;
import static crx.converter.engine.PharmMLTypeChecker.isGeneralError;
import static crx.converter.engine.PharmMLTypeChecker.isIndependentVariable;
import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
import static crx.converter.engine.PharmMLTypeChecker.isInt;
import static crx.converter.engine.PharmMLTypeChecker.isLocalVariable;
import static crx.converter.engine.PharmMLTypeChecker.isLogicalBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isObservationError;
import static crx.converter.engine.PharmMLTypeChecker.isPopulationParameter;
import static crx.converter.engine.PharmMLTypeChecker.isRandomVariable;
import static crx.converter.engine.PharmMLTypeChecker.isStructuredError;
import static crx.converter.engine.PharmMLTypeChecker.isSymbolReference;
import static crx.converter.engine.PharmMLTypeChecker.isVariabilityLevelDefinition;
import static crx.converter.engine.PharmMLTypeChecker.isVariableReference;
import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.SymbolReader;
import crx.converter.engine.VariableDeclarationContext;
import crx.converter.engine.assoc.DependencyGraph;
import crx.converter.engine.assoc.DependencyLexer;
import crx.converter.engine.assoc.DependencyRef;
import static crx.converter.engine.assoc.DependencyRef.createElementsUnderConsideration;
import static crx.converter.engine.assoc.DependencyRef.updateDependencyContext;
import crx.converter.engine.common.DataFiles;
import crx.converter.engine.common.ObservationParameter;
import crx.converter.engine.common.TabularDataset;
import crx.converter.engine.common.TemporalDoseEvent;
import crx.converter.spi.ILexer;
import eu.ddmore.converters.unipv.winbugs.parts.EstimationStepImpl;
import eu.ddmore.converters.unipv.winbugs.parts.ParameterBlockImpl;
import eu.ddmore.converters.unipv.winbugs.parts.CovariateBlockImpl;
import eu.ddmore.converters.unipv.winbugs.parts.StructuralBlockImpl;
import eu.ddmore.converters.unipv.winbugs.parts.ObservationBlockImpl;
import crx.converter.spi.IParser;
import crx.converter.spi.blocks.CovariateBlock;
import crx.converter.spi.blocks.ObservationBlock;
import crx.converter.spi.blocks.ParameterBlock;
import crx.converter.spi.blocks.StructuralBlock;
import crx.converter.spi.blocks.TrialDesignBlock;
import crx.converter.spi.blocks.VariabilityBlock;
import crx.converter.spi.steps.BaseStep;
import crx.converter.spi.steps.EstimationStep;
import crx.converter.spi.steps.SimulationStep;
import crx.converter.tree.BaseTreeMaker;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.NestedTreeRef;
import crx.converter.tree.Node;
import crx.converter.tree.TreeMaker;
import java.io.IOException;

import eu.ddmore.libpharmml.ILibPharmML;
import eu.ddmore.libpharmml.IPharmMLResource;
import eu.ddmore.libpharmml.IValidationReport;
import eu.ddmore.libpharmml.PharmMlFactory;
import eu.ddmore.libpharmml.dom.IndependentVariable;
import eu.ddmore.libpharmml.dom.PharmML;
import eu.ddmore.libpharmml.dom.commontypes.CommonVariableDefinition;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.FunctionDefinition;
import eu.ddmore.libpharmml.dom.commontypes.FunctionParameter;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.Name;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLElement;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnMapping;
import eu.ddmore.libpharmml.dom.dataset.ColumnReference;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.HeaderColumnsDefinition;
import eu.ddmore.libpharmml.dom.dataset.MapType;
import eu.ddmore.libpharmml.dom.dataset.TargetMapping;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType.FunctionArgument;
import eu.ddmore.libpharmml.dom.maths.LogicBinOp;
import eu.ddmore.libpharmml.dom.maths.Piece;
import eu.ddmore.libpharmml.dom.maths.Piecewise;
import eu.ddmore.libpharmml.dom.modeldefn.CommonParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateModel;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelation;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelation;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ModelDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationModel;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterModel;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomEffect;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.modeldefn.StructuralModel;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredModel;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredModel.LinearCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError;
import eu.ddmore.libpharmml.dom.modeldefn.TransformedCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.VariabilityDefnBlock;
import eu.ddmore.libpharmml.dom.modeldefn.VariabilityLevelDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro;
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacroList;
import eu.ddmore.libpharmml.dom.modellingsteps.CommonModellingStep;
import eu.ddmore.libpharmml.dom.modellingsteps.Estimation;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps;
import eu.ddmore.libpharmml.dom.trialdesign.ExternalDataSet;
import eu.ddmore.libpharmml.dom.trialdesign.TrialDesign;
import eu.ddmore.libpharmml.dom.uncertml.VarRefType;
import eu.ddmore.libpharmml.impl.PharmMLVersion;
import eu.ddmore.libpharmml.pkmacro.exceptions.InvalidMacroException;
import eu.ddmore.libpharmml.pkmacro.translation.Input;
import eu.ddmore.libpharmml.pkmacro.translation.MacroOutput;
import eu.ddmore.libpharmml.pkmacro.translation.Translator;
import eu.ddmore.convertertoolbox.api.domain.LanguageVersion;
import eu.ddmore.convertertoolbox.api.domain.Version;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.domain.ConversionReportImpl;
import eu.ddmore.convertertoolbox.domain.LanguageVersionImpl;
import eu.ddmore.convertertoolbox.domain.VersionImpl;
import static eu.ddmore.libpharmml.dom.dataset.ColumnType.ADM;
import static eu.ddmore.libpharmml.dom.dataset.ColumnType.DOSE;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import eu.ddmore.converters.unipv.winbugs.parts.VariabilityBlockImpl;
import static eu.ddmore.converters.unipv.winbugs.Parser.winbugsDir;
import static eu.ddmore.converters.unipv.winbugs.PascalParser.PMetricsFileName;
import static eu.ddmore.converters.unipv.winbugs.PascalParser.piecewiseFileName;
import static eu.ddmore.converters.unipv.winbugs.PascalParser.covariateFileName;
import eu.ddmore.converters.unipv.winbugs.parts.TrialDesignBlockImpl;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Properties;

public class ConverterProvider extends DependencyLexer implements ILexer {

    private static eu.ddmore.libpharmml.dom.commontypes.ObjectFactory cof = new eu.ddmore.libpharmml.dom.commontypes.ObjectFactory();
    private static eu.ddmore.libpharmml.dom.dataset.ObjectFactory ds_of = new eu.ddmore.libpharmml.dom.dataset.ObjectFactory();
    private static Manager m = new Manager();
    private static eu.ddmore.libpharmml.dom.maths.ObjectFactory math_of = new eu.ddmore.libpharmml.dom.maths.ObjectFactory();
    IParser parser;

    private boolean permit_objective_ETAs = true;
    private boolean permitEmptyTrialDesignBlock = false;
    private boolean remove_illegal_chars = false, filter_reserved_words = false;
    private IPharmMLResource res = null;
    private boolean resetRegToCov = true;
    private ScriptDefinition sd = new ScriptDefinition();
    private boolean sort_parameter_model_by_context = false;
    private boolean sort_structural_model = false;
    private LanguageVersion source = null;
    private LanguageVersion target = null;
    private PharmMLVersion target_level = PharmMLVersion.V0_8_1;
    private boolean terminate_with_duff_xml = false;
    private Translator tr = new Translator();
    private boolean translate_macros = true; //to force column mapping
    private boolean useGlobalConditionalDoseVariable = false;
    private boolean usePiecewiseAsEvents = false;
    private boolean validate_xml = false;
    private boolean created_parameter_context = false;
    private Map<StructuralModel, List<PKMacro>> macro_input_map = new HashMap<StructuralModel, List<PKMacro>>();
    private Map<StructuralModel, MacroOutput> macro_output_map = new HashMap<StructuralModel, MacroOutput>();
    private String model_filename = new String();
    private static final String SCRIPT_FILE_SUFFIX = "txt";
    private static final String WINBUGSDIR = "BlackBoxWinBUGS/";
//    private static final String WINBUGSSODIR = "./";//Files2Return/";
    private String pascalN = "ODEPascal";
    private String WBScriptFName = "RunScript.txt";

    String rootName = ".";
    private static final String pascalExtension = ".txt";
    public static final int NOPASCAL = 0;
    public static final int PASCAL1 = 1;
    public static final int PASCAL2 = 2;
    protected static final String RK45SOLVER = "RK45";
    protected static final String LSODASOLVER = "LSODA";
    public static final int ODESOLVER_RK45 = 1;
    public static final int ODESOLVER_LSODA = 2;
    private Version converterVersion = new VersionImpl(1, 7, 0);
    private String output_dir = ".";
    private ILibPharmML lib = null;
    private String name = "Core Lexer";
    private List<String> ordered_levels = new ArrayList<>();
    private Map<PopulationParameter, ParameterContext> param_context_map = new HashMap<>();
    private ParameterBlock pb = null;
    private IValidationReport validation_report = null;
    private boolean is_echo_exception = true;
    private DataFiles data_files = new DataFiles();
    private StructuralBlock currentSb = null;
    private boolean hasResetColumnUsageRegToCov = false;
    private HashMap<Object, String> index_symbol_map = new HashMap<>();
    private boolean isolate_conditional_dose_variable = true;
    private boolean isolate_dt = true;

    private String outputDirPascal1;
    private String outputDirPascal2;
    private String outputDirPascal2Mod;
    private String outputDirPiecewisePascal;
    private String pwFileName;
    private String covFileName;
    private String bbCompileScriptDir;

    private int parserType;
    private int solverType;
    private int totCov;
//    private int nGrid;
    private int num1, update;

    public String getPwFileName() {
        return pwFileName;
    }

    public String getCovFileName() {
        return covFileName;
    }

    public void setPwFileName(String pwFileName) {
        this.pwFileName = outputDirPascal2Mod + pwFileName;
    }

    public void setCovFileName(String pwFileName) {
        this.covFileName = outputDirPascal2Mod + pwFileName;
    }

    public ConverterProvider(String fName, String gName) throws IOException, NullPointerException {
        super();
        initAll();
    }

    public ConverterProvider() throws IOException, NullPointerException {
        super();
        initLibrary();
        tm = new BaseTreeMaker();
        initAll();
    }

    public void setParserType(int parserType) {
        this.parserType = parserType;
    }

    private void initAll() throws IOException, NullPointerException {

//        System.out.println("InitAll() ");
        VersionImpl source_version = new VersionImpl(0, 8, 1);
        source = new LanguageVersionImpl("PharmML", source_version);
        VersionImpl target_version = new VersionImpl(1, 4, 3);
        target = new LanguageVersionImpl("WINBUGS", target_version);
        converterVersion = new VersionImpl(1, 0, 0);

        this.getTreeMaker().setPermitParameterWithoutAssignment(true);
        initParser();

    }

    public static String getModelName(File src) {
        String name = src.getPath();
        int idx = name.lastIndexOf("/");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        idx = name.lastIndexOf("\\");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        name = name.substring(0, name.indexOf("."));
        return name;
    }

    /**
     * Set the output directory for the converter instance.
     *
     * @param dir Output Directory
     */
    public void setOutputDirectory(File dir) {
        boolean done = false;
        if (dir == null) {
            throw new NullPointerException("Output directory is NULL.");
        }
        if (dir.isDirectory()) {
            if (dir.canRead() && dir.canWrite()) {
                output_dir = dir.getAbsolutePath();
                done = true;
            }
        }

        if (!done) {
            throw new IllegalStateException("Unable to assign output directory path.");
        }
    }

    @Override
    public ConversionReport performConvert(File src, File outputDirectory) {

        File f = null;
        Parser theParser;
        String separ = "/";
        if (src.getAbsolutePath().lastIndexOf(separ) < 0) {
            separ = "\\";
        }
        String jobD = src.getAbsolutePath().substring(0, src.getAbsolutePath().lastIndexOf(separ));
        System.out.println("jobD = " + jobD);
        theParser = (Parser) getParser();
        theParser.setJobDir(jobD);
        theParser.setOutputDir(output_dir);

        setParserProp(src, (Parser) this.getParser());
        ((Parser) this.getParser()).setXmlFileName(src); 
        getScriptDefinition().flushAllSymbols();
        try {
            parser.setRunId(m.generateRunId());
            setOutputDirectory(outputDirectory);
            try {
                createWinBUGSModel(outputDirectory, src);

                f = theParser.createScriptWinbugs(src,
                        new File(winbugsDir + "/" + WBScriptFName),
                        outputDirectory.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Winbugs converter Exception = " + e.getMessage());

            }
            return getCrxSuccessReport(f);
        } catch (Exception e) {
            return getExceptionReport(e);
        }
    }

    /**
     * Initialise the PharmML library handle bound to the Lexer instance.
     */
    private void initLibrary() {
        lib = PharmMlFactory.getInstance().createLibPharmML();
    }

    /**
     * Load a PharmML file.
     *
     * @param xml_file_path File path to XML
     * @throws IOException
     */
    protected void loadPharmML(File xml_file_path) throws IOException {
        InputStream in = new FileInputStream(xml_file_path);
        res = lib.createDomFromResource(in);
        in.close();

        dom = res.getDom();
        getAccessor();

        model_filename = xml_file_path.getAbsolutePath();
        parser.setPharmMLWrittenVersion(dom.getWrittenVersion());

        if (validate_xml) {
            validation_report = res.getCreationReport();
            if (validation_report != null) {
                if (!validation_report.isValid() && terminate_with_duff_xml) {
                    throw new IllegalStateException("The XML of the PharmML input model is not well-formed and valid.");
                }
            }
        }

        translatePKMacros(); 
    }

    @Override
    public String getOutputDirectory() {
        return output_dir;
    }

    private void setParserProp(File src, Parser p) {
        String name = src.getPath();
        if (parser instanceof PascalParser2) {
            ((PascalParser2) p).setPascalFileName(new File(this.outputDirPascal2 + "/" + pascalN + pascalExtension));
            ((PascalParser2) p).setPascalModFileName(new File(this.outputDirPascal2Mod + "/" + pascalN + "_Mod" + pascalExtension));
            ((PascalParser2) p).setPascalPKmodel(this.outputDirPascal2 + "/" + PMetricsFileName);
            ((PascalParser2) p).setPascalPKmetricsFileName(new File(this.outputDirPascal2 + "/" + PMetricsFileName));

            this.setPwFileName(piecewiseFileName);
            this.setCovFileName(covariateFileName);
            ((PascalParser2) p).setPiecewisePascalName(getPwFileName());
            ((PascalParser2) p).setCovariatePascalName(getCovFileName());
            ((PascalParser2) p).setBbCompileScriptDir(bbCompileScriptDir);
        }

    }

    @Override
    public void setParser(IParser parser_) {
        if (parser_ == null) {
            throw new NullPointerException("The parser is null.");
        } else {
            parser = parser_;
        }
    }

    public void initParser() throws FileNotFoundException, IOException {
        System.out.println("InitParser() ");

        this.setParserType(parserType);
        Parser p = null;
        try {
            p = new PascalParser2();
            // DEFAULT SOVER 
            ((PascalParser2) p).setOdeSolver(RK45SOLVER);
//             ((PascalParser2) p).setOdeSolver(LSODASOLVER);

            if (solverType == ODESOLVER_LSODA) {
                ((PascalParser2) p).setOdeSolver(LSODASOLVER);
            }
            ((PascalParser2) p).setOutputDirPascal2(outputDirPascal2);
            ((PascalParser2) p).setOutputDirPascal2Mod(outputDirPascal2Mod);
            ((PascalParser2) p).setOutputDirPiecewisePascal(outputDirPascal2Mod);

            p.setLexer(this);

            p.setModelNum(1);
            if (System.getenv("SEE_HOME") != null) {
                p.setWinbugsTemplateDir(System.getenv("SEE_HOME") + "/" + WINBUGSDIR + "Templates" + "/");
                p.setWinbugsDir(System.getenv("SEE_HOME") + "/" + WINBUGSDIR);
            } else {
                p.setWinbugsDir(".");

                p.setWinbugsTemplateDir("Templates/");
            }

            if (System.getenv("SEE_HOME") != null) {
                rootName = System.getenv("SEE_HOME");
                this.outputDirPascal1 = rootName + WINBUGSDIR + "WBDiff/Mod/";
                this.outputDirPascal2 = rootName + WINBUGSDIR + "Pmetrics/Mod/";
                this.outputDirPascal2Mod = rootName + WINBUGSDIR + "WBDev/Mod/";
                this.outputDirPiecewisePascal = rootName + WINBUGSDIR + "WBDev/Mod/";
                this.bbCompileScriptDir = rootName + WINBUGSDIR + "/";

            } else {
                this.outputDirPascal1 = "SEE/WBDiff/Mod/";
                this.outputDirPascal2 = "SEE/Pmetrics/Mod/";
                this.outputDirPascal2Mod = "SEE/WBDev/Mod/";
                this.outputDirPiecewisePascal = "SEE/WBDev/Mod/";
                this.bbCompileScriptDir = rootName + "/";
            }

            setParser(p);
        } catch (IOException ex) {
            Logger.getLogger(ConverterProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("FINE InitParser() ");
    }

    public void setSolverType(int solverType) {
        this.solverType = solverType;
    }

    public void setTotCov(int tot) {
        this.totCov = tot;
    }

    public String getScriptFilename(String output_dir, String modelFileName) {
        String format = "%s/%s.%s";
        return String.format(format, output_dir, modelFileName, SCRIPT_FILE_SUFFIX);
    }

    public void createBlocks(File outputDirectory) throws IOException {
        parser.initialise();
        if (outputDirectory == null) {
            throw new NullPointerException("The output directroy is NULL");
        }

        createFunctionTrees();  // Must be called first.
        createVariabilityMap(); // Must be called second to load variability scope for parameters.
        createObservationMap(); // Must be called after Function trees
        createTrialDesignMaps();
        createParameterMap();
        createStepMap();
        createStepTrees(); // Re-order so the column mappings established prior to structural block creation.
        createStateMap();
        createCovariateMap();

        createParameterTrees();
        createStructuralTrees();
        createBlockTrees();
        createCovariateTrees();

        if (permit_objective_ETAs) {
            doObjectiveETAs();
        }
        sortElementOrdering();
    }

    private void createBlockTrees() {
        buildPartTrees(sd.getBlocksMap());
    }

    private void createCovariateMap() {
        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }

        List<CovariateModel> cmts = def.getListOfCovariateModel();
        if (cmts == null) {
            return;
        }

        for (CovariateModel cmt : cmts) {
            getCovariateBlocks().add(new CovariateBlockImpl(cmt, this));
        }
    }

    private void createCovariateTrees() {
        for (CovariateBlock cb : getCovariateBlocks()) {
            cb.buildTrees();
        }
    }

    /**
     * Expose the estimation step constructor so that it can be replaced as
     * necessary.<br/>
     * Allows a Lexer to slot in a replace Estimation step Constructor if the
     * default step logic is too restrictive.
     *
     * @return EstimationStep
     */
    private EstimationStep createEstimationStep(Estimation est_) {
        return new EstimationStepImpl(est_, this);
    }

    private void buildPartTrees(Map<String, Part> parts) {
        if (parts == null) {
            return;
        }
        Set<String> keys = parts.keySet();
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Part part = parts.get(key);
            if (part == null) {
                continue;
            }
            part.buildTrees();
        }
    }

    /**
     * Create the trees representing model functions.
     */
    private void createFunctionTrees() {
        if (dom == null) {
            return;
        }
        List<FunctionDefinition> funcs = dom.getListOfFunctionDefinition();

        if (funcs == null) {
            return;
        } else if (funcs.isEmpty()) {
            return;
        }

        for (FunctionDefinition func : funcs) {
            BinaryTree bt = tm.newInstance(func);
            if (bt == null) {
                throw new IllegalStateException("The funcion assignment block cannot be null.");
            }
            sd.getStatementsMap().put(func, bt);
            sd.getFunctions().add(func);
        }
    }

    private void createObservationMap() {
        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }

        List<ObservationModel> observations = def.getListOfObservationModel();
        for (ObservationModel ob : observations) {
            if (ob == null) {
                continue;
            }
            ObservationBlock obb = new ObservationBlockImpl(ob, this);
            obb.buildTrees();
            getObservationBlocks().add(obb);
        }
    }

    /**
     * Create a parameter block
     *
     * @param pmt Parent parameter block
     * @return ParameterBlock
     */
    private ParameterBlock createParameterBlock(ParameterModel pmt) {
        return new ParameterBlockImpl(pmt, this);
    }

    @Override
    public void createParameterContext() {
        if (created_parameter_context) {
            return;
        }

        ParameterBlock pdb = getParameterBlock();
        if (pdb == null) {
            return;
        }

        List<PopulationParameter> params = pdb.getParameters();
        for (PopulationParameter p : params) {
            getParameterContext(p);
        }

        doParameterContext_ObservationBlocks();
        doParameterContext_Individuals(pdb);

        created_parameter_context = true; // So not to call this method twice.
    }

    private void createParameterMap() {
        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }

        List<ParameterModel> param_models = def.getListOfParameterModel();
        if (param_models.isEmpty()) {
            return;
        }

        for (ParameterModel pm : param_models) {
            if (pm == null) {
                continue;
            } else {
                sd.getParameterBlocks().add(new ParameterBlockImpl(pm, this));
            }
        }
    }

    private void createParameterTrees() {
        for (ParameterBlock pb : sd.getParameterBlocks()) {
            if (pb == null) {
                continue;
            }
            pb.buildTrees();
        }
    }
   
    private File createScript(File src, File outputDirectory) throws Exception {
        if (outputDirectory == null) {
            throw new NullPointerException("The output directroy is NULL");
        }
        model_filename = src.getName().replace(".xml", "");
        String output_filename = getScriptFilename(outputDirectory.getAbsolutePath(), model_filename);
        PrintWriter fout = new PrintWriter(output_filename);

        Parser p = (PascalParser) parser;
        p.writePreMainBlockElements(fout, src);
        p.writeAllModelFunctions(outputDirectory);
        if (p.hasDiffEquations) {
            p.adjustDataFile();
        }
        parser.cleanUp();

        File f = new File(output_filename);

        return f;
    }

    private void createStateMap() {
        if (dom == null) {
            return;
        }

        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }

        List<StructuralModel> smts = def.getListOfStructuralModel();
        if (smts.isEmpty()) {
            return;
        }

        for (StructuralModel sm : smts) {
            if (sm == null) {
                continue;
            } else {
                StructuralBlock sb = createStructuralBlock(sm);
                if (macro_input_map.containsKey(sm)) {
                    sb.setPKMacros(macro_input_map.get(sm));
                }
                if (macro_output_map.containsKey(sm)) {
                    sb.setPKMacroOutput(macro_output_map.get(sm));
                }
                getStructuralBlocks().add(sb);
            }
        }
    }

    /**
     * Create the step map of registered tasks to be done to a model.
     */
    private void createStepMap() {
        if (dom == null) {
            return;
        }

        ModellingSteps steps_ = dom.getModellingSteps();
        TrialDesign td = dom.getTrialDesign();

        if (steps_ == null) {
            throw new IllegalStateException("The PharmML file does not have the required 'ModellingSteps' section.");
        }
        if (td == null) {
            throw new NullPointerException("Model trial design is not specified.");
        }

        // Parse for estimations first and then simulations.
        for (JAXBElement<? extends CommonModellingStep> o : steps_.getCommonModellingStep()) {
            CommonModellingStep step = o.getValue();
            if (step == null) {
                continue;
            }
            if (isEstimation(step)) {
                Estimation est_step = (Estimation) step;
                if (est_step.getOid() == null) {
                    throw new IllegalStateException("Estimation step OID cannot be NULL");
                }
                sd.getStepsMap().put(est_step.getOid(), createEstimationStep(est_step));
            }
        }

    }

    private void createStepTrees() {
        buildPartTrees(sd.getStepsMap());
    }

    private StructuralBlock createStructuralBlock(StructuralModel sm) {
        return new StructuralBlockImpl(sm, this);
    }

    private void createStructuralTrees() {
        for (StructuralBlock sb : getStructuralBlocks()) {
            if (sb == null) {
                continue;
            }
            sb.buildTrees();
        }
    }

    @Override
    public BinaryTree createTree(Object o) {
        BinaryTree bt = tm.newInstance(o);
        addStatement(o, bt);
        updateNestedTrees();
        return bt;
    }

    @Override
    public void addStatement(Object element, BinaryTree tree) {
        if (element != null && tree != null) {
            sd.getStatementsMap().put(element, tree);
        }
    }

    public boolean addStatement(NestedTreeRef ref) {
        if (ref != null) {
            if (ref.element != null && ref.bt != null) {
                if (sd.getStatementsMap().containsKey(ref.element)) {
                    sd.getStatementsMap().remove(ref.element);
                }
                sd.getStatementsMap().put(ref.element, ref.bt);
                return true;
            }
        }

        return false;
    }

    /**
     * Factory method to create a trial design block. The trial design block
     * does basic processing of a trial design schema in PharmML. If the default
     * processing of TD is not appropriate, override this method and replace
     * with different implementation of the TrialDesign block.
     *
     * @param td Trial Design Model
     * @return TrialDesignBlock
     */
    private TrialDesignBlock createTrialDesignBlock(TrialDesign td) {
        return new TrialDesignBlockImpl(td, this);
    }

    private void createTrialDesignMaps() throws IOException {
        if (dom == null) {
            return;
        }

        TrialDesign td = dom.getTrialDesign();

        if (td == null) {
            return;
        } else {
            sd.setTrialDesignBlock(createTrialDesignBlock(td));
        }

        sd.getBlocksMap().put(getTrialDesign().getName(), getTrialDesign()); // Use the same event structure as step classes.
    }

    private void createVariabilityMap() {
        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }

        List<VariabilityDefnBlock> vms = def.getListOfVariabilityModel();
        if (vms == null) {
            return;
        }

        for (VariabilityDefnBlock vm : vms) {
            if (vm == null) {
                continue;
            }
            VariabilityBlock vb = new VariabilityBlockImpl(vm, this);
            sd.getVariabilityBlocks().add(vb);
        }
    }

    private void doObjectiveETAs() {
        List<ObservationBlock> obs = sd.getObservationBlocks();
        ParameterBlock pm = getParameterBlock();

        for (ObservationBlock ob : obs) {
            if (ob == null) {
                continue;
            }
            if (ob.hasIndividualParameters()) {
                for (IndividualParameter ip : ob.getIndividualParameters()) {
                    if (ip == null) {
                        continue;
                    }
                    pm.addIndividualParameter(ip);
                }
                setMixedEffect(true);
            }
        }
    }

    private void doParameterContext_CovariateRelation(List<CovariateRelation> covs) {
        if (covs == null) {
            return;
        }
        if (covs.isEmpty()) {
            return;
        }

        for (CovariateRelation cov : covs) {
            if (cov == null) {
                continue;
            }
            for (FixedEffectRelation fixed_effect : cov.getListOfFixedEffect()) {
                if (fixed_effect == null) {
                    continue;
                }
                SymbolRef ref = fixed_effect.getSymbRef();
                if (ref == null) {
                    continue;
                }
                Object o = accessor.fetchElement(ref);
                if (isPopulationParameter(o)) {
                    PopulationParameter p = (PopulationParameter) o;
                    ParameterContext ctx = getParameterContext(p);
                    if (ctx != null) {
                        ctx.theta_fixed_effect = true;
                    }
                }
            }
        }
    }

    private void doParameterContext_Individuals(ParameterBlock pdb) {
        // Look for 'Group' variable context, i.e. a generalised covariate.
        // Or a THETA context.
        List<IndividualParameter> ips = pdb.getIndividualParameters();
        for (IndividualParameter ip : ips) {
            if (ip == null) {
                continue;
            }

            if (ip.getStructuredModel() != null) {
                StructuredModel gm = ip.getStructuredModel();
                doParameterContext_LinearCovariate(gm.getLinearCovariate());
                doParameterContext_RandomEffects(gm.getListOfRandomEffects());
            }
        }
    }

    private void doParameterContext_LinearCovariate(LinearCovariate lc) {
        if (lc == null) {
            return;
        }

        doParameterContext_PopulationValue_(lc.getPopulationValue());
        doParameterContext_CovariateRelation(lc.getListOfCovariate());
    }

    private void doParameterContext_ObservationBlocks() {
        List<ObservationBlock> obs = getObservationBlocks();
        for (ObservationBlock ob : obs) {
            if (ob == null) {
                continue;
            }

            ObservationError error_model = ob.getObservationError();
            List<SymbolRef> residuals = new ArrayList<SymbolRef>();
            List<NestedTreeRef> error_trees = new ArrayList<NestedTreeRef>();
            if (isStructuredError(error_model)) {
                StructuredObsError goe = (StructuredObsError) error_model;
                error_trees.add(new NestedTreeRef(goe, tm.newInstance(goe)));

                StructuredObsError.ErrorModel error = goe.getErrorModel();
                if (error == null) {
                    throw new NullPointerException("Gaussian erorr model not specified.");
                }
                error_trees.add(new NestedTreeRef(error, tm.newInstance(error.getAssign())));

                StructuredObsError.Output output = goe.getOutput();
                if (output == null) {
                    throw new NullPointerException("Gaussian erorr model, output variable not specified.");
                }
                if (output.getSymbRef() == null) {
                    throw new NullPointerException("Gaussian Output variable not specified (symbId='" + goe.getSymbId() + "')");
                }

                StructuredObsError.ResidualError residual_error = goe.getResidualError();
                if (residual_error == null) {
                    throw new NullPointerException("Gaussian erorr model, residual variable not specified.");
                }
                if (residual_error.getSymbRef() == null) {
                    throw new NullPointerException("Gaussian Residual error variable not specified (symbId='" + goe.getSymbId() + "')");
                } else {
                    residuals.add(residual_error.getSymbRef());
                }
            } else if (isGeneralError(error_model)) {
                GeneralObsError goe = (GeneralObsError) error_model;
                error_trees.add(new NestedTreeRef(goe, tm.newInstance(goe)));
                error_trees.add(new NestedTreeRef(goe.getAssign(), tm.newInstance(goe.getAssign())));
            }

            for (NestedTreeRef ntr : error_trees) {
                if (ntr == null) {
                    continue;
                }
                for (Node node : ntr.bt.nodes) {
                    if (isFunctionCall(node.data)) {
                        FunctionCallType call = (FunctionCallType) node.data;
                        for (FunctionArgument arg : call.getListOfFunctionArgument()) {
                            if (arg == null) {
                                continue;
                            }

                            if (arg.getSymbRef() == null) {
                                continue; // Native type so 2 point squinting at the type.
                            }
                            PharmMLRootType element = accessor.fetchElement(arg.getSymbRef());
                            if (isPopulationParameter(element)) {
                                ParameterContext ctx = getParameterContext((PopulationParameter) element);
                                if (ctx != null) {
                                    ctx.error_model = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void doParameterContext_PopulationValue_(StructuredModel.LinearCovariate.PopulationValue pv) {
        if (pv == null) {
            return;
        }
        BinaryTree bt = tm.newInstance(pv);

        List<BinaryTree> bts = new ArrayList<BinaryTree>();
        bts.add(bt);
        for (NestedTreeRef ref : tm.getNestedTrees()) {
            if (ref == null) {
                continue;
            }
            bts.add(bt);
        }

        for (BinaryTree bt_ : bts) {
            for (Node node : bt_.nodes) {
                if (node == null) {
                    continue;
                }
                if (isSymbolReference(node.data)) {
                    SymbolRef sref = (SymbolRef) node.data;
                    PharmMLRootType element = accessor.fetchElement(sref);
                    if (isPopulationParameter(element)) {
                        PopulationParameter p = (PopulationParameter) element;
                        ParameterContext ctx = getParameterContext(p);
                        if (ctx == null) {
                            continue;
                        }
                        ctx.theta = true;
                    }
                }
            }
        }
    }

    private void doParameterContext_RandomEffects(List<ParameterRandomEffect> rfs) {
        if (rfs == null) {
            return;
        }
        if (rfs.isEmpty()) {
            return;
        }

        for (ParameterRandomEffect rf : rfs) {
            if (rf == null) {
                continue;
            }
            for (SymbolRef ref : rf.getSymbRef()) {
                if (ref == null) {
                    continue;
                }
                PharmMLRootType element = accessor.fetchElement(ref);
                if (element != null) {
                    List<BinaryTree> bts = new ArrayList<BinaryTree>();
                    bts.add(tm.newInstance(element));
                    for (NestedTreeRef nref : tm.getNestedTrees()) {
                        if (ref == null) {
                            continue;
                        }
                        bts.add(nref.bt);
                    }

                    for (BinaryTree bt : bts) {
                        for (Node node : bt.nodes) {
                            if (isVariableReference(node.data)) {
                                VarRefType vref = (VarRefType) node.data;
                                PharmMLRootType o = accessor.fetchElement(vref);
                                if (isPopulationParameter(o)) {
                                    PopulationParameter p = (PopulationParameter) o;
                                    ParameterContext ctx = getParameterContext(p);
                                    if (ctx == null) {
                                        continue;
                                    }
                                    ctx.omega = true;
                                }
                            } else if (isSymbolReference(node.data)) {
                                SymbolRef sref = (SymbolRef) node.data;
                                PharmMLRootType o = accessor.fetchElement(sref);
                                if (isPopulationParameter(o)) {
                                    PopulationParameter p = (PopulationParameter) o;
                                    ParameterContext ctx = getParameterContext(p);
                                    if (ctx == null) {
                                        continue;
                                    }
                                    ctx.omega = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Access the accessor handle
     *
     * @return Accessor
     */
    public Accessor getAccessor() {
        if (accessor == null) {
            accessor = new Accessor(dom);
        }
        return accessor;
    }

    @Override
    public Version getConverterVersion() {
        return converterVersion;
    }

    @Override
    public CovariateBlock getCovariateBlock() {
        List<CovariateBlock> cbs = getCovariateBlocks();
        if (cbs != null) {
            if (!cbs.isEmpty()) {
                return cbs.get(0);
            }
        }

        return null;
    }

    @Override
    public List<CovariateBlock> getCovariateBlocks() {
        return sd.getCovariateBlocks();
    }

    @Override
    public List<CovariateDefinition> getCovariates() {
        List<CovariateDefinition> list = new ArrayList<CovariateDefinition>();

        for (CovariateBlock cb : getCovariateBlocks()) {
            List<CovariateDefinition> covs = cb.getCovariates();
            for (CovariateDefinition cov : covs) {
                if (cov == null) {
                    continue;
                }
                list.add(cov);
            }
        }

        return list;
    }

    /**
     * Generate a conversion report.
     *
     * @param path Path to the generated file.
     * @return eu.ddmore.convertertoolbox.api.response.ConversionReport
     */
    private ConversionReport getCrxSuccessReport(File path) {
        ConversionReport report = new ConversionReportImpl();
        ConversionDetail_ detail = new ConversionDetail_();
        detail.addInfo("status", "script created.");
        if (path != null) {
            detail.setFile(path);
        }
        detail.setSeverity(ConversionDetail.Severity.INFO);
        report.addDetail(detail);
        report.setReturnCode(ConversionReport.ConversionCode.SUCCESS);
        return report;
    }

    @Override
    public DataFiles getDataFiles() {
        return data_files;
    }

    @Override
    public List<ObservationBlock> getErrorModels() {
        return getObservationBlocks();
    }

    /**
     * Generate a conversion report with an exception
     *
     * @param e Exception
     * @return eu.ddmore.convertertoolbox.api.response.ConversionReport
     */
    private ConversionReport getExceptionReport(Exception e) {
        ConversionReport report = new ConversionReportImpl();

        if (is_echo_exception) {
            e.printStackTrace(System.err);
        }

        ConversionDetail_ detail = new ConversionDetail_();
        detail.addInfo("error_message", e.getMessage());
        detail.setSeverity(ConversionDetail.Severity.ERROR);
        detail.setMessage(e.getMessage());
        report.addDetail(detail);
        report.setReturnCode(ConversionReport.ConversionCode.FAILURE);

        return report;
    }

    @Override
    public String getIndexSymbol(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getIndividualParameterIndex(String symbol) {
        Integer idx = -1;

        if (symbol != null) {
            if (!sd.getParameterBlocks().isEmpty()) {
                if (getParameterBlock() != null) {
                    List<IndividualParameter> ips = getParameterBlock().getIndividualParameters();
                    int i = 1;
                    for (IndividualParameter ip : ips) {
                        if (ip == null) {
                            continue;
                        }
                        String currentSymbol = ip.getSymbId();
                        if (currentSymbol != null) {
                            if (currentSymbol.equals(symbol)) {
                                idx = i;
                            }
                        }
                        i++;
                    }
                }
            }
        }

        return idx;
    }

    /**
     * Get the PharmML library instance bound to the converter.
     *
     * @return eu.ddmore.libpharmml.ILibPharmML
     */
    public ILibPharmML getLibrary() {
        return lib;
    }

    @Override
    public List<VariableDefinition> getLocalVariables() {
        if (!sd.getStructuralBlocks().isEmpty()) {
            StructuralBlock structuralBlock = getStructuralBlocks().get(0);
            if (structuralBlock != null) {
                return structuralBlock.getLocalVariables();
            } else {
                return new ArrayList<VariableDefinition>();
            }
        } else {
            return new ArrayList<VariableDefinition>();
        }
    }

    @Override
    public String getModelFilename() {
        return model_filename;
    }

    @Override
    public String getModelName() {
        String name = null;

        if (dom != null) {
            Name n = dom.getName();
            if (n == null) {
                return name;
            } else {
                name = n.getValue();
                if (name != null) {
                    name = name.replaceAll("\n", "");
                    name = name.replaceAll("\\s+", " ");
                    name = name.replace('\\', '/');
                }
            }
        }

        return name;
    }

    @Override
    public Integer getModelParameterIndex(String name) {
        Integer idx = -1;
        if (!sd.getParameterBlocks().isEmpty()) {
            if (getParameterBlock() != null) {
                idx = getParameterBlock().getParameterIndex(name);
            }
        }
        return idx;
    }

    @Override
    public List<PopulationParameter> getModelParameters() {
        if (!sd.getParameterBlocks().isEmpty()) {
            if (getParameterBlock() != null) {
                return getParameterBlock().getParameters();
            } else {
                return new ArrayList<PopulationParameter>();
            }
        } else {
            return new ArrayList<PopulationParameter>();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public List<ObservationBlock> getObservationBlocks() {
        return sd.getObservationBlocks();
    }

    @Override
    public ObservationParameter getObservationParameter(PopulationParameter p) {
        ObservationParameter op = null;
        for (ObservationBlock ob : getObservationBlocks()) {
            if (ob == null) {
                continue;
            }
            if (ob.isObservationParameter(p)) {
                op = ob.getObservationParameter(p);
                break;
            }
        }

        return op;
    }

    @Override
    public ParameterBlock getParameterBlock() {
        if (pb == null) {
            if (!sd.getParameterBlocks().isEmpty()) {
                pb = sd.getParameterBlocks().get(0); // Assume only 1 PM per model.
            } else {
                ParameterModel pmt = new ParameterModel();
                pmt.setBlkId("pm");
                ModelDefinition def = dom.getModelDefinition();
                if (def == null) {
                    def = new ModelDefinition();
                    dom.setModelDefinition(def);
                }
                def.getListOfParameterModel().add(pmt);
                pb = createParameterBlock(pmt);
            }
        }

        return pb;
    }

    private ParameterContext getParameterContext(PopulationParameter p) {
        ParameterContext ctx = null;

        if (p == null) {
            throw new NullPointerException("Parameter is NULL.");
        }
        if (param_context_map.containsKey(p)) {
            ctx = param_context_map.get(p);
        } else {
            ctx = new ParameterContext(p);
            param_context_map.put(p, ctx);
        }

        return ctx;
    }

    @Override
    public Map<PopulationParameter, ParameterContext> getParameterContextMap() {
        return param_context_map;
    }

    @Override
    public IParser getParser() {
        return parser;
    }

    /**
     * Get the PharmML resource instance bound to the converter.
     *
     * @return eu.ddmore.libpharmml.IPharmMLResource
     */
    public IPharmMLResource getResource() {
        return res;
    }

    public ScriptDefinition getScriptDefinition() {
        return sd;
    }

    @Override
    public List<String> getSimulationOutputNames() {
        List<String> names = new ArrayList<String>();
        SimulationStep step = getSimulationStep();
        SymbolReader z = parser.getSymbolReader();

        if (step != null) {
            List<SymbolRef> outputs = step.getContinuousList();
            for (SymbolRef ref : outputs) {
                PharmMLRootType element = accessor.fetchElement(ref);
                if (element != null) {
                    names.add(z.get(element));
                }
            }
        }

        return names;
    }

    @Override
    public Map<Integer, CommonVariableDefinition> getSimulationOutputs() {
        StructuralBlock block = getStrucuturalBlock();
        return getSimulationOutputs(block);
    }

    @Override
    public Map<Integer, CommonVariableDefinition> getSimulationOutputs(StructuralBlock sb) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimulationStep getSimulationStep() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getSortedVariabilityLevels() {
        return ordered_levels;
    }

    @Override
    public LanguageVersion getSource() {
        return source;
    }

    @Override
    public BinaryTree getStatement(Object key) {
        BinaryTree bt = null;

        if (key != null) {
            if (sd.getStatementsMap().containsKey(key)) {
                bt = sd.getStatementsMap().get(key);
                sd.getStatementsMap().remove(key);
            }
        }

        return bt;
    }

    @Override
    public Integer getStateVariableIndex(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<StructuralBlock> getStructuralBlocks() {
        return sd.getStructuralBlocks();
    }

    @Override
    public StructuralBlock getStrucuturalBlock() {
        if (currentSb == null) {
            if (!sd.getStructuralBlocks().isEmpty()) {
                currentSb = sd.getStructuralBlocks().get(0);
            }
        }

        return currentSb;
    }

    @Override
    public LanguageVersion getTarget() {
        return target;
    }

    private CommonVariableDefinition getTranslatedElement(Integer adm) {
        if (adm == null) {
            return null;
        }

        for (MacroOutput output : macro_output_map.values()) {
            if (output == null) {
                continue;
            }
            for (Input input : output.getListOfInput()) {
                if (input == null) {
                    continue;
                }
                if (isInt(input.getAdm())) {
                    IntValue i = (IntValue) input.getAdm();
                    if (i.getValue() == null) {
                        continue;
                    }
                    if (adm.intValue() == i.getValue().intValue()) {
                        return input.getTarget();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Translator getTranslator() {
        return tr;
    }

    @Override
    public TreeMaker getTreeMaker() {
        tm.setPermitDeclarationOnlyVariables(true);
        return tm;
    }

    @Override
    public TrialDesignBlock getTrialDesign() {
        return sd.getTrialDesignBlock();
    }

    @Override
    public IValidationReport getValidationReport() {
        return validation_report;
    }

    @Override
    public VariabilityBlock getVariabilityBlock(SymbolRef ref) {
        VariabilityBlock vb = null;

        if (ref != null) {
            String symbId = ref.getSymbIdRef();
            String blkId = ref.getBlkIdRef();

            if (blkId != null && symbId != null) {
                for (VariabilityBlock vb_ : sd.getVariabilityBlocks()) {
                    if (vb_ == null) {
                        continue;
                    }
                    if (blkId.equals(vb_.getName())) {
                        if (vb_.hasSymbolId(symbId)) {
                            vb = vb_;
                            break;
                        }
                    }
                }
            }

            // Look-up with just the symbol name identifier if
            // the BlkId is not specified.
            if (vb == null) {
                if (symbId != null) {
                    for (VariabilityBlock vb_ : sd.getVariabilityBlocks()) {
                        if (vb_ == null) {
                            continue;
                        }
                        if (vb_.hasSymbolId(symbId)) {
                            vb = vb_;
                            break;
                        }
                    }
                }
            }
        }

        return vb;
    }

    private boolean guessColumnDoseContext(BaseStep step, VariableDefinition v) {
        if (step == null || v == null) {
            return false;
        }

        if (!step.hasTemporalDoseEvent()) {
            return false;
        }

        TemporalDoseEvent tde = step.getTemporalDoseEvent();
        if (tde == null) {
            return false;
        }

        if (v.equals(tde.getTargetElement())) {
            return false; // DT variable so can't be a dose.
        }
        return true; // If reach here, infer it is a dose variable.
    }

    @Override
    public boolean hasDoneEstimation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasEstimation() {
        for (Part step : sd.getStepsMap().values()) {
            if (step instanceof EstimationStep) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasEvents() {
        StructuralBlock sb = getStrucuturalBlock();
        if (sb != null) {
            return sb.hasEvents();
        }
        return false;
    }

    @Override
    public boolean hasExternalDatasets() {
        return !data_files.getExternalDataSets().isEmpty();
    }

    /**
     * Flag if the Lexer has adjustment an external dataset column usage
     * declaration from a Monolix (Regressor) to a NONMEM (covariate) setting.
     *
     * @return boolean
     */
    public boolean hasResetColumnFromRegToCov() {
        return hasResetColumnUsageRegToCov;
    }

    @Override
    public boolean hasSimulation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasStatement(Object key) {
        if (key != null) {
            return sd.getStatementsMap().containsKey(key);
        }
        return false;
    }

    @Override
    public boolean hasTrialDesign() {
        return getTrialDesign() != null;
    }

    @Override
    public boolean hasWashout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isADMEScript() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAtLastStructuralBlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCategoricalCovariate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCategoricalDiscrete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDDE() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDiscrete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDuplicateVariablesPermitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFilterReservedWords() {
        return filter_reserved_words;
    }

    @Override
    public boolean isGeneratedDosingParameter(PopulationParameter p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIndexFromZero() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIndividualParameter_(String symbol) {
        if (symbol != null) {
            if (!sd.getParameterBlocks().isEmpty()) {
                if (getParameterBlock() != null) {
                    List<IndividualParameter> ips = getParameterBlock().getIndividualParameters();
                    for (IndividualParameter ip : ips) {
                        if (ip == null) {
                            continue;
                        }
                        String currentSymbol = ip.getSymbId();
                        if (currentSymbol == null) {
                            continue;
                        }
                        if (currentSymbol.equals(symbol)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Re-map the dose target post PK Macro translation. Override as necessary
     *
     * @param mapping Mapping Declaration
     * @param table Column Looker-Upper
     */
    private void remapColumnPostPKMacroTranslation(ColumnMapping mapping, TabularDataset table) {
        if (mapping == null) {
            return;
        }
        if (mapping.getListOfTargetMapping().isEmpty()) {
            return;
        }

        List<TargetMapping> targets = new ArrayList<TargetMapping>();
        targets.addAll(mapping.getListOfTargetMapping());

        ColumnReference cref = mapping.getColumnRef();
        if (cref == null) {
            throw new NullPointerException("Column reference in an element mapping is NULL");
        }

        ColumnDefinition col = table.getColumn(cref);
        if (col == null) {
            throw new NullPointerException("A column declaration is undefined (name='" + cref.getColumnIdRef() + "')");
        }

        mapping.getListOfTargetMapping().clear();
        if (DOSE.equals(col.getListOfColumnType().get(0))) {
            for (TargetMapping target : targets) {
                if (target == null) {
                    continue;
                }
                int size = target.getListOfMap().size();
                if (size > 0) {
                    remapDoseTarget(table, mapping, target.getListOfMap());
                } else {
                    throw new UnsupportedOperationException("Unrecognised dosing target cardinality (name='" + cref.getColumnIdRef() + "')");
                }
            }
        } else {
            throw new UnsupportedOperationException("Unrecognised target mapping context on a data column (name='" + cref.getColumnIdRef() + "')");
        }
    }

    private void remapDoseTarget(TabularDataset table, ColumnMapping mapping, List<MapType> maps) { // da CCopI_Mono_GIT

        if (mapping == null || maps == null || table == null) {
            return;
        }
        if (maps.isEmpty()) {
            return;
        }

        Piecewise pw = mapping.createPiecewise();
        boolean flag = false;
        for (MapType map : maps) {
            if (map == null) {
                throw new NullPointerException("A dose target mapping record is NULL");
            }
            Integer adm = map.getAdmNumber();
            String dataSymbol = map.getDataSymbol();

            if (adm == null) {
                throw new NullPointerException("The administration number is not specified");
            }

            CommonVariableDefinition v = getTranslatedElement(adm);
            if (v == null) {
//                throw new IllegalStateException("Unable to determine model element from the translated PK macro"); // 8/6/2016
                continue;
            }
            flag = true;
            if (dataSymbol != null) {
                remapDoseTargetWithAdministrationAndCompartment(table, v, pw, dataSymbol);
            } else {
                remapDoseTargetWithAdministrationOnly(table, v, pw);
            }
        }
        if (!flag) {
            throw new IllegalStateException("Unable to determine model element from the translated PK macro");
        }
    }

    private static Piece addPiece(Piecewise block, boolean createCondition) {
        Piece piece = null;

        if (block != null) {
            piece = new Piece();
            block.getListOfPiece().add(piece);

            if (createCondition) {
                Condition cond = new Condition();
                piece.setCondition(cond);
            }
        }
        return piece;
    }

    private void remapDoseTargetWithAdministrationAndCompartment(TabularDataset table, CommonVariableDefinition target, Piecewise pw, String dataSymbol) {
        if (table == null || target == null || pw == null || dataSymbol == null) {
            return;
        }
        ColumnDefinition adm_col = table.getColumn(ADM);
        ColumnDefinition dose_col = table.getColumn(DOSE);

        if (dose_col == null) {
            throw new IllegalStateException("The dose column is not specified in the model external dataset declaration");
        }
        if (adm_col == null) {
            throw new IllegalStateException("The administration column (ADM) is not specified in the model external dataset declaration");
        }

        Piece piece = addPiece(pw, true);
        piece.setValue(symbolRef(target, accessor));

        LogicBinOp ADM_clause = logicalBinaryOperator("eq", new Object[]{adm_col, Integer.valueOf(dataSymbol)}, accessor);
        LogicBinOp DOSE_clause = logicalBinaryOperator("gt", new Object[]{dose_col, 0}, accessor);
        LogicBinOp lbop = logicalBinaryOperator("and", new Object[]{ADM_clause, DOSE_clause}, accessor);
        piece.getCondition().setLogicBinop(lbop);
    }

    private void remapDoseTargetWithAdministrationOnly(TabularDataset table, CommonVariableDefinition target, Piecewise pw) {
        if (table == null || target == null || pw == null) {
            return;
        }
        ColumnDefinition dose_col = table.getColumn(DOSE);

        if (dose_col == null) {
            throw new IllegalStateException("The dose column is not specified in the model external dataset declaration");
        }

        Piece piece = addPiece(pw, true);
        piece.setValue(symbolRef(target, accessor));

        LogicBinOp DOSE_clause = logicalBinaryOperator("gt", new Object[]{dose_col, 0}, accessor);
        piece.getCondition().setLogicBinop(DOSE_clause);
    }

    @Override
    public void setCurrentStructuralBlock(StructuralBlock sb) {
        if (sb == null) {
            throw new NullPointerException("Structural block is NULL.");
        }
        currentSb = sb;
    }

    @Override
    public void setDeactivateIdFactory(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDom(PharmML dom) {
        this.dom = dom;
    }

    @Override
    public void setDuplicateVariablesPermitted(boolean decision) {
        throw new UnsupportedOperationException();
    }

    /**
     * Instruct a converter to actively
     *
     * @param decision
     */
    @Override
    public void setEchoException(boolean decision) {
        is_echo_exception = decision;
    }

    @Override
    public void setFilterReservedWords(boolean decision) {
        filter_reserved_words = decision;
    }

    @Override
    public void setIndexFromZero(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIsolateConditionalDoseVariable(boolean decision) {
        isolate_conditional_dose_variable = decision;
    }

    @Override
    public void setIsolateDoseTimingVariable(boolean decision) {
        isolate_dt = decision;
    }

    @Override
    public void setLexOnly(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoadOnly(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMixedEffect(boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the model filename if not explicitly set by filepath on the
     * performConvert() method.
     *
     * @param model_filename_ Path/Name to the inputted model file.
     */
    public void setModelFilename(String model_filename_) {
        model_filename = model_filename_;
    }

    @Override
    public void setPermitEmptyTrialDesignBlock(boolean decision) {
        permitEmptyTrialDesignBlock = decision;
    }

    @Override
    public void setRemoveIllegalCharacters(boolean decision) {
        remove_illegal_chars = decision;
    }

    /**
     * Set the run identifier for output file stem.
     *
     * @param run_id_ Run Identifier
     */
    public void setRunId(String run_id_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSaveRenamedSymbolList(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSaveSimulationOutput(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setScriptDefinition(ScriptDefinition sd_) {
        sd = sd_;
    }

    @Override
    public void setSortParameterModel(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSortParameterModelByClustering(boolean decision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSortParameterModelByContext(boolean decision) {
        sort_parameter_model_by_context = decision;
    }
//

    @Override
    public void setSortStructuralModel(boolean decision) {
        sort_structural_model = decision;
    }

    @Override
    public void setTranslator(Translator tr_) {
        if (tr_ != null) {
            tr = tr_;
        }
    }

    public void setTreeMaker(TreeMaker tm) {
        if (tm != null) {
            this.tm = tm;
        }
    }

    public void setUseGlobalConditionalDoseVariable(boolean decision) {
        useGlobalConditionalDoseVariable = decision;
    }

    public void setUsePiecewiseAsEvents(boolean decision) {
        usePiecewiseAsEvents = decision;
    }

    @Override
    public void setValidateXML(boolean decision) {
        validate_xml = decision;
    }

    private void sortElementOrdering() throws NullPointerException, IOException {
        if (sort_parameter_model_by_context) {
            sortParameterBlockByContext(getParameterBlock());
        }
        if (sort_structural_model) {
            sortStructuralBlock((StructuralBlockImpl) getStrucuturalBlock());
        }
    }

    private void sortParameterBlockByContext(ParameterBlock pb) throws NullPointerException, IOException {
        createParameterContext();
        List<PharmMLRootType> old_list = new ArrayList<PharmMLRootType>();
        List<PharmMLRootType> new_list = new ArrayList<PharmMLRootType>();

        old_list.addAll(pb.getListOfDeclarations());

        List<DependencyRef> refs = new ArrayList<DependencyRef>();
        Map<Object, DependencyRef> dep_map = new HashMap<Object, DependencyRef>();
        for (PharmMLRootType o : old_list) {
            DependencyRef ref = new DependencyRef(o);
            BinaryTree bt = tm.newInstance(o);
            addDependency(ref, bt);

            List<NestedTreeRef> ntrefs = tm.getNestedTrees();
            for (NestedTreeRef ntref : ntrefs) {
                addDependency(ref, ntref.bt);
            }

            refs.add(ref);
            dep_map.put(o, ref);
        }

        for (int i = 0; i < refs.size(); i++) {
            DependencyRef ref = refs.get(i);
            if (ref.hasDependendsUpon()) {
                for (int j = 0; j < ref.getDependsUpon().size(); j++) {
                    PharmMLElement depends_upon = ref.getDependsUpon().get(j);
                    DependencyRef other_ref = dep_map.get(depends_upon);
                    if (other_ref != null) {
                        if (other_ref.hasDependendsUpon()) {
                            for (int k = 0; k < other_ref.getDependsUpon().size(); k++) {
                                ref.addDependency(other_ref.getDependsUpon().get(k));
                            }
                        }
                    }
                }
            }
        }

        List<PharmMLElement> elements_under_consideration = createElementsUnderConsideration(refs);
        if (elements_under_consideration.isEmpty()) {
            return;
        }
        updateDependencyContext(elements_under_consideration, refs);

        dep_map = new HashMap<Object, DependencyRef>();
        for (DependencyRef ref : refs) {
            dep_map.put(ref.getElement(), ref);
        }

        // Get the individuals first.
        // Use those to fix the parameter order in the 1st instance.
        List<IndividualParameter> ips = pb.getIndividualParameters();
        if (ips.size() > 0) {
            old_list.removeAll(ips);

            // Assign the primary THETAs first.
            for (IndividualParameter ip : ips) {
                if (ip == null) {
                    continue;
                }
                DependencyRef ref = dep_map.get(ip);
                if (ref == null) {
                    continue;
                }

                List<PharmMLElement> depends_upon = ref.getDependsUpon();
                for (PharmMLElement o : depends_upon) {
                    if (isPopulationParameter(o)) {
                        PopulationParameter p = (PopulationParameter) o;
                        ParameterContext ctx = getParameterContext(p);
                        if (ctx == null) {
                            continue;
                        }
                        if (ctx.theta) {
                            if (!new_list.contains(p)) {
                                new_list.add(p);
                            }
                            if (old_list.contains(p)) {
                                old_list.remove(p);
                            }
                        }
                    }
                }
            }
        }

        // Add the Error model scoped THETAs (if any).
        List<PharmMLElement> elements_to_remove = new ArrayList<PharmMLElement>();
        for (PharmMLRootType o : old_list) {
            if (isPopulationParameter(o)) {
                PopulationParameter p = (PopulationParameter) o;
                ParameterContext ctx = getParameterContext(p);
                if (ctx.error_model) {
                    if (!new_list.contains(p)) {
                        new_list.add(p);
                    }
                    if (!elements_to_remove.contains(p)) {
                        elements_to_remove.add(p);
                    }
                }
            }
        }
        if (!elements_to_remove.isEmpty()) {
            old_list.removeAll(elements_to_remove);
            elements_to_remove.clear();
        }

        // Added the fixed effect linked THETAs (if any)
        if (ips.size() > 0) {
            for (IndividualParameter ip : ips) {
                if (ip == null) {
                    continue;
                }
                DependencyRef ref = dep_map.get(ip);
                if (ref == null) {
                    continue;
                }

                List<PharmMLElement> depends_upon = ref.getDependsUpon();
                for (PharmMLElement o : depends_upon) {
                    if (isPopulationParameter(o)) {
                        PopulationParameter p = (PopulationParameter) o;
                        ParameterContext ctx = getParameterContext(p);
                        if (ctx == null) {
                            continue;
                        }
                        if (ctx.theta_fixed_effect) {
                            if (!new_list.contains(p)) {
                                new_list.add(p);
                            }
                            if (old_list.contains(p)) {
                                old_list.remove(p);
                            }
                        }
                    }
                }
            }
        }

        // Add the OMEGAs
        if (ips.size() > 0) {
            for (IndividualParameter ip : ips) {
                if (ip == null) {
                    continue;
                }
                DependencyRef ref = dep_map.get(ip);
                if (ref == null) {
                    continue;
                }

                List<PharmMLElement> depends_upon = ref.getDependsUpon();
                for (PharmMLElement o : depends_upon) {
                    if (isPopulationParameter(o)) {
                        PopulationParameter p = (PopulationParameter) o;
                        ParameterContext ctx = getParameterContext(p);
                        if (ctx == null) {
                            continue;
                        }
                        if (ctx.omega) {
                            if (!new_list.contains(p)) {
                                new_list.add(p);
                            }
                            if (old_list.contains(p)) {
                                old_list.remove(p);
                            }
                        }
                    }
                }
            }
        }

        // Add the ETAs (if any).
        if (ips.size() > 0) {
            for (IndividualParameter ip : ips) {
                if (ip == null) {
                    continue;
                }
                DependencyRef ref = dep_map.get(ip);
                if (ref == null) {
                    continue;
                }
                List<PharmMLElement> depends_upon = ref.getDependsUpon();
                for (PharmMLElement o : depends_upon) {
                    if (isRandomVariable(o)) {
                        if (!new_list.contains(o)) {
                            new_list.add((PharmMLRootType) o);
                        }
                        if (old_list.contains(o)) {
                            old_list.remove(o);
                        }
                    }
                }
            }
        }

        // Main PK study terms.
        if (ips.size() > 0) {
            for (IndividualParameter ip : ips) {
                if (!new_list.contains(ip)) {
                    new_list.add((PharmMLRootType) ip);
                }
                if (old_list.contains(ip)) {
                    old_list.remove(ip);
                }
            }
        }

        // Bolt on any remaining, sorting by dependency not the usual ETA, THETA, OMEGA nonsense.
        if (old_list.size() == 1) {
            new_list.addAll(old_list);
            old_list.clear();
        } else if (old_list.size() > 1) {
            refs.clear();
            for (PharmMLRootType o : old_list) {
                if (o == null) {
                    continue;
                }
                DependencyRef ref = dep_map.get(o);
                if (ref != null) {
                    if (!refs.contains(ref)) {
                        refs.add(ref);
                    }
                }
            }

            elements_under_consideration = createElementsUnderConsideration(refs);
            if (!elements_under_consideration.isEmpty()) {
                updateDependencyContext(elements_under_consideration, refs);
                DependencyGraph g = new DependencyGraph(refs);
                g.createVertices();
                int edgeCount = g.createEdges();
                if (edgeCount > 0) {
                    g.sort();
                    List<PharmMLElement> ordered_variables = g.getSortedElements();
                    for (PharmMLElement o : ordered_variables) {
                        if (!new_list.contains(o)) {
                            new_list.add((PharmMLRootType) o);
                        }
                        if (old_list.contains(o)) {
                            old_list.remove(o);
                        }
                    }
                }
            }

            // Tag on any remaining
            if (!old_list.isEmpty()) {
                for (PharmMLRootType o : old_list) {
                    if (!new_list.contains(o)) {
                        new_list.add(o);
                    }
                }
            }
        }

        List<PharmMLElement> ordered_variables = new ArrayList<PharmMLElement>();
        ordered_variables.addAll(new_list);
        pb.setOrderedVariableList(ordered_variables);

        if (hasEstimation()) {
            EstimationStep est = getEstimationStep();
            if (est != null) {
                est.update();
            }
        }
    }

    private void sortStructuralBlock(StructuralBlock sb) throws NullPointerException, IOException {
        if (sb == null) {
            return;
        }

        List<PharmMLRootType> list = sb.getListOfDeclarations();
        List<DependencyRef> refs = new ArrayList<DependencyRef>();
        Map<Object, DependencyRef> dep_map = new HashMap<Object, DependencyRef>();
        for (PharmMLRootType o : list) {
            DependencyRef ref = new DependencyRef(o);
            BinaryTree bt = tm.newInstance(o);

            List<NestedTreeRef> ntrefs = tm.getNestedTrees();
            List<NestedTreeRef> local_ntrefs = new ArrayList<NestedTreeRef>();
            local_ntrefs.addAll(ntrefs);

            addDependency(ref, bt);
            for (NestedTreeRef ntref : local_ntrefs) {
                addDependency(ref, ntref.bt);
            }

            refs.add(ref);
            dep_map.put(o, ref);
        }

        for (int i = 0; i < refs.size(); i++) {
            DependencyRef ref = refs.get(i);
            if (ref.hasDependendsUpon()) {
                for (int j = 0; j < ref.getDependsUpon().size(); j++) {
                    PharmMLElement depends_upon = ref.getDependsUpon().get(j);
                    DependencyRef other_ref = dep_map.get(depends_upon);
                    if (other_ref != null) {
                        if (other_ref.hasDependendsUpon()) {
                            for (int k = 0; k < other_ref.getDependsUpon().size(); k++) {
                                ref.addDependency(other_ref.getDependsUpon().get(k));
                            }
                        }
                    }
                }
            }
        }
        List<PharmMLElement> elements_under_consideration = createElementsUnderConsideration(refs);

        if (elements_under_consideration.isEmpty()) {
            return;
        }
        updateDependencyContext(elements_under_consideration, refs);

        DependencyGraph g = new DependencyGraph(refs);

        g.createVertices();
        int edgeCount = g.createEdges();
        if (edgeCount == 0) {
            return;
        }

        g.sort();

        List<PharmMLElement> ordered_variables = g.getSortedElements();
        if (ordered_variables.isEmpty()) {
            return;
        }

        sb.setOrderedVariableList(ordered_variables);
    }

    @Override
    public String toString() {
        return String.format("PharmMLToWINBUGSConverter [source=%s, target=%s, converterVersion=%s]", source, target, converterVersion);
    }

    /**
     * Function called to translate PK macros.<br/>
     * Override to change application logic.
     */
    private void translatePKMacros() {
        if (!translate_macros) {
            return;
        }

        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }

        List<StructuralModel> sms_with_macros = new ArrayList<StructuralModel>();
        List<StructuralModel> sms = def.getListOfStructuralModel();

        Accessor a = new Accessor(dom);
        for (StructuralModel sm : sms) {
            if (sm == null) {
                continue;
            }

            PKMacroList macros = a.getPKMacros(sm);
            if (macros == null) {
                continue;
            }
            if (macros.getListOfMacro().isEmpty()) {
                continue;
            }
            sms_with_macros.add(sm);
        }

        if (sms_with_macros.size() == 0) {
            return;
        }

        if (tr == null) {
            throw new NullPointerException("The Macro translator is NULL.");
        }

        for (StructuralModel sm : sms_with_macros) {
            try {
                MacroOutput output = tr.translate(sm, target_level, ((Parser) getParser()).getIndependentVariable());
//                MacroOutput output = tr.translate(sm, target_level);
                StructuralModel sm_translated = output.getStructuralModel();
                if (!replace(dom, sm, sm_translated)) {
                    throw new IllegalStateException("PK macros translation failed (blkId='" + sm.getBlkId() + "')");
                }

                // Register the macro input and outputs so that they can be associated 
                // with the translated structural block later on.
                PKMacroList macros = accessor.getPKMacros(sm);

                if (!macro_output_map.containsKey(sm_translated)) {
                    macro_output_map.put(sm_translated, output);
                }
                if (!macro_input_map.containsKey(sm_translated)) {
                    macro_input_map.put(sm_translated, macros.getListOfMacro());
                }
            } catch (InvalidMacroException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        updatePKMacroDataMappings();
    }

    private static boolean replace(PharmML dom, StructuralModel old_sm, StructuralModel new_sm) {
        if (dom == null || old_sm == null || new_sm == null) {
            return false;
        }

        String blkId = old_sm.getBlkId();
        if (blkId == null) {
            return false;
        }

        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return false;
        }

        List<StructuralModel> sms_original = def.getListOfStructuralModel();
        if (sms_original == null) {
            return false;
        }
        if (sms_original.isEmpty()) {
            return false;
        }

        List<StructuralModel> sms = new ArrayList<StructuralModel>();
        sms.addAll(sms_original);

        boolean foundBlock = false;
        for (StructuralModel sm : sms) {
            if (sm == null) {
                continue;
            }
            if (sm.equals(old_sm)) {
                foundBlock = true;
                break;
            }
        }

        if (!foundBlock) {
            return false;
        }

        // Replace the old structural model with the new one.
        new_sm.setBlkId(blkId);
        sms_original.clear();
        for (StructuralModel sm : sms) {
            if (sm == null) {
                continue;
            }
            if (sm.equals(old_sm)) {
                sms_original.add(new_sm);
            } else {
                sms_original.add(sm);
            }
        }

        return true;
    }

    @Override
    public void updateNestedTrees() {
        for (NestedTreeRef ref : tm.getNestedTrees()) {
            addStatement(ref);
        }
        tm.getNestedTrees().clear();
    }

    private void updatePKMacroDataMappings() {
        TrialDesign td = dom.getTrialDesign();
        if (td == null) {
            return;
        }

        List<ExternalDataSet> exds = td.getListOfExternalDataSet();
        if (exds == null) {
            return;
        }

        ModelDefinition def = dom.getModelDefinition();
        if (def == null) {
            return;
        }
        getAccessor();

        for (ExternalDataSet exd : exds) {
            if (exd == null) {
                continue;
            }

            TabularDataset table = new TabularDataset(exd.getDataSet(), this);
            for (PharmMLRootType element : exd.getListOfColumnMappingOrColumnTransformationOrMultipleDVMapping()) {
                if (!isColumnMapping(element)) {
                    continue;
                }
                remapColumnPostPKMacroTranslation((ColumnMapping) element, table);
            }
        }
    }

    @Override
    public boolean useCachedDependencyList() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPlottingBlock() {
        return false;
    }

    @Override
    public void setAddPlottingBlock(boolean addPlottingBlock) {
        throw new UnsupportedOperationException();
    }

    public static SymbolRef symbolRef(PharmMLRootType o, Accessor a) {
        String symbId = null;

        boolean addScope = false;

        if (isSymbolReference(o)) {
            return (SymbolRef) o;
        } else if (isCommonParameter(o)) {
            symbId = ((CommonParameter) o).getSymbId();
            addScope = true;
        } else if (isLocalVariable(o)) {
            symbId = ((VariableDefinition) o).getSymbId();
            addScope = true;
        } else if (isDerivative(o)) {
            symbId = ((DerivativeVariable) o).getSymbId();
            addScope = true;
        } else if (isIndividualParameter(o)) {
            symbId = ((IndividualParameter) o).getSymbId();
            addScope = true;
        } else if (isRandomVariable(o)) {
            symbId = ((ParameterRandomVariable) o).getSymbId();
            addScope = true;
        } else if (isIndependentVariable(o)) {
            symbId = ((IndependentVariable) o).getSymbId();
        } else if (isCovariate(o)) {
            symbId = ((CovariateDefinition) o).getSymbId();
            addScope = true;
        } else if (isFunctionParameter(o)) {
            symbId = ((FunctionParameter) o).getSymbId();
        } else if (isFunction(o)) {
            symbId = ((FunctionDefinition) o).getSymbId();
        } else if (isObservationError(o)) {
            symbId = ((ObservationError) o).getSymbId();
            addScope = true;
        } else if (isColumnDefinition(o)) {
            symbId = ((ColumnDefinition) o).getColumnId();
            addScope = false;
        } else if (isContinuousCovariate(o)) {
            ContinuousCovariate ccov = (ContinuousCovariate) o;

            // INFO: Assuming a unary application for this release. 
            for (CovariateTransformation trans : ccov.getListOfTransformation()) {
                if (trans == null) {
                    continue;
                }

                TransformedCovariate tc = trans.getTransformedCovariate();
                if (tc == null) {
                    continue;
                }

                symbId = tc.getSymbId();
                addScope = true;
                break;
            }
        } else if (isVariabilityLevelDefinition(o)) {
            VariabilityLevelDefinition level = (VariabilityLevelDefinition) o;
            symbId = level.getSymbId();
            addScope = true;
        } else if (isGeneralError(o)) {
            GeneralObsError goe = (GeneralObsError) o;
            symbId = goe.getSymbId();
            addScope = true;
        } else {
            throw new UnsupportedOperationException("Unsupported Symbol reference (src='" + o + "')");
        }

        if (symbId == null) {
            throw new NullPointerException("SymbId is NULL.");
        }

        SymbolRef ref = new SymbolRef();
        ref.setSymbIdRef(symbId);

        if (addScope) {
            String blkId = a.getBlockId(o);
            if (blkId == null) {
                throw new NullPointerException("BlkId is not known (symbId='" + symbId + "', class='" + crx.converter.engine.Utils.getClassName(o) + "')");
            }

            ref.setBlkIdRef(blkId);
        }

        return ref;
    }

    public static LogicBinOp logicalBinaryOperator(String pharmml_symbol, Object[] elements, Accessor a) {
        if (elements == null || pharmml_symbol == null || a == null) {
            throw new NullPointerException("A function argument cannot be NULL.");
        }

        LogicBinOp op = new LogicBinOp();
        op.setOp(pharmml_symbol);

        int addedElements = 0;
        for (Object o : elements) {
            if (isInteger(o)) {
                op.getContent().add(createScalar((Integer) o));
                addedElements++;
            } else if (isLogicalBinaryOperation(o)) {
                op.getContent().add(math_of.createLogicBinop((LogicBinOp) o));
                addedElements++;
            } else if (isColumnDefinition(o)) {
                ColumnDefinition col = (ColumnDefinition) o;
                ColumnReference ref = new ColumnReference();
                ref.setColumnIdRef(col.getColumnId());
                op.getContent().add(ds_of.createColumnRef(ref));
                addedElements++;
            }

        }
        if (addedElements != 2) {
            throw new IllegalStateException("A binary operator can only contains 2 elements.");
        }

        return op;
    }

    private static JAXBElement<IntValue> createScalar(Integer value) {
        IntValue o = new IntValue(value);
        return cof.createInt(o);
    }

    @Override
    public void checkForTrialDesignIfRandomisedModel() {
        boolean has_mixed_effect = false;
        for (StructuralBlock sb : getStructuralBlocks()) {
            if (sb.isMixedEffect()) {
                has_mixed_effect = true;
                break;
            }
        }

        if (has_mixed_effect) {
            if (getTrialDesign() == null) {
                throw new IllegalStateException("Trial design not included in model even though contains individual parameter terms.");
            }
        }
    }

    @Override
    public void addIndexSymbol(Object key, String value) {
        if (key != null && value != null) {
            if (!index_symbol_map.containsKey(key)) {
                index_symbol_map.put(key, value);
            }
        }
    }

    public boolean isIndividualParameterSym(String symbol) {
        if (symbol != null) {
            if (!sd.getParameterBlocks().isEmpty()) {
                if (getParameterBlock() != null) {
                    List<IndividualParameter> ipL = getParameterBlock().getIndividualParameters();
                    for (IndividualParameter ip : ipL) {
                        if (ip == null) {
                            continue;
                        }
                        String currentSymbol = ip.getSymbId();
                        if (currentSymbol == null) {
                            continue;
                        }
                        if (currentSymbol.equals(symbol)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private ConversionReport createWinBUGSModel(File outputDirectory, File src) {
        String run_id = "";
        try {
            if (run_id != null) {
                parser.setRunId(new Manager().generateRunId());
            } else {
                parser.setRunId(m.generateRunId());
            }
        } catch (Exception e) {
            return getExceptionReport(e);
        }

        try {
            setOutputDirectory(outputDirectory);
        } catch (Exception e) {
            return getExceptionReport(e);
        }

        try {
            loadPharmML(src);

        } catch (Exception e) {
            return getExceptionReport(e);
        }

        // Parse each of the available structural blocks.
        try {
            createBlocks(outputDirectory);
            File f = createScript(src, outputDirectory);
            return getCrxSuccessReport(f);
        } catch (Exception e) {
            return getExceptionReport(e);
        }
    }

    @Override
    public EstimationStep getEstimationStep() {
        for (Object o : sd.getStepsMap().values()) {
            if (o instanceof EstimationStepImpl) {
                return (EstimationStep) o;
            }
        }

        return null;
    }

    @Override
    public VariableDeclarationContext guessContext(VariableDefinition v) {
        if (v == null) {
            return VariableDeclarationContext.UNKNOWN;
        }

        if (v.getAssign() != null) {
            return VariableDeclarationContext.ASSIGNED;
        }

        String symbol = v.getSymbId();
        if (symbol == null) {
            return VariableDeclarationContext.UNKNOWN;
        }

        if (hasEstimation()) {
            EstimationStep est = getEstimationStep();
            if (est.hasTemporalDoseEvent()) {
                PharmMLRootType element = est.getTemporalDoseEvent().getTargetElement();
                if (isLocalVariable(element)) {
                    VariableDefinition o = (VariableDefinition) element;
                    if (symbol.equals(o.getSymbId())) {
                        return VariableDeclarationContext.DT;
                    }
                }
            }
        }

        EstimationStep est = getEstimationStep();
        boolean isConditionalDoseEventTarget = false;

        if (est != null) {
            if (est.isConditionalDoseEventTarget(v)) {
                isConditionalDoseEventTarget = true;
            }
        }
        if (isConditionalDoseEventTarget) {
            return VariableDeclarationContext.DOSE;
        }
        if (guessColumnDoseContext(est, v)) {
            return VariableDeclarationContext.DOSE;
        }

        return VariableDeclarationContext.UNKNOWN;
    }

    @Override
    public boolean hasDosing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasTranslatedPKMacros() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasUntranslatedPKMacros() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isIsolateConditionalDoseVariable() {
        return isolate_conditional_dose_variable;
//       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isIsolatingDoseTimingVariable() {
        return isolate_dt;
    }

    @Override
    public boolean isMixedEffect() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isModelParameter(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isObservationParameter(PopulationParameter pp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isObservationParameter(SymbolRef sr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isPermitEmptyTrialDesignBlock() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isRemoveIllegalCharacters() {
        return remove_illegal_chars;
    }

    @Override
    public boolean isSaveSimulationOutput() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isStateVariable(String name) {
        boolean isState = false;

        if (!sd.getStructuralBlocks().isEmpty() && name != null) {
            for (StructuralBlock sb : sd.getStructuralBlocks()) {
                if (sb == null) {
                    continue;
                }
                isState = sb.isStateVariable(name);
                if (isState) {
                    break;
                }
            }
        }

        return isState;
    }

    @Override
    public boolean isStructuralBlockWithDosing(StructuralBlock sb) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isTranslate() {
        return translate_macros;
    }

    @Override
    public boolean isTTE() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isUseGlobalConditionalDoseVariable() {
        return useGlobalConditionalDoseVariable;
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isUsePiecewiseAsEvents() {
        return usePiecewiseAsEvents;
    }

    @Override
    public boolean loadFunctionLibrary(File file) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void permitObjectiveETAs(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSortStructuralModelByClustering(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSortVariabilityLevels(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTerminateWithInvalidXML(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTranslate(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isIsolateGloballyScopedVariables() {
        return false; 
    }

    @Override
    public void setIsolateGloballyScopedVariables(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
