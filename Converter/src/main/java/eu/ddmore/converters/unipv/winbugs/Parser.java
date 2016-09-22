/**
 * *****************************************************************************
 * Copyright (C) 2016 - BMS University of Pavia - All rights reserved.
 * ****************************************************************************
 */
package eu.ddmore.converters.unipv.winbugs;

import crx.converter.engine.Accessor;
import crx.converter.engine.BaseEngine;
import static crx.converter.engine.BaseEngine.isBigInteger;
import static crx.converter.engine.BaseEngine.isBoolean;
import static crx.converter.engine.BaseEngine.isDouble;
import static crx.converter.engine.BaseEngine.isInteger;
import static crx.converter.engine.BaseEngine.isString_;
import crx.converter.engine.CategoryRef_;
import crx.converter.engine.ConversionDetail_;
import crx.converter.engine.FixedParameter;
import crx.converter.engine.Part;
import crx.converter.engine.PharmMLTypeChecker;
import static crx.converter.engine.PharmMLTypeChecker.isBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isColumnDefinition;
import static crx.converter.engine.PharmMLTypeChecker.isCommonParameter;
import static crx.converter.engine.PharmMLTypeChecker.isCorrelation;
import static crx.converter.engine.PharmMLTypeChecker.isCovariateTransform;
import static crx.converter.engine.PharmMLTypeChecker.isFalse;
import static crx.converter.engine.PharmMLTypeChecker.isInitialCondition;
import static crx.converter.engine.PharmMLTypeChecker.isInt;
import static crx.converter.engine.PharmMLTypeChecker.isLogicalBinaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isLogicalUnaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isObservationModel;
import static crx.converter.engine.PharmMLTypeChecker.isParameter;
import static crx.converter.engine.PharmMLTypeChecker.isParameterEstimate;
import static crx.converter.engine.PharmMLTypeChecker.isPiece;
import static crx.converter.engine.PharmMLTypeChecker.isProbOnto;
import static crx.converter.engine.PharmMLTypeChecker.isReal;
import static crx.converter.engine.PharmMLTypeChecker.isString;
import static crx.converter.engine.PharmMLTypeChecker.isTrue;
import static crx.converter.engine.PharmMLTypeChecker.isUnivariateDistribution;
import static crx.converter.engine.PharmMLTypeChecker.isVariableReference;
import static crx.converter.engine.PharmMLTypeChecker.isCommonVariable;
import static crx.converter.engine.PharmMLTypeChecker.isConstant;
import static crx.converter.engine.PharmMLTypeChecker.isDelay;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isFunction;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionCall;
import static crx.converter.engine.PharmMLTypeChecker.isFunctionParameter;
import static crx.converter.engine.PharmMLTypeChecker.isGeneralError;
import static crx.converter.engine.PharmMLTypeChecker.isIndependentVariable;
import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
import static crx.converter.engine.PharmMLTypeChecker.isInterpolation;
import static crx.converter.engine.PharmMLTypeChecker.isInterval;
import static crx.converter.engine.PharmMLTypeChecker.isJAXBElement;
import static crx.converter.engine.PharmMLTypeChecker.isLocalVariable;
import static crx.converter.engine.PharmMLTypeChecker.isMatrix;
import static crx.converter.engine.PharmMLTypeChecker.isMatrixSelector;
import static crx.converter.engine.PharmMLTypeChecker.isMatrixUnaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isNestedPiecewise;
import static crx.converter.engine.PharmMLTypeChecker.isObservationError;
import static crx.converter.engine.PharmMLTypeChecker.isPiecewise;
import static crx.converter.engine.PharmMLTypeChecker.isProbability;
import static crx.converter.engine.PharmMLTypeChecker.isProduct;
import static crx.converter.engine.PharmMLTypeChecker.isRandomVariable;
import static crx.converter.engine.PharmMLTypeChecker.isRhs;
import static crx.converter.engine.PharmMLTypeChecker.isScalarInterface;
import static crx.converter.engine.PharmMLTypeChecker.isSequence;
import static crx.converter.engine.PharmMLTypeChecker.isSum;
import static crx.converter.engine.PharmMLTypeChecker.isSymbolReference;
import static crx.converter.engine.PharmMLTypeChecker.isUnaryOperation;
import static crx.converter.engine.PharmMLTypeChecker.isVariabilityLevelDefinition;
import static crx.converter.engine.PharmMLTypeChecker.isVector;
import static crx.converter.engine.PharmMLTypeChecker.isVectorSelector;
import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.SymbolReader;
import crx.converter.engine.common.Continuous;
import crx.converter.engine.common.CorrelationRef;
import crx.converter.engine.common.IndividualParameterAssignment;
import crx.converter.engine.common.NestedPiecewiseParser;
import crx.converter.engine.common.ObservationParameter;
import crx.converter.engine.common.ParameterAssignmentFromEstimation;
import crx.converter.engine.common.SimulationOutput;
import crx.converter.spi.ILexer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import crx.converter.spi.blocks.CovariateBlock;
import crx.converter.spi.blocks.ObservationBlock;
import crx.converter.spi.blocks.ParameterBlock;
import crx.converter.spi.blocks.StructuralBlock;
import crx.converter.spi.blocks.VariabilityBlock;
import crx.converter.spi.steps.EstimationStep;
import crx.converter.spi.steps.SimulationStep;
import crx.converter.tree.BinaryTree;
import crx.converter.tree.Node;
import crx.converter.tree.TreeMaker;
import eu.ddmore.converters.unipv.winbugs.parts.EstimationStepImpl;
import eu.ddmore.converters.unipv.winbugs.parts.TaskParameters;
import eu.ddmore.converters.unipv.winbugs.parts.WinBugsParametersName;
import javax.xml.bind.JAXBElement;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.libpharmml.dom.IndependentVariable;
import eu.ddmore.libpharmml.dom.commontypes.AnnotationType;
import eu.ddmore.libpharmml.dom.commontypes.CategoryRef;
import eu.ddmore.libpharmml.dom.commontypes.CommonVariableDefinition;
import eu.ddmore.libpharmml.dom.commontypes.Delay;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.FalseBoolean;
import eu.ddmore.libpharmml.dom.commontypes.FunctionDefinition;
import eu.ddmore.libpharmml.dom.commontypes.FunctionParameter;
import eu.ddmore.libpharmml.dom.commontypes.InitialCondition;
import eu.ddmore.libpharmml.dom.commontypes.IntValue;
import eu.ddmore.libpharmml.dom.commontypes.Interpolation;
import eu.ddmore.libpharmml.dom.commontypes.Interval;
import eu.ddmore.libpharmml.dom.commontypes.LevelReference;
import eu.ddmore.libpharmml.dom.commontypes.Matrix;
import eu.ddmore.libpharmml.dom.commontypes.MatrixRow;
import eu.ddmore.libpharmml.dom.commontypes.MatrixRowValue;
import eu.ddmore.libpharmml.dom.commontypes.MatrixSelector;
import eu.ddmore.libpharmml.dom.commontypes.MatrixVectorIndex;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLElement;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.Product;
import eu.ddmore.libpharmml.dom.commontypes.RealValue;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.Scalar;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.maths.Binop;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.Equation;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.LogicBinOp;
import eu.ddmore.libpharmml.dom.maths.Piece;
import eu.ddmore.libpharmml.dom.maths.Piecewise;
import eu.ddmore.libpharmml.dom.maths.Uniop;
import eu.ddmore.libpharmml.dom.modeldefn.CategoricalCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.ContinuousCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateRelation;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.Distribution;
import eu.ddmore.libpharmml.dom.commontypes.Sequence;
import eu.ddmore.libpharmml.dom.commontypes.StringValue;
import eu.ddmore.libpharmml.dom.commontypes.Sum;
import eu.ddmore.libpharmml.dom.commontypes.TrueBoolean;
import eu.ddmore.libpharmml.dom.commontypes.Vector;
import eu.ddmore.libpharmml.dom.commontypes.VectorSelector;
import eu.ddmore.libpharmml.dom.commontypes.VectorValue;
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition;
import eu.ddmore.libpharmml.dom.dataset.ColumnMapping;
import eu.ddmore.libpharmml.dom.dataset.ColumnReference;
import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import eu.ddmore.libpharmml.dom.maths.Constant;
import eu.ddmore.libpharmml.dom.maths.LogicUniOp;
import eu.ddmore.libpharmml.dom.maths.MatrixUniOp;
import eu.ddmore.libpharmml.dom.modeldefn.CommonParameter;
import eu.ddmore.libpharmml.dom.modeldefn.Correlation;
import eu.ddmore.libpharmml.dom.modeldefn.FixedEffectRelation;
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ObservationError;
import eu.ddmore.libpharmml.dom.modeldefn.Parameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.modeldefn.Probability;
import eu.ddmore.libpharmml.dom.modeldefn.StructuralModel;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredModel;
import eu.ddmore.libpharmml.dom.modeldefn.StructuredObsError;
import eu.ddmore.libpharmml.dom.modeldefn.TransformedCovariate;
import eu.ddmore.libpharmml.dom.modeldefn.UncertML;
import eu.ddmore.libpharmml.dom.modeldefn.VariabilityDefnBlock;
import eu.ddmore.libpharmml.dom.modeldefn.VariabilityLevelDefinition;
import eu.ddmore.libpharmml.dom.modellingsteps.Estimation;
import eu.ddmore.libpharmml.dom.modellingsteps.EstimationOperation;
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps;
import eu.ddmore.libpharmml.dom.modellingsteps.OperationProperty;
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate;
import eu.ddmore.libpharmml.dom.probonto.DistributionParameter;
import eu.ddmore.libpharmml.dom.probonto.ParameterName;

import eu.ddmore.libpharmml.dom.uncertml.AbstractContinuousUnivariateDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.AbstractDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.CauchyDistribution;
import eu.ddmore.libpharmml.dom.uncertml.ContinuousValueType;
import eu.ddmore.libpharmml.dom.uncertml.GeometricDistribution;
import eu.ddmore.libpharmml.dom.uncertml.LogNormalDistribution;
import eu.ddmore.libpharmml.dom.uncertml.LogisticDistribution;
import eu.ddmore.libpharmml.dom.uncertml.NormalDistribution;
import eu.ddmore.libpharmml.dom.uncertml.NormalDistributionType;
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistribution;
import eu.ddmore.libpharmml.dom.uncertml.PositiveRealValueType;
import eu.ddmore.libpharmml.dom.uncertml.VarRefType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.UnsupportedDataTypeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;
import eu.ddmore.libpharmml.dom.probonto.ProbOnto;
import eu.ddmore.libpharmml.dom.trialdesign.ExternalDataSet;
import eu.ddmore.libpharmml.dom.trialdesign.MultipleDVMapping;
import eu.ddmore.libpharmml.pkmacro.translation.Input;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class Parser extends NestedPiecewiseParser {//BaseParser {

    protected static boolean DEBUG = false;
    protected PrintStream outDebug;
    protected String odeSolver;
    protected File tsoFile;
    protected PrintStream toSO;
    private String SODirName = "WinBugsSO/";
    private String toSOName = "SO.properties";
    private String resLabel = "residual";
    private String predLabel = "pred";
    protected String pascalFileNameSuffix = ".txt";
    protected String nCovContName = "n_cov_cont";
    protected String nCovCatName = "n_cov_cat";
    protected String[] dataCovKeywords = {"max_m_", "N_t_", "grid_"};
    protected List<String> dataToRemove = new ArrayList<>(); // dati da eliminare dal file dati  perchè non è utilizzato

    private List<String> resList = new ArrayList<>();
    private List<String> predList = new ArrayList<>();

    protected String num1Default = "0";
    protected String nIterDefault = "5000";
    protected String burnInDeafult = "1000";
    protected String nChainsDefault = "1";
    protected String thinUpdaterDefault = "1";
    protected String thinSamplesDefault = "1";
    protected String solverDefault = ConverterProvider.RK45SOLVER;
    protected String dicDefault = "false";
    protected String winbugsGUIDefault = "false";
    protected String thinUpdaterComment = "";
    protected String thinSamplesComment = "";
    protected String winBUgsGUIComment = "";

    protected String num1, totGrid, burnIn, nIter, nChains, thinUpdater, thinSamples, dic, winbugsGUI;

    private static final String parametersFile = "pascal.properties";
    protected String pascalAssignSymbol = ":=";

    protected String leftArrayBracket;
    protected String rightArrayBracket;
    private List<FixedParameter> fixed_parameters = new ArrayList<>();
    private HashMap<Object, String> transformation_stmt_map = new HashMap<Object, String>();
    protected ScriptDefinition sd = new ScriptDefinition();
    private ParameterBlock pb = null;

    public static String unassigned_symbol = "@";

    protected static final String WinbugsExecutionScriptName = "runWBScript.txt";
    protected static final String WinbugsCompileScriptTemplateName = "bbscript-BlackBox-WinBUGS-template.txt";
    protected static final String WinbugsCompileScriptName = "bbscript-BlackBox-WinBUGS.txt";
    protected static final String catCovTemplateFileName = "TemplateCovariate.txt";
    protected String covariatePascalName;

    protected static String winbugsTemplateDir;
    protected static String outputDir;
    protected static String bbCompileScriptDir;
    protected static String winbugsDir;
    protected static String jobDir;
    protected static String workingDir;
    protected String pascalBody;
    protected List<String> pmetricsList = new ArrayList<>();
    protected List<String> wbdevList = new ArrayList<>();
    protected List<String> pwList = new ArrayList<>();
    protected List<String> priorList = new ArrayList<>();

    /**
     * Format for combining 2 elements for a substraction.
     */
    private String binaryFormat = "(%s%s%s)";
    protected String modelName;
    protected int modelNum;
    protected final static String piecewiseSymb = "VECTOR";
    protected final static String piecePrefix = "piece_";
    protected int pwNum = 0;

    List<SymbolRef> listRefs = new ArrayList<>();
    protected boolean openAssign = false;
    protected boolean hasCovariate = false;
    protected boolean hasDiffEquations = false;
    protected boolean inPW = false;
    protected String pascalOdeCall = "";
    protected List<String> piecewiseFileList = new ArrayList<>();
    protected VariabilityLevelDefinition priorLevel;
    protected Map<PharmMLRootType, String> priorPar = new HashMap<>();

    // char constants used to specify the loop of a statement 
    // they are setup in the getSymbol method
    // that returns the lvalue of a statement
    // used in the printLines method (called by closedLoops)
    // at the end of writeModelFunction method
    protected final static char INDIV_INDEX = 's'; // subject
    protected final static char TIME_INDEX = 't'; // time
    protected final static char BOTH_INDEX = 'b';
    protected final static char NO_INDEX = ' ';
    protected final static char INDQ_INDEX = 'q';
    protected final static char INDM_INDEX = 'm';

    // pharmml filename used to check assignements of simpleP_Parameters
    // it is setup in the method performConvert of ConverterProvider
    private File xmlFileName;
    private List<SymbolRef> simpleP_Parameters = new ArrayList<>(); // NECESSARI per inserimento in mappa 
    private List<SymbolRef> localV_Parameters = new ArrayList<>();
    private List<SymbolRef> population_Parameters = new ArrayList<>();
    private List<SymbolRef> randVar_Parameters = new ArrayList<>();

    protected int n_loops; // number of variability blocks in the model
    // strings assigned in loops
    protected static final String NT = "N_t";
    protected static final String NS = "N_subj";
    protected static final String IND_T = "ind_t";
    protected static final String IND_T2 = "ind_q";
    protected static final String IND_S = "ind_subj";
    protected static final String IND_max_m_W = "max_m_W";

    protected String interpSuffix = "interp";
    protected String piecewiseSuffix = "piecewise";
    protected String categSuffix = "categ";
    protected String piecewiseFunctionSuffix = "function." + piecewiseSuffix;
    protected String categFunctionSuffix = "function." + categSuffix;
    protected String maxNamePrefix = "max_m_";
    protected String NtNamePrefix = "N_t_";
    protected String gridPrefix = "grid_";
    protected String NT_INDIV;
    private static String[] listIndexes = new String[2];
    protected String pow_format = "MathFunc.Power(%s,%s)";

    private boolean isCommonVariable = false;
    private boolean isDerivativeDependent = false;
    protected boolean odeInitialValueSubjDep = false;
    protected List<SymbolRef> vars = new ArrayList<>();//, vars_unipv = new ArrayList<>();

    // unary operators management
    // contains the sublist of supported winbugs uniOperator
    private List<String> winbugs_unary_operators_list = new ArrayList<>();
    private List<String> doseVar = new ArrayList<>();
    private List<String> dosingTime = new ArrayList<>();

    // Parser MAPS
    // maps the pharmml function names with winbugs function names
    private Map<String, String> winbugsUnaryOperatorsMap = new HashMap<>();
    protected Map<String, String> variablesAssignementMap = new HashMap<>();
    private Map<String, String> tmpVariablesAssignementMap = new HashMap<>();
    protected Map<String, Integer> piecewiseIndexMap = new HashMap<>();
    protected Map<String, Boolean> piecewiseODE = new HashMap<>();
    protected Map<String, String> piecewiseMap = new HashMap<>();
    // piecewiseVarToPascal to generate export vector  and relative indexes from bugs to piecewise pascal
    protected Map<String, Map<Integer, String>> piecewiseVarToPascal = new HashMap<>();
    protected Map<Object, String> parameterDefinitionMap = new HashMap<>();
    protected final Map<String, String> derivativeMap = new HashMap<>();
    protected final Map<String, String> derivativeMapNew = new HashMap<>();
    protected final Map<String, List<Piece>> doseTargetMap = new HashMap<>();
    protected final Map<String, String> cmtMap = new HashMap<>();
    protected final Map<Integer, String> covFilesMap = new HashMap<>();
    protected final Map<String, String> covBlockMap = new HashMap<>();
    protected final Map<String, String> covCatMap = new HashMap<>();

    // maps variables with their definition (algebric expression)
    // used in  methods: winbugsDerivativeDepVarAssignement() and makeCompactDerivativeModelLines()
    // to manage the names replacement in the algebric expression
    // and the assignements in the ind_q loop
    private final Map<String, String> derivativeDefFromMap = new HashMap<>();
    protected final Map<String, List<SymbolRef>> parVariablesFromMap = new HashMap<>();
    protected final Map<String, List<SymbolRef>> randVariablesFromMap = new HashMap<>();
    protected final Map<String, Integer[]> matrixMap = new HashMap<>();
    protected final Map<String, Integer> vectorMap = new HashMap<>();
    private Integer notAssignedIndex = 0;
    private Integer vectorIndex = notAssignedIndex;
    // maps used to replace covariate algebric expression with its meanId in covariate linesVar
    // map continuous covariates with their indexed meanId
    protected Map<Object, String> continuousCovariateIdMap = new HashMap<>();
    protected Map<Object, String> continuousMap = new HashMap<>();
    // maps categorical covariates with their indexed meanId
    protected Map<Object, Integer> categoricalCovariateIdMap = new HashMap<>();
    protected Map<String, List<CategoryRef_>> categoricalMap = new HashMap<>(); // maps categories to values
    // maps covariate with its transformation
    protected Map<Object, String> covariateTransformationMap = new HashMap<>();
    protected Map<Object, String> covariateDefinitionMap = new HashMap<>();
    protected Map<Object, String> covariateMap = new HashMap<>();

    // maps pascal unaryOp to winbugs unaryOp 
    protected Map<String, String> unaryOpMap = new HashMap<>();
    protected Map<String, String> unaryOperandEqMap = new HashMap<>();
    protected Map<String, String> transfMap = new HashMap<>();
    protected Map<String, String> pascalIndivMap = new HashMap<>();
    protected Map<String, String> binOpMap = new HashMap<>();
    protected Map<String, Object> pascalVarMap = new TreeMap<>();

    protected int nOpenB = 0; // open braces counter

    protected String IND_BOTH;
    protected String IND_BOTH2;
    protected String IND_TIME;
    protected String IND_TIME2;
    protected String IND_SUBJ;

    protected static final String assignSymbol = "<-";
    protected static final String openModel = "model {";
    protected static final String dist_symb = "~";

    protected static final String forStartString = "for (";
    protected static final String forInString = " in 1:";
    protected static final String forEndString = ") {";
    protected String varLabel = "VAR";

    protected String timeLoop;
    protected String timeIndivLoop;
    protected static String time2Loop;
    protected static String time2IndivLoop;
    protected String indivLoop;

    private static final String meanSuffixLabel = "_mean";
    private static final String varianceSuffixLabel = "_prec";
    private static final String resSuffixLabel = "_res";
    private List<String> assignedSimpleParList = new ArrayList<>(); // created  reading  pharmml model
    private List<String> parametersList = new ArrayList<>();  // created  reading  pharmml model
    private List<String> parametersToEstimate = new ArrayList<>();  // created  reading  pharmml model

    // for correation linesVar generation
    private final String etaName = "ETA";
    private final String omegaName = "OMEGA";
    private final String muName = "mu";
    private final String tName = "T";

// Other variables 
    // contains linesVar related to derivative, variables and simple parameters
    private List<String> commonVarLines = new ArrayList<>();
    private List<String> parameterLines = new ArrayList<>();

    // vectors of correlated variables
    // contains initialization  statements for individual parameters
    protected List<String> winbugsIndivLines = new ArrayList<>();
    protected List<String> catCovFiles = new ArrayList<>();

    // contains initialization  statements for individual parameters
    protected List<String> correlationLines = new ArrayList<>();
    // contains initialization  statements for individual parameters
    protected List<String> pascalIndivLines = new ArrayList<>();
    protected List<String> matrixVectorLines = new ArrayList<>();
    protected List<String> pascalDiffEqLines = new ArrayList<>();
    protected List<String> covToNotRemove = new ArrayList<>();
    protected List<String> pwBugsList = new ArrayList<>();
    protected Map<String, String> pascalAssignIndivEqLines = new HashMap<>();
    protected Map<String, String> pascalAssignIndivEqLinesMap = new HashMap<>();
    protected Map<String, String> pascalAssignVarEqLines = new HashMap<>();
    protected String pascalAssignIndivEq = new String();
    protected String pascalAssignVarEq = new String();
    protected String pascalIndivEq = new String();

    protected List<SymbolRef> stateV_Parameters = new ArrayList<>(); // sublist of state variables parameters
    protected List<SymbolRef> indivV_Parameters = new ArrayList<>(); // sublist of individual variables parameters
    protected List<SymbolRef> theta_Parameters = new ArrayList<>(); // sublist of theta parameters
    protected List<SymbolRef> leafOdeParameters = new ArrayList<>(); // elenco delle var da cui dipendono direttamente o indirettamente le variabili di stato identico a stateV_Parameters ma ricavato in modo diverso con getDerLeafs() per ora non usata ma generata
    protected List<SymbolRef> leafVariables = new ArrayList<>(); // elenco delle var foglia
    protected List<SymbolRef> leafPiecewiseParameters = new ArrayList<>(); // elenco delle var foglia
    protected Map<String, List<SymbolRef>> piecewiseLeafParameters = new HashMap<>(); // elenco delle var da cui dipendono direttamente le variabili piecewise 

    // covariates management
    private List<String> variableLines = new ArrayList<>();
    private List<String> variableCompleteList = new ArrayList<>();
    protected List<SymbolRef> usedCovNames = new ArrayList<>();
    protected List<SymbolRef> usedOdeContCovNames = new ArrayList<>();
    protected List<SymbolRef> usedOdeCatCovNames = new ArrayList<>();
    protected List<SymbolRef> usedPwCovNames = new ArrayList<>();
    protected List<String> uniOpList = new ArrayList<>();

// Observation error model
    private List<String> obsModelLines = new ArrayList<>();
    private List<String> indivParamsLines = new ArrayList<>();
    private List<String> standardModelLines = new ArrayList<>();
    private List<String> initialEstimatesLines = new ArrayList<>();

    private List<String> randVarLines = new ArrayList<>();
    private String errorTransformation = "";
    private final List<Object> reversePolishStack = new ArrayList<>(); // to  manage General Observation model

    // ODE solver
    protected static int smNumber;
    private static final String suffixDerDepLabel = "_unipv";
    protected static final String upperSuffixDerDepLabel = suffixDerDepLabel.toUpperCase();
    protected static final String stateLabel = "der99wb" + suffixDerDepLabel;
    protected static final String upperStateLabel = stateLabel.toUpperCase();
    protected List<String> derivativeModelLines = new ArrayList<>();
    protected List<String> winbugsPiecewiseLines = new ArrayList<>();
    protected List<String> winbugsCovariateLines = new ArrayList<>();
    protected List<String> winbugsPiecewiseDerivDepLines = new ArrayList<>();
    protected List<String> stateVarAssignementLines = new ArrayList<>();
    protected List<String> initialValuesLines = new ArrayList<>();
    protected final List<String> diffEqLines = new ArrayList<>();
    protected List<String> populationParametersLines = new ArrayList<>(); // equazioni che assegnano i simple parameters
    protected List<String> populationParametersMatrixLines = new ArrayList<>(); // equazioni che assegnano i simple parameters
    protected List<SymbolRef> odeParameters = new ArrayList<>(); // elenco delle var da cui dipendono direttamente le variabili di stato 
    protected List<SymbolRef> odeParametersOld = new ArrayList<>(); // elenco delle var da cui dipendono direttamente le variabili di stato 
    protected Map<String, List<SymbolRef>> piecewiseParameters = new HashMap<>(); // elenco delle var da cui dipendono direttamente le variabili piecewise 
    protected List<VariableDefinition> piecewiseVariables = new ArrayList<>();
    protected List<PharmMLRootType> piecewiseCompleteList = new ArrayList<>();
    protected List<PopulationParameter> piecewisePopPars = new ArrayList<>();
    protected List<String> piecewiseVariablesId = new ArrayList<>();

    protected List<SymbolRef> odeParameters1 = new ArrayList<>(); // elenco delle var da cui dipendono le variabili di stato ripulite delle var. di stato + var da cui dipendono le variable (incluse le var. di stato)
    protected List<SymbolRef> piecewiseParameters1 = new ArrayList<>(); // elenco delle var da cui dipendono le variabili piecewise ripulite delle var. di stato + var da cui dipendono le variable (incluse le var. di stato)
    List<SymbolRef> odeParameters2 = new ArrayList<>(); // elenco delle variabili da cui dipendono le variable (foglie)
    List<SymbolRef> piecewiseParameters2 = new ArrayList<>(); // elenco delle variabili da cui dipendono le variable (foglie)
    protected List<SymbolRef> covPascalList = new ArrayList<>(); // covariate foglia serve per verificare se il modello contiene delle covariate

    // ODE management
    protected String odeLine = "";
    protected List<DerivativeVariable> completeStateVariablesList = new ArrayList<>();
    protected List<StructuralBlock> stateVariablesStructuralBlockList = new ArrayList<>();

    protected static final String gridLabel = "grid";
    protected static final String initLabel = "initial_value";
    protected static final String originLabel = "origin";
    protected static final String tolVal = "0.001";
    String defaultInitialTime = "0.0";
    protected static final String odeName = "ode";
    List<String> derivativeSymbols = new ArrayList<>();

    // functions management
    Map<Object, BinaryTree> functionMap = new HashMap<>(); // maps function with binary tree
    Map<Object, String> functionDefinition = new HashMap<>(); // maps the function with the algebric expression

    public Parser() throws IOException {

        init();
        this.num1 = "0";
        winbugs_unary_operators_list = new ArrayList<>();
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("Parser.properties"));
        binary_operator_format_minus = binaryFormat;
        binary_operator_format_plus = binaryFormat;
        binary_operator_format_power = binaryFormat;
        binary_operator_format_times = binaryFormat;
        binary_operator_format_divide = binaryFormat;

    }

    public static String getJobDir() {
        return jobDir;
    }

    public static void setJobDir(String jobDir) {
        Parser.jobDir = jobDir;
    }

    public String getOdeSolver() {
        return odeSolver;
    }

    public void setOdeSolver(String odeSolver) {
        this.odeSolver = odeSolver;
    }

    public void setTotGrid(int totGrid) {
        this.totGrid = "" + totGrid;
    }

    public String getTotGrid() {
        return totGrid;
    }

    public String getFVal(String id) {
        String val = null;
        for (StructuralBlock sb : this.lexer.getScriptDefinition().getStructuralBlocks()) {
            if (sb.getPKMacroOutput() == null) {
                continue;
            }
            for (Input el : sb.getPKMacroOutput().getListOfInput()) {
                if (el.getTarget().getSymbId().equals(id)) {
                    if (el.getP() != null) {
                        if (el.getP() instanceof SymbolRef) {
                            val = delimit(((SymbolRef) el.getP()).getSymbIdRef());
                            if (variablesAssignementMap.containsKey(val)) {
                                val = variablesAssignementMap.get(val);
                            }
                            return val;
                        } else if (el.getP() instanceof Scalar) {
                            return ((Scalar) el.getP()).asString();
                        }
                    }
                }
            }
        }

        return val;
    }

    public String getTlagVal(String id) {
        String val = null;
        for (StructuralBlock sb : this.lexer.getScriptDefinition().getStructuralBlocks()) {
            if (sb.getPKMacroOutput() == null) {
                continue;
            }
            for (Input el : sb.getPKMacroOutput().getListOfInput()) {
                if (el.getTarget().getSymbId().equals(id)) {
                    if (el.getTlag() != null) {
                        if (el.getTlag() instanceof SymbolRef) {
                            val = delimit(((SymbolRef) el.getTlag()).getSymbIdRef());
                            if (variablesAssignementMap.containsKey(val)) {
                                val = variablesAssignementMap.get(val);
                            }
                            return val;
                        } else if (el.getTlag() instanceof Scalar) {
                            val = ((Scalar) el.getTlag()).asString();
                            return val;
                        }
                    }
                }
            }
        }
        return val;
    }

    /**
     * Generic script header.<br/>
     *
     * @param fout Output stream to the output file.
     * @param model_file Path to PharmML source file. Set to NULL if doing
     * in-memory conversion.
     * @throws IOException
     */
    protected void writeScriptHeader(PrintWriter fout, String model_file) throws IOException {
        if (fout == null) {
            return;
        }

        String format = "%s Script generated by the UniPV WinBUGS converter\n";
        fout.write(String.format(format, comment_char));

        format = "%s 'WinBUGSConverter' copyright (c) BMS - University of Pavia (2016)\n";
        fout.write(String.format(format, comment_char));

        format = "%s Converter Version: %s\n";
        fout.write(String.format(format, comment_char, lexer.getConverterVersion()));

        format = "%s Source: %s\n";
        fout.write(String.format(format, comment_char, lexer.getSource()));

        format = "%s Target: %s\n";
        fout.write(String.format(format, comment_char, lexer.getTarget()));

        format = "%s Run ID: %s\n";
        fout.write(String.format(format, comment_char, run_id));

        String title = lexer.getModelName();
        if (title != null) {
            format = "%s Model: %s\n";
            fout.write(String.format(format, comment_char, title));
        }

        format = "%s File: %s\n";
        fout.write(String.format(format, comment_char, model_file));

        format = "%s Dated: %s\n\n";
        fout.write(String.format(format, comment_char, new Date()));
    }

    private static String getBbCompileScriptDir() {
        return bbCompileScriptDir;
    }

    public static void setOutputDir(String outputDir) {
        Parser.outputDir = outputDir;
    }

    public static void setBbCompileScriptDir(String bbCompileScriptDir) {
        Parser.bbCompileScriptDir = bbCompileScriptDir;
    }

    @Override
    protected void init() {
        smNumber = 0;
        isCommonVariable = false;
        isDerivativeDependent = false;
        comment_char = "#";
        script_file_suffix = "txt";
        objective_dataset_file_suffix = "csv";
        output_file_suffix = "csv";
        solver = "ode";
        leftArrayBracket = "[";
        rightArrayBracket = "]";
        IND_BOTH = leftArrayBracket + IND_S + "," + IND_T + rightArrayBracket; // [ind_subj,ind_t]
        IND_BOTH2 = leftArrayBracket + IND_S + "," + IND_T2 + rightArrayBracket; // [ind_subj,ind_q]
        IND_TIME = leftArrayBracket + IND_T + rightArrayBracket; // [ind_t]
        IND_TIME2 = leftArrayBracket + IND_T2 + rightArrayBracket; // [ind_q]
        IND_SUBJ = leftArrayBracket + IND_S + rightArrayBracket; // [ind_subj]
        NT_INDIV = NT + leftArrayBracket + IND_S + rightArrayBracket;// N_t[ind_subj]
        listIndexes[0] = IND_S; // ind_subj
        listIndexes[1] = IND_T; // ind_t
        timeLoop = forStartString + IND_T + forInString + NT + forEndString; // for (ind_t in 1:N_t) {
        timeIndivLoop = forStartString + IND_T + forInString + NT_INDIV + forEndString; // for (ind_t in 1:N_t[ind_subj]) {
        time2Loop = forStartString + IND_T2 + forInString + NT + forEndString; // for (ind_q in 1:N_t) {
        time2IndivLoop = forStartString + IND_T2 + forInString + NT_INDIV + forEndString; // for (ind_q in 1:N_t[ind_subj]) {
        indivLoop = forStartString + IND_S + forInString + NS + forEndString; // for (ind_subj in 1:N_subj) {
        initSupportedWinBugsUnaryOperators();
        super.init(); //To change body of generated methods, choose Tools | Templates.

    }

    protected void setModelName() {
        modelName = lexer.getModelName();
        String c = "" + modelName.charAt(0);
        modelName = c.toUpperCase() + modelName.substring(1);

    }

    public void setNum1(int num1) {
        this.num1 = "" + num1;
    }

    protected static void setWinbugsTemplateDir(String winbugsTemplateDir) {
        Parser.winbugsTemplateDir = winbugsTemplateDir;
    }

    protected void setXmlFileName(File xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

    private File getXmlFileName() {
        return xmlFileName;
    }

    protected void setModelNum(int modelNum) {
        this.modelNum = modelNum;
    }

    List<String> toMonitor() throws FileNotFoundException {
        List<String> out = new ArrayList<>();
        List<String> outEstim = new ArrayList<>();
        List<String> outParameters = new ArrayList<>();
//        toRemove.addAll(Util.getList(simpleP_Parameters));
        List<ParameterEstimate> list = new ArrayList<>();
        Map<String, Part> tmp = lexer.getScriptDefinition().getStepsMap();
        for (Part ms : tmp.values()) {
            if (ms instanceof EstimationStepImpl) {
                list.addAll(((EstimationStepImpl) ms).getParametersToEstimate());
            }
        }
        for (ParameterEstimate l : list) {
            outEstim.add(l.getSymbRef().getSymbIdRef());
            if (DEBUG) {
                outDebug.print("parametersToEstimate to monitor: " + l.getSymbRef().getId());
            }
        }
        List<OperationProperty> prop = getOperation("BUGS");
        if (prop != null) {
            for (OperationProperty pr : prop) {
                if (pr.getName().equals("parameters")) {
                    String[] pars = pr.getAssign().getScalar().valueToString().split(",");
                    for (String p : pars) {
                        outParameters.add(p.trim());
                    }
                }
            }
        }
        out.addAll(outEstim);
        out.addAll(outParameters);
        out.addAll(resList);
        out.addAll(predList);

        out = Util.getUniqueString(out);
        return out;
    }

    // creates the script to execute the Winbugs model in batch mode 
    protected File createScriptWinbugs(File src, File name, String outputDir) throws IOException { // CCoPIMOno cambiato valore restituito per performconvert
        outDebug = new PrintStream(new FileOutputStream(new File(winbugsDir + "/log" + "/winbugs-converter.log"), true));
        outDebug.println("script name: " + name.getAbsolutePath());
        outDebug.println("PharmML model name: " + src.getAbsolutePath());
        String template = getTemplate(WinbugsExecutionScriptName);
        outDebug.println("output directory: " + outputDir + "\n");
        outputDir += "/";
        String modelName = src.getName().substring(0, src.getName().indexOf(".xml"));
        StringBuilder sb = new StringBuilder();
        List<String> toMonitor = Util.getUniqueString(toMonitor());

        for (String l : toMonitor) {
            sb.append(String.format("set(%s)\n", l));
        }
        String outputS = String.format(template,
                modelName,
                Util.clean(this.nChains), //compile
                thinSamplesComment, Util.clean(this.thinSamples),
                thinUpdaterComment, Util.clean(this.thinUpdater),
                //Util.clean(this.dic), //thin.u       Util.clean(this.thin), //thinUpdater.samples
                Util.clean(this.burnIn), // update
                sb.toString(),
                Util.clean(this.nIter),
                winBUgsGUIComment);// update
        output(outputS, name, true);
        outDebug.close();
        return name;
    }

    private void setTaskObjectDeafult() {
        thinUpdaterComment = comment_char;
        thinSamplesComment = comment_char;
        String msg = " - WARNING: set to default value";
        String msg1 = " --|";
        if (this.nChains == null) {
            outDebug.println(msg1 + " nChains = " + WinBugsParametersName.N_CHAINS_DEFAULT + msg);
            this.nChains = WinBugsParametersName.N_CHAINS_DEFAULT;
        }
        if (this.odeSolver == null) {
            outDebug.println(msg1 + " odesolver = " + WinBugsParametersName.ODE_SOLVER_DEFAULT + msg);
            this.odeSolver = WinBugsParametersName.ODE_SOLVER_DEFAULT;
        }
        if (this.burnIn == null) {
            outDebug.println(msg1 + " burnin = " + WinBugsParametersName.BURN_IN_DEFAULT + msg);
            this.burnIn = WinBugsParametersName.BURN_IN_DEFAULT;
        }
        if (this.nIter == null) {
            outDebug.println(msg1 + " niter = " + WinBugsParametersName.N_ITER_DEFAULT + msg);
            this.nIter = WinBugsParametersName.N_ITER_DEFAULT;
        }
        if (this.thinUpdater == null) {
            thinUpdaterComment = comment_char;
            outDebug.println(msg1 + " thinpdater = " + WinBugsParametersName.THIN_UPDATER_DEFAULT + msg);
            this.thinUpdater = WinBugsParametersName.THIN_UPDATER_DEFAULT;
        } else {
            thinUpdaterComment = "";
        }
        if (this.thinSamples == null) {
            outDebug.println(msg1 + " thinsamples = " + WinBugsParametersName.THIN_SAMPLES_DEFAULT + msg);
            thinSamplesComment = comment_char;
            this.thinSamples = WinBugsParametersName.THIN_SAMPLES_DEFAULT;
        } else {
            thinSamplesComment = "";
        }
        if (this.winbugsGUI == null) {
            outDebug.println(msg1 + " winbugsgui = " + WinBugsParametersName.WINBUGSGUI_DEFAULT + msg);
            winBUgsGUIComment = "";
            this.winbugsGUI = WinBugsParametersName.WINBUGSGUI_DEFAULT;
        } else if (this.winbugsGUI.equals("true")) {
            winBUgsGUIComment = comment_char;
        }
        if (this.dic == null) {
            outDebug.println(msg1 + " dic = " + WinBugsParametersName.DIC_DEFAULT + msg);
            this.dic = WinBugsParametersName.DIC_DEFAULT;
        }
    }

    private void setTaskObjectDeafultNew() {
        thinUpdaterComment = comment_char;
        thinSamplesComment = comment_char;
        String msg = " - WARNING: set to default value";
        String msg1 = " --|";
        if (this.nChains == null) {
            this.nChains = TaskParameters.N_CHAINS.defaultVal();
            outDebug.println(msg1 + " nChains = " + this.nChains + msg);
        }
        if (this.odeSolver == null) {
            this.odeSolver = TaskParameters.ODE_SOLVER.defaultVal();
            outDebug.println(msg1 + " odesolver = " + this.odeSolver + msg);
        }
        if (this.burnIn == null) {
            this.burnIn = TaskParameters.BURN_IN.defaultVal();
            outDebug.println(msg1 + " burnin = " + this.burnIn + msg);
        }
        if (this.nIter == null) {
            this.nIter = TaskParameters.N_ITER.defaultVal();
            outDebug.println(msg1 + " niter = " + this.nIter + msg);
        }
        if (this.thinUpdater == null) {
            this.thinUpdater = TaskParameters.THIN_UPDATER.defaultVal();
            thinUpdaterComment = comment_char;
            outDebug.println(msg1 + " thinpdater = " + this.thinUpdater + msg);
        } else {
            thinUpdaterComment = "";
        }
        if (this.thinSamples == null) {
            this.thinSamples = TaskParameters.THIN_SAMPLES.defaultVal();
            outDebug.println(msg1 + " thinsamples = " + this.thinSamples + msg);
            thinSamplesComment = comment_char;
        } else {
            thinSamplesComment = "";
        }
        if (this.winbugsGUI == null) {
            this.winbugsGUI = TaskParameters.WINBUGS_GUI.defaultVal();
            outDebug.println(msg1 + " winbugsgui = " + this.winbugsGUI + msg);
            winBUgsGUIComment = "";
        } else if (this.winbugsGUI.equals("true")) {
            winBUgsGUIComment = comment_char;
        }
        if (this.dic == null) {
            this.dic = TaskParameters.DIC.defaultVal();
            outDebug.println(msg1 + " dic = " + this.dic + msg);

        }
    }

    private boolean getTaskObject() {
        String msg = " - INFO: assigned";
        String msg1 = " --| ";
        List<OperationProperty> prop = getOperation("BUGS");
        if (prop == null) {
            return false;
        }
        outDebug.println("---------------\nTaskObject parameters\n---------------");
        for (OperationProperty pr : prop) {
            outDebug.print(msg1 + pr.getName() + " = " + pr.getAssign().getScalar() + " ");
            switch (pr.getName()) {
                case "nchains":
                    this.nChains = pr.getAssign().getScalar().valueToString();//parse(pr, lexer.getTreeMaker().newInstance(pr));
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.ODE_SOLVER:
                    this.odeSolver = pr.getAssign().getScalar().valueToString();
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.BURN_IN:
                    this.burnIn = parse(pr, lexer.getTreeMaker().newInstance(pr));
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.N_ITER:
                    this.nIter = parse(pr, lexer.getTreeMaker().newInstance(pr));
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.THIN_UPDATER:
                    this.thinUpdater = parse(pr, lexer.getTreeMaker().newInstance(pr));
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.THIN_SAMPLES:
                    this.thinSamples = parse(pr, lexer.getTreeMaker().newInstance(pr));
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.DIC:
                    this.dic = pr.getAssign().getScalar().valueToString();
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.WINBUGSGUI:
                    this.winbugsGUI = pr.getAssign().getScalar().valueToString();
                    outDebug.println(msg);
                    break;
                case WinBugsParametersName.PARAMETERS:
                    outDebug.println();
                    break;
                default:
                    outDebug.println(" - WARNING: not recognized");
                    break;

            }
        }
        setTaskObjectDeafult();
        return true;
    }

    private boolean getTaskObjectNew() {
        String msg = " - INFO: assigned";
        String msg1 = " --| ";
        List<OperationProperty> prop = getOperation("BUGS");
        if (prop == null) {
            return false;
        }
        outDebug.println("---------------\nTaskObject parameters\n---------------");
        for (OperationProperty pr : prop) {
            outDebug.print(msg1 + pr.getName() + " = " + pr.getAssign().getScalar() + " ");
            if (pr.getName().equals(TaskParameters.N_CHAINS)) {
                this.nChains = pr.getAssign().getScalar().valueToString();//parse(pr, lexer.getTreeMaker().newInstance(pr));
            }
            outDebug.println(msg);

            if (pr.getName().equals(TaskParameters.ODE_SOLVER)) {
                this.odeSolver = pr.getAssign().getScalar().valueToString();
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.BURN_IN)) {
                this.burnIn = parse(pr, lexer.getTreeMaker().newInstance(pr));
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.N_ITER)) {
                this.nIter = parse(pr, lexer.getTreeMaker().newInstance(pr));
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.THIN_UPDATER)) {
                this.thinUpdater = parse(pr, lexer.getTreeMaker().newInstance(pr));
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.THIN_SAMPLES)) {
                this.thinSamples = parse(pr, lexer.getTreeMaker().newInstance(pr));
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.DIC)) {
                this.dic = pr.getAssign().getScalar().valueToString();
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.WINBUGS_GUI)) {
                this.winbugsGUI = pr.getAssign().getScalar().valueToString();
                outDebug.println(msg);
            } else if (pr.getName().equals(TaskParameters.PARAMETERS)) {
                outDebug.println();
            } else {
                outDebug.println(" - WARNING: not recognized");
            }

        }

        setTaskObjectDeafult();

        return true;
    }

    protected void createCompileScriptWinbugs() throws IOException {
        String templateCompilazione = getTemplate(WinbugsCompileScriptTemplateName);
        StringBuilder srcList = new StringBuilder(), pmetricsModulesList = new StringBuilder(), wbdevModulesList = new StringBuilder();
        StringBuilder wbdevPwCovList = new StringBuilder();
        for (int i = 0; i < piecewiseFileList.size(); i++) {
            wbdevPwCovList.append(" WBDevFunctionPiecewise" + (i + 1));
        }
        for (Map.Entry<Integer, String> el : covFilesMap.entrySet()) {
            wbdevPwCovList.append(" WBDevFunctionCovariate" + el.getKey());
        }
        srcList.append(concat(" ", pmetricsList) + concat(" ", wbdevList) + concat(" ", pwList));
        if (hasDiffEquations) {

            pmetricsModulesList.append("PmetricsPKModels PmetricsFunctionModel1");
        }

        if (hasDiffEquations) {
            wbdevModulesList.append("WBDevModelPascal1 ");
        }
        wbdevModulesList.append(wbdevPwCovList);
        output(String.format(templateCompilazione, srcList.toString(), pmetricsModulesList.toString(), wbdevModulesList.toString()),
                new File(getWinbugsDir() + "/" + WinbugsCompileScriptName), false);

        PrintWriter outDelete = new PrintWriter(new File(getWinbugsDir() + "/" + "toDel.txt"));
        if (hasDiffEquations) {
            for (String s : pmetricsModulesList.toString().split(" ")) {
                if (s.trim().length() < 1) {
                    continue;
                }
                outDelete.println("Pmetrics " + s.substring("Pmetrics".length()));
            }
        }
        for (String s : wbdevModulesList.toString().split(" ")) {
            if (s.trim().length() < 1) {
                continue;
            }
            outDelete.println("WBDev " + s.substring("WBDev".length()));
        }
        outDelete.close();
    }

    protected void output(String body, File f, boolean toSO) throws FileNotFoundException {
        outDebug.println("--> file " + f.getAbsolutePath() + " created");

        if (f.exists()) {
            f.delete();
        }
        PrintStream out = new PrintStream(f);
        out.println(body);
        out.close();

        if (toSO) {
            File fout = new File(jobDir + "/" + Util.getFileName(f) + ".txt");
            out = new PrintStream(fout);
            out.println(body);
            out.close();
            outDebug.println("--> file " + fout.getAbsolutePath() + " created");
        }
    }

    @Override
    public void cleanUp() throws IOException {
        super.cleanUp();
        outDebug.println("=========================================");
        outDebug.println(new Date(System.currentTimeMillis()) + " conversion END ");
        outDebug.println("=========================================");

        outDebug.close();//To change body of generated methods, choose Tools | Templates.
    }

    // gets the body of the file to be used as format for output generation
    // used for script files
    protected String getTemplate(String fName) throws FileNotFoundException, IOException {

        BufferedReader in = new BufferedReader(new FileReader(new File(winbugsTemplateDir + "/" + fName)));
        return getStringFile(in);
    }

    protected String getStringFile(BufferedReader in) throws FileNotFoundException, IOException {

        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    protected boolean isPopulationModel() {

        List<VariabilityBlock> variability = lexer.getScriptDefinition().getVariabilityBlocks();
        for (VariabilityBlock v : variability) {
            for (String s : v.getSymbolIds()) {
                if (s.equals("indiv")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String adjustStatement(String line) {
        String left, symbol = " ";
        if (line.contains(assignSymbol)) {
            symbol = " " + assignSymbol + " ";
        } else if (line.contains(dist_symb)) {
            symbol = " " + dist_symb + " ";
        }
        left = getLeft(line, symbol.trim());
        String oldLeft = left;
        String right = getRight(line, symbol.trim());
        if (!left.contains("(")) { // caso in cui non ci sono trasformazioni
            left = adjustIndexes(left, right);
            if (!oldLeft.equals(left)) {
                if (variablesAssignementMap.get(oldLeft) == null
                        || !variablesAssignementMap.get(oldLeft).equals(left)) {
                    tmpVariablesAssignementMap.put(oldLeft, left);
                }
            }
            if (left.contains(IND_T2)) {
                right = right.replace(IND_T, IND_T2);
            }
            return left + symbol + right;
        } else {
            line = adjustTransformedIndexes(line, symbol);
            return line;
        }
    }

    private String getLeft(String line) {
        if (line.contains(assignSymbol)) {
            return getLeft(line, assignSymbol);
        } else if (line.contains(dist_symb)) {
            return getLeft(line, dist_symb);
        } else {
            return line;
        }
    }

    private String getLeft(String line, String symbol) {
        int pos = 0;
        if (line.contains(symbol)) {
            pos = line.indexOf(symbol);
        }
        String left = line.substring(0, pos);
        return left.trim();
    }

    private String getRight(String line, String symbol) {
        int pos = 0;
        if (line.contains(symbol)) {
            pos = line.indexOf(symbol);
        }
        String right = line.substring(pos + symbol.length());
        return right.trim();
    }

    String adjustIndexes(String left, String right) {
        String adjusted = left;

        if (left.contains(leftArrayBracket)
                && !right.contains(leftArrayBracket)) {
            return left;
        }

        if (left.contains(IND_BOTH)
                || left.contains(IND_BOTH2)
                || (left.contains(IND_S)
                && left.contains(IND_T))
                || (left.contains(IND_S)
                && left.contains(IND_T2))) {
            return left;
        }

        if (right == null) {
            return left;
        }
        if (!left.contains(leftArrayBracket)
                && right.contains(leftArrayBracket)) {
            if (right.contains(IND_BOTH)
                    || (right.contains(IND_S)
                    && right.contains(IND_T))) {
                adjusted = removeIndexes(left) + IND_BOTH;
            } else if (right.contains(IND_BOTH2)
                    || (right.contains(IND_S)
                    && right.contains(IND_T2))) {
                adjusted = removeIndexes(left) + IND_BOTH2;
            } else if (right.contains(IND_SUBJ)) {
                adjusted = removeIndexes(left) + IND_SUBJ;
            }
        } else if (right.contains(IND_BOTH)
                || (right.contains(IND_S)
                && right.contains(IND_T))) {
            adjusted = delimit(Util.clean(removeIndexes(left)) + IND_BOTH);
        } else if (right.contains(IND_BOTH2)
                || (right.contains(IND_S)
                && right.contains(IND_T2))) {
            adjusted = removeIndexes(left) + IND_BOTH2;
        }
        return adjusted;// 18 luglio
    }

    String adjustTransformedIndexes(String line, String symbol) {
        String left = getLeft(line, symbol.trim());
        String right = getRight(line, symbol.trim());
        String pre = left.substring(0, left.indexOf("(") + 1);
        String post = left.substring(left.indexOf(")"));
        left = left.substring(left.indexOf("(") + 1, left.indexOf(")"));
        String oldLeft = left;
        left = adjustIndexes(left, right);
        if (!oldLeft.equals(left)) {
            tmpVariablesAssignementMap.put(oldLeft, left);
        }
        return pre + left + post + symbol + right;// 18 luglio
    }

    private String changeIndexes(String in, String index) {
        String toReplace = "";
        if (in.contains(IND_SUBJ)) {
            toReplace = IND_SUBJ;
        } else if (in.contains(IND_TIME)) {
            toReplace = IND_TIME;
        }
        return in.replace(toReplace, index);
    }

    protected boolean isObservationParameter(Object p) {
        if (p != null) {
            for (ObservationBlock ob : this.getLexer().getObservationBlocks()) {
                if (ob == null) {
                    continue;
                }
                if (isPopulationParameter(p) && ob.isObservationParameter((PopulationParameter) p)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean toBeInPascal(Object context, String id) {
        if ((isInList(context, leafOdeParameters) //                || isInList(context, piecewiseLeafParameters)// 1 agosto
                ) //if (isInList(context, stateV_Parameters) // 13 4 15
                && !isInList(context, theta_Parameters)
                && !isPiecewiseVar(id)
                || isInList(context, odeParameters)) {
            return true;
        }
        return false;
    }

    protected boolean toBeInFuncPW(Object context, String id) {
        if ((isInList(context, piecewiseLeafParameters) //                || isInList(context, piecewiseLeafParameters)// 1 agosto
                ) //if (isInList(context, stateV_Parameters) // 13 4 15
                //                && !isInList(context, theta_Parameters)
                && !isPiecewiseVar(id) //                || isInList(context, odeParameters)
                ) {
            return true;
        }
        return false;
    }

    private void generatePascalEquation(Object context, Node leaf) {
        if (this instanceof PascalParser) {
            ((PascalParser) this).pascalEquation(context, leaf);
            ((PascalParser) this).pascalPiecewiseEquation(context, leaf);
        }
    }

    @Override
    protected void rootLeafHandler(Object context, Node leaf, PrintWriter fout) {
        String oldSymbol = "";
        if (leaf == null) {
            throw new NullPointerException("Tree leaf is NULL.");
        }
        if (leaf.data != null) {
            boolean inPiecewise = false;
            if (isPiecewise(leaf.data)) {
                inPiecewise = true;
            }
            if (!isString_(leaf.data)) {
                leaf.data = delimit(getSymbol(leaf.data));
            }
            String current_value = "", current_symbol = "_FAKE_", comment = "";
            String format;
            if (isPopulationParameter(context)) {
                oldSymbol = ((PopulationParameter) context).getSymbId();
            } else if (isLocalVariable(context)) {
                oldSymbol = ((VariableDefinition) context).getSymbId();
            }
            if (isPopulationParameter(context)
                    || isLocalVariable(context)) {
                if (isPopulationParameter(context)) {
                    comment = comment_char + " PopPar";
                    PopulationParameter pp = (PopulationParameter) context;
                    if (pp.getAssign() != null && pp.getAssign().getMatrixUniop() != null) {
                        format = "%s " + assignSymbol + " %s";
                        current_symbol = delimit(pp.getSymbId());

                    } else {
                        format = "%s " + assignSymbol + " %s";
                        current_symbol = getSymbol(context);
                        current_symbol = adjustIndexes(current_symbol, leaf.data.toString());
                        if (!oldSymbol.equals(current_symbol)) { // 21 luglio attenzione alle "
                            putVariableMap(delimit(oldSymbol), delimit(current_symbol));
                        }
                        generatePascalEquation(context, leaf);
                        if (Util.clean(((String) leaf.data)).trim().length() > 0) {
                            current_value = String.format(format, delimit(current_symbol), leaf.data, comment);
                        }
                    }
                    parameterDefinitionMap.put(context, current_symbol);
                    populationParametersLines.add(current_value);
                } else if (isLocalVariable(context)) {
                    VariableDefinition var = (VariableDefinition) context;
                    if (this instanceof PascalParser) {
                        String tmp = leaf.data.toString();
                        if (var.getAssign() != null && var.getAssign().getPiecewise() == null) {
                            derivativeDefFromMap.put(((VariableDefinition) context).getSymbId(), tmp);
                        }
                        generatePascalEquation(context, leaf);

                    }
                    if (odeParameters.size() == 0) {
                        if (!inPiecewise) {
                            current_symbol = getSymbol(context);
                            format = "%s " + assignSymbol + " %s";
                            current_value = String.format(format, current_symbol, leaf.data, comment);

                            variableLines.add(current_value);
                        }
                        generatePascalEquation(context, leaf);
                    }
                } else if (context instanceof StructuredModel) {
                    StructuredModel gm = (StructuredModel) context;
                    StructuredModel.GeneralCovariate gc = gm.getGeneralCovariate();
                    StructuredModel.LinearCovariate lc = gm.getLinearCovariate();
                    if (gc != null) {
                        format = "%s " + assignSymbol + " %s + %s";
                        current_value = String.format(format, gm.getId(), gm.getGeneralCovariate(), gm.getListOfRandomEffects());
                    } else if (lc != null) {
                        format = "%s " + assignSymbol + " %s + %s + %s";
                        current_value = String.format(format, gm.getId(), gm.getGeneralCovariate(), lc.getListOfCovariate(), gm.getListOfRandomEffects());
                    }
                } else {
                    generatePascalEquation(context, leaf);

                    if (!variableLines.contains(current_value)) {
                        variableLines.add(current_value);
                    }
                }
                current_value = null;
            } else if (isPopulationParameter(context) && isObservationParameter((PopulationParameter) context)) {
                ObservationParameter op = (ObservationParameter) context;
                format = "%s " + assignSymbol + " %s";
                oldSymbol = extract(op.getName());
                oldSymbol = delimit(oldSymbol);
                current_symbol = adjustIndexes(extract(op.getName()), leaf.data.toString());
                if (!delimit(oldSymbol).equals(current_symbol)) {
                    putVariableMap(delimit(oldSymbol), delimit(current_symbol));
                }
                parameterDefinitionMap.put(context, current_symbol);
                if (isInList(assignedSimpleParList, extract(op.getName()))) {
                    current_value = String.format(format, current_symbol, leaf.data, comment);
                }
                variableLines.add(current_value);
                if (this instanceof PascalParser) {
                    if (isInOdeParameters1(((ObservationParameter) context))) {
                        variableLines.add(current_value);
                    }
                    derivativeDefFromMap.put(((ObservationParameter) context).getName(), leaf.data.toString());

                } else {
                    variableLines.add(current_value);
                }
                generatePascalEquation(context, leaf);
                current_value = null;
            } else if (isDerivative(context)) {
                if (this instanceof PascalParser) {
                    ((PascalParser) this).pascalEquation(context, leaf);
                } else {
                    diffEqLines.add(winbugsOnLineDiffEquation(context, leaf));
                }
                current_value = null;
            } else if (isIndividualParameter(context)) {

                format = "%s " + assignSymbol + " %s";
                current_symbol = delimit(getSymbol(context));
                current_symbol = adjustIndexes(current_symbol, leaf.data.toString());
                oldSymbol = ((CommonParameter) context).getSymbId();
                if (!delimit(oldSymbol).equals(current_symbol)) {
                    putVariableMap(delimit(oldSymbol), delimit(current_symbol));
                }
                current_value = String.format(format, current_symbol, leaf.data);

                if (this instanceof PascalParser) {
                    if (isInList(context, odeParameters1)) {
                        variableLines.add(current_value);
                    }
                    derivativeDefFromMap.put(((IndividualParameter) context).getSymbId(), leaf.data.toString());

                } else {
                    variableLines.add(current_value);
                }
                indivParamsLines.add(current_value);
                generatePascalEquation(context, leaf); // 31 luglio
                current_value = null;
            } else if (isInitialCondition(context) || isFunction(context) || isSequence(context) || isVector(context)) {
                format = "(%s) ";
                current_value = String.format(format, (String) leaf.data);
                if (isFunction(context)) {
                    // transform adds delimiters to the symbolIds in the definitition 
                    functionDefinition.put(context, current_value);
                }
            } else if (isPiece(context)) {
                format = "%s ";
                current_value = String.format(format, (String) leaf.data);
            } else if (isParameter(context)) {
                format = "%s ";
                current_value = String.format(format, (String) leaf.data);
            } else if (isContinuous(context)) {
                current_symbol = delimit(getSymbol(context));
                format = "%s " + assignSymbol + " %s;";
                current_value = String.format(format, current_symbol, leaf.data);
            } else if (isRandomVariable(context)) {
                LevelReference level_ref = ((ParameterRandomVariable) context).getVariabilityReference();
                VariabilityBlock vb = lexer.getVariabilityBlock(level_ref.getSymbRef());
                if (vb.isParameterVariability()) {
                    if (((ParameterRandomVariable) context).getDistribution().getUncertML() != null) {
                        if (priorPar.get(context) == null) {
                            randVarLines.addAll(writeRandomVarDistribution(((ParameterRandomVariable) context)));
                        }
                    } else if (((ParameterRandomVariable) context).getDistribution().getProbOnto() != null) {
                        if (priorPar.get(context) == null) {
                            randVarLines.addAll(writeRandomVarProbOntoDistribution(((ParameterRandomVariable) context)));
                        }
                    }
                }
                generatePascalEquation(context, leaf);// 31 luglio ULTIMO BUG
                current_value = null;
            } else if (isCovariateTransform(context)) {
                current_value = (String) leaf.data;
                covariateTransformationMap.put(context, current_value);
                ((PascalParser) this).pascalEquation(context, leaf);
            } else if (isCovariate(context)) {
                current_value = (String) leaf.data;
                covariateMap.put(context, current_value);
            } else if (isCorrelation(context)) {
                current_value = (String) leaf.data;
            } else if (isCategoricalCovariate(context)) {
                current_value = (String) leaf.data;
            } else if (isObservationModel(context)) {
                current_value = ((String) leaf.data).trim();
            } else if (isParameterEstimate(context) || isFixedParameter(context) || isObjectiveFunctionParameter(context)) {
                current_value = (String) leaf.data;
            } else if (PharmMLTypeChecker.isIndependentVariable(context)) {
                format = "%s";
                current_value = String.format(format, ((IndependentVariable) context).getSymbId());
            } else if (PharmMLTypeChecker.isStructuredError(context)) {
                try {
                    //context)) {// CRI 0.7.3
                    List<String> tmp = writeObsStandardModelDistribution((StructuredObsError) context);
                    standardModelLines.addAll(tmp);
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                    Logger
                            .getLogger(Parser.class
                                    .getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                format = "%s";
                current_value = String.format(format, (String) leaf.data);
            }
            if (current_value != null) {
                if (inPiecewise) {
                    if (current_symbol != null) {
                        current_value = current_value.replaceAll(field_tag, current_symbol) + "\n";
                    }
                }
                if (!current_value.trim().equals("")) {
                    fout.write(current_value);
                }
            }
        } else {
            throw new IllegalStateException("Should be a statement string bound to the root.data element.");
        }

    }

    String delimit(String in) {
        if (!in.startsWith(Util.delimiter) && !in.endsWith(Util.delimiter)) {
            return Util.delimiter + in + Util.delimiter;
        } else {
            return in;
        }
    }

    String cleanPascal(String in) {
        String key, value;
        for (Map.Entry<String, String> s : derivativeMap.entrySet()) {
            key = s.getKey().toString();
            value = removeIndexes(s.getValue().toString());
            if (in.contains(value)) {
                in = in.replace(delimit(value), delimit(varLabel + getStateVarIndex(key)));//derivativeMapNew.get(key).toString());
            }
        }
        return in.replaceAll(Util.delimiter, "");
    }

    // inserts in the function definition delimiters for each formal argument
    // generates diff. equations (online version) DEPRECATED
    // D(Q_UNIPV[1], t) <- (varQ2_UNIPV - ((k10 + k12) * Q_UNIPV[1])) # Q1
    protected String winbugsOnLineDiffEquation(Object context, Node leaf) {
        String format = "%s " + assignSymbol + " %s # %s";
        String current_symbol = getSymbol(context);
        String comment = getDerivativeSymbol((DerivativeVariable) context);
        // the derivative names are saved in a sublist to be used in the method winbugsDerivativeDepVarAssignement()
        derivativeSymbols.add(getDerivativeSymbol((DerivativeVariable) context));
        // the indivWinbugsEquation hat to be transformed to substitute true names with vetcor elements
        String line = String.format(format, current_symbol, leaf.data, comment);
        return line;
    }

    protected String doIndividualParameterAssignment(IndividualParameterAssignment ipa) {
        IndividualParameter ip = ipa.parameter;
        StringBuilder stmt = new StringBuilder();

        String variableSymbol = z.get(ip);

        if (ip.getAssign() != null) {
            stmt.append(String.format("%s " + assignSymbol + " " + variableSymbol));
            String assignment = parse(ip, lexer.getStatement(ip.getAssign()));
            stmt.append(assignment);
        }
        return stmt.toString();
    }

    protected String doIndividualParameter(IndividualParameter value) {
        return z.get(value.getSymbId());
    }

    protected String doDerivative(DerivativeVariable o) {
        String symbol = unassigned_symbol;
        String format = "";
        Integer idx = getStateVarIndex(o.getSymbId());
        if (n_loops == 1) {
            format = "D(" + upperStateLabel + leftArrayBracket + "%s" + rightArrayBracket + ", " + getIndependentVariableOriginalSymbol() + ")";
        } else if (n_loops == 2) {
            format = "D(" + upperStateLabel + leftArrayBracket + IND_S + ",%s" + rightArrayBracket + "," + getIndependentVariableOriginalSymbol() + ")";
        }
        symbol = String.format(format, idx);
        return symbol;
    }

    protected String doDerivative(SymbolRef s) {
        String symbol = upperStateLabel;
        Integer idx = getStateVarIndex(s.getSymbIdRef());
        String format = "%s" + leftArrayBracket + "%s" + rightArrayBracket;
        symbol = String.format(format, symbol, idx);
        return symbol;
    }

    protected int getStateVarIndex(String id) {
        int i = 1;
        for (DerivativeVariable var : completeStateVariablesList) {
            if (var.getSymbId().equals(id)) {
                break;
            }
            i++;
        }
        return i;
    }

    protected String getDerivativeSymbol(DerivativeVariable o) {
        return o.getSymbId();
    }

    protected String doParameter(PopulationParameter p) {
        return p.getSymbId();
    }

    protected String doVectorSelector(VectorSelector p) {
        String ind = ((MatrixVectorIndex) p.getCellOrSegment().get(0)).getAssign().toMathExpression();
        if (variablesAssignementMap.get(delimit(p.getSymbRef().getSymbIdRef())) != null) {
            return p.getSymbRef().getSymbIdRef() + "[" + IND_S + "," + ind + "]";
        } else {
            return p.getSymbRef().getSymbIdRef() + "[" + ind + "]";
        }
    }

    protected String doFalse() {
        return "FALSE";
    }

    protected String doTrue() {
        return "TRUE";
    }

    protected String getIndependentVariableOriginalSymbol() {
        return lexer.getDom().getListOfIndependentVariable().get(0).getSymbId();
    }

    protected IndependentVariable getIndependentVariable() {
        return lexer.getDom().getListOfIndependentVariable().get(0);
    }

    protected void openLoops(PrintWriter fout) {
        switch (n_loops) {
            case 2:
                fout.write(getTab(nOpenB++) + indivLoop + "\n");
                fout.write(getTab(nOpenB++) + timeIndivLoop + "\n");
                break;
            case 1:
                fout.write(getTab(nOpenB++) + timeLoop + "\n");
                break;
        }
    }

    private void printLines(PrintWriter fout, List<String> lines, char loop) {

        for (String l : lines) {
            if (l.trim().length() > 0) {
                if (getIndexes(l) == loop) {
                    fout.write(getTab(nOpenB));
                    if (l.contains("{")) {
                        nOpenB++;
                    }
                    fout.write(Util.clean(l).trim() + "\n");
                    if (l.contains(time2IndivLoop) || l.contains(time2Loop)) {
                        openAssign = true;
                    }
                }
            }
        }
    }

    private void closeLoop(PrintWriter fout) {
        if (nOpenB > 1) {
            fout.write(getTab(--nOpenB));
            fout.write("}");
            fout.write("\n");
        }
    }

    List<String> adjustTmpLines() {
        List<String> lines = new ArrayList<>();
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(variableLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(standardModelLines))); // QUI
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(obsModelLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(indivParamsLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(randVarLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(commonVarLines)));

        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(winbugsIndivLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(correlationLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(initialEstimatesLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(winbugsPiecewiseLines)));

        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(derivativeModelLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(initialValuesLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(stateVarAssignementLines))); // QUI
        if (!(this instanceof PascalParser)) {
            lines = Util.getUniqueString(Util.append(lines, parseScriptLines(adjustDerivativeModelLines())));
        }
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(populationParametersLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(populationParametersMatrixLines)));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(winbugsCovariateLines)));

        String tmp, oldtmp;
        for (String l : lines) {
            for (Map.Entry<String, String> pair : variablesAssignementMap.entrySet()) {
                if (l.contains(pair.getKey())) {
                    oldtmp = tmp = pair.getValue().toString().trim();
                    {
                        if (variablesAssignementMap.get(oldtmp) != null) {
                            while (tmp != null) {
                                oldtmp = tmp;
                                tmp = variablesAssignementMap.get(oldtmp);
                            }
                        }
                        l = l.replace(pair.getKey().toString(), oldtmp);
                    }
                }
            }
        }
        return lines;
    }

    protected void closeLoops(PrintWriter fout, int nSb) {
        List<String> lines = new ArrayList<>();
        List<String> tmpLines = new ArrayList<>();
        generateCovariateLines();// 31 luglio
        tmpLines = adjustTmpLines();
        lines = Util.append(lines, Util.getUniqueString(Util.clean(parseScriptLines(tmpLines))));
        lines.add("# PRIORS");
        lines = Util.append(lines, Util.clean(genPrior()));
        lines = Util.append(lines, Util.getUniqueString(parseScriptLines(matrixVectorLines)));
        if (nSb == lexer.getStructuralBlocks().size()) {
            lines = Util.getUniqueString(Util.getUniqueString(lines)); // 19 luglio
        }
        List<String> uniqueLines = new ArrayList<>();
        for (String l : lines) {
            if (!uniqueLines.contains(l.trim())) {
                uniqueLines.add(l);
            }
        }

        switch (n_loops) {
            case 2:
                printLines(fout, uniqueLines, BOTH_INDEX);
                closeLoop(fout);
                printLines(fout, uniqueLines, INDIV_INDEX);
                closeLoop(fout);
                break;
            case 1:
                printLines(fout, uniqueLines, TIME_INDEX);
                closeLoop(fout);
                break;
        }
        closeLoop(fout);
        printLines(fout, uniqueLines, NO_INDEX);
        printLines(fout, uniqueLines, INDQ_INDEX);
    }

    protected StructuralModel manageSimulationSteps(SimulationStep step, PrintWriter fout, StructuralBlock sb) {
        String format = null;
        StructuralModel smt = sb.getModel();
        return smt;
    }

    protected void manageCommonVariables(StructuralModel smt) throws FileNotFoundException, IOException {
// Assuming that the PharmML contains a single structural model definition.
        String derivExp;

        List<PharmMLElement> tmp = smt.getListOfStructuralModelElements();
        for (PharmMLElement el : smt.getListOfStructuralModelElements()) {
            if (el instanceof CommonVariableDefinition) {

                CommonVariableDefinition value = (CommonVariableDefinition) el;
                if (value == null) {
                    throw new NullPointerException("Structural model component is NULL");
                }
                if (isDerivative(value)
                        || isGeneralError(value)
                        || isRandomVariable(value)) {
                    if (!lexer.hasStatement(value)) {
                        continue;
                    }
                    BinaryTree bt = lexer.getStatement(value);
                    derivExp = parse(value, bt);
//                derivativeDefMap.put(value, derivExp);
                    commonVarLines.add(derivExp);
                } else if (isLocalVariable(value)
                        || isPopulationParameter(value.getSymbId())) { // isParameter(value)) { CRI 0.7.3
                    if (!lexer.hasStatement(value)) {
                        continue;
                    }
                    isCommonVariable = true;
                    if (isDerivativeDependentVariable(value.getSymbId())) {
                        isDerivativeDependent = true;
                    }
                    commonVarLines.add(parse(value, lexer.getTreeMaker().newInstance(value)));
                    isCommonVariable = false;
                    isDerivativeDependent = false;
                    commonVarLines = Util.clean(commonVarLines);
                }
            }
        }
    }

    protected void manageParameters(StructuralModel smt) throws FileNotFoundException, IOException {
        Object o;
        for (String name : parametersList) {
            if (!Util.getList(leafOdeParameters).contains(name)) { // 19/6/2016
                o = lexer.getAccessor().fetchElement(name);
                if (!isPiecewiseVar(name)) {
                    parameterLines.add(parse(o, lexer.getTreeMaker().newInstance(o)));
                }
            }
        }
        parameterLines = Util.clean(commonVarLines);
    }

    // check if covariates are continuous 
    // if a covariate is categorical an exception is thrown
    protected void manageCovariates() throws FileNotFoundException, IOException {
// Assuming that the PharmML contains a single structural model definition.
        String symb = "@";;
        int index = 1;
        for (CovariateDefinition cd : lexer.getCovariates()) {
            if (cd.getCategorical() != null) {
                // categorical covariates management
                CategoricalCovariate cc = cd.getCategorical();
                categoricalCovariateIdMap.put(cd, index++);
                if (cc.getListOfCategory().size() > 1) {
                    categoricalMap.put(cd.getSymbId(), lexer.getAccessor().fetchCategoryList(cd));
                }
            }
            if (cd.getContinuous() != null && toBeInFuncPW(cd, cd.getSymbId())) {
                if (cd.getContinuous().getListOfTransformation() != null && !cd.getContinuous().getListOfTransformation().isEmpty()) {
                    getParse(cd.getContinuous().getListOfTransformation().get(0));
                }
            }
        }
    }

    private void generateCovariateLines() {

        for (Map.Entry pair : covariateTransformationMap.entrySet()) {
            String id = ((CovariateTransformation) pair.getKey()).getTransformedCovariate().getSymbId();
            if (variablesAssignementMap.get(delimit(id)) != null) {
                id = variablesAssignementMap.get(delimit(id));
            }
            variableLines.add(String.format("%s %s %s", id, assignSymbol, pair.getValue().toString()));

        }
    }

    protected void manageStateVariables(StructuralBlock sb, int nSb) throws FileNotFoundException, IOException {
        if (nSb == lexer.getStructuralBlocks().size()) {
            List<String> sts = winbugsOdeInitialValues(sb);
            initialValuesLines.addAll(sts);
            sts = winbugsPascalOdeCallGen();
            derivativeModelLines.addAll(sts);
            stateVarAssignementLines.addAll(winbugsDerivativeVariableAssignement(sb));
        }
    }

    protected List<String> winbugsPascalOdeCallGen() throws FileNotFoundException, IOException {
        List<String> lines = new ArrayList<>();
        int derivativeNumber = completeStateVariablesList.size();
        String odeFormat;
        String NT_loop = NT;
        if (n_loops == 2) {
            NT_loop = NT_INDIV;
        }
        {
            odeFormat = "%s[%s, 1:%s, 1:%s]" + assignSymbol + " " + odeName + "(%s[1:%s], %s[%s, 1:%s], D(%s[%s, 1:%s],%s), %s, %s)";
            odeLine = String.format(odeFormat, stateLabel, IND_S, NT_loop, derivativeNumber
                    + "", initLabel,
                    derivativeNumber + "", gridLabel, IND_S, NT_loop,
                    upperStateLabel, IND_S, derivativeNumber + "",
                    getIndependentVariableOriginalSymbol(), originLabel, tolVal);
        }
        lines.add(odeLine);
        return lines;
    }

    protected String getStateInitialCondition(DerivativeVariable v) {
        String initialCondition = "";
        InitialCondition ic = v.getInitialCondition();
        StandardAssignable initV = ic.getInitialValue(); // CRI 0.7.3
        if (initV != null) {
            if (initV.getAssign() != null) {// setting initial values
                if (initV.getAssign().getScalar() != null) {
                    initialCondition = "" + initV.getAssign().getScalar().valueToString();
                } else if (initV.getAssign().getSymbRef() != null) {
                    initialCondition = "" + doSymbolRef(initV.getAssign().getSymbRef());
                } else if (initV.getAssign() != null) {
                    BinaryTree bt = lexer.getTreeMaker().newInstance(ic);
                    initialCondition = parse(initV, bt);
                }
            }
        }
        return initialCondition;
    }

    protected String getStateInitialTime(DerivativeVariable v) {
        String initialTime = "";
        InitialCondition ic = v.getInitialCondition();
        StandardAssignable initT = ic.getInitialTime(); // CRI 0.7.3

        if (initT.getAssign() != null) {
            if (initT.getAssign().getScalar() != null) {
                initialTime = "" + initT.getAssign().getScalar().valueToString();  // CRI 0.7.3
            } else if (initT.getAssign().getSymbRef() != null) {
                initialTime = "" + initT.getAssign().getSymbRef().getSymbIdRef();
            } else if (initT.getAssign() != null) {
                if (initT.getAssign().getSymbRef() != null) {
                    initialTime = initT.getAssign().getSymbRef().asString();
                }
            }

        }
        return initialTime;
    }

    // generates assignements in ind_q loop
// example: Q1[ind_q] <- q_unipv[ind_q,1]
    protected List<String> winbugsDerivativeVariableAssignement(StructuralBlock _sb) {
        List<String> lines = new ArrayList<>();
        String format = "%s[%s] " + assignSymbol + " %s[%s,%s] + %s\n";
        String formatIndiv = "%s[%s,%s] " + assignSymbol + " %s[%s,%s,%s] + %s\n";

        if (n_loops == 1) {
            lines.add(time2Loop + "\n");
        } else {
            lines.add(time2IndivLoop + "\n");
        }
        int i = 1;
        String s;
        for (DerivativeVariable dv : _sb.getStateVariables()) {
            s = dv.getSymbId(); // 21/06/2016
            if (n_loops == 1) {
                lines.add(String.format(format, s, IND_T2, stateLabel, IND_T2, i, getStateInitialCondition(dv)));
            } else {
                lines.add(String.format(formatIndiv, s, IND_S, IND_T2, stateLabel, IND_S, IND_T2, i, getStateInitialCondition(dv)));
            }
            i++;
        }
        lines.addAll(winbugsPiecewiseDerivDepLines);
        lines.addAll(winbugsDerivativeDepVarAssignement());
        return lines;
    }

    // generates the statements related to the variables that depend on a derivative variable
    // example: varQ2_UNIPV <- (k21 * Q2[ind_q])
    // creo il ciclo su ind_q
    List<String> winbugsDerivativeDepVarAssignement() {
        List<String> lines = new ArrayList<>();
        String formatVar = "%s[%s] " + assignSymbol + " %s\n";
        String formatVarIndiv = "%s[%s,%s] " + assignSymbol + " %s\n";

        // generation of assignement statements for variables dependent on derivative
        // example: varQ2[ind_q] <- (k21 * Q2[ind_q])
        List<VariableDefinition> listV = lexer.getLocalVariables();
        for (VariableDefinition var : listV) {
            String fromLabel = derivativeDefFromMap.get(var.getSymbId()); // gets the variable assignement to consider piecewise
            if (fromLabel != null) {
                String toLabel = fromLabel;
                for (String id : derivativeSymbols) { // loops on the derivative variables
                    String fromName = delimit(derivativeMap.get(id));
                    String toName = delimit(id);
                    if (!fromName.equals(toName)) {
                        toLabel = toLabel.replace(fromName, toName);
                    }
                    if (n_loops == 1) {
                        toLabel = toLabel.replace(delimit(id), delimit(id + leftArrayBracket + IND_T2 + rightArrayBracket));
                    } else {
                        toLabel = toLabel.replace(delimit(id), delimit(id + leftArrayBracket + IND_S + "," + IND_T2 + rightArrayBracket));
                    }
                }
                toLabel = toLabel.replaceAll(IND_T, IND_T2);
                // generates and saves the assignement statement
                if (n_loops == 1) {
                    lines.add(String.format(formatVar, var.getSymbId(), IND_T2, toLabel));
                } else {
                    lines.add(String.format(formatVarIndiv, var.getSymbId(), IND_S, IND_T2, toLabel));
                }
            }
        }
        return lines;
    }

    protected String getValue(JAXBElement el) throws UnsupportedDataTypeException {
        Object o = el.getValue();
        if (o instanceof RealValue) {
            return "" + (((RealValue) o).getValue());
        } else if (o instanceof IntValue) {
            return "" + (((IntValue) o).getValue());
        } else {
            throw new UnsupportedDataTypeException(o.getClass().getSimpleName());
        }
    }

    // generates initial values
    // example: initial_value[1] <- 30.0
    protected List<String> winbugsOdeInitialValues(StructuralBlock _sb) throws UnsupportedDataTypeException {
        List<String> linesV = new ArrayList<>();
        List<String> linesT = new ArrayList<>();
        String formatInit = "%s[%s] <- %s";
        String formatOrigin = "%s <- %s";
        String formatIndivInit = "%s[%s,%s] <- %s";
        String formatIndivOrigin = "%s[%s] <- %s";
        int n, nInitTime = 0;
        String oldInitTime = "";

        for (int i = 0; i < _sb.getStateVariables().size(); i++) {
            DerivativeVariable var = _sb.getStateVariables().get(i);
            lexer.setCurrentStructuralBlock(_sb);

            int index = getStateVarIndex(var.getSymbId());
            if (this instanceof PascalParser) {
                index += 1;
            }
            InitialCondition ic = var.getInitialCondition();
            StandardAssignable initV = ic.getInitialValue(); // CRI 0.7.3 
            StandardAssignable initT = ic.getInitialTime(); // CRI 0.7.3 
            String initialCondition = "0.0";
            String initialTime = defaultInitialTime;

            // setting initial values
            if (initV != null) {
                initialCondition = parse(initV, lexer.getStatement(ic));
                if (!initialCondition.contains(IND_S)) {
                    linesV.add(String.format(formatInit, initLabel, index, initialCondition));
                } else {
                    odeInitialValueSubjDep = true;
                    linesV.add(String.format(formatIndivInit, initLabel, IND_S, index, initialCondition));
                }

            }

            // setting initial time
            if (initT != null) {
                BinaryTree btT = lexer.getTreeMaker().newInstance(initT);
                if (btT != null) {
                    initialTime = parse(ic, btT);
                }
                nInitTime++;
                if (!initialTime.contains(IND_S)) {
                    linesT.add(String.format(formatOrigin, originLabel, initialTime));
                } else {
                    odeInitialValueSubjDep = true;
                    linesT.add(String.format(formatIndivOrigin, originLabel, IND_S, initialTime));
                }
                if (nInitTime > 1 && !oldInitTime.equals(initialTime)) {
                    throw new IllegalStateException("Multiple ODE initial times are not allowed.");
                }

                oldInitTime = initialTime;
            }
        }
        if (nInitTime == 0) {
            linesT.add(String.format(formatOrigin, originLabel, defaultInitialTime));
        }

        linesV = setInitValIndex(Util.getUniqueString(linesV));
        linesT = setInitValIndex(Util.getUniqueString(linesT));
        linesV.addAll(linesT);
        return linesV;
    }

    private List<String> setInitValIndex(List<String> ls) {
        boolean indiv = false;
        for (String l : ls) {
            if (l.contains(IND_S)) {
                indiv = true;
                odeInitialValueSubjDep = true;
            }
        }
        List<String> lines = new ArrayList<>();
        if (indiv) {
            for (String l : ls) {
                if (!l.contains(IND_S)) {
                    int pos = l.indexOf(leftArrayBracket);
                    lines.add(l.substring(0, pos + 1) + IND_S + "," + l.substring(pos + 1));
                } else {
                    lines.add(l);
                    odeInitialValueSubjDep = true;
                }
            }
        } else {
            lines.addAll(ls);
        }
        return lines;
    }

    // returns the sublist of simple parameters defined in:
    // parameter, structural and covariate blocks
    // currently it's not possible obtain the simple parameters in an observation block
    // unless getting them directly from dom
    protected List<PopulationParameter> getModelParameters() {
        List<PopulationParameter> sps = new ArrayList<>();
        for (ParameterBlock pb : lexer.getScriptDefinition().getParameterBlocks()) {
            List<PopulationParameter> tmp = pb.getParameters();
            sps.addAll(pb.getParameters());
        }

        for (StructuralBlock sb : lexer.getScriptDefinition().getStructuralBlocks()) {
            sps.addAll(sb.getParameters());
            sb.buildTrees();
        }

        for (CovariateBlock ct : lexer.getScriptDefinition().getCovariateBlocks()) {
            sps.addAll(ct.getParameters());
        }
        return sps;
    }

    private String extract(String format) {
        return format.substring(format.indexOf("_") + 1);
    }

    protected void manageRandomVariables(ParameterBlock pb) throws UnsupportedDataTypeException {
        List<ParameterRandomVariable> rvs = pb.getRandomVariables();
        for (ParameterRandomVariable value : rvs) {

            if (isRandomVariable(value)) {
                if (!lexer.hasStatement(value)) {
                    continue;
                } else {
                    String tmp = parse(value, lexer.getTreeMaker().newInstance(value));
                    if (tmp.trim().length() > 0) {
                        randVarLines.add(tmp);
                    }
                }
            }
            reUpdateMatrixVectorMapsWithAllMaps();
            updatePriorList(value);
        }
    }

    protected void manageRandomVariables(ObservationBlock ob) throws UnsupportedDataTypeException {
        List<ParameterRandomVariable> rvs = ob.getRandomVariables();
        for (ParameterRandomVariable value : rvs) {
            if (isRandomVariable(value)) {
                if (!lexer.hasStatement(value)) {
                    continue;
                }
                randVarLines.add(parse(value, lexer.getStatement(value)));
            }
            updatePriorList(value);
        }
    }

    protected void manageObservationBlocks(PrintWriter fout) throws UnsupportedDataTypeException {
        List<ObservationBlock> ot = lexer.getObservationBlocks();

        for (ObservationBlock ob : ot) {
            manageRandomVariables(ob);

            // Assuming that the PharmML contains a single structural model definition.
            ObservationError value = ob.getObservationError();
            if (value != null && ob.getModel().getContinuousData() != null) {
                value = ob.getModel().getContinuousData().getObservationError();
            }

            if (PharmMLTypeChecker.isGeneralError(ob)) {
                Object o = parseObservationErrorModel(((GeneralObsError) value).getAssign(), null);
                outDebug.println("\nfrom Recursive: = " + o.getClass());
                outDebug.println("o = " + ((SymbolRef) (((JAXBElement) o).getValue())).getSymbIdRef());
                if (!lexer.hasStatement(value)) {
                    continue;
                }
                updatePriorList(((GeneralObsError) value));
                obsModelLines.add(parse(value, lexer.getStatement(value)));
            } else if (PharmMLTypeChecker.isStructuredError(value)) {//.isObservationError(value)) {// (isGaussianError(value)) { CRI 0.7.3
                StructuredObsError tmp = (StructuredObsError) value;
                if (tmp.getTransformation() != null) { // if ((((GaussianObsError) value).getTransformation()) != null) { CRI 0.7.3
                    BinaryTree bt = lexer.getTreeMaker().newInstance(tmp);
                    errorTransformation = parse(tmp, bt);
                }
                updatePriorList(value);
                rootLeafHandler(value, new Node(""), fout);
            } else if (PharmMLTypeChecker.isDistribution(value)) {
                throw new UnsupportedOperationException("Distrinution Observation Model not supported");
            }
        }
    }

    protected void scriptPrepare(PrintWriter fout, StructuralBlock sb) throws IOException {

        // output files initialization
        PrintWriter out = new PrintWriter(new File(getWinbugsDir() + "/" + "toConv.txt"));
        out.close();
        out = new PrintWriter(new File(getWinbugsDir() + "/" + "toDel.txt"));
        out.close();

        StructuralModel smt = sb.getModel();

        smNumber++;
        if (fout == null) {
            throw new NullPointerException();
        }

        createUsefulStructures();

        // writes the header of the generated script
        writeScriptHeader(fout, lexer.getModelFilename());
        // opens the model
        fout.write(openModel + "\n");
        nOpenB++;
        n_loops = 2;
        // inserts the loops on time and, if n_loops>1, on subjects
        openLoops(fout);
        if (DEBUG) {
            outDebug.println(" manageCovariates \n------------------------\n");
        }
        manageCovariates();
        if (smNumber == 1) {
            // do not move after other statements
            if (DEBUG) {
                outDebug.println(" managePopulationParameters \n------------------------\n");
            }
            try {
                managePopulationParameters();

            } catch (ParserConfigurationException ex) {
                Logger.getLogger(Parser.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (SAXException ex) {
                Logger.getLogger(Parser.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (DEBUG) {
            outDebug.println(" manageCommonVariables \n------------------------\n");
        }
        manageCommonVariables(smt);
        manageParameters(smt);

        if (smNumber == 1) {
            if (DEBUG) {
                outDebug.println(" manageObservationBlocks \n------------------------\n");
            }
            manageObservationBlocks(fout);

            List<ParameterBlock> pbs = getParameterBlocks();
            for (ParameterBlock pb : pbs) {
                if (DEBUG) {
                    outDebug.println(" manageRandomVariables \n------------------------\n");
                }
                manageRandomVariables(pb);

                if (DEBUG) {
                    outDebug.println(" manageParameterBlocks \n------------------------\n");
                }
                manageParameterBlocks(pb);
                manageCorrelations(pb);
            }
        }
        managePiecewiseVariables();
        if (DEBUG) {
            outDebug.println(" manageStateVariables \n------------------------\n");
        }
        updateNum1();
        manageStateVariables(sb, smNumber);
        manageInitialEstimates();

    }

    protected void updateNum1() throws IOException {
        toRemoveCovariates();
        this.num1 = "" + removeNotUsedCovariatesFromDataFile(true);
    }

    public void writePreMainBlockElements(PrintWriter fout, File src) throws IOException {
        if (fout == null) {
            return;
        }

        writeScriptHeader(fout, lexer.getModelFilename());

        ScriptDefinition sd = lexer.getScriptDefinition();
        for (FunctionDefinition function : sd.getFunctions()) {
            writeFunction(function);
        }
        writeScriptLibraryReferences(fout);
    }

    private void loadParameters(FileReader in) throws IOException {
        Properties props = new Properties();
        props.load(in);

        this.num1 = props.getProperty("NUM1", num1Default);
        if (props.getProperty("NUM2") == null) {
            throw new RuntimeException("Grid dimension not specified");
        } else {
            this.totGrid = props.getProperty("NUM2");
        }
        if (!getTaskObject()) {
            outDebug.println("WARNING: TaskObject not found.\nDefault values  assigned to parameters");
            setTaskObjectDeafult();
        }
        toSO = new PrintStream(new FileOutputStream(tsoFile, true));
        toSO.println("solver=" + this.odeSolver);
        toSO.println("burnin=" + Integer.parseInt(Util.clean(this.burnIn)));
        toSO.println("niter=" + Integer.parseInt(Util.clean(this.nIter)));
        toSO.println("nchains=" + Integer.parseInt(Util.clean(this.nChains)));
        toSO.println("winbugsgui=" + this.winbugsGUI);
        toSO.close();
    }

    private void loadParametersNew(FileReader in) throws IOException {
        Properties props = new Properties();
        props.load(in);

        this.num1 = props.getProperty(TaskParameters.N_COV.label(), TaskParameters.N_COV.defaultVal());
        if (props.getProperty(TaskParameters.N_GRID.label()) == null) {
            throw new RuntimeException("Grid dimension not specified");
        } else {
            this.totGrid = props.getProperty(TaskParameters.N_GRID.label());
        }
        if (!getTaskObjectNew()) {
            outDebug.println("WARNING: TaskObject not found.\nDefault values  assigned to parameters");
            setTaskObjectDeafultNew();
        }
        toSO = new PrintStream(new FileOutputStream(tsoFile, true));
        toSO.println(TaskParameters.ODE_SOLVER.label() + "=" + this.odeSolver);
        toSO.println(TaskParameters.BURN_IN.label() + "=" + Integer.parseInt(Util.clean(this.burnIn)));
        toSO.println(TaskParameters.N_ITER.label() + "=" + Integer.parseInt(Util.clean(this.nIter)));
        toSO.println(TaskParameters.N_CHAINS.label() + "=" + Integer.parseInt(Util.clean(this.nChains)));
        toSO.println(TaskParameters.WINBUGS_GUI.label() + "=" + this.winbugsGUI);
        toSO.close();
    }

    protected void writeAllModelFunctions(File output_dir) throws IOException {
        tsoFile = new File(jobDir + "/" + toSOName);
        outDebug = new PrintStream(new FileOutputStream(new File(winbugsDir + "/log" + "/winbugs-converter.log"), true));
        outDebug.println("open file " + tsoFile.getAbsolutePath());
        Calendar cal = Calendar.getInstance();
        toSO = new PrintStream(new FileOutputStream(tsoFile));
        toSO.println("time=" + cal.getTimeInMillis());
        toSO.close();

        outDebug.println("=========================================");
        outDebug.println(new Date(System.currentTimeMillis()) + " conversion START ");
        outDebug.println("model: " + xmlFileName.getAbsolutePath());
        outDebug.println("=========================================");

        outDebug.println("winbugsDir: " + winbugsDir);
        outDebug.println("winbugsTemplateDir: " + winbugsTemplateDir);
        outDebug.println("outputDir: " + outputDir);
        outDebug.println("bbCompileScriptDir: " + bbCompileScriptDir);
        outDebug.println("jobDir: " + jobDir);
        outDebug.println("workingDir: " + workingDir);
        loadParameters(new FileReader(jobDir + "/" + parametersFile));
        List<StructuralBlock> sbs = lexer.getStructuralBlocks();

        for (StructuralBlock sb : sbs) {
            if (sb == null) {
                continue;
            }
            lexer.setCurrentStructuralBlock(sb);

            String output_filename = getModelFunctionFilename(output_dir.getAbsolutePath(), sb);
            outDebug.println("output Winbugs model file: " + output_filename);
            PrintWriter mout = new PrintWriter(output_filename);
            writeModelFunction(mout, sb);

            mout.close();
            mout = null;
            toSO = new PrintStream(new FileOutputStream(tsoFile, true));
            toSO.println(resLabel + "=" + Util.createList(resList, ","));
//            toSO.println(resLabel + "=" + Util.createList((removeIndexes(Util.clean(resList))), ","));
            toSO.println(predLabel + "=" + Util.createList(Util.clean(predList), ","));
//            toSO.println(predLabel + "=" + Util.createList((removeIndexes(Util.clean(predList))), ","));
            toSO.println("model=" + getModelName());
            toSO.close();
        }
    }

    public void writeModelFunction(PrintWriter fout, StructuralBlock sb) throws IOException {
        scriptPrepare(fout, sb);
        closeLoops(fout, smNumber);
        fout.write("}\n");
    }

    @Override
    public String getModelFunctionFilename(String output_dir, StructuralBlock sb) {
        String format = "%s%s%s_BUGS.%s";
        return winbugsDir + "/" + "model_BUGS.txt";
    }

    @Override
    public void initialise() {
        if (!lexer.hasExternalDatasets()) {
            lexer.checkForTrialDesignIfRandomisedModel();
        }
        lexer.setRemoveIllegalCharacters(true);
        z.setReplacementCharacter('_');
        z.setIllegalCharacters(new char[]{'~', '@', '+', '*', '-', '/', '$', '!', '.', '(', ')', '[', ']', ',', '#', '%', "\n".charAt(0), " ".charAt(0)});

        lexer.setFilterReservedWords(true);
        try {
            z.loadReservedWords();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("WARNING: Failed to read reserved word map for SymbolReader.");
        }
    }

    protected String doRandomVariable(ParameterRandomVariable rv) {
        String symbol = getSymbol(getDist(rv));
        return symbol;
    }

    protected String doDerivativeRef(String id) {
        String symbol = "";
        if (id != null) {
            Integer idx = getStateVarIndex(id);
            if (n_loops == 1) {
                symbol = upperStateLabel + leftArrayBracket + idx + rightArrayBracket;
            } else if (n_loops == 2) {
                symbol = upperStateLabel + leftArrayBracket + IND_S + "," + idx + rightArrayBracket;
            }
            derivativeMap.put(id, symbol);
        }
        return symbol;
    }
// adds brackets and indexes to the rigth side of an assignement statement

    protected String doStateSymbolRef(SymbolRef s) {
        String symbol = s.getSymbIdRef();
        symbol = s.getSymbIdRef();
        if (n_loops == 2) {
            symbol += IND_BOTH;
        }
        return symbol;
    }

    protected String doSymbolRef(SymbolRef s) {
        String symbol = s.getSymbIdRef();
        if (lexer.isStateVariable(symbol)) {
            if (isCommonVariable && !isDerivative(s) && !isDerivativeDependent) {
                if (n_loops == 1) {
                    symbol += IND_TIME;
                } else if (n_loops == 2) {
                    symbol += IND_BOTH;
                }

            } else if (isDerivativeDependentVariable(symbol) && isInList(odeParameters1, s)) {
                symbol = doAppendSuffix(s);
            } else // for derivative variables
            {
                symbol = doStateSymbolRef(s);
            }

        } else if (isPopulationParameter(symbol)) {
            if (isPiecewiseVar(symbol)) {
                String oldSymbol = symbol;
                updatepiecewiseIndexMap(symbol);
                String tmpOld = oldSymbol;
                symbol = piecewiseSuffix + "_" + symbol;
                if (n_loops == 1) {
                    oldSymbol += IND_TIME;
                    symbol += IND_TIME;
                } else if (n_loops == 2) {
                    oldSymbol += IND_BOTH;
                    symbol += IND_BOTH;
                }
                piecewiseMap.put(delimit(oldSymbol), delimit(symbol));
                variablesAssignementMap.put(delimit(tmpOld), delimit(oldSymbol));

            }
            if (parameterDefinitionMap.get(getParameter(symbol)) != null) {
                symbol = parameterDefinitionMap.get(getParameter(symbol));
            }
            if (variablesAssignementMap.get(symbol) != null) {
                symbol = variablesAssignementMap.get(symbol);
            } else if (variablesAssignementMap.get(delimit(symbol)) != null) {
                symbol = variablesAssignementMap.get(delimit(symbol));
            }

        } else if (isCovariate(s) && !isTransformedCovariate(s)) {
            CovariateDefinition cd = getCovariate(symbol);
            String oldSymbol = symbol;
            if (cd.getContinuous() != null) {
                if (!Util.getList(usedCovNames).contains(symbol)) {
                    usedCovNames.add(s);
                }
                // INTERP
                symbol += interpSuffix;
                symbol += IND_BOTH;
                doInterpLine(oldSymbol, symbol);
            } else if (cd.getCategorical() != null) {
                if (!Util.getList(usedCovNames).contains(symbol)) {
                    usedCovNames.add(s);
                }
                symbol += interpSuffix;
                symbol += IND_BOTH;
                doInterpLineCat(oldSymbol, symbol);
            }

        } else if (isTransformedCovariate(s)) {
            CovariateDefinition cd = getCovariate(symbol);
            String oldSymbol = symbol;
            if (cd.getContinuous() != null && !cd.getContinuous().getListOfTransformation().isEmpty()) {
                if (DEBUG) {
                    outDebug.println("doSymbolRef covariate symbol = " + symbol);
                }
                symbol += IND_BOTH;
                variablesAssignementMap.put(delimit(oldSymbol), delimit(symbol));
            }
        } else if (lexer.isIndividualParameter_(symbol)) {
            symbol += IND_SUBJ;
        } else if (isIndependentVariableSym(symbol)) {
            symbol = gridLabel;
            if (n_loops == 1) {
                symbol += IND_TIME;
            } else if (n_loops == 2) {
                symbol += IND_BOTH;
            }
        } else if (isVariable(symbol)) {
            if (isDerivativeDependentVariable(symbol)) {
                if (isInList(odeParameters1, s)) {
                    symbol = doAppendSuffix(s);
                } else if (n_loops == 1) {
                    symbol += IND_TIME;
                } else if (n_loops == 2) {
                    symbol += IND_BOTH;
                }
            }
            if (isPiecewiseVar(symbol)) {
                String oldSymbol = symbol;
                updatepiecewiseIndexMap(symbol);
                symbol = piecewiseSuffix + "_" + symbol;
                if (n_loops == 1) {
                    oldSymbol += IND_TIME;
                    symbol += IND_TIME;
                } else if (n_loops == 2) {
                    oldSymbol += IND_BOTH;
                    symbol += IND_BOTH;
                }
                piecewiseMap.put(delimit(oldSymbol), delimit(symbol));

            } else {
                if (n_loops == 1) {
                    symbol += IND_TIME;
                } else if (n_loops == 2) {
                    symbol += IND_BOTH;
                } else if (!hasDiffEquations && isDoseVariable(symbol)) {
                    symbol += leftArrayBracket + IND_S + ",1" + rightArrayBracket;
                }
            }
        } else if (isRandomVar(symbol)) { // getRandomVar(symbol)!=null){//
            ParameterRandomVariable rv = getRandomVar(symbol);
            LevelReference level_ref = ((ParameterRandomVariable) rv).getVariabilityReference();
            VariabilityBlock vb = lexer.getVariabilityBlock(level_ref.getSymbRef());

            if (vb.isResidualError()) {
                if (n_loops == 1) {
                    symbol += IND_TIME;
                } else if (n_loops == 2) {
                    symbol += IND_BOTH;
                }
            } else if (vb.isParameterVariability()) {
                symbol += IND_SUBJ;
            }
        } else {
            if (!hasDiffEquations && isDoseVariable(symbol)) {
                symbol += leftArrayBracket + IND_S + ",1" + rightArrayBracket;
            } else { // 16 luglio 2016 verificare se le parentesi dell'else sono corrette
                if (lexer.isRemoveIllegalCharacters()) {
                    SymbolReader.ModifiedSymbol result = z.removeIllegalCharacters(s, symbol);
                    if (result.isModified()) {
                        symbol = result.modified_value;
                    }
                }
                if (lexer.isFilterReservedWords()) {
                    if (z.isReservedWord(symbol)) {
                        Accessor a = lexer.getAccessor();
                        PharmMLRootType element = a.fetchElement(s);
                        if (element == null) {
                            throw new NullPointerException("Unable to resolve symbol (symbIdRef=('" + s.getSymbIdRef() + "') for reserved word filtering.");
                        }

                        symbol = z.replacement4ReservedWord(s.getSymbIdRef());
                        if (symbol == null) {
                            throw new NullPointerException("Replacement symbol for reserved word (symbIdRef=('" + s.getSymbIdRef() + "') undefined.");
                        }

                        SymbolReader.ModifiedSymbol result = new SymbolReader.ModifiedSymbol(element, s.getSymbIdRef(), symbol);
                        if (result.isModified()) {
                            z.add(result);
                        }
                    }
                }
            }
        }

        if (!delimit(symbol).equals(delimit(s.getSymbIdRef()))) {
            putVariableMap(delimit(s.getSymbIdRef()), delimit(symbol));
        }
        return delimit(symbol);
    }

    static protected boolean isIn(SymbolRef s, List<SymbolRef> l) {
        for (SymbolRef sr : l) {
            if (sr.getSymbIdRef().equals(s.getSymbIdRef())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDoseVariable(String symbol) {
        List<Piece> pi = null;
        String var = null;
        ExternalDataSet eds = lexer.getScriptDefinition().getTrialDesignBlock().getModel().getListOfExternalDataSet().get(0);
        for (ColumnDefinition cd : eds.getDataSet().getDefinition().getListOfColumn()) {
            if (cd.getListOfColumnType().get(0) != null && ((ColumnType) cd.getListOfColumnType().get(0)).value().equalsIgnoreCase("dose")) {
                var = cd.getColumnId();
                break;
            }
        }
        if (var != null) {
            ColumnMapping cm;
            MultipleDVMapping mdvm;
            List<PharmMLRootType> ext = eds.getListOfColumnMappingOrColumnTransformationOrMultipleDVMapping();
            for (PharmMLRootType el : ext) {
                if (el instanceof ColumnMapping) {
                    cm = (ColumnMapping) el;
                    if (cm.getColumnRef().getColumnIdRef().equals(var)) {
                        if (cm.getPiecewise() != null) {
                            for (Piece piece : cm.getPiecewise().getListOfPiece()) {
                                if (piece.getValue() instanceof SymbolRef) {
                                    if (((SymbolRef) piece.getValue()).getSymbIdRef().equals(symbol)) {
                                        doseVar.add(symbol);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } else if (el instanceof MultipleDVMapping) {
                    mdvm = (MultipleDVMapping) el;
                    if (mdvm.getColumnRef().getColumnIdRef().equals(var)) {
                        if (mdvm.getPiecewise() != null) {
                            for (Piece piece : mdvm.getPiecewise().getListOfPiece()) {
                                if (piece.getValue() instanceof SymbolRef) {
                                    if (((SymbolRef) piece.getValue()).getSymbIdRef().equals(symbol)) {
                                        doseVar.add(symbol);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return false;
    }

    protected boolean isDosingTime(String symbol) {
        List<Piece> pi = null;
        String var = null;
        ExternalDataSet eds = lexer.getScriptDefinition().getTrialDesignBlock().getModel().getListOfExternalDataSet().get(0);
        for (ColumnDefinition cd : eds.getDataSet().getDefinition().getListOfColumn()) {
            if (cd.getListOfColumnType().get(0) != null && ((ColumnType) cd.getListOfColumnType().get(0)).value().equalsIgnoreCase("idv")) {
                var = cd.getColumnId();
                break;
            }
        }
        if (var != null) {
            List<PharmMLRootType> ext = eds.getListOfColumnMappingOrColumnTransformationOrMultipleDVMapping();
            for (PharmMLRootType el : ext) {
                if (el instanceof ColumnMapping) {
                    ColumnMapping cm = (ColumnMapping) el;
                    if (cm.getColumnRef().getColumnIdRef().equals(var)) {
                        if (cm.getPiecewise() != null) {
                            for (Piece piece : cm.getPiecewise().getListOfPiece()) {
                                if (piece.getValue() instanceof SymbolRef) {
                                    if (((SymbolRef) piece.getValue()).getSymbIdRef().equals(symbol)) {
                                        dosingTime.add(symbol);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } else if (el instanceof MultipleDVMapping) {
                    MultipleDVMapping cm = (MultipleDVMapping) el;
                    if (cm.getColumnRef().getColumnIdRef().equals(var)) {
                        if (cm.getPiecewise() != null) {
                            for (Piece piece : cm.getPiecewise().getListOfPiece()) {
                                if (piece.getValue() instanceof SymbolRef) {
                                    if (((SymbolRef) piece.getValue()).getSymbIdRef().equals(symbol)) {
                                        dosingTime.add(symbol);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Write code for local variable reference.
     *
     * @param v Local Variable
     * @return String
     */
    private String doLocalVariable(VariableDefinition v) {
        return z.get(v.getSymbId());
    }

    // gets a variable label and adds brackets and indexes to an lvalue 
    //  puts also the statement in the rigth loop
    @Override
    public String getSymbol(Object o) {
        String symbol = unassigned_symbol;
        char indexType = NO_INDEX;
        if (isSymbolReference(o)) {
            symbol = doSymbolRef((SymbolRef) o);
        } else if (isLocalVariable(o)) {
            if (!isDerivativeDependentVariable(((VariableDefinition) o).getSymbId())) {
                symbol = doLocalVariable((VariableDefinition) o);
                if (n_loops == 1) {
                    indexType = TIME_INDEX;
                } else {
                    indexType = BOTH_INDEX;
                }
            } else {
                symbol = doDerivativeDependentVariable((VariableDefinition) o);
                if (!(this instanceof PascalParser)) {
                    if (n_loops == 1) {
                        indexType = TIME_INDEX;
                    } else {
                        indexType = BOTH_INDEX;
                    }
                }
            }
        } else if (isIndividualParameter(o)) {
            symbol = ((IndividualParameter) o).getSymbId();
            indexType = INDIV_INDEX;
        } else if (isIndividualParameterAssignment(o)) {
            IndividualParameter par = ((IndividualParameterAssignment) o).parameter;
            String s1 = getSymbol(par);
            symbol = s1 + assignSymbol + doIndividualParameterAssignment((IndividualParameterAssignment) o);
        } else if (isParameter(o)) {
            symbol = ((Parameter) o).getSymbId();
        } else if (PharmMLTypeChecker.isIndependentVariable(o)) {
            symbol = doIndependentVariable((IndependentVariable) o);
            if (n_loops == 1) {
                indexType = TIME_INDEX;
            } else {
                indexType = BOTH_INDEX;
            }
        } else if (isGeneralError(o)) {
            symbol = doGeneralObservationError((GeneralObsError) o);
            if (n_loops == 1) {
                indexType = TIME_INDEX;
            } else {
                indexType = BOTH_INDEX;
            }
        } else if (PharmMLTypeChecker.isStructuredError(o)) {
            symbol = doGaussianObservationError((StructuredObsError) o);
            if (n_loops == 1) {
                indexType = TIME_INDEX;
            } else {
                indexType = BOTH_INDEX;
            }
        } else if (isCovariate(o)) {
            symbol = doCovariate((CovariateDefinition) o);
            indexType = BOTH_INDEX;
        } else if (isCategoricalCovariate(o)) {
            symbol = doCovariate((CovariateDefinition) o);
            indexType = BOTH_INDEX;
        } else if (isContinuousCovariate(o)) {
            symbol = doContinuousCovariate((ContinuousCovariate) o);
            indexType = BOTH_INDEX;
        } else if (isCovariateTransform(o)) {
            symbol = doCovariateTransformationNew((CovariateTransformation) o);
            indexType = BOTH_INDEX;
        } else if (isRandomVariable(o)) {
            symbol = ((ParameterRandomVariable) o).getSymbId();
            LevelReference level_ref = ((ParameterRandomVariable) o).getVariabilityReference();
            VariabilityBlock vb = lexer.getVariabilityBlock(level_ref.getSymbRef());

            if (vb.isResidualError()) {
                if (n_loops == 1) {
                    indexType = TIME_INDEX;
                } else {
                    indexType = BOTH_INDEX;
                }
            } else if (vb.isParameterVariability()) {
                indexType = INDIV_INDEX;
            }
        } else if (isDerivative(o)) {
            symbol = doDerivative((DerivativeVariable) o);
        } else if (isPopulationParameter(o)) {
            symbol = doParameter((PopulationParameter) o);
        } else if (isObservationParameter(o)) {
            symbol = doParameter((PopulationParameter) o);
        } else if (isString_(o)) {
            symbol = doString((String) o);
        } else if (isVectorSelector(o)) {
            symbol = doVectorSelector((VectorSelector) o);
        } else if (isReal(o)) {
            symbol = doReal((RealValue) o);
        } else if (isFalse(o)) {
            symbol = doFalse();
        } else if (isTrue(o)) {
            symbol = doTrue();
        } else if (isString(o)) {
            symbol = doStringValue((StringValue) o);
        } else if (isInt(o)) {
            symbol = doInt((IntValue) o);
        } else if (isConstant(o)) {
            symbol = doConstant((Constant) o);
        } else if (isFunctionCall(o)) {
            symbol = doFunctionCallNew((FunctionCallType) o);
        } else if (isSequence(o)) {
            symbol = doSequence((Sequence) o);
        } else if (isVector(o)) {
            symbol = doVector((Vector) o);
        } else if (isPiecewise(o)) {
            symbol = doPiecewise((Piecewise) o);
        } else if (isLogicalBinaryOperation(o)) {
            symbol = doLogicalBinaryOperator((LogicBinOp) o);
        } else if (isLogicalUnaryOperation(o)) {
            symbol = doLogicalUnaryOperator((LogicUniOp) o);
        } else if (isParameterEstimate(o)) {
            symbol = doParameterEstimate((ParameterEstimate) o);
        } else if (isJAXBElement(o)) {
            symbol = doElement((JAXBElement<?>) o);
        } else if (isUnivariateDistribution(o)) {
            symbol = doUnivariateDistribution((AbstractContinuousUnivariateDistributionType) o);
        } else if (isProbOnto(o)) {
            symbol = doProbonto((ProbOnto) o);
        } else if (isVariableReference(o)) {
            symbol = doVarRef((VarRefType) o);
        } else if (isCorrelation(o)) {
            symbol = doCorrelation((CorrelationRef) o);
        } else if (o instanceof CategoryRef) {
            symbol = ((CategoryRef) o).getCatIdRef();
        } else if (PharmMLTypeChecker.isStructuredError(o)) {
            symbol = doGaussianObservationError((StructuredObsError) o);
        } else if (isBigInteger(o)) {
            symbol = doBigInteger((BigInteger) o);
        } else if (isFixedParameter(o)) {
            symbol = doFixedParameter((FixedParameter) o);
        } else if (isObjectiveFunctionParameter(o)) {
            symbol = doParameterAssignmentFromEstimation((ParameterAssignmentFromEstimation) o);
            if (n_loops == 1) {
                indexType = TIME_INDEX;
            } else {
                indexType = BOTH_INDEX;
            }
        } else if (isCommonVariable(o)) {
            symbol = doCommonVariable((CommonVariableDefinition) o);
            if (n_loops == 1) {
                indexType = TIME_INDEX;
            } else {
                indexType = BOTH_INDEX;
            }
        } else if (isMatrixSelector(o)) {
            symbol = doMatrixSelector((MatrixSelector) o);

        } else if (isPiece(o)) {
            symbol = doPiece((Piece) o);
        } else if (isMatrixUnaryOperation(o)) {
            symbol = doMatrixUniOp((MatrixUniOp) o);
        } else {
            String format = "WARNING: Unknown symbol, %s\n";
            String msg = String.format(format, o.toString());
            ConversionDetail detail = new ConversionDetail_();
            detail.setSeverity(ConversionDetail.Severity.WARNING);
            detail.addInfo("warning", msg);

            System.err.println(msg);
        }
        switch (indexType) {
            case TIME_INDEX:
                symbol += IND_TIME;
                break;
            case BOTH_INDEX:
                symbol += IND_BOTH;
                break;
            case INDIV_INDEX:
                symbol += IND_SUBJ;
                break;
        }
        return symbol;
    }

    @Override
    protected void parseBinaryOperation(BinaryTree bt, Node leaf) {
        Binop b_op = (Binop) leaf.parent.data;
        Node parent = leaf.parent;
        if (parent.left != null && parent.right != null) {
            String leftStatement = "";
            if (parent.left.stmt != null) {
                leftStatement = delimit(parent.left.stmt);
            } else {
                leftStatement = delimit(getSymbol(parent.left.data));
            }

            parent.left = null;
            String rightStatement = "";
            if (parent.right.stmt != null) {
                rightStatement = delimit(parent.right.stmt);
            } else {
                rightStatement = delimit(getSymbol(parent.right.data));
            }
            parent.right = null;
            parent.data = doBinaryOperation(b_op, leftStatement, rightStatement);
        }
    }

    @Override
    protected void parseUnaryOperation(BinaryTree bt, Node leaf) {
        Uniop u_op = (Uniop) leaf.parent.data;
        Node parent = leaf.parent;
        if (parent.left != null) {
            String leftStatement = "";

            if (parent.left.stmt != null) {
                leftStatement = delimit(parent.left.stmt);
            } else {
                leftStatement = delimit(getSymbol(parent.left.data));
            }
            parent.left = null;
            parent.data = doUnaryOperation(u_op, leftStatement);
        }
    }

    protected String doGeneralObservationError(GeneralObsError goe) {
        return goe.getSymbId();
    }

    @Override
    protected String doUnaryOperation(Uniop u_op, String leftStatement) {
        String winbugsUnaryOperator;
        switch (u_op.getOperator().getOperator()) {
            case "factorial":
                winbugsUnaryOperator = String.format("exp(logfact(%s))", leftStatement);
                break;
            case "ceiling":
                winbugsUnaryOperator = String.format("trunc(%s)+1", leftStatement);
                break;
            case "logistic":
                winbugsUnaryOperator = String.format("1/(1+exp(-%s))", leftStatement);
                break;
            case "tan":
                winbugsUnaryOperator = String.format("sin(%s)/cos(%s)", leftStatement, leftStatement);
                break;
            case "sec":
                winbugsUnaryOperator = String.format("1/cos(%s)", leftStatement);
                break;
            case "csc":
                winbugsUnaryOperator = String.format("1/sin(%s)", leftStatement);
                break;
            case "cot":
                winbugsUnaryOperator = String.format("cos(%s)/sin(%s)", leftStatement, leftStatement);
                break;
            case "sinh":
                winbugsUnaryOperator = String.format("(exp(%s)-exp(-%s))/2", leftStatement, leftStatement);
                break;
            case "cosh":
                winbugsUnaryOperator = String.format("(exp(%s)+exp(-%s))/2", leftStatement, leftStatement);
                break;
            case "tanh":
                winbugsUnaryOperator = String.format("(exp(%s)-exp(-%s))/(exp(%s)+exp(-%s))", leftStatement, leftStatement, leftStatement, leftStatement);
                break;
            case "coth":
                winbugsUnaryOperator = String.format("(exp(%s)+exp(-%s))/(exp(%s)-exp(-%s))", leftStatement, leftStatement, leftStatement, leftStatement);
                break;
            case "sech":
                winbugsUnaryOperator = String.format("2/(exp(%s)+exp(-%s))", leftStatement, leftStatement);
                break;
            case "csch":
                winbugsUnaryOperator = String.format("2/(exp(%s)-exp(-%s))", leftStatement, leftStatement);
                break;
            case "arcsinh":
                winbugsUnaryOperator = String.format("log((%s)+sqrt(pow(%s,2)+1))", leftStatement, leftStatement);
                break;
            case "arccosh":
                winbugsUnaryOperator = String.format("log((%s)+sqrt(pow(%s,2)-1))", leftStatement, leftStatement);
                break;
            case "arctanh":
                winbugsUnaryOperator = String.format("1/2*log((1+%s)/(1-%s))", leftStatement, leftStatement);
                break;
            case "arccoth":
                winbugsUnaryOperator = String.format("1/2*log((%s+1)/(%s-1))", leftStatement, leftStatement);
                break;
            default:
                winbugsUnaryOperator = super.doUnaryOperation(u_op, leftStatement);
                break;
        }
        return "(" + winbugsUnaryOperator.trim() + ")";
    }

    @Override
    protected String doBinaryOperation(Binop b_op, String leftStatement, String rightStatement) {
        String winbugsBinOp = super.doBinaryOperation(b_op, leftStatement, rightStatement);
        String op = b_op.getOperator().toString();
        if (op == null) {
            throw new NullPointerException("The binary operator is NULL.");
        }
        if (op.equals(BaseEngine.POWER)) {
            winbugsBinOp = String.format("pow(%s,%s)", leftStatement, rightStatement);
        } else if (op.equals(BaseEngine.ROOT)) {
            winbugsBinOp = String.format("pow(%s,1/%s)", leftStatement, rightStatement);
        } else if (op.equals(BaseEngine.LOGX)) {
            winbugsBinOp = String.format("log(%s)/log(%s)", leftStatement, rightStatement);
        } else if (op.equals(BaseEngine.MAX)) {
            winbugsBinOp = String.format("max(%s,%s)", leftStatement, rightStatement);
        } else if (op.equals(BaseEngine.MIN)) {
            winbugsBinOp = String.format("min(%s,%s)", leftStatement, rightStatement);
        }
        return delimit(winbugsBinOp);
    }

    @Override
    protected String getRootFormat() {
        return super.getRootFormat();
    }

    protected String getPowFormat() {
        return pow_format;
    }

    @Override
    protected String getRemFormat() {
        return super.getRemFormat();
    }

    @Override
    protected String getMinFormat() {
        return super.getMinFormat();
    }

    @Override
    protected String getMaxFormat() {
        return super.getMaxFormat();
    }

    @Override
    protected String getLogXFormat() {
        return super.getLogXFormat();
    }

    @Override
    protected String doBinaryLogicalOperation(LogicBinOp l_b_op, String leftStatement, String rightStatement) {
        return super.doBinaryLogicalOperation(l_b_op, leftStatement, rightStatement);
    }

    protected String doGaussianObservationError(StructuredObsError goe) {
        return goe.getSymbId();
    }

    public boolean isVariable(String symbol) {
        if (symbol != null) {
            if (!lexer.getStructuralBlocks().isEmpty()) {
                List<StructuralBlock> sbs = lexer.getStructuralBlocks();
                for (StructuralBlock sb : sbs) {
                    if (sb != null) {
                        List<VariableDefinition> ips = sb.getLocalVariables();
                        for (VariableDefinition ip : ips) {
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
        }

        return false;
    }

    public boolean isPiecewiseVar(String symbol) {
        if (symbol != null) {
            if (!lexer.getStructuralBlocks().isEmpty()) {
                List<StructuralBlock> sbs = lexer.getStructuralBlocks();
                for (StructuralBlock sb : sbs) {
                    if (sb != null) {
                        List<VariableDefinition> ips = sb.getLocalVariables();
                        for (VariableDefinition ip : ips) {
                            if (ip == null) {
                                continue;
                            }
                            String currentSymbol = ip.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol)) {
                                VariableDefinition var = getVariable(symbol);
                                if (var.getAssign() != null && var.getAssign().getPiecewise() != null) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            if (lexer.getParameterBlock() != null) {

                for (PopulationParameter par : lexer.getParameterBlock().getParameters()) {
                    if (par == null) {
                        continue;
                    }
                    String currentSymbol = par.getSymbId();
                    if (currentSymbol == null) {
                        continue;
                    }
                    if (currentSymbol.equals(symbol)) {
                        PopulationParameter var = getPopulationParameter(symbol);
                        if (var.getAssign() != null && var.getAssign().getPiecewise() != null) {
                            return true;
                        }
                    }
                }

                for (PharmMLElement par : lexer.getParameterBlock().getModel().getListOfParameterModelElements()) {
                    if (par == null) {
                        continue;
                    }
                    String currentSymbol = null;
                    if (par instanceof CommonParameter) {
                        currentSymbol = ((CommonParameter) par).getSymbId();
                    }
                    if (currentSymbol == null) {
                        continue;
                    }
                    if (currentSymbol.equals(symbol)) {
                        PharmMLRootType var = lexer.getAccessor().fetchElement(symbol);
                        if (var instanceof Parameter
                                && ((Parameter) var).getAssign() != null
                                && ((Parameter) var).getAssign().getPiecewise() != null) {
                            return true;
                        }
                    }
                }

            }
        }
        return false;

    }

    public boolean isPopulationParameter(Object o) {
        return (o instanceof PopulationParameter);
    }

    public boolean isPopulationParameter(String symbol) {
        if (symbol != null) {
            for (PopulationParameter par : lexer.getModelParameters()) {
                if (par == null) {
                    continue;
                }
                String currentSymbol = par.getSymbId();
                if (currentSymbol == null) {
                    continue;
                }
                if (currentSymbol.equals(symbol)) {
                    return true;
                }
            }
            List<ObservationBlock> obs = lexer.getObservationBlocks();
            for (ObservationBlock ob : obs) {
                for (ObservationParameter par : ob.getObservationParameters()) {
                    if (par == null) {
                        continue;
                    }
                    String currentSymbol = extract(par.getName());
                    if (currentSymbol == null) {
                        continue;
                    }
                    if (currentSymbol.equals(symbol)) {
                        return true;
                    }
                }
            }
            if (!lexer.getStructuralBlocks().isEmpty()) {
                List<StructuralBlock> sbs = lexer.getStructuralBlocks();
                for (StructuralBlock sb : sbs) {
                    if (sb != null) {
                        for (PopulationParameter par : sb.getParameters()) {
                            if (par == null) {
                                continue;
                            }
                            String currentSymbol = par.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol)) {
                                return true;
                            }
                        }
                    }
                }
                if (!lexer.getStructuralBlocks().isEmpty()) {
                    if (lexer.getParameterBlock() != null) {
                        for (PopulationParameter par : lexer.getParameterBlock().getParameters()) {
                            if (par == null) {
                                continue;
                            }
                            String currentSymbol = par.getSymbId();
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
        }

        return false;
    }

    public boolean isRandomVar(String symbol) {
        if (symbol != null) {
            List<ParameterBlock> pbs = getParameterBlocks();
            for (ParameterBlock pb : pbs) {

                List<ParameterRandomVariable> rvs = pb.getRandomVariables();
                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return true;
                    }
                }
            }

            List<ObservationBlock> obs = lexer.getObservationBlocks();
            for (ObservationBlock ob : obs) {
                List<ParameterRandomVariable> rvs = ob.getRandomVariables();
                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isLinkedRandomVar(String symbol) {
        if (symbol != null) {
            List<ParameterBlock> pbs = getParameterBlocks();
            for (ParameterBlock pb : pbs) {

                List<ParameterRandomVariable> rvs = pb.getLinkedRandomVariables();
                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return true;
                    }
                }
            }

            List<ObservationBlock> obs = lexer.getObservationBlocks();
            for (ObservationBlock ob : obs) {
                List<ParameterRandomVariable> rvs = ob.getLinkedRandomVariables();
                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCovariate(Object o) {
        return (o instanceof CovariateDefinition);
    }

    public boolean isCategoricalCovariate(Object o) {
        return (o instanceof CovariateDefinition && ((CovariateDefinition) o).getCategorical() != null);
    }

    public boolean isCategoricalCovariateType(SymbolRef symbol) {
        if (symbol != null) {
            if (!lexer.getCovariateBlocks().isEmpty()) {
                for (CovariateBlock cb : lexer.getCovariateBlocks()) {
                    if (cb != null) {
                        List<CovariateDefinition> cds = cb.getCovariates();
                        for (CovariateDefinition cc : cds) {
                            if (cc == null) {
                                continue;
                            }
                            String currentSymbol = cc.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol.getSymbIdRef())) {
                                if (cc.getCategorical() != null) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isContinuousCovariate(Object o) {
        return (o instanceof ContinuousCovariate);
    }

    public boolean isCovariate(SymbolRef symbol) {
        if (symbol != null) {
            if (!lexer.getCovariateBlocks().isEmpty()) {
                for (CovariateBlock cb : lexer.getCovariateBlocks()) {
                    if (cb != null) {
                        List<CovariateDefinition> cds = cb.getCovariates();
                        for (CovariateDefinition cc : cds) {
                            if (cc == null) {
                                continue;
                            }
                            String currentSymbol = cc.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol.getSymbIdRef())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isCategoricalCovariate(SymbolRef symbol) {
        if (symbol != null) {
            if (!lexer.getCovariateBlocks().isEmpty()) {
                for (CovariateBlock cb : lexer.getCovariateBlocks()) {
                    if (cb != null) {
                        List<CovariateDefinition> cds = cb.getCovariates();
                        for (CovariateDefinition cc : cds) {
                            if (cc == null) {
                                continue;
                            }
                            String currentSymbol = cc.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol.getSymbIdRef())) {
                                if (cc.getCategorical() != null) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isContinuousCovariate(SymbolRef symbol) {
        if (symbol != null) {
            if (!lexer.getCovariateBlocks().isEmpty()) {
                for (CovariateBlock cb : lexer.getCovariateBlocks()) {
                    if (cb != null) {
                        List<CovariateDefinition> cds = cb.getCovariates();
                        for (CovariateDefinition cc : cds) {
                            if (cc == null) {
                                continue;
                            }
                            String currentSymbol = cc.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol.getSymbIdRef())) {
                                if (cc.getContinuous() != null) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isTransformedCovariate(SymbolRef symbol) {
        if (symbol != null) {
            if (!lexer.getCovariateBlocks().isEmpty()) {
                for (CovariateBlock cb : lexer.getCovariateBlocks()) {
                    if (cb != null) {
                        List<CovariateDefinition> cds = cb.getCovariates();
                        for (CovariateDefinition cc : cds) {
                            if (cc == null) {
                                continue;
                            }
                            String currentSymbol = cc.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (cc.getContinuous() != null) {
                                if (!cc.getContinuous().getListOfTransformation().isEmpty()) {
                                    for (CovariateTransformation ct : cc.getContinuous().getListOfTransformation()) {
                                        if (ct.getTransformedCovariate().getSymbId().equals(symbol.getSymbIdRef())) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected boolean isDerivativeVar(String symbol) {
        if (symbol != null) {
            List<StructuralBlock> sbs = lexer.getStructuralBlocks();
            for (StructuralBlock sb : sbs) {
                List<DerivativeVariable> rvs = sb.getStateVariables();
                for (DerivativeVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isVariableDefinition(String symbol) {
        if (symbol != null) {
            List<StructuralBlock> sbs = lexer.getStructuralBlocks();
            for (StructuralBlock sb : sbs) {
                List<VariableDefinition> rvs = sb.getLocalVariables();
                for (VariableDefinition rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // returns the covariate, given its symbolId, if it exists, otherwise, if the variable is not a covariate, returns null 
    public CovariateDefinition getCovariate(String symbol) {
        if (symbol != null) {
            for (CovariateDefinition cd : lexer.getCovariates()) {
                if (cd.getSymbId().equals(symbol)) {
                    return cd;
                } else if (cd.getContinuous() != null) {
                    if (!cd.getContinuous().getListOfTransformation().isEmpty()) {
                        for (CovariateTransformation ct : cd.getContinuous().getListOfTransformation()) {
                            if (ct.getTransformedCovariate().getSymbId().equals(symbol)) {
                                return cd;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public CovariateDefinition getCategoricalCovariate(String symbol) {
        if (symbol != null) {
            for (CovariateDefinition cd : lexer.getCovariates()) {
                if (cd.getSymbId().equals(symbol)) {
                    return cd;
                } else if (cd.getCategorical() != null) {

                    return cd;
                }
            }
        }
        return null;
    }

    // returns the covariate, given its symbolId, if it exists, otherwise, if the variable is not a covariate, returns null 
    public DerivativeVariable getDerivativeVariable(String symbol) {
        if (symbol != null) {
            for (StructuralModel sb : dom.getModelDefinition().getListOfStructuralModel()) {
                for (DerivativeVariable cd : sb.fetchDerivativeVariables()) {
                    if (cd.getSymbId().equals(symbol)) {
                        return cd;
                    }
                }
            }
        }
        return null;
    }

    // returns the simple parameter, given its symbolId, if it exists, otherwise, if the variable is not a simple parameter, returns null 
    public PopulationParameter getParameter(String symbol) {
        if (symbol != null) {
            for (PopulationParameter cd : lexer.getModelParameters()) {
                if (cd.getSymbId().equals(symbol)) {
                    return cd;
                }
            }
        }
        return null;
    }

    // returns the random variable, given its symbolId, if it exists, otherwise, if the variable is not random, returns null 
    public ParameterRandomVariable getRandomVar(String symbol) {
        if (symbol != null) {
            List<ParameterBlock> pbs = getParameterBlocks();
            for (ParameterBlock pb : pbs) {
                List<ParameterRandomVariable> rvs = pb.getRandomVariables();

                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return rv;
                    }
                }
            }
            List<ObservationBlock> obs = lexer.getObservationBlocks();
            for (ObservationBlock ob : obs) {
                List<ParameterRandomVariable> rvs = ob.getRandomVariables();
                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return rv;
                    }
                }
            }
        }
        return null;
    }

    // returns the random variable, given its symbolId, if it exists, otherwise, if the variable is not random, returns null 
    public FunctionDefinition getFunctionByName(String symbol) {
        if (symbol != null) {
            List<FunctionDefinition> fd = lexer.getScriptDefinition().getFunctions();
            for (FunctionDefinition f : fd) {
                if (symbol.equals(f.getSymbId())) {
                    return f;
                }
            }
        }
        return null;
    }

    public ILexer getLexer() {
        return lexer;
    }

    private List<JAXBElement<?>> getBinopContent(Binop bop) {
        List<JAXBElement<?>> cont = new ArrayList<>();
        cont.add(bop.getOperand1().toJAXBElement());
        cont.add(bop.getOperand2().toJAXBElement());
        return cont;

    }

    private List<JAXBElement<?>> getMatrixContent(MatrixUniOp mat) {
        List<JAXBElement<?>> cont = new ArrayList<>();
        if (mat.getValue() instanceof Matrix) {
            if (((Matrix) mat.getValue()).getListOfMatrixElements() != null) {
                List<PharmMLRootType> list = ((Matrix) mat.getValue()).getListOfMatrixElements();

                for (PharmMLRootType el : list) {
                    for (MatrixRowValue e : ((MatrixRow) el).getListOfValues()) {
                        if (e instanceof SymbolRef) {
                            cont.add(((SymbolRef) e).toJAXBElement());
                            String tmp = ((SymbolRef) e).getSymbIdRef();
                        }
                    }
                }
            }

        } else if (mat.getValue() instanceof SymbolRef) {
            cont.add(((SymbolRef) mat.getValue()).toJAXBElement());
        }

        return cont;

    }

    private JAXBElement<? extends AbstractContinuousUnivariateDistributionType> getDistributionType(Object context) {
        BinaryTree tmp;
        String s = null;
        if (context instanceof ParameterRandomVariable) {
            tmp = lexer.getTreeMaker().newInstance(((ParameterRandomVariable) context).getDistribution());
            Distribution distr = ((ParameterRandomVariable) context).getDistribution();
            if (distr.getUncertML() != null) {
                return distr.getUncertML().getAbstractContinuousUnivariateDistribution();
            } else if (distr.getProbOnto() != null) {
                return null;
            }
        } else if (context instanceof StructuredObsError) {
            s = ((StructuredObsError) context).getResidualError().getSymbRef().getSymbIdRef();
            if (isRandomVar(s)) {
                ParameterRandomVariable rv = getRandomVar(s);
                tmp = lexer.getTreeMaker().newInstance(rv.getDistribution());
                if (rv.getDistribution().getUncertML() != null) {
                    return rv.getDistribution().getUncertML().getAbstractContinuousUnivariateDistribution();
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    private ProbOnto getProbOntoDistributionType(Object context) {

        String s = null;
        BinaryTree tmp;
        if (context instanceof ParameterRandomVariable) {
            tmp = lexer.getTreeMaker().newInstance(((ParameterRandomVariable) context).getDistribution());
            return (ProbOnto) tmp.nodes.get(0).data;
        } else if (context instanceof StructuredObsError) {
            s = ((StructuredObsError) context).getResidualError().getSymbRef().getSymbIdRef();
            if (isRandomVar(s)) {
                ParameterRandomVariable rv = getRandomVar(s);
                tmp = lexer.getTreeMaker().newInstance(rv);
                return (ProbOnto) tmp.nodes.get(0).data;
            }
        } else if (context instanceof PopulationParameter) {
            tmp = lexer.getTreeMaker().newInstance(((PopulationParameter) context).getDistribution());
            return (ProbOnto) tmp.nodes.get(0).data;
        }
        return null;
    }

    List<VariableDefinition> getVariables(StructuralBlock sb) {
        List<VariableDefinition> l0 = new ArrayList<>();
        for (VariableDefinition sr : sb.getLocalVariables()) {
            if (isInList(Util.getList(odeParameters1), sr.getSymbId())) {
                l0.add(sr);
            }
        }
        return l0;
    }

    List<PharmMLRootType> getPiecewiseList() {
        List<PharmMLRootType> list = new ArrayList<>();
        for (StructuralBlock _sb : lexer.getStructuralBlocks()) {
            list.addAll(getPiecewiseList(getSymbIdList(_sb.getLocalVariables())));
        }
        for (PopulationParameter ppar : piecewisePopPars) {
            list.add(ppar);
        }
        list.addAll(piecewisePopPars);
        Set<PharmMLRootType> ls = new HashSet<>(list);
        return new ArrayList<>(ls);
    }

    List<VariableDefinition> getPiecewiseList(List<String> vars) {
        List<VariableDefinition> l0 = new ArrayList<>();
        for (String sr : vars) {
            VariableDefinition var = getVariable(sr);
            if (var.getAssign() != null && var.getAssign().getPiecewise() != null) {
                l0.add(getVariable(sr));

            }
        }
        return l0;
    }

    public static void setWinbugsDir(String winbugsDir) {
        Parser.winbugsDir = winbugsDir;
    }

    public static String getWinbugsDir() {
        return winbugsDir;
    }

    protected List<SymbolRef> getDerLeafs() {
        List<SymbolRef> list = odeParameters;
        List<SymbolRef> tmp = new ArrayList<>(), toRemove = new ArrayList<>(), toAdd = new ArrayList<>();
        list.addAll(completeList(odeParameters));
        list = Util.getUniqueSymbolRef(list);
        for (SymbolRef s : list) {
            if (isCovariate(s)) {
                tmp.clear();
                CovariateDefinition cov = getCovariate(s.getSymbIdRef());
                if (cov.getContinuous() != null
                        && !cov.getContinuous().getListOfTransformation().isEmpty()
                        && cov.getContinuous().getListOfTransformation().get(0) != null) {
                    tmp = getParSubList(cov);
                }
                if (!tmp.isEmpty()) {
                    toRemove.add(s);
                    toAdd.addAll(tmp);
                }
            } else {
                tmp = parVariablesFromMap.get(s.getSymbIdRef());
                if (tmp != null && !tmp.isEmpty()) {
                    toRemove.add(s);
                    toAdd.addAll(tmp);
                } else {
                    tmp = new ArrayList<>();
                }
            }
        }
        list.removeAll(toRemove);
        list.addAll(toAdd);
        list = completeList(list);
        return Util.getUniqueSymbolRef(list);
    }

    protected List<SymbolRef> getPiecewiseLeafs(String id) {
        List<SymbolRef> list = new ArrayList<>();
        list.addAll(piecewiseParameters.get(id));
        List<SymbolRef> tmp = new ArrayList<>(), toRemove = new ArrayList<>(), toAdd = new ArrayList<>();
        list = Util.getUniqueSymbolRef(list);
        for (SymbolRef s : list) {
            tmp = parVariablesFromMap.get(s.getSymbIdRef());
            if (tmp != null && !tmp.isEmpty()) {
                toAdd.addAll(tmp);
            } else {
                tmp = new ArrayList<>();
            }
        }
        list.addAll(toAdd);
        list = completeList(list);
        return Util.getUniqueSymbolRef(list);
    }

    protected List<SymbolRef> getDerLeafs(List<SymbolRef> list) {
        List<SymbolRef> tmp = new ArrayList<>(), toRemove = new ArrayList<>(), toAdd = new ArrayList<>();
        list.addAll(completeList(list));
        list = Util.getUniqueSymbolRef(list);
        for (SymbolRef s : list) {

            tmp.clear();
            tmp = getParSubList(getVariable(s.getSymbIdRef()));
            if (!tmp.isEmpty()) {
                toRemove.add(s);
                toAdd.addAll(tmp);
            }
            {
                tmp = parVariablesFromMap.get(s.getSymbIdRef());
                if (tmp != null && !tmp.isEmpty()) {
                    toAdd.addAll(tmp);
                } else {
                    tmp = new ArrayList<>();
                }
            }
        }
        list.removeAll(toRemove);
        list.addAll(toAdd);
        list = completeList(list);
        return Util.getUniqueSymbolRef(list);
    }

    private List<SymbolRef> getThetaParameters() {
        List<SymbolRef> list = new ArrayList<>();
        List<SymbolRef> toRemove = new ArrayList<>();
        for (SymbolRef s : stateV_Parameters) {
            if (parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                for (SymbolRef s1 : parVariablesFromMap.get(s.getSymbIdRef())) {
                    if (!parVariablesFromMap.containsKey(s1.getSymbIdRef())) {
                        if (!isCovariate(s1)) {
                            if (!list.contains(s1)) {
                                list.add(s1);
                            }
                        } else {
                            if (!covPascalList.contains(s1)) {
                                covPascalList.add(s1);
                            }
                        }
                    }
                }
            } else {
                if (!isCovariate(s)) {
                    if (!list.contains(s)) {
                        list.add(s);
                    }
                } else {
                    if (!covPascalList.contains(s)) {
                        covPascalList.add(s);
                    }
                }
            }
        }
        for (SymbolRef s : list) {
            if (s.getSymbIdRef().equals(lexer.getDom().getListOfIndependentVariable().get(0).getSymbId())) {
                toRemove.add(s);
            } else if (isVariableDefinition(s.getSymbIdRef())) {
                toRemove.add(s);
            }
        }
        list.removeAll(toRemove);
        return list;
    }

    private List<SymbolRef> getThetaParameters_NEW() {
        List<SymbolRef> list = leafOdeParameters;
        List<SymbolRef> toReturn = new ArrayList<>();
        List<SymbolRef> toRemove = new ArrayList<>();
        for (SymbolRef s : list) {
            if (!isCovariate(s)) {
                toReturn.add(s);
            }
            if (parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                toRemove.add(s);
            } else if (isVariableDefinition(s.getSymbIdRef())) {
                toRemove.add(s);
            }
        }
        toReturn.removeAll(toRemove);
        return toReturn;
    }

    protected List<SymbolRef> getPiecewiseParameters_NEW(List<SymbolRef> list) {
        List<SymbolRef> toReturn = new ArrayList<>();
        List<SymbolRef> toRemove = new ArrayList<>();
        for (SymbolRef s : list) {
            toReturn.add(s);
            if (parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                toRemove.add(s);
            }
        }
        toReturn.removeAll(toRemove);
        return toReturn;
    }

    protected List<SymbolRef> getPiecewiseParameters_Pascal(List<SymbolRef> list) {
        List<SymbolRef> tmp = new ArrayList<>(), toRemove = new ArrayList<>(), toAdd = new ArrayList<>();
        list = Util.getUniqueSymbolRef(list);
        for (SymbolRef s : list) {
            tmp = parVariablesFromMap.get(s.getSymbIdRef());
            if (tmp != null && !tmp.isEmpty()) {
                toAdd.addAll(tmp);
            } else {
                tmp = new ArrayList<>();
            }
        }
        list.removeAll(toRemove);
        list.addAll(toAdd);
        list = completeList(list);
        return Util.getUniqueSymbolRef(list);
    }

    private SymbolRef getEl(List<SymbolRef> l, String sym) {
        for (SymbolRef s : l) {
            if (s.getSymbIdRef().equals(sym)) {
                return s;
            }
        }
        return null;
    }

    private List<SymbolRef> getOdeUsedContCov() {
        List<SymbolRef> list = leafOdeParameters, used = new ArrayList<>();
        for (SymbolRef s : list) {
            if (isCovariate(s)) {
                if (((CovariateDefinition) lexer.getAccessor().fetchElement(s)).getContinuous() != null) {
                    used.add(s);
                }
            }
        }

        return used;
    }

    private List<SymbolRef> getOdeUsedCatCov() {
        List<SymbolRef> list = leafOdeParameters, used = new ArrayList<>();
        for (SymbolRef s : list) {
            if (isCovariate(s)) {
                if (((CovariateDefinition) lexer.getAccessor().fetchElement(s)).getCategorical() != null) {
                    used.add(s);
                }
            }
        }

        return used;
    }

    private List<SymbolRef> getPwUsedCov() {
        List<SymbolRef> list = leafPiecewiseParameters, used = new ArrayList<>();
        for (SymbolRef s : list) {
            if (isCovariate(s)) {
                used.add(s);
            }
        }

        return used;
    }

    protected boolean isUsedCov(CovariateDefinition cov) {
        for (SymbolRef s : usedOdeContCovNames) {
            if (s.getSymbIdRef().equals(cov.getSymbId())) {
                return true;
            }
        }
        return false;
    }

    protected String getModelName() {
        return ConverterProvider.getModelName(xmlFileName);
    }

    private void printList(List<Object> list, String name) {
        if (!DEBUG) {
            return;

        }
        if (list.size() > 0) {
            if (list.get(0) instanceof VariableDefinition) {
                outDebug.println("----  " + list.size() + " " + name + " " + concatVars((List<VariableDefinition>) (List<?>) list) + "\n");
            } else if (list.get(0) instanceof StructuralBlock) {
                outDebug.println("----  " + list.size() + " " + name + " " + concatSblocks((List<StructuralBlock>) (List<?>) list) + "\n");
            } else {
                outDebug.println("----  " + list.size() + " " + name + " " + concat((List<SymbolRef>) (List<?>) list) + "\n");
            }
        } else {
            outDebug.println("----  " + list.size() + " " + name + "\n");
        }

    }

    private void printCompleteList(List<Object> list, String name) {
        if (!DEBUG) {
            return;
        }
        outDebug.println("----  " + list.size() + " " + name + " ");
        if (list.size() > 0) {
            if (list.get(0) instanceof CommonVariableDefinition) {
                for (CommonVariableDefinition o : (List<CommonVariableDefinition>) (List<?>) list) {
                    outDebug.print(o.getSymbId() + " ");
                }
            } else if (list.get(0) instanceof SymbolRef) {
                for (SymbolRef o : (List<SymbolRef>) (List<?>) list) {
                    outDebug.print(o.getId() + " ");
                }
            }
        }
        outDebug.println("");
    }

    private void updateCovariateLists() {
        for (SymbolRef cv : usedCovNames) {
            if (!Util.getList(usedOdeContCovNames).contains(cv.getSymbIdRef()) && isContinuousCovariate(cv)) {
                usedOdeContCovNames.add(cv);
            } else if (!Util.getList(usedOdeCatCovNames).contains(cv.getSymbIdRef()) && isCategoricalCovariate(cv)) {
                usedOdeCatCovNames.add(cv);
            }
        }
    }

    protected void createUsefulStructures() throws UnsupportedDataTypeException {
        this.setModelName();
        List<VariabilityLevelDefinition> varLevDef;

        VariabilityDefnBlock vmPrior = null;
        for (VariabilityDefnBlock vm : lexer.getDom().getModelDefinition().getListOfVariabilityModel()) {
            if (vm.getType().value().equals("parameterVariability")) {
                vmPrior = vm;
                break;
            }
        }
        if (vmPrior == null) {
            System.err.println("No priors provided");
        } else {
            varLevDef = vmPrior.getLevel();
            for (VariabilityLevelDefinition vl : varLevDef) {
                if (vl.getParentLevel() == null) {
                    priorLevel = vl;
                }
            }
        }
        completeStateVariablesList = new ArrayList<>();
        stateVariablesStructuralBlockList = new ArrayList<>();
        hasCovariate = lexer.getCovariates().size() > 0;
        if (DEBUG) {
            outDebug.println("----  " + lexer.getCovariates().size() + " covariates ");
            for (CovariateDefinition cv : lexer.getCovariates()) {
                outDebug.print(cv.getSymbId() + " ");
            }
            outDebug.println("");
        }
        for (StructuralBlock sb : lexer.getStructuralBlocks()) {
            hasDiffEquations = sb.getStateVariables().size() > 0;
        }

        for (StructuralBlock _sb : lexer.getStructuralBlocks()) {
            completeStateVariablesList.addAll(_sb.getStateVariables());
            stateV_Parameters = getAllCommonTypesList((List< PharmMLRootType>) (List<?>) _sb.getStateVariables());
            simpleP_Parameters = getAllCommonTypesList((List< PharmMLRootType>) (List<?>) _sb.getParameters());
            localV_Parameters = getAllCommonTypesList((List< PharmMLRootType>) (List<?>) _sb.getLocalVariables());

            List<ParameterBlock> pbs = getParameterBlocks();
            for (ParameterBlock pb : pbs) {
                indivV_Parameters = getAllCommonTypesList((List< PharmMLRootType>) (List<?>) pb.getIndividualParameters());
                simpleP_Parameters.addAll(getAllCommonTypesList((List< PharmMLRootType>) (List<?>) pb.getParameters()));
                randVar_Parameters.addAll(getAllCommonTypesList((List< PharmMLRootType>) (List<?>) pb.getRandomVariables()));
            }
            List<SymbolRef> covVarList = new ArrayList<>();
            covVarList.addAll(getAllCommonTypesList((List< PharmMLRootType>) (List<?>) lexer.getCovariates()));
            stateV_Parameters = completeList(stateV_Parameters);
            indivV_Parameters = completeList(indivV_Parameters);
            outDebug.println("");
            printList((List<Object>) (List<?>) stateV_Parameters, "stateV_Parameters");
            printList((List<Object>) (List<?>) indivV_Parameters, "indivV_Parameters");
            simpleP_Parameters = completeList(simpleP_Parameters);
            localV_Parameters = completeList(localV_Parameters);
            printList((List<Object>) (List<?>) simpleP_Parameters, "simpleP_Parameters");
            printList((List<Object>) (List<?>) localV_Parameters, "localV_Parameters");
            stateVariablesStructuralBlockList.add(_sb);
            piecewiseVariables.addAll(getPiecewiseList(getSymbIdList(_sb.getLocalVariables())));
            printList((List<Object>) (List<?>) stateVariablesStructuralBlockList, "stateVariablesStructuralBlockList");
            printList((List<Object>) (List<?>) piecewiseVariables, "piecewiseVariables");
            piecewiseCompleteList = getPiecewiseList();
            if (this instanceof PascalParser) {
                String id = "";
                odeParameters.addAll(getAllDerLeafVariable(_sb.getStateVariables()));
                for (Object pw : piecewiseCompleteList) {
                    if (pw instanceof VariableDefinition) {
                        id = ((VariableDefinition) pw).getSymbId();
                    } else if (pw instanceof PopulationParameter) {
                        id = ((PopulationParameter) pw).getSymbId();
                    }
                    List<String> srl = new ArrayList<>();
                    srl.add(id);
                    piecewiseParameters.put(id, getAllPiecewiseLeafVariable(srl));
                }
                odeParameters1.addAll(getDerPurgedList(odeParameters));
                List<VariableDefinition> vl = getVariables(_sb);
                List<VariableDefinition> sbl = _sb.getLocalVariables();
                odeParameters1.addAll(getAllVarLeafVariable((vl)));
                odeParameters2.addAll(getAllVarLeafVariable(sbl));
            }
        }
        odeParameters = Util.getUniqueSymbolRef(odeParameters); // elenco delle var da cui dipendono direttamente le variabili di stato 
        odeParameters1 = Util.getUniqueSymbolRef(odeParameters1); // elenco delle var da cui dipendono le variabili di stato (foglie) ripulite delle var. di stato
        odeParameters2 = Util.getUniqueSymbolRef(odeParameters2); // lista delle variabili da cui dipendono le variables
        piecewiseParameters2 = Util.getUniqueSymbolRef(piecewiseParameters2);
        printList((List<Object>) (List<?>) odeParameters, "odeParameters");
        printList((List<Object>) (List<?>) odeParameters1, "odeParameters1");
        printList((List<Object>) (List<?>) odeParameters2, "odeParameters2");
        odeParametersOld.addAll(odeParameters);
        for (Map.Entry<String, List<SymbolRef>> pair : piecewiseParameters.entrySet()) {
            printList((List<Object>) (List<?>) pair.getValue(), "piecewiseParameters di " + pair.getKey());
        }
        printList((List<Object>) (List<?>) piecewiseParameters2, "piecewiseParameters2");
        printCompleteList((List<Object>) (List<?>) piecewiseCompleteList, "piecewiseCompleteList");

        if (this instanceof PascalParser) {
            leafOdeParameters = getDerLeafs();
            for (Map.Entry<String, List<SymbolRef>> pair : piecewiseParameters.entrySet()) {
                leafPiecewiseParameters.addAll(getPiecewiseLeafs(pair.getKey())); // 1 agosto
            }
            leafPiecewiseParameters = Util.getUniqueSymbolRef(leafPiecewiseParameters);
            printList((List<Object>) (List<?>) leafOdeParameters, "leafOdeParameters");
            printList((List<Object>) (List<?>) leafPiecewiseParameters, "leafPiecewiseParameters");
            theta_Parameters = getThetaParameters_NEW();
            for (Object pw : piecewiseCompleteList) {
                String id = getId(pw);
                piecewiseLeafParameters.put(id, getPiecewiseParameters_NEW(piecewiseParameters.get(id))); // foglie 
            }
            theta_Parameters = Util.getUniqueSymbolRef(theta_Parameters); // lista di tutte le variabili foglie delle eq. diff. ripulite delle covariate
            printList((List<Object>) (List<?>) theta_Parameters, "theta_Parameters");
            for (Map.Entry<String, List<SymbolRef>> pair : piecewiseLeafParameters.entrySet()) {
                printList((List<Object>) (List<?>) pair.getValue(), "piecewiseLeafParameters di " + pair.getKey());
            }
            boolean isOdeCovDep = false;
            for (CovariateDefinition cd : lexer.getCovariates()) {

                if (isInList(cd, odeParameters) // var da cui dipendono direttamente le variabili di stato.
                        || isInList(cd, odeParameters1) // 
                        || isInList(cd, odeParameters2) // 
                        || isInList(cd, theta_Parameters) // va tolto?
                        || isInList(cd, covPascalList)
                        || isInList(cd, leafOdeParameters)) { // covariate foglia, non sono in theta_Parameters
                    isOdeCovDep = true;
                    break;
                }
            }
            for (CovariateDefinition cd : lexer.getCovariates()) {
                CovariateTransformation ts;
                String sym;
                if (cd.getContinuous() != null && !cd.getContinuous().getListOfTransformation().isEmpty()) {
                    ts = cd.getContinuous().getListOfTransformation().get(0);
                    sym = ts.getTransformedCovariate().getSymbId();
                    if (isInList(Util.getList(odeParameters), sym) // var da cui dipendono direttamente le variabili di stato.
                            || isInList(Util.getList(odeParameters1), sym) // 
                            || isInList(Util.getList(odeParameters2), sym) // 
                            || isInList(Util.getList(theta_Parameters), sym)
                            || isInList(Util.getList(covPascalList), sym)
                            || isInList(Util.getList(leafOdeParameters), sym)) { // covariate foglia, non sono in theta_Parameters
                        isOdeCovDep = true;
                        break;
                    }
                }
            }
            usedOdeContCovNames = getOdeUsedContCov();
            usedOdeCatCovNames = getOdeUsedCatCov();

            usedPwCovNames = getPwUsedCov();
            printList((List<Object>) (List<?>) usedOdeContCovNames, "usedOdeCovNames");
            printList((List<Object>) (List<?>) usedPwCovNames, "usedPwCovNames");
            hasCovariate = isOdeCovDep && (!usedOdeContCovNames.isEmpty() || !usedOdeCatCovNames.isEmpty()); // 26 luglio
            List<SymbolRef> thetaTest;
            thetaTest = getThetaParameters_NEW();
            if (DEBUG) {
                outDebug.println("----  " + thetaTest.size() + " thetaTest " + concat(thetaTest) + "\n");
                if (thetaTest.size() != theta_Parameters.size()) {
                    outDebug.println("*** ERROR ***");
                }
                outDebug.println("----  " + parVariablesFromMap.size() + " Variables Map ");
                for (Map.Entry<String, List<SymbolRef>> v : parVariablesFromMap.entrySet()) {
                    outDebug.print(String.format("\n\t %s:\n\t\t", v.getKey()));
                    for (SymbolRef sr : v.getValue()) {
                        outDebug.print(sr.getSymbIdRef() + " ");
                    }
                }
                outDebug.println("");
            }
        }
    }

    String getId(Object o) {
        if (o instanceof VariableDefinition) {
            return ((VariableDefinition) o).getSymbId();
        } else if (o instanceof IndividualParameter) {
            return ((IndividualParameter) o).getSymbId();
        } else if (o instanceof CommonVariableDefinition) {
            return ((CommonVariableDefinition) o).getSymbId();
        } else if (o instanceof PopulationParameter) {
            return ((PopulationParameter) o).getSymbId();
        } else if (o instanceof ParameterRandomVariable) {
            return ((ParameterRandomVariable) o).getSymbId();
        } else if (o instanceof VariableDefinition) {
            return ((VariableDefinition) o).getSymbId();
        }
        return "@";
    }

    protected List<SymbolRef> getAllDerLeafVariable(List<DerivativeVariable> derivatives) {
        List<SymbolRef> list = new ArrayList<>();
        List<SymbolRef> subList = new ArrayList<>();
        for (DerivativeVariable dv : derivatives) {
            if (dv.getAssign() != null) {
                subList = getParSubList(dv.getAssign());
            }
            list.addAll(subList);
        }
        return Util.getUniqueSymbolRef(list);
    }

    protected List<SymbolRef> getAllPiecewiseLeafVariable(List<String> vars) {
        List<SymbolRef> list = new ArrayList<>();
        List<SymbolRef> subList = new ArrayList<>();

        for (String dv : vars) {
            if (getVariable(dv) instanceof VariableDefinition) {
                VariableDefinition v = getVariable(dv);
                subList = getParSubList(v.getAssign());
            } else if (getPopulationParameter(dv) instanceof PopulationParameter) {
                PopulationParameter v = getPopulationParameter(dv);
                subList = getParSubList(v.getAssign());
            }
            list.addAll(subList);
        }
        list = completeList(list);
        return Util.getUniqueSymbolRef(list);
    }

    protected List<SymbolRef> getAllVarLeafVariable(List<VariableDefinition> vars) {
        List<SymbolRef> list = new ArrayList<>();
        List<SymbolRef> subList = new ArrayList<>();
        for (VariableDefinition vd : vars) {
            if (vd.getAssign() != null) {
                subList = getParSubList(vd.getAssign());
            }
            list.addAll(subList);
        }
        return Util.getUniqueSymbolRef(list);
    }

    protected List<SymbolRef> getAllParLeafVariable(List<PopulationParameter> pars) {
        List<SymbolRef> list = new ArrayList<>();
        List<SymbolRef> subList = new ArrayList<>();
        for (PopulationParameter sp : pars) {
            if (sp.getAssign() != null) {
                subList = getParSubList(sp.getAssign());
            }
            list.addAll(subList);
        }
        return Util.getUniqueSymbolRef(list);
    }

    private List<SymbolRef> getDerPurgedList(List<SymbolRef> list) {
        List<SymbolRef> toRemove = new ArrayList();
        for (SymbolRef s : list) {
            if (isDerivativeVar(s.getSymbIdRef())) {
                toRemove.add(s);
            }
        }
        list.removeAll(toRemove);
        return Util.getUniqueSymbolRef(list);
    }

    private List<SymbolRef> getPiecewisePurgedList(List<SymbolRef> list) {
        List<SymbolRef> toRemove = new ArrayList();
        for (SymbolRef s : list) {
            if (isPiecewiseVar(s.getSymbIdRef())) {
                toRemove.add(s);
            }
        }
        list.removeAll(toRemove);
        return Util.getUniqueSymbolRef(list);
    }

    protected void updatepiecewiseIndexMap(String id) {
        if (piecewiseIndexMap.get(id) == null) {
            if (!piecewiseIndexMap.containsKey(id)) {
                piecewiseIndexMap.put(id, ++pwNum);
            }
        }
    }

    protected List<SymbolRef> getAllCommonTypesList(List<PharmMLRootType> t) throws UnsupportedDataTypeException {

        List<SymbolRef> list = new ArrayList<>();
        List<SymbolRef> subList = new ArrayList<>();
        if (t == null) {
            return list;
        }
        ParameterRandomVariable rv;
        PopulationParameter sp;
        IndividualParameter ip;
        VariableDefinition vd;
        DerivativeVariable dv;
        CovariateDefinition cov;
        CovariateTransformation covT;

        for (PharmMLRootType element : t) {
            if (element instanceof IndependentVariable) {
                continue;
            } else if (element instanceof PopulationParameter) {
                sp = (PopulationParameter) element;
                if (sp.getAssign() != null && sp.getAssign().getPiecewise() != null) {
                    piecewisePopPars.add(sp);
                    updatepiecewiseIndexMap(sp.getSymbId());
                }
                if (sp.getDistribution() != null) {
                    subList = new ArrayList<>();
                    List<DistributionParameter> parList = sp.getDistribution().getProbOnto().getListOfParameter();
                    for (DistributionParameter dp : parList) {
                        subList.addAll(getParSubList(dp.getAssign()));
                    }
                    randVariablesFromMap.put(sp.getSymbId(), subList);
                }
                subList = getParSubList(sp);
                if (subList.size() > 0) {
                    parVariablesFromMap.put(sp.getSymbId(), subList);
                }
            } else if (element instanceof IndividualParameter) {
                ip = (IndividualParameter) element;
                subList = getParSubList(ip);
                if (subList.size() > 0) {
                    parVariablesFromMap.put(ip.getSymbId(), subList);
                }
                if (ip.getDistribution() != null) {
                    subList = new ArrayList<>();
                    List<DistributionParameter> parList = ip.getDistribution().getProbOnto().getListOfParameter();
                    for (DistributionParameter dp : parList) {
                        subList.addAll(getParSubList(dp.getAssign()));
                    }
                    randVariablesFromMap.put(ip.getSymbId(), subList);
                }
            } else if (element instanceof VariableDefinition) {
                vd = (VariableDefinition) element;
                subList = getParSubList(vd);
                if (subList.size() > 0) {
                    parVariablesFromMap.put(vd.getSymbId(), subList);// getAllCommonTypesList VariableDefinition
                }
            } else if (element instanceof CovariateTransformation) {
                covT = (CovariateTransformation) element;
                subList = getParSubList(covT);
                if (subList.size() > 0) {
                    parVariablesFromMap.put(covT.getTransformedCovariate().getSymbId(), subList);// getAllCommonTypesList CovariateTransformation
                }
            } else if (element instanceof CovariateDefinition) {
                cov = (CovariateDefinition) element;
                String id = cov.getSymbId();
                if (cov.getContinuous() != null && !cov.getContinuous().getListOfTransformation().isEmpty()) {// 17 luglio 2016
                    covT = (CovariateTransformation) cov.getContinuous().getListOfTransformation().get(0);
                    id = covT.getTransformedCovariate().getSymbId();
                }
                subList = getParSubList(cov);
                if (subList.size() > 0) {
                    parVariablesFromMap.put(id, subList);
                }
            } else if (element instanceof DerivativeVariable) {
                dv = (DerivativeVariable) element;
                doDerivativeRef(dv.getSymbId());
                subList = getParSubList(dv);
                if (subList.size() > 0) {
                    parVariablesFromMap.put(dv.getSymbId(), subList);
                }
            } else if (element instanceof ParameterRandomVariable) {
                rv = (ParameterRandomVariable) element;
                doRandomVariable(rv);
                subList = new ArrayList<>();
                if (rv.getDistribution() != null) {
                    List<DistributionParameter> parList = rv.getDistribution().getProbOnto().getListOfParameter();
                    for (DistributionParameter dp : parList) {
                        subList.addAll(getParSubList(dp.getAssign()));
                    }
                    randVariablesFromMap.put(rv.getSymbId(), subList);
                }

            } else {
                throw new UnsupportedDataTypeException(t.getClass().getSimpleName());
            }
            list.addAll(subList);
        }
        List<SymbolRef> newList = Util.getUniqueSymbolRef(list);
        return newList;
    }

    List<SymbolRef> completeList(List<SymbolRef> l) {
        List<SymbolRef> completeList = new ArrayList<>();
        completeList.addAll(l);
        List<SymbolRef> tmpList = new ArrayList<>();
        do {
            tmpList.clear();
            for (SymbolRef sr : completeList) {
                List<SymbolRef> currList = parVariablesFromMap.get(sr.getSymbIdRef());
                if (currList != null) {
                    for (SymbolRef r : currList) {
                        if (!completeList.contains(r)
                                && !isIndependentVariableSym(r.getSymbIdRef())) {
                            tmpList.add(r);
                        }
                    }
                }
            }
            l = tmpList;
            completeList.addAll(Util.getUniqueSymbolRef(tmpList));
        } while (!tmpList.isEmpty());
        completeList.addAll(tmpList);
        return Util.getUniqueSymbolRef(completeList);
    }

    List<String> completeIdList(List<String> l) {
        List<String> completeList = new ArrayList<>();
        completeList.addAll(l);
        List<String> tmpList = new ArrayList<>();
        do {
            tmpList.clear();
            for (String sr : l) {
                if (parVariablesFromMap.get(sr) != null) {
                    List<String> currList = Util.getList(parVariablesFromMap.get(sr));
                    if (currList != null && currList.size() > 0) {
                        for (String r : currList) {
                            if (!completeList.contains(r)
                                    && !isIndependentVariableSym(r)) {
                                tmpList.add(r);
                            }
                        }
                    }
                }
            }
            l = tmpList;
            completeList.addAll(Util.getUniqueString(tmpList));
        } while (!tmpList.isEmpty());
        completeList.addAll(tmpList);
        return Util.getUniqueString(completeList);
    }

    List<SymbolRef> getFunParameters(FunctionCallType fun) {
        List<SymbolRef> subList = new ArrayList<>();
        for (FunctionCallType.FunctionArgument arg : fun.getListOfFunctionArgument()) {
            if (arg.getSymbRef() != null) {
                subList.add(arg.getSymbRef());
            } else if (arg.getAssign() != null) {
                subList.addAll(getParSubList(arg.getAssign()));
            }
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(Rhs eq) {
        List<SymbolRef> subList = new ArrayList<>();

        if (eq.getBinop() != null) {
            subList.addAll(getJaXBParList(getBinopContent(eq.getBinop())));
        } else if (eq.getUniop() != null) {
            subList.addAll(getParSubList(eq.getUniop()));
        } else if (eq.getSymbRef() != null) {
            subList.add(eq.getSymbRef());
        } else if (eq.getMatrixUniop() != null) {
            subList.addAll(getParSubList(eq.getMatrixUniop()));
        } else if (eq.getFunctionCall() != null) {
            subList.addAll(getFunParameters(eq.getFunctionCall()));
        } else if (eq.getPiecewise() != null) {
            subList.addAll(getParSubList(eq.getPiecewise()));
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(Equation eq) {
        List<SymbolRef> subList = new ArrayList<>();
        if (eq.getBinop() != null) {
            subList.addAll(getJaXBParList(getBinopContent(eq.getBinop())));
        } else if (eq.getUniop() != null) {
            subList.addAll(getParSubList(eq.getUniop()));
        } else if (eq.getSymbRef() != null) {
            subList.add(eq.getSymbRef());
        } else if (eq.getFunctionCall() != null) {
            subList.addAll(getFunParameters(eq.getFunctionCall()));
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(ExpressionValue ev) {
        List<SymbolRef> subList = new ArrayList<>();
        if (ev instanceof Binop) {
            subList.addAll(getJaXBParList(getBinopContent((Binop) ev)));
        } else if (ev instanceof Uniop) {
            subList.addAll(getParSubList((Uniop) ev));
        } else if (ev instanceof SymbolRef) {
            subList.add((SymbolRef) ev);
        } else if (ev instanceof MatrixUniOp) {
            subList.addAll(getJaXBParList(getMatrixContent((MatrixUniOp) ev)));
        } else if (ev instanceof FunctionCallType) {
            subList.addAll(getFunParameters((FunctionCallType) ev));
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(Uniop uOp) {
        List<SymbolRef> subList = new ArrayList<>();
        ExpressionValue ev = uOp.getValue();
        if (ev instanceof Binop) {
            subList.addAll(getJaXBParList(getBinopContent((Binop) (uOp.getValue()))));
        } else if (ev instanceof Uniop) {
            subList.addAll(getParSubList((Uniop) (uOp.getValue())));
        } else if (ev instanceof SymbolRef) {
            subList.add((SymbolRef) ev);
        } else if (ev instanceof FunctionCallType) {
            subList.addAll(getFunParameters((FunctionCallType) ev));
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(Piecewise pw) {
        List<SymbolRef> subList = new ArrayList<>();
        ExpressionValue pv;
        Condition pc;
        for (Piece piece : pw.getPiece()) {
            pv = piece.getValue();
            subList.addAll(getParSubList(pv));

            pc = piece.getCondition();
            if (pc.getLogicBinop() != null) {
                List<JAXBElement<?>> content = pc.getLogicBinop().getContent();
                for (JAXBElement<?> el : content) {
                    {
                        subList.addAll(getParSubList(el));
                    }
                }
            } else if (pc.getLogicUniop() != null);
        }
        return Util.getUniqueSymbolRef(subList);
    }

    protected List<SymbolRef> getParSubList(JAXBElement je) {
        return getParSubList(je.getValue());
    }

    protected List<SymbolRef> getParSubList(Object o) {
        List<SymbolRef> list0 = new ArrayList<>();
        if (o instanceof Uniop) {
            return getParSubList(((Uniop) o).getValue());
        } else if (o instanceof PopulationParameter) {
            return getParSubList(((PopulationParameter) o));
        } else if (o instanceof IndividualParameter) {
            return getParSubList(((IndividualParameter) o));
        } else if (o instanceof DerivativeVariable) {
            return getParSubList(((DerivativeVariable) o));
        } else if (o instanceof CovariateDefinition) {
            return getParSubList(((CovariateDefinition) o));
        } else if (o instanceof ContinuousCovariate) {
            return getParSubList(((ContinuousCovariate) o));
        } else if (o instanceof Rhs) {
            return getParSubList(((Rhs) o));
        } else if (o instanceof VariableDefinition) {
            return getParSubList(((VariableDefinition) o));
        } else if (o instanceof Binop) {
            return getParSubList((Binop) o);
        } else if (o instanceof FunctionCallType) {
            return getParSubList((FunctionCallType) o);
        } else if (o instanceof Piecewise) {
            return getParSubList((Piecewise) o);
        } else if (o instanceof Distribution) {
            return getParSubList(((Distribution) o));
        } else if (o instanceof SymbolRef) {
            list0.add((SymbolRef) o);
            return list0;
        } else if (o instanceof IntValue) {
            return list0;
        } else if (o instanceof CategoryRef) {
            return list0;
        } else {
            throw new UnsupportedOperationException(o.getClass().getSimpleName());
        }
    }

    protected List<SymbolRef> getParSubList(PopulationParameter sp) {
        List<SymbolRef> subList = new ArrayList<>();
        if (sp.getDistribution() != null && sp.getDistribution().getProbOnto() != null) {
            ProbOnto value = sp.getDistribution().getProbOnto();
            List<DistributionParameter> parList = value.getListOfParameter();
        } else if (sp.getAssign() != null) {
            subList.addAll(getParSubList(sp.getAssign()));
        } else if (sp.getAssign().getSymbRef() != null) {
            subList.add(sp.getAssign().getSymbRef());
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(DerivativeVariable der) {
        List<SymbolRef> subList = new ArrayList<>();
        if (der.getAssign() != null) {
            if (der.getAssign() != null) {
                subList.addAll(getParSubList(der.getAssign()));
            } else if (der.getAssign().getSymbRef() != null) {
                subList.add(der.getAssign().getSymbRef());
            }
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(IndividualParameter ip) {
        StructuredModel ipGM;
        StructuredModel.LinearCovariate ipLC;
        StructuredModel.GeneralCovariate ipGC = null;
        String transf = null;
        ipGM = ip.getStructuredModel();
        List<SymbolRef> subList = new ArrayList<>();
        if (ipGM != null) {
            subList.add(ipGM.getListOfRandomEffects().get(0).getSymbRef().get(0));
        }
        if (ip.getAssign() != null) {
            subList.addAll(getParSubList(ip.getAssign()));
        } else if (ipGM != null) {
            ipGC = ipGM.getGeneralCovariate();
            ipLC = ipGM.getLinearCovariate();
            if (ipGC != null) {
                if (ipGC.getAssign() != null) {
                    subList.addAll(getParSubList(ipGC.getAssign()));
                }
            } else if (ipLC != null) {
                StructuredModel.LinearCovariate.PopulationValue pp = ipLC.getPopulationValue();
                List<CovariateRelation> cov = ipLC.getListOfCovariate();
                subList.addAll(getParSubList(pp.getAssign()));
                if (cov.size() > 0) {
                    for (CovariateRelation c : cov) {
                        for (FixedEffectRelation fe : c.getListOfFixedEffect()) {
                            if (fe.getSymbRef() != null) {
                                subList.add(fe.getSymbRef());
                            }
                            if (c.getSymbRef() != null) {
                                subList.add(c.getSymbRef());
                            }
                        }
                    }
                }
            }
            if (ipGM.getTransformation() != null) {
                transf = ipGM.getTransformation().getType().toString().toLowerCase();
                if (transf.equals("identity")) {
                    transf = null;
                }
            }
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(VariableDefinition var) {
        List<SymbolRef> subList = new ArrayList<>();
        if (var != null && var.getAssign() != null) {
            if (var.getAssign() != null) {
                subList.addAll(getParSubList(var.getAssign()));
            } else if (var.getAssign().getSymbRef() != null) {
                subList.add(var.getAssign().getSymbRef());
            }
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(Distribution var) {
        List<SymbolRef> subList = new ArrayList<>();
        List<DistributionParameter> dpList = var.getProbOnto().getListOfParameter();
        for (DistributionParameter dp : dpList) {
            subList.addAll(getParSubList(dp));
        }
        return subList;
    }

    protected List<SymbolRef> getParSubList(CovariateDefinition cov) {
        List<SymbolRef> subList = new ArrayList<>();
        ContinuousCovariate cc = cov.getContinuous();

        CovariateTransformation ct = null;
        if (cc != null) {
            if (!cc.getListOfTransformation().isEmpty()
                    && (ct = cov.getContinuous().getListOfTransformation().get(0)) != null) {
                if (ct.getAssign() != null) {
                    subList.addAll(getParSubList(ct.getAssign()));
                }
            }
        }

        return subList;
    }

    protected List<SymbolRef> getParSubList(CategoricalCovariate cov) {
        List<SymbolRef> subList = new ArrayList<>();
        List<CategoryRef_> list = categoricalMap.get(cov.getId());

        return subList;
    }

    protected List<SymbolRef> getJaXBParList(List<JAXBElement<?>> cont) {
        List<SymbolRef> list0 = new ArrayList<>();
        if (cont != null) {
            for (JAXBElement<?> c : cont) {
                list0.addAll(getJaXBParList(c));
            }
        }
        return list0;
    }

    private List<SymbolRef> getJaXBParList(JAXBElement<?> c) {
        List<SymbolRef> list0 = new ArrayList<>();
        Object el = c.getValue();
        if (el instanceof SymbolRef) {
            if (!isDerivativeVar(((SymbolRef) el).getSymbIdRef())
                    || isRandomVar(((SymbolRef) el).getSymbIdRef())) {
                list0.add((SymbolRef) el);
            }
            if (isDerivativeVar(((SymbolRef) el).getSymbIdRef())) {
                if (!Util.getList(piecewiseParameters2).contains(((SymbolRef) el).getSymbIdRef())) {
                    ;
                } else {
                    list0.add((SymbolRef) el);
                }
            }

        } else if (el instanceof Binop) {
            list0.addAll(getJaXBParList(getBinopContent((Binop) el)));
        } else if (el instanceof Uniop) {
            list0.addAll(getParSubList((Uniop) el));
        } else if (el instanceof FunctionCallType) {
            list0.addAll(getFunParameters(((FunctionCallType) el)));
        }

        return list0;
    }

    protected boolean isodeParIndividual() {
        for (SymbolRef s : odeParameters) {
            if (lexer.isIndividualParameter_(s.getSymbIdRef())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isDerivativeDependentVariable(String s) {
        for (DerivativeVariable var : completeStateVariablesList) {
            if (var.getSymbId().equals(s)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInOdeParameters1(ObservationParameter v) {
        for (SymbolRef sr : odeParameters1) {
            if (sr.getSymbIdRef().equals(extract(v.getName()))) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isInList(List<SymbolRef> list, SymbolRef s) {
        for (SymbolRef s0 : list) {
            if (s0.getSymbIdRef().equals(s.getSymbIdRef())) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isInList(List<String> list, String s) {
        for (String s0 : list) {
            if (s0.equals(s)) {
                return true;
            }
        }
        return false;
    }

    protected List<String> getSymbIdList(List<VariableDefinition> list) {
        List<String> sList = new ArrayList<>();
        for (VariableDefinition sr : list) {
            sList.add(sr.getSymbId());
        }
        return sList;
    }

    protected List<String> getPopSymbIdList(List<PopulationParameter> list) {
        List<String> sList = new ArrayList<>();
        for (PopulationParameter sr : list) {
            sList.add(sr.getSymbId());
        }
        return sList;
    }

    protected boolean isInList(Object o, List<SymbolRef> l) {
        String id0, id1 = "";
        for (SymbolRef s : l) {
            id0 = s.getSymbIdRef();
            if (o instanceof IndividualParameter) {
                id1 = ((IndividualParameter) o).getSymbId();
            } else if (o instanceof VariableDefinition) {
                id1 = ((VariableDefinition) o).getSymbId();
            } else if (o instanceof PopulationParameter) {
                id1 = ((PopulationParameter) o).getSymbId();
            } else if (o instanceof CovariateDefinition) {
                id1 = ((CovariateDefinition) o).getSymbId();
            } else if (o instanceof CovariateTransformation) {
                id1 = ((CovariateTransformation) o).getTransformedCovariate().getSymbId();
            }
            if (id0.equals(id1)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInList(Object o, Map<String, List<SymbolRef>> l) {
        String id0, id1 = "";
        for (Map.Entry<String, List<SymbolRef>> pair : l.entrySet()) {
            List<SymbolRef> list = pair.getValue();
            for (SymbolRef s : list) {
                id0 = s.getSymbIdRef();
                if (o instanceof IndividualParameter) {
                    id1 = ((IndividualParameter) o).getSymbId();
                } else if (o instanceof VariableDefinition) {
                    id1 = ((VariableDefinition) o).getSymbId();
                } else if (o instanceof PopulationParameter) {
                    id1 = ((PopulationParameter) o).getSymbId();
                } else if (o instanceof CovariateDefinition) {
                    id1 = ((CovariateDefinition) o).getSymbId();
                }
                if (id0.equals(id1)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean isInNameList(List<String> list, String s) {
        for (String s0 : list) {
            if (s0.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private Object parseObservationErrorModel(Object context, PharmMLRootType pt) {
        Object res = context;
        if (context instanceof PharmMLRootType) {
            PharmMLRootType o = (PharmMLRootType) context;
            if (o instanceof Binop) {
                Binop bin = (Binop) o;
                reversePolishStack.add(bin);
                res = parseObservationErrorModel(bin.getOperand1(), bin);
                res = parseObservationErrorModel(bin.getOperand2(), bin);
            } else if (o instanceof Rhs) {
                res = parseObservationErrorModel(getNotNull(o), null);
            } else if (o instanceof Equation) {
                res = parseObservationErrorModel(getNotNull(o), null);
            } else if (o instanceof Uniop) {
                res = parseObservationErrorModel(getNotNull(o), (Uniop) o);
                reversePolishStack.add(o);
                reversePolishStack.add(getNotNull(o));
            } else if (o instanceof SymbolRef) {
                SymbolRef ref = (SymbolRef) o;
                reversePolishStack.add(o);
                if (isRandomVar(ref.getSymbIdRef())) {
                    outDebug.println("parametro " + o.getClass() + " id " + o.getId());
                    outDebug.println("variabile " + ref.getSymbIdRef() + " random");
                    outDebug.println("pt " + pt.getId());
                    return o;
                } else {
                    return null;
                }
            }
        } else if (context instanceof JAXBElement) {
            JAXBElement<?> el = (JAXBElement<?>) context;
            if (el.getValue() instanceof SymbolRef) {
                SymbolRef refVar = ((SymbolRef) el.getValue());
                reversePolishStack.add(refVar);
                if (isRandomVar(refVar.getSymbIdRef())) {
                    if (DEBUG) {
                        outDebug.println("\nJAXB - RANDOM \"" + refVar.getSymbIdRef() + "\"");
                    }
                    //                    randomBranch.add(context);
                    return context;
                } else {
                    if (DEBUG) {
                        outDebug.println("\nJAXB - ALGEBRICA \"" + refVar.getSymbIdRef() + "\"");
                    }
                    return null;
                }
            } else if (el.getValue() instanceof Binop) {
                Binop bin = (Binop) el.getValue();
                reversePolishStack.add(bin);
                res = parseObservationErrorModel(bin.getOperand1(), bin);
                res = parseObservationErrorModel(bin.getOperand2(), bin);
            } else if (el.getValue() instanceof Uniop) {
                Uniop uniop = (Uniop) el.getValue();
                reversePolishStack.add(uniop);
                res = parseObservationErrorModel(uniop.toJAXBElement(), uniop);
            }
        }
        return res;
    }

    private Object getNotNull(PharmMLRootType ob) {
        return ob;
    }

    private boolean isNormalDistribution(AbstractContinuousUnivariateDistributionType d) {
        if (d instanceof NormalDistributionType) {
            return true;
        } else {
            return false;
        }
    }

    private String getDistName(Object context) {
        Distribution dist;
        String s;
        BinaryTree tmp;
        if (context instanceof ParameterRandomVariable) {
            dist = ((ParameterRandomVariable) context).getDistribution();
            if (dist.getUncertML() != null) {
                if (dist.getUncertML().getAbstractContinuousUnivariateDistribution() != null) {
                    return dist.getUncertML().getAbstractContinuousUnivariateDistribution().getName().toString();
                } else {
                    return ((ProbOnto) context).getName().value();
                }
            } else if (dist.getProbOnto() != null) {
                return dist.getProbOnto().getName().value();
            }
        } else if (context instanceof StructuredObsError) {
            s = ((StructuredObsError) context).getResidualError().getSymbRef().getSymbIdRef();

            if (isRandomVar(s)) {
                ParameterRandomVariable rv = getRandomVar(s);

                tmp = lexer.getStatement(rv);
                if (tmp.nodes.get(0).data instanceof ProbOnto) {
                    return ((ProbOnto) tmp.nodes.get(0).data).getName().value();
                } else if (tmp.nodes.get(0).data instanceof AbstractContinuousUnivariateDistributionType) {
                    return rv.getDistribution().getUncertML().getAbstractContinuousUnivariateDistribution().getName().toString();
                }

            } else {

                return ((ProbOnto) context).getName().value();
            }
        }

        return "Distribution unknown";
    }

    private String getDistName(ProbOnto context) {
        String name = context.getName().value();
        switch (name) {

            case "Normal1":
            case "Normal2":
            case "Normal3":
                return "dnorm";
            case "Beta1":
                return "dbeta";
            case "Exponential1":
            case "Exponential2":
                return "dexp";
            case "Gamma1":
            case "Gamma2":
            case "InverseGamma1":
                return "dgamma";
            case "LogNormal1":
            case "LogNormal2":
            case "LogNormal3":
            case "LogNormal4":
            case "LogNormal5":
            case "LogNormal6":
                return "dlnorm";
            case "StudentT1":
            case "StudentT2":

                return "dt";
            case "Uniform1":
                return "dunif";
            case "Weibull1":
            case "Weibull2":
                return "dweib";
            case "Wishart1":
            case "Wishart2":
            case "InverseWishart1":
            case "InverseWishart2":
                return "dwish";
            case "MultivariateNormal1":
            case "MultivariateNormal2":
                return "dmnorm";
            case "MultivariateStudentT1":
            case "MultivariateStudentT2":
                return "dmt";
            default:
                throw new UnsupportedOperationException("Distribution " + name + " not suppported");
        }
    }

    private String getDistName(JAXBElement<? extends AbstractContinuousUnivariateDistributionType> value) {
        String name = value.getDeclaredType().getSimpleName();
        switch (name) {
            case "NormalDistribution":
                return "dnorm";
            case "LogisticDistribution":
                return "logistic(";
            case "CauchyDistribution":
                return "cauchy";
            case "GeometricDistribution":
                return "geometric";
            case "LogNormalDistribution":
                return "logNormal";
            case "PoissonDistribution":
                return "poisson";
            default:
                return null;
        }
    }

    private String getDistName(AbstractDistributionType value) {
        String name = value.getDefinition();
        if (value instanceof NormalDistribution) {
            return "dnorm";
        } else if (value instanceof LogisticDistribution) {
            return "logistic(";
        } else if (value instanceof CauchyDistribution) {
            return "cauchy";
        } else if (value instanceof GeometricDistribution) {
            return "geometric";
        } else if (value instanceof LogNormalDistribution) {
            return "logNormal";
        } else if (value instanceof PoissonDistribution) {
            return "poisson";
        } else {
            return null;
        }
    }

    private String getVariance(PositiveRealValueType variance) {
        if (variance == null) {
            return "";
        }
        if (variance.getPrVal() != null) {
            return "" + variance.getPrVal();
        } else {
            return "" + ((VarRefType) variance.getVar()).getVarId();
        }
    }

    private String doVarTransformation(String errorTransformation, String current_symbol, String formatErrTransf, List<String> lines) {
        errorTransformation = Util.clean(errorTransformation).trim();
        if (errorTransformation.length() > 0) {
            switch (errorTransformation.trim()) {
                case "identity":
                    errorTransformation = "";
                case "log":
                case "logit":
                    String tmp = errorTransformation + current_symbol;
                    String tmp1 = String.format(formatErrTransf, errorTransformation, current_symbol);
                    lines.add(String.format("%s %s %s", tmp, assignSymbol, tmp1));
                    current_symbol = tmp;
                    break;
                default:
                    throw new UnsupportedOperationException(errorTransformation.trim() + " transformation " + " not allowed");
            }
        }
        return current_symbol;
    }

    private List<String> selectErrorModel(StructuredObsError context) throws FileNotFoundException {
        if (getDistributionType(context) != null) {
            return doErrorModel(context);
        } else {
            return doErrorModelProbOnto(context);
        }
    }

    private List<String> doErrorModel(StructuredObsError context) throws FileNotFoundException {
        StructuredObsError.Output output = context.getOutput();
        StructuredObsError.ErrorModel error = context.getErrorModel();
        String formatDistMeanVal = " %s + %s";
        String formatDistVarVal = "%s 1 / ((pow(%s,2))*%s)";
        String formatErrTransf = "%s(%s)";

        if ((context.getTransformation()) != null && context.getTransformation().getListOfParameter().size() > 0) {
            BinaryTree bt = lexer.getTreeMaker().newInstance(context);
            errorTransformation = parse(context, bt);
        } else {
            errorTransformation = "";
        }

        List<String> lines = new ArrayList<>();
        BinaryTree bt = lexer.getTreeMaker().newInstance(error.getAssign());
        String errStr = "";
        if (bt != null) {
            errStr = parse(error.getAssign(), bt);
        }

        if (error.getAssign() != null && error.getAssign().getFunctionCall() != null) {
            FunctionCallType fc = error.getAssign().getFunctionCall();

            errStr = doFunctionCallNew(fc);
        }
        JAXBElement<? extends AbstractContinuousUnivariateDistributionType> distribution = getDistributionType(context);
        if (distribution != null) {
            String distName = getDistName(distribution);

            ContinuousValueType mean = ((NormalDistribution) (distribution.getValue())).getMean();
            String meanAssign = delimit("" + (mean.getVar() == null ? mean.getRVal() : mean.getVar().getVarId()));
            NormalDistribution value = ((NormalDistribution) (distribution.getValue()));
            String varianceAssign = getVarAssign(value);
            String rhsMeanExpression = delimit(String.format(formatDistMeanVal, doSymbolRef(output.getSymbRef()), meanAssign));
            String rhsVarExpression = String.format(formatDistVarVal, assignSymbol, errStr, varianceAssign);

            String current_symbol = getSymbol(context);
            String res_symbol = addSuffix(getSymbol(context), resSuffixLabel);
            resList.add(Util.clean(removeIndexes(res_symbol)));
            String meanTmpName = delimit((context).getSymbId() + meanSuffixLabel);
            errorTransformation = delimit(Util.clean(checkTransformation(errorTransformation)));
            String meanCompleteLabel = delimit(Util.clean(doMeanLabel(errorTransformation, rhsMeanExpression, meanTmpName)));
            String oldString = meanCompleteLabel;
            meanCompleteLabel = adjustIndexes(meanCompleteLabel, current_symbol);
            resList.add(Util.clean(removeIndexes(res_symbol)));
            String residual_value = String.format("%s-%s", delimit(current_symbol), meanCompleteLabel);
            if (!delimit(meanCompleteLabel).equals(oldString)) {
                putVariableMap(delimit(oldString), delimit(Util.clean(meanCompleteLabel)));
            }
            String meanAssignStmt = doMeanTransformation(errorTransformation, meanTmpName, rhsMeanExpression, formatErrTransf);
            lines.add(meanAssignStmt);
            current_symbol = doVarTransformation(errorTransformation, current_symbol, formatErrTransf, lines);
            String brackets = getIndexes(rhsVarExpression) == NO_INDEX ? "" : getBracketsGen(rhsVarExpression);
            String varTmpName = context.getSymbId() + varianceSuffixLabel;
            String formatDistVarAssignement = varTmpName + "%s %s";
            String varAssignStmt = String.format(formatDistVarAssignement, brackets, rhsVarExpression);
            String varCompleteLabel = delimit(Util.clean(varTmpName + brackets));
            lines.add(varAssignStmt);

            String statementDistFormat = "%s " + dist_symb + " %s(%s, %s)";
            String current_value = String.format(statementDistFormat, delimit(current_symbol), distName, meanCompleteLabel, varCompleteLabel);
            lines.add(current_value);
            String res_line = String.format("%s %s %s", res_symbol, assignSymbol, residual_value);
            lines.add(res_line);
        }
        return lines;
    }

    private List<String> doErrorModelProbOnto(StructuredObsError context) throws FileNotFoundException {
        StructuredObsError.Output output = context.getOutput();
        StructuredObsError.ErrorModel error = context.getErrorModel();

        String formatDistMeanVal = " %s + %s";
        String formatDistVarVal = "%s 1 / ((pow(%s,2))*%s)";
        String formatDistStdVal = "pow(%s,2)";
        String formatDistVarianceVal = "%s";
        String formatDistPrecVal = "1/(%s)";
        String formatErrTransf = "%s(%s)";

        if ((context.getTransformation()) != null && context.getTransformation().getListOfParameter().size() > 0) { // MOD 11 marzo
            BinaryTree bt = lexer.getTreeMaker().newInstance(context);
            errorTransformation = parse(context, bt);
        } else {
            errorTransformation = "";
        }

        List<String> lines = new ArrayList<>();
        BinaryTree bt = lexer.getTreeMaker().newInstance(error.getAssign());
        String errStr = "";
        if (bt != null) {
            errStr = parse(error.getAssign(), bt);
        }

        if (error.getAssign() != null && error.getAssign().getFunctionCall() != null) {
            FunctionCallType fc = error.getAssign().getFunctionCall();
            errStr = doFunctionCallNew(fc);
        }

        ProbOnto distribution = getProbOntoDistributionType(context);
        if (distribution != null) {
            String distName = getDistName(distribution);
            String brackets;
            String varTmpName;
            String formatDistVarAssignement;
            String varAssignStmt;
            String statementDistFormat;
            String current_value;
            String res_line;

            switch (distribution.getName().value()) {
                case "Normal1":
                case "Normal2":
                case "Normal3":
                    DistributionParameter mean = distribution.getParameter(ParameterName.MEAN);
                    DistributionParameter var;
                    String varianceAssign = "";
                    String meanAssign = delimit(parse(mean, lexer.getTreeMaker().newInstance(mean)));
                    if (distribution.getName().value().toString().equals("Normal1")) {
                        var = distribution.getParameter(ParameterName.STDEV);
                        varianceAssign = String.format(formatDistStdVal, delimit(parse(var, lexer.getTreeMaker().newInstance(var))));
                    } else if (distribution.getName().value().toString().equals("Normal2")) {
                        var = distribution.getParameter(ParameterName.VAR);
                        varianceAssign = String.format(formatDistVarianceVal, delimit(parse(var, lexer.getTreeMaker().newInstance(var))));
                    } else if (distribution.getName().value().toString().equals("Normal3")) {
                        var = distribution.getParameter(ParameterName.PRECISION);
                        varianceAssign = String.format(formatDistPrecVal, delimit(parse(var, lexer.getTreeMaker().newInstance(var))));
                    }
                    String rhsMeanExpression = delimit(String.format(formatDistMeanVal, doSymbolRef(output.getSymbRef()), meanAssign));
                    String rhsVarExpression = String.format(formatDistVarVal, assignSymbol, errStr, varianceAssign);

                    String current_symbol = getSymbol(context);
                    String res_symbol = addSuffix(getSymbol(context), resSuffixLabel);
                    resList.add(Util.clean(removeIndexes(res_symbol)));
                    String meanTmpName = delimit((context).getSymbId() + meanSuffixLabel);

                    errorTransformation = delimit(Util.clean(checkTransformation(errorTransformation)));
                    String meanCompleteLabel = delimit(Util.clean(doMeanLabel(errorTransformation, rhsMeanExpression, meanTmpName)));
                    String oldString = meanCompleteLabel;
                    meanCompleteLabel = delimit(adjustIndexes(Util.clean(meanCompleteLabel), current_symbol));
                    predList.add(Util.clean(removeIndexes(meanCompleteLabel)));
                    String residual_value = String.format("%s-%s", delimit(current_symbol), meanCompleteLabel);
                    if (!delimit(meanCompleteLabel).equals(oldString)) {
                        putVariableMap(delimit(oldString), delimit(Util.clean(meanCompleteLabel)));
                    }
                    String meanAssignStmt = doMeanTransformation(errorTransformation, meanTmpName, rhsMeanExpression, formatErrTransf);
                    lines.add(meanAssignStmt);
                    current_symbol = doVarTransformation(errorTransformation, current_symbol, formatErrTransf, lines);
                    brackets = getIndexes(rhsVarExpression) == NO_INDEX ? "" : getBracketsGen(rhsVarExpression);
                    varTmpName = context.getSymbId() + varianceSuffixLabel;
                    formatDistVarAssignement = varTmpName + "%s %s";
                    varAssignStmt = String.format(formatDistVarAssignement, brackets, rhsVarExpression);
                    String varCompleteLabel = delimit(Util.clean(varTmpName + brackets));
                    lines.add(varAssignStmt);
                    statementDistFormat = "%s " + dist_symb + " %s(%s, %s)";
                    current_value = String.format(statementDistFormat, delimit(current_symbol), distName, meanCompleteLabel, varCompleteLabel);
                    lines.add(current_value);
                    res_line = String.format("%s %s %s", res_symbol, assignSymbol, residual_value);
                    lines.add(res_line);
                    break;

                default:
                    DistributionParameter par1 = distribution.getParameter(ParameterName.MEAN);
                    String par1Assign = delimit(parse(par1, lexer.getTreeMaker().newInstance(par1)));
                    String par2Assign = delimit(parse(par1, lexer.getTreeMaker().newInstance(distribution.getParameter(ParameterName.VAR))));
                    String rhsPar1Expression = delimit(String.format(formatDistMeanVal, doSymbolRef(output.getSymbRef()), par1Assign));
                    String rhsPar2Expression = String.format(formatDistVarVal, assignSymbol, errStr, par2Assign);

                    String curr_sym = getSymbol(context);
                    String res_sym = addSuffix(getSymbol(context), resSuffixLabel);
                    resList.add(Util.clean(removeIndexes(res_sym)));

                    String par1TmpName = delimit((context).getSymbId() + meanSuffixLabel);

                    errorTransformation = delimit(Util.clean(checkTransformation(errorTransformation)));
                    String par1CompleteLabel = delimit(Util.clean(doPar1Label(errorTransformation, rhsPar1Expression, par1TmpName)));
                    String oldStr = par1CompleteLabel;
                    par1CompleteLabel = adjustIndexes(par1CompleteLabel, curr_sym);
                    predList.add(Util.clean(removeIndexes(par1CompleteLabel)));
                    String res_value = String.format("%s-%s", delimit(curr_sym), par1CompleteLabel);
                    if (!delimit(par1CompleteLabel).equals(oldStr)) {
                        putVariableMap(delimit(oldStr), delimit(Util.clean(par1CompleteLabel)));
                    }
                    String par1AssignStmt = doMeanTransformation(errorTransformation, par1TmpName, rhsPar1Expression, formatErrTransf);
                    lines.add(par1AssignStmt);
                    curr_sym = doVarTransformation(errorTransformation, curr_sym, formatErrTransf, lines);
                    brackets = getIndexes(rhsPar2Expression) == NO_INDEX ? "" : getBracketsGen(rhsPar2Expression);
                    varTmpName = context.getSymbId() + varianceSuffixLabel;
                    formatDistVarAssignement = varTmpName + "%s %s";
                    varAssignStmt = String.format(formatDistVarAssignement, brackets, rhsPar2Expression);
                    String par2CompleteLabel = delimit(Util.clean(varTmpName + brackets));
                    lines.add(varAssignStmt);
                    statementDistFormat = "%s " + dist_symb + " %s(%s, %s)";
                    current_value = String.format(statementDistFormat, delimit(curr_sym), distName, par1CompleteLabel, par2CompleteLabel);
                    lines.add(current_value);
                    res_line = String.format("%s %s %s", res_sym, assignSymbol, res_value);
                    lines.add(res_line);
                    break;
            }
        }
        return lines;
    }

    private List<String> writeObsStandardModelDistribution(StructuredObsError context) throws FileNotFoundException {
        List<String> lines = new ArrayList<>();
        StructuredObsError.ErrorModel error = context.getErrorModel();
        if (error != null) {
            return selectErrorModel(context);
        }
        return lines;
    }

    private void writeFunctions(PrintWriter fout, List<FunctionDefinition> functions) throws IOException {
        if (fout == null) {
            return;
        }
        if (functions == null) {
            throw new NullPointerException("The Function list is NULL.");
        }
        if (functions.size() == 0) {
            return;
        }
        for (FunctionDefinition f : functions) {
            if (f == null) {
                continue;
            }
            if (functionMap.get(f) == null) {
                throw new IllegalStateException("Function " + f.getSymbId() + " declaration has no binary tree.");
            }
            if (f.getDescription() != null) {
                AnnotationType annotation = f.getDescription();
                String value = annotation.getValue();
                if (value != null) {
                    String format = "%s %s";
                    fout.write(Util.clean(String.format(format, comment_char, value)));
                }
            }
            String format = "function [value] = %s(";
            fout.write(Util.clean(String.format(format, z.get(f))));
            int i = 0;
            List<FunctionParameter> args = f.getFunctionArgument();
            for (FunctionParameter arg : args) {
                if (i > 0) {
                    fout.write(", ");
                }
                fout.write(Util.clean(z.get(arg)));
                i++;
            }
            fout.write(")\n");
            fout.write("value = ");
            parse(f, functionMap.get(f), fout);
            fout.write(";\nend\n\n");
        }
    }

    private void checkMap(String before, String after) {
        if (!delimit(after).equals(delimit(before))) {
            putVariableMap(delimit(before), delimit(after));
        }
    }

    protected String doInverseTransformation(String transf, String arg) {
        String t;
        arg = pascalNamesTransform(arg);
        switch (transf) {
            case "logit":
                t = String.format("Math.Exp(%s)/(1+Math.Exp(%s))", arg, arg);
                break;
            case "probit":
                t = String.format("%s(%s)", "MathFunc.Phi", arg);
                break;
            case "log":
                t = String.format("%s(%s)", "Math.Exp", arg);
                break;
            default:
                throw new UnsupportedOperationException(transf);
        }
        return t;
    }

    private String manageGC(IndividualParameter ip) {
        String format = "%s " + assignSymbol + " %s";
        String formatTransf = "%s(%s)" + assignSymbol + " %s";
        String indivEquation, randEffId, indivPascalEquation;
        String indivTransformation = null, oldString;
        StructuredModel ipGM = ip.getStructuredModel();
        SymbolRef randEff = ipGM.getListOfRandomEffects().get(0).getSymbRef().get(0);
        StructuredModel.GeneralCovariate ipGC = ipGM.getGeneralCovariate();
        if (ipGM.getTransformation() != null) {
            indivTransformation = ipGM.getTransformation().getType().toString().toLowerCase();
            if (indivTransformation.equals("identity")) {
                indivTransformation = null;
            }
        }
        indivEquation = parse(ipGM, lexer.getStatement(ipGC));
        indivEquation = delimit(indivEquation);
        randEffId = doSymbolRef(randEff);
        indivEquation += " + " + randEffId;
        oldString = ip.getSymbId();
        randEffId = adjustIndexes(ip.getSymbId(), indivEquation);
        checkMap(ip.getSymbId(), randEffId);
        indivPascalEquation = pascalNamesTransform(indivEquation);
        if (indivTransformation == null) {
            pascalIndivMap.put(delimit(randEffId), indivPascalEquation);
            unaryOperandEqMap.put(delimit(Util.clean(randEffId)), indivEquation);
            return String.format(format, delimit(randEffId), indivEquation);
        } else {
            pascalIndivMap.put(delimit(randEffId), indivPascalEquation);
            unaryOperandEqMap.put(delimit(Util.clean(randEffId)), indivEquation);
            transfMap.put(delimit(String.format("%s(%s)", indivTransformation, randEffId)), delimit(Util.clean(randEffId)));
            return String.format(formatTransf, indivTransformation, randEffId, indivEquation);
        }
    }

    private String getCategoryVal(List<CategoryRef_> list, String cat) {
        for (CategoryRef_ s : list) {
            if (s.getModelSymbol().equals(cat)) {
                return s.getDataSymbol();
            }
        }
        return null;

    }

    private String manageLC(IndividualParameter ip) throws IOException {
        String format = "%s " + assignSymbol + " %s";
        String formatTransf = "%s(%s)" + assignSymbol + " %s";
        String indivWinbugsEquation, indivPascalEquation, randEffId, popPar, prod, prodpascal;
        String indivTransformation = null, pascalPopPar = "";
        SymbolRef fixedEff, randEff, linCov;
        StructuredModel ipGM = ip.getStructuredModel();

        randEff = ipGM.getListOfRandomEffects().get(0).getSymbRef().get(0); 
        StructuredModel.LinearCovariate ipLC = ipGM.getLinearCovariate();
        ipLC = ipGM.getLinearCovariate();
        if (ipGM.getTransformation() != null) {
            indivTransformation = ipGM.getTransformation().getType().toString().toLowerCase();
            if (indivTransformation.equals("identity")) {
                indivTransformation = null;
            } else if (indivTransformation.equals("probit")) {
                throw new UnsupportedOperationException("Probit transformation is not allowed at the right end side.");
            }

        }
        popPar = parse(ipLC, lexer.getStatement(ipLC.getPopulationValue()));
        randEffId = delimit(doSymbolRef(randEff));
        if (indivTransformation != null) {
            pascalPopPar = doPascalTransformation(indivTransformation, popPar);
            popPar = indivTransformation + "(" + delimit(popPar) + ")";
        } else {
            pascalPopPar = popPar;
        }
        prod = "";
        String left = "";
        String pieceName;
        String symbol = "";
        String oldSymbol;
        if (ipLC.getListOfCovariate() != null && ipLC.getListOfCovariate().size() > 0) {
            for (CovariateRelation cov : ipLC.getListOfCovariate()) {
                fixedEff = cov.getListOfFixedEffect().get(0).getSymbRef(); 
                linCov = cov.getSymbRef();
                pieceName = piecePrefix + linCov.getSymbIdRef();
                if (isCategoricalCovariateType(linCov)) {
                    covCatMap.put(linCov.getSymbIdRef(), pieceName);

                    int num = categoricalCovariateIdMap.get(lexer.getAccessor().fetchElement(linCov));
                    String id = getCategoryVal(categoricalMap.get(linCov.getSymbIdRef()), cov.getListOfFixedEffect().get(0).getCategory().getCatId());
                    covFilesMap.put(num, ((PascalParser) this).pascalCatCovCodeFileGeneration(num, id, linCov.getSymbIdRef()));
                    covBlockMap.put(linCov.getSymbIdRef(), ((PascalParser) this).pascalCatCovCodeBlockGeneration(linCov.getSymbIdRef(), id, pieceName));
                    String tmp = adjustStatement(String.format("%s %s function.covariate%s(%s)", covCatMap.get(linCov.getSymbIdRef()) 
                            , assignSymbol, num, doSymbolRef(linCov)));
                    winbugsCovariateLines.add(tmp);
                    left = tmp.substring(0, tmp.indexOf(assignSymbol));
                } else {
                    oldSymbol = linCov.getSymbIdRef();
                    symbol = oldSymbol + interpSuffix;
                    symbol += IND_BOTH;
                    variablesAssignementMap.put(delimit(oldSymbol), delimit(symbol));
                    doInterpLine(oldSymbol, symbol);
                    left = linCov.getSymbIdRef();
                }
                prod += " + " + delimit(doSymbolRef(fixedEff)) + " * " + delimit(left);
            }
            prodpascal = pascalNamesTransform(prod); 
            indivWinbugsEquation = popPar + prod + " + " + randEffId;
            indivPascalEquation = pascalPopPar + prodpascal + " + " + randEffId;
        } else {
            indivWinbugsEquation = popPar + " + " + randEffId;
            indivPascalEquation = pascalPopPar + " + " + randEffId;
        }
        indivPascalEquation = pascalNamesTransform(indivPascalEquation);
        String oldSymb
                = variablesAssignementMap.get(delimit(ip.getSymbId())) != null
                        ? variablesAssignementMap.get(delimit(ip.getSymbId()))
                        : ip.getSymbId();
        randEffId = delimit(adjustIndexes(oldSymb, indivWinbugsEquation));
        checkMap(ip.getSymbId(), randEffId);

        if (indivTransformation == null || indivTransformation.equals("identity")) {
            pascalIndivMap.put(delimit(randEffId), indivPascalEquation);
            return String.format(format, randEffId, indivWinbugsEquation);
        } else {
            unaryOperandEqMap.put(delimit(Util.clean(randEffId)), indivWinbugsEquation);
            transfMap.put(delimit(String.format("%s(%s)", indivTransformation, randEffId)), delimit(Util.clean(randEffId)));
            pascalIndivMap.put(delimit(randEffId), indivPascalEquation);
            return String.format(formatTransf, indivTransformation, randEffId, indivWinbugsEquation);
        }
    }

    private String managePV(IndividualParameter ip) {
        String format = "%s " + assignSymbol + " %s";
        String formatTransf = "%s(%s)" + assignSymbol + " %s";
        String indivWinbugsEquation, indivPascalEquation, randEffId, popPar, prod, prodpascal;
        String fixedEff, indivTransformation = null, pascalPopPar = "";
        SymbolRef randEff;
        StructuredModel ipGM = ip.getStructuredModel();

        randEff = ipGM.getListOfRandomEffects().get(0).getSymbRef().get(0);
        StructuredModel.PopulationValue ipPV = ipGM.getPopulationValue();
        if (ipGM.getTransformation() != null) {
            indivTransformation = ipGM.getTransformation().getType().toString().toLowerCase();
            if (indivTransformation.equals("identity")) {
                indivTransformation = null;
            } else if (indivTransformation.equals("probit")) {
                throw new UnsupportedOperationException("Probit transformation is not allowed at the right end side.");
            }

        }
        popPar = parse(ipGM, lexer.getTreeMaker().newInstance(ipPV.getAssign()));
        randEffId = doSymbolRef(randEff);
        if (indivTransformation != null) {
            pascalPopPar = doPascalTransformation(indivTransformation, popPar);
            popPar = indivTransformation + "(" + popPar + ")";
        } else {
            pascalPopPar = popPar;
        }
        if (ipPV.getAssign() != null) {
            fixedEff = parse(ipPV, lexer.getTreeMaker().newInstance(ipPV.getAssign()));
            prod = "" + fixedEff;
            prodpascal = pascalNamesTransform(prod); 
            indivWinbugsEquation = popPar + " + " + randEffId;
            indivPascalEquation = pascalPopPar + " + " + randEffId;
        } else {
            indivWinbugsEquation = popPar + " + " + randEffId;
            indivPascalEquation = pascalPopPar + " + " + randEffId;
        }
        indivPascalEquation = pascalNamesTransform(indivPascalEquation);
        randEffId = adjustIndexes(ip.getSymbId(), indivWinbugsEquation);
        checkMap(ip.getSymbId(), randEffId);
        if (indivTransformation == null || indivTransformation.equals("identity")) {
            pascalIndivMap.put(delimit(randEffId), indivPascalEquation);
            return String.format(format, randEffId, indivWinbugsEquation);
        } else {
            unaryOperandEqMap.put(delimit(Util.clean(randEffId)), indivWinbugsEquation);
            transfMap.put(delimit(String.format("%s(%s)", indivTransformation, randEffId)), delimit(Util.clean(randEffId)));
            pascalIndivMap.put(delimit(randEffId), indivPascalEquation);
            return String.format(formatTransf, indivTransformation, randEffId, indivWinbugsEquation);
        }
    }

    private String manageGM(IndividualParameter ip) throws IOException {
        StructuredModel ipGM = ip.getStructuredModel();
        if (ipGM.getGeneralCovariate() != null) {
            winbugsIndivLines.add(manageGC(ip));
        } else if (ipGM.getLinearCovariate() != null) {
            winbugsIndivLines.add(manageLC(ip));
        } else if (ipGM.getPopulationValue() != null) {
            winbugsIndivLines.add(managePV(ip));
        }
        return "";
    }

    public void setCovariatePascalName(String name) {
        this.covariatePascalName = name;
    }

    public String getCovariatePascalName() {
        return covariatePascalName;
    }

    protected void manageParameterBlocks(ParameterBlock pb) throws UnsupportedDataTypeException, FileNotFoundException, IOException {
        List<IndividualParameter> pbs = pb.getIndividualParameters();
        for (IndividualParameter ip : pbs) {
            if (!lexer.hasStatement(ip)) {
                continue;
            }
            if (ip.getAssign() != null) {
                winbugsIndivLines.add(parse(ip, lexer.getStatement(ip.getAssign())));
            } else if ((ip.getStructuredModel()) != null) {
                manageGM(ip);
            }
        }
        for (Map.Entry<Integer, String> el : covFilesMap.entrySet()) {
            output(el.getValue(), new File(this.covariatePascalName + el.getKey() + pascalFileNameSuffix), true);
        }
    }

    protected String doPascalTransformation(String u_op, String leftStatement) {
        String pascalUnaryOperator;
        switch (u_op) {
            case "abs":
                pascalUnaryOperator = String.format("ABS(%s)", leftStatement);
                break;
            case "log":
                pascalUnaryOperator = String.format("Math.Ln(%s)", leftStatement);
                break;
            case "logit":
                pascalUnaryOperator = String.format("Math.Ln(%s)-Math.Ln(1-%s)", leftStatement, leftStatement);
                break;
            case "sqrt":
                pascalUnaryOperator = String.format("Math.Sqrt(%s)", leftStatement);
                break;
            case "exp":
                pascalUnaryOperator = String.format("Math.Exp(%s)", leftStatement);
                break;
            case "factln":
                pascalUnaryOperator = String.format("MathFunc.LogFactorial(%s)", leftStatement);
                break;
            case "floor":
                pascalUnaryOperator = String.format("Math.Floor(%s)", leftStatement);
                break;
            case "gammaln":
                pascalUnaryOperator = String.format("MathFunc.LogGammaFunc(%s))", leftStatement);
                break;
            case "minus":
                pascalUnaryOperator = String.format("-(%s)", leftStatement);
                break;
            case "normcdf":
                pascalUnaryOperator = String.format("MathFunc.Phi(%s)", leftStatement);
                break;
            case "factorial":
                pascalUnaryOperator = String.format("Math.Exp(MathFunc.LogFactorial(%s))", leftStatement);
                break;
            case "ceiling":
                pascalUnaryOperator = String.format("Math.Ceiling(%s)", leftStatement);
                break;
            case "logistic":
                pascalUnaryOperator = String.format("1/(1+Math.Exp(-%s))", leftStatement);
                break;
            case "sim":
                pascalUnaryOperator = String.format("Math.Sin(%s)", leftStatement);
                break;
            case "cos":
                pascalUnaryOperator = String.format("Math.Cos(%s)", leftStatement);
                break;
            case "tan":
                pascalUnaryOperator = String.format("Math.Tan(%s)", leftStatement);
                break;
            case "sec":
                pascalUnaryOperator = String.format("1/Math.Cos(%s)", leftStatement);
                break;
            case "csc":
                pascalUnaryOperator = String.format("1/Math.Sin(%s)", leftStatement);
                break;
            case "cot":
                pascalUnaryOperator = String.format("Math.Cos(%s)/Math.Sin(%s)", leftStatement, leftStatement);
                break;
            case "sinh":
                pascalUnaryOperator = String.format("(Math.Sinh(%s)", leftStatement);
                break;
            case "cosh":
                pascalUnaryOperator = String.format("(Math.Cosh(%s)", leftStatement);
                break;
            case "tanh":
                pascalUnaryOperator = String.format("Math.Tanh(%s)", leftStatement);
                break;
            case "coth":
                pascalUnaryOperator = String.format("(Math.Exp(%s)+Math.Exp(-%s))/(Math.Exp(%s)-Math.Exp(-%s))", leftStatement, leftStatement, leftStatement, leftStatement);
                break;
            case "sech":
                pascalUnaryOperator = String.format("2/(Math.Exp(%s)+Math.Exp(-%s))", leftStatement, leftStatement);
                break;
            case "csch":
                pascalUnaryOperator = String.format("2/(Math.Exp(%s)-Math.Exp(-%s))", leftStatement, leftStatement);
                break;
            case "arcsinh":
                pascalUnaryOperator = String.format("Math.ArcSinh(%s)", leftStatement);
                break;
            case "arccosh":
                pascalUnaryOperator = String.format("Math.ArcCosh(%s)", leftStatement);
                break;
            case "arctanh":
                pascalUnaryOperator = String.format("Math.ArcTanh(%s)", leftStatement);
                break;
            case "arccoth":
                pascalUnaryOperator = String.format("1/2*Math.Ln((%s+1)/(%s-1))", leftStatement, leftStatement);
            default:
                throw new UnsupportedOperationException(u_op);
        }
        return pascalUnaryOperator;
    }

    protected String doPascalTransformation(String b_op, String leftStatement, String rightStatement) {
        String pascalBinaryOperator;

        if (b_op == null) {
            throw new NullPointerException("The binary operator is NULL.");
        }
        if (b_op.equals(BaseEngine.POWER)) {
            pascalBinaryOperator = String.format("MathFunc.Power(%s,%s)", leftStatement, rightStatement);
        } else if (b_op.equals(BaseEngine.ROOT)) {
            pascalBinaryOperator = String.format("MathFunc.Power(%s,1/%s)", leftStatement, rightStatement);
        } else if (b_op.equals(BaseEngine.LOGX)) {
            pascalBinaryOperator = String.format("Math.Ln(%s)/Math.Ln(%s)", leftStatement, rightStatement);
        } else if (b_op.equals(BaseEngine.MAX)) {
            pascalBinaryOperator = String.format("MAX(%s,%s)", leftStatement, rightStatement);
        } else if (b_op.equals(BaseEngine.MIN)) {
            pascalBinaryOperator = String.format("min(%s,%s)", leftStatement, rightStatement);
        } else {
            throw new UnsupportedOperationException(b_op);
        }
        return pascalBinaryOperator;
    }

    private int getIndexes(String value) {
        if (!value.contains(leftArrayBracket) && !value.contains(IND_T2)) {
            return NO_INDEX;
        } else if (value.contains(IND_T2)) {
            if (value.contains(IND_S)) {
                return INDIV_INDEX;
            } else {
                return INDQ_INDEX;
            }
        } else if (value.contains(IND_T)) {
            if (value.contains(IND_S)) {
                return BOTH_INDEX;
            } else {
                return TIME_INDEX;
            }
        } else if (value.contains(IND_S)) {
            return INDIV_INDEX;
        } else {
            return NO_INDEX;
        }
    }

    private String getBrackets(String value) {
        int i1 = value.indexOf(leftArrayBracket);
        int i2 = value.indexOf(rightArrayBracket, i1);
        return value.substring(i1, i2 + 1);
    }

    private String getBracketsGen(String value) {
        StringBuilder s = new StringBuilder();
        for (String t : listIndexes) {
            if (value.contains(t)) {
                s.append(t);
                s.append(',');
            }
        }
        String ind = s.toString();
        return leftArrayBracket + ind.substring(0, ind.length() - 1) + rightArrayBracket;
    }

    protected String doSequence(Sequence o) {
        throw new UnsupportedOperationException();
    }

    protected String doConstant(Constant c) {
        throw new UnsupportedOperationException();
    }

    protected String doVector(Vector v) {

        return v.getId();
    }

    protected String doFunctionCall(FunctionCallType call) {
        if (call == null) {
            throw new NullPointerException("A function call definition is null.");
        }

        ArrayList<String> arg_symbols = new ArrayList<String>();
        for (FunctionCallType.FunctionArgument arg : call.getFunctionArgument()) {
            String arg_symbol = "0.0";
            if (arg.getScalar() != null) {
                Object v = arg.getScalar().getValue();
                arg_symbol = getSymbol(v);
            } else if (arg.getSymbRef() != null) {
                arg_symbol = getSymbol(arg.getSymbRef());
            } else if (arg != null) {
                arg_symbol = getSymbol(arg);
            }
            arg_symbols.add(arg_symbol);
        }

        StringBuffer args_string = new StringBuffer();
        int i = 0;
        for (String symbol : arg_symbols) {
            if (i > 0) {
                args_string.append(",");
            }
            args_string.append(symbol);
            i++;
        }

        String format = "(%s(%s))";

        return String.format(format, call.getSymbRef().getSymbIdRef(), args_string);
    }

    protected String doIndependentVariable(IndependentVariable v) {
        return gridLabel;
    }

    protected String doFunctionCallNew(FunctionCallType call) {

        if (call == null) {
            throw new NullPointerException("A function call definition is null.");
        }
        FunctionDefinition func = getFunctionByName(call.getSymbRef().getSymbIdRef());

        List<String> formalList = new ArrayList<>();
        List<String> actualIds = new ArrayList<>();
        doFunctionCall(call, formalList, actualIds);
        updateBinopMap(formalList, actualIds);
        return doFunctionDefinition(func, formalList, actualIds);
    }

    String doFunctionDefinition(FunctionDefinition func, List<String> formalList, List<String> actualIds) {
        String funcS = getFunction(func);
        String definition = funcS.substring(funcS.indexOf("=") + 1).trim();
        for (int i = 0; i < formalList.size(); i++) {
            definition = definition.replace(formalList.get(i), actualIds.get(i)).trim();
        }
        return definition;
    }

    void doFunctionCall(FunctionCallType call, List<String> formalList, List<String> actualIds) {
        String actualArg = "";
        for (FunctionCallType.FunctionArgument arg : call.getListOfFunctionArgument()) {
            formalList.add(delimit(arg.getSymbId()));
            actualArg = getArg(arg);
            actualIds.add(delimit(actualArg));
        }
    }

    private String getArg(JAXBElement arg) {
        if (arg.getValue() instanceof SymbolRef) {
            return delimit(doSymbolRef(((SymbolRef) arg.getValue())));
        } else if (arg.getValue() instanceof Scalar) {
            return (((Scalar) arg.getValue()).asString());
        } else if (arg.getValue() instanceof Uniop) {
            Uniop ut = (Uniop) arg.getValue();
            return delimit(doUnaryOperation(ut, getArg(ut)));
        } else if (arg.getValue() instanceof Binop) {
            return getArg((Binop) arg.getValue());
        } else if (arg.getValue() instanceof FunctionCallType) {
            return doFunctionCallNew((FunctionCallType) arg.getValue());
        }
        return "@";
    }

    private String getArg(Binop arg) {
        String s0 = delimit(getArg(arg.getOperand1().toJAXBElement()));
        String s1 = delimit(getArg(arg.getOperand2().toJAXBElement()));
        return delimit(doBinaryOperation(arg, s0, s1));
    }

    private String getArg(Uniop arg) {
        return getArg(((Uniop) arg.getValue()).toJAXBElement());
    }

    private String getArg(FunctionCallType.FunctionArgument arg) {
        if (arg.getSymbRef() != null) {
            return delimit(doSymbolRef(arg.getSymbRef()));
        } else if (arg.getScalar() != null) {
            if (arg.getScalar().getValue() instanceof RealValue) {
                return delimit(((RealValue) arg.getScalar().getValue()).getValue() + "");
            } else if (arg.getScalar().getValue() instanceof IntValue) {
                return delimit(((IntValue) arg.getScalar().getValue()).getValue() + "");
            }
        } else if (arg != null) {
            return getArg(arg);
        }
        return "@";
    }

    protected String doCovariate(CovariateDefinition cov) {
        String symbol = unassigned_symbol;

        if (cov.getCategorical() != null) {
            throw new UnsupportedOperationException("CovariateDefinition::categorical not supported yet.");
        } else if (cov.getContinuous() != null) {
            if (!cov.getContinuous().getListOfTransformation().isEmpty()) { 
                symbol = covariateTransformationMap.get(cov);
            } else {
                TreeMaker tm = lexer.getTreeMaker();
                symbol = parse(cov, tm.newInstance(cov));
            }
        }

        return symbol;
    }

    private String doContinuousCovariate(ContinuousCovariate continuous) {
        return continuous.getId();
    }

    private String doCovariateTransformationNew(CovariateTransformation cov) {
        String symbol = unassigned_symbol;

        if (transformation_stmt_map.containsKey(cov)) {
            symbol = transformation_stmt_map.get(cov);
        } else {
            symbol = cov.getTransformedCovariate().getSymbId();
            transformation_stmt_map.put(cov, symbol);
        }
        return symbol;
    }

    public void writeFunction(FunctionDefinition func) {
        BinaryTree bt = lexer.getStatement(func);
        functionMap.put(func, bt);
        parse(func, bt);
    }

    private String getFunctionParametersString(FunctionDefinition func) {
        List<FunctionParameter> params = func.getFunctionArgument();
        StringBuilder sb = new StringBuilder();
        for (FunctionParameter p : params) {
            sb.append(p.getSymbId());
            sb.append(',');
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    private List<String> getFunctionParametersId(FunctionDefinition func) {
        List<FunctionParameter> params = func.getFunctionArgument();
        List<String> idList = new ArrayList<>();
        for (FunctionParameter p : params) {
            idList.add(p.getSymbId());
        }
        return idList;
    }

    public String getFunction(FunctionDefinition func) {
        List<FunctionParameter> params = func.getFunctionArgument();
        StringBuilder sb = new StringBuilder();
        for (FunctionParameter p : params) {
            sb.append(delimit(p.getSymbId()));
            sb.append(',');
        }
        String parList = sb.toString().substring(0, sb.toString().length() - 1);
        String formatF = "%s(" + parList + ") = %s";
        return String.format(formatF, func.getSymbId(), functionDefinition.get(func));

    }

    /**
     *
     * @param func
     * @param output_dir
     * @param map
     * @throws IOException
     */
    protected void writeFunction(FunctionDefinition func, String output_dir, boolean t) throws IOException {
        if (func == null) {
            return;
        }

        String format = "%s%s%s.m";
        String output_filename = String.format(format, output_dir, File.separator, z.get(func));

        ArrayList<FunctionDefinition> list = new ArrayList<FunctionDefinition>();
        list.add(func);
        PrintWriter fout = new PrintWriter(output_filename);
        writeScriptHeader(fout, lexer.getModelFilename());
        writeFunctions(fout, list);
        fout.close();
    }

    public void writeInterpreterPath(PrintWriter fout) {
        throw new UnsupportedOperationException();
    }

    public void writeADMESimulationBlock(PrintWriter fout, File output_dir, SimulationStep ss) throws IOException {
        return; 
    }

    public void writeParameters(PrintWriter fout) {
        throw new UnsupportedOperationException();
    }

    public void writeScriptLibraryReferences(PrintWriter fout) throws IOException {

        if (fout == null) {
            return;
        }
    }

    private List<String> writeRandomVarDistribution(ParameterRandomVariable context) { 
        List<String> lines = new ArrayList<>();
        String formatDistMeanValAssign = assignSymbol + " %s";
        String formatDistVarValAssign = assignSymbol + " (1/(%s))";
        String formatDistMeanVal = "%s";
        Distribution distrib = context.getDistribution();
        BinaryTree tmpstmt = lexer.getStatement(distrib);

        UncertML uDist = distrib.getUncertML();
        if (uDist != null && uDist.getAbstractContinuousUnivariateDistribution() != null) {
            if (uDist.getAbstractContinuousUnivariateDistribution().getValue() != null) {
                Object oDist = uDist.getAbstractContinuousUnivariateDistribution().getValue();
                if (oDist instanceof NormalDistribution) {
                    NormalDistribution value = (NormalDistribution) oDist;
                    String distName = getDistName(value);
                    ContinuousValueType mean = value.getMean();
                    SymbolRef srM = new SymbolRef();
                    if (mean.getVar() != null) {
                        srM.setSymbIdRef(mean.getVar().getVarId());
                    }
                    String meanAssign = delimit("" + (mean.getVar() == null ? mean.getRVal() : getSymbol(srM)));
                    String varianceAssign = "";
                    varianceAssign = delimit(getVarAssign(value));
                    String rhsVarExpression = String.format(formatDistVarValAssign, varianceAssign);
                    String rhsMeanExpression = String.format(formatDistMeanValAssign, meanAssign);
                    String meanExpression = delimit(String.format(formatDistMeanVal, meanAssign));
                    String brackets = getIndexes(rhsMeanExpression) == NO_INDEX ? "" : getBrackets(rhsMeanExpression);
                    brackets = getIndexes(rhsVarExpression) == NO_INDEX ? "" : getBracketsGen(rhsVarExpression);
                    String varTmpName = (context).getSymbId() + varianceSuffixLabel;
                    String formatDistVarAssignement = varTmpName + "%s %s";
                    String varAssignStmt = String.format(formatDistVarAssignement, brackets, rhsVarExpression);
                    String varCompleteLabel = delimit(Util.clean(varTmpName + brackets));
                    lines.add(varAssignStmt);
                    String current_symbol = getSymbol(context);
                    String statementDistFormat = "%s " + dist_symb + " %s(%s, %s)";
                    String current_value = String.format(statementDistFormat, delimit(current_symbol), distName, meanExpression, varCompleteLabel);
                    lines.add(current_value);
                }
            } else {
                throw new RuntimeException("Distribution not supported");
            }
            return lines;
        }
        throw new RuntimeException("Distribution not supported");
    }

    private List<String> doProbOntoDistributionPrior(PharmMLRootType el) { 
        List<String> lines = new ArrayList<>();
        String name = "";
        List<String> ret = new ArrayList<>();
        if (el instanceof PopulationParameter) {
            PopulationParameter context = (PopulationParameter) el;

            ProbOnto pb = context.getDistribution().getProbOnto();
            name = pb.getName().value();
        } else if (el instanceof ParameterRandomVariable) {
            ParameterRandomVariable context = (ParameterRandomVariable) el;

            ProbOnto pb = context.getDistribution().getProbOnto();
            name = pb.getName().value();
        }
        switch (name) {
            case "Normal1":
            case "Normal2":
            case "Normal3":
                ret = doNormalPriors(el);
                break;
            case "Gamma1":
            case "Gamma2":
                ret = doGammaPriors(el);
                break;
            case "Exponential1":
            case "Exponential2":
                ret = doExponentialPriors(el);
                break;
            case "LogNormal1":
            case "LogNormal2":
            case "LogNormal3":
            case "LogNormal4":
            case "LogNormal5":
            case "LogNormal6":
                ret = doLogNormalPriors(el);
                break;
            case "Beta1":
                ret = doBetaPriors(el);
                break;
            case "StudentT1":
            case "StudentT2":
                ret = doStudentPriors(el);
                break;
            case "Uniform1":
                ret = doUniformPriors(el);
                break;
            case "Weibull1":
            case "Weibull2":
                ret = doWeibullPriors(el);
                break;
            case "InverseGamma1":
                ret = doInverseGammaPriors(el);
                break;
            case "Wishart1":
            case "Wishart2":
                ret = doWishartPriors(el);
                break;
            case "InverseWishart1":
            case "InverseWishart2":
                ret = doInverseWishartPriors(el);
                break;
            case "MultivariateNormal1":
            case "MultivariateNormal2":
                ret = doMultivariateNormalPriors(el);
                break;
            case "MultivariateStudentT1":
            case "MultivariateStudentT2":
                ret = doMultivariateStudentTPriors(el);
                break;
            default:
                throw new UnsupportedOperationException("ProbOnto distribution " + name + " not supported");

        }
        lines.addAll(ret);
        return lines;

    }

    private List<String> writeRandomVarProbOntoDistribution(ParameterRandomVariable context) { 
        List<String> lines = new ArrayList<>();
        ProbOnto pb = context.getDistribution().getProbOnto();
        String name = pb.getName().value();
        String ret = null;
        switch (name) {
            case "Normal1":
            case "Normal2":
            case "Normal3":
                ret = doNormalProbOntoDist(context, lines);
                break;
            case "Gamma1":
            case "Gamma2":
                ret = doGamma(context, lines);
                break;
            case "Exponential1":
            case "Exponential2":
                ret = doExponential(context, lines);
                break;
            case "LogNormal1":
            case "LogNormal2":
            case "LogNormal3":
            case "LogNormal4":
            case "LogNormal5":
            case "LogNormal6":
                ret = doLogNormal(context, lines);
                break;
            case "Beta1":
                ret = doBeta(context, lines);
                break;
            case "StudentT1":
            case "StudentT2":
                ret = doStudent(context, lines);
                break;
            case "Uniform1":
                ret = doUniform(context, lines);
                break;
            case "Weibull1":
            case "Weibull2":
                ret = doWeibull(context, lines);
                break;
            case "InverseGamma1":
                ret = doInverseGamma(context, lines);
                break;
            case "Wishart1":
            case "Wishart2":
                ret = doWishart(context, lines);
                break;
            case "InverseWishart1":
            case "InverseWishart2":
                ret = doInverseWishart(context, lines);
                break;
            case "MultivariateNormal1":
            case "MultivariateNormal2":
                ret = doMultivariateNormal(context, lines);
                break;
            case "MultivariateStudentT1":
            case "MultivariateStudentT2":
                ret = doMultivariateTStudent(context, lines);
                break;
            default:
                throw new UnsupportedOperationException("ProbOnto distribution " + name + " not supported");
        }
        lines.add(ret);
        return lines;

    }

    private String doGamma(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s)";
        String formatPar1 = "%s %s 1/%s";
        String formatPar2 = "%s %s %s";
        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();

        String id_shape = context.getSymbId() + "_shape";
        String id_rate = context.getSymbId() + "_rate";

        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_shape, id_rate));
        if (distName.equals("Gamma2")) {
            lines.add(String.format(formatPar2, id_shape, assignSymbol, getParameter(value.getParameter(ParameterName.SHAPE))));
            lines.add(String.format(formatPar2, id_rate, assignSymbol, getParameter(value.getParameter(ParameterName.RATE))));
        } else if (distName.equals("Gamma1")) {
            lines.add(String.format(formatPar2, id_shape, assignSymbol, getParameter(value.getParameter(ParameterName.SHAPE))));
            lines.add(String.format(formatPar1, id_rate, assignSymbol, getParameter(value.getParameter(ParameterName.SCALE))));
        }
        return distName + "(" + id_shape + "," + id_rate + ")";
    }

    private String doBeta(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s)";
        String formatPar2 = "%s %s %s";

        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        String id_alpha = context.getSymbId() + "_alpha";
        String id_beta = context.getSymbId() + "_beta";
        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_alpha, id_beta));
        lines.add(String.format(formatPar2, id_alpha, assignSymbol, getParameter(value.getParameter(ParameterName.ALPHA))));
        lines.add(String.format(formatPar2, id_beta, assignSymbol, getParameter(value.getParameter(ParameterName.BETA))));
        return distName + "(" + id_alpha + "," + id_beta + ")";
    }

    private String doExponential(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s)";
        String formatPar1 = "%s %s 1/%s";
        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        String id_rate = context.getSymbId() + "_rate";
        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_rate));
        if (distName.equals("Exponential1")) {
            lines.add(String.format("%s %s %s", id_rate, assignSymbol, getParameter(value.getParameter(ParameterName.RATE))));
        } else if (distName.equals("Exponential2")) {
            lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_rate));
            lines.add(String.format(formatPar1 + "1/(%s)", id_rate, assignSymbol, getParameter(value.getParameter(ParameterName.MEAN))));
        }
        return distName + "(" + id_rate + ")";
    }

    private List<String> doLogNormalPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doLogNormal((CommonParameter) elem, lines);
        return lines;
    }

    private String doLogNormal(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s)";
        String formatPar1 = "%s %s";
        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        String id_meanLog = context.getSymbId() + "_meanLog";
        String id_prec = context.getSymbId() + "_prec";
        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_meanLog, id_prec));
        if (distName.equals("LogNormal1")) {
            lines.add(String.format(formatPar1 + " %s", id_meanLog, assignSymbol, getParameter(value.getParameter(ParameterName.MEAN_LOG))));
            lines.add(String.format(formatPar1 + "1/pow(%s,2)", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.STDEV_LOG))));
        } else if (distName.equals("LogNormal2")) {
            lines.add(String.format("%s %s %s", id_meanLog, assignSymbol, getParameter(value.getParameter(ParameterName.MEAN_LOG))));
            lines.add(String.format(formatPar1 + "(1/(%s))", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.VAR_LOG))));
        } else if (distName.equals("LogNormal3")) {
            lines.add(String.format(formatPar1 + "log(%s)", id_meanLog, assignSymbol, getParameter(value.getParameter(ParameterName.MEDIAN))));
            lines.add(String.format(formatPar1 + "1/pow(%s,2)", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.STDEV_LOG))));
        } else if (distName.equals("LogNormal4")) {
            lines.add(String.format(formatPar1 + "log(%s)", id_meanLog, assignSymbol, getParameter(value.getParameter(ParameterName.MEDIAN))));
            lines.add(String.format(formatPar1 + "1/(log(pow(%s,2)+1))", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.COEF_VAR))));
        } else if (distName.equals("LogNormal5")) {
            lines.add(String.format("%s %s %s", id_meanLog, assignSymbol, getParameter(value.getParameter(ParameterName.MEAN_LOG))));
            lines.add(String.format("%s %s %s", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.PRECISION))));
        } else if (distName.equals("LogNormal6")) {
            lines.add(String.format(formatPar1 + "log(%s)", id_meanLog, assignSymbol, getParameter(value.getParameter(ParameterName.MEDIAN))));
            lines.add(String.format(formatPar1 + "1/pow(log(%s),2)", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.GEOM_STDEV))));
        }
        return distName + "(" + id_prec + "," + id_meanLog + ")";
    }

    private String doStudent(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s,%s)";
        String formatPar2 = "%s %s %s";

        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        String id_mean = context.getSymbId() + "_mean";
        String id_dof = context.getSymbId() + "_dof";
        String id_scale = context.getSymbId() + "_scale";

        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_mean, id_scale, id_dof));

        lines.add(String.format(formatPar2, id_dof, assignSymbol, getParameter(value.getParameter(ParameterName.DEGREES_OF_FREEDOM))));

        if (distName.equals("StudentT2")) {
            lines.add(String.format(formatPar2, id_mean, assignSymbol, getParameter(value.getParameter(ParameterName.MEAN))));
            lines.add(String.format(formatPar2, id_scale, assignSymbol, getParameter(value.getParameter(ParameterName.SCALE))));

        } else if (distName.equals("StudentT1")) {
            lines.add(String.format(formatPar2, id_mean, assignSymbol, 0));
            lines.add(String.format(formatPar2, id_scale, assignSymbol, 1));

        }
        return distName + "(" + id_mean + "," + id_scale + "," + id_dof + ")";
    }

    private List<String> doUniformPriors(PharmMLRootType elem) {

        List<String> lines = new ArrayList<>();
        doUniform((CommonParameter) elem, lines);
        return lines;
    }

    private String doUniform(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s)";
        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        String id_max = context.getSymbId() + "_max";
        String id_min = context.getSymbId() + "_min";
        String formatPar2 = "%s %s %s";
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);

        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_max, id_min));
        lines.add(String.format(formatPar2, id_max, assignSymbol, getParameter(value.getParameter(ParameterName.MINIMUM))));
        lines.add(String.format(formatPar2, id_min, assignSymbol, getParameter(value.getParameter(ParameterName.MAXIMUM))));
        return distName + "(" + id_min + "," + id_max + ")";
    }

    private String doInverseGamma(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s)";
        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        String name = context.getSymbId() + "_inv";

        String id_shape = context.getSymbId() + "_shape";
        String id_rate = context.getSymbId() + "_rate";
        lines.add(String.format(formatDist, name, dist_symb, getDistName(getProbOntoDistributionType(context)), id_shape, id_rate));

        lines.add(String.format("%s %s %s", id_shape, assignSymbol, getParameter(value.getParameter(ParameterName.SHAPE))));
        lines.add(String.format("%s %s 1/%s", id_rate, assignSymbol, getParameter(value.getParameter(ParameterName.SCALE))));
        lines.add(String.format("%s %s 1/%s", context.getSymbId(), assignSymbol, name));

        return distName + "(" + id_shape + "," + id_rate + ")";
    }

    private String doWishart(CommonParameter cp, List<String> lines) {
        String formatDist = "%s[1:%s,1:%s] %s %s(%s_inverseScaleMatrix[,],%s)";
        ProbOnto value = (ProbOnto) Util.getDistribution(cp);
        String distName = value.getName().value();
        List<DistributionParameter> listP = value.getListOfParameter();
        DistributionParameter doF = value.getParameter(ParameterName.DEGREES_OF_FREEDOM);
        DistributionParameter scaleMat, invScaleMat;
        String par1 = getParameter(doF);
        String par2;
        Integer dim;

        switch (distName) {
            case "Wishart1":
                scaleMat = value.getParameter(ParameterName.SCALE_MATRIX);

                String scalePar = getParameter(scaleMat);
                dim = matrixMap.get(cp.getSymbId())[0];
                lines.add(String.format(formatDist, cp.getSymbId(), dim, dim, dist_symb, getDistName(value), cp.getSymbId(), par1));

                lines.add(String.format("%s_inverseScaleMatrix[1:%s,1:%s] %s inverse(%s[,])", cp.getSymbId(), dim, dim, assignSymbol, scalePar));
                break;
            case "Wishart2":
                invScaleMat = value.getParameter(ParameterName.INVERSE_SCALE_MATRIX);
                par2 = getParameter(invScaleMat);
                dim = matrixMap.get(cp.getSymbId())[0];
                lines.add(String.format("%s[1:%s, 1:%s] %s %s(%s[,],%s)", cp.getSymbId(), dim, dim, dist_symb, getDistName(value), par2, par1));

                break;
        }
        return "";
    }

    private String doInverseWishart(CommonParameter cp, List<String> lines) {
        String formatDist = "inverse%s[1:%s,1:%s] %s %s(%s_inverseScaleMatrix[,],%s)";
        ProbOnto value = (ProbOnto) Util.getDistribution(cp);
        String distName = value.getName().value();
        List<DistributionParameter> listP = value.getListOfParameter();
        DistributionParameter doF = value.getParameter(ParameterName.DEGREES_OF_FREEDOM);
        DistributionParameter scaleMat, invScaleMat;
        String par1 = getParameter(doF);
        String par2;
        Integer dim;

        scaleMat = value.getParameter(ParameterName.SCALE_MATRIX);
        par2 = getParameter(scaleMat);
        dim = matrixMap.get(par2)[0];
        lines.add(String.format(formatDist, cp.getSymbId(), dim, dim, dist_symb, getDistName(value), cp.getSymbId(), par1));

        lines.add(String.format("%s_inverseScaleMatrix[1:%s,1:%s] %s inverse(%s[,])", cp.getSymbId(), dim, dim, assignSymbol, par2));
        lines.add(String.format("%s[1:%s,1:%s] %s inverse(inverse%s[,])", cp.getSymbId(), dim, dim, assignSymbol, cp.getSymbId()));
        return "";
    }

    private String getParse(Object o) {
        String tmp = parse(o, lexer.getTreeMaker().newInstance(o));
        return tmp;
    }

    private List<String> getMeanVector(DistributionParameter o, String idVect, String index, Integer len) {
        List<String> list = new ArrayList<>();
        int i = 1;
        if (o.getAssign().getVector() != null) {
            Vector vet = o.getAssign().getVector();
            if (vet.getVectorElements().getListOfElements().size() > 1) {
                for (VectorValue ve : vet.getVectorElements().getListOfElements()) {
                    list.add(String.format("%s[%s%s] %s %s", idVect, index, i++, assignSymbol, myParse(ve)));
                }
            } else if (vet.getVectorElements().getListOfElements().size() == 1) {
                VectorValue sr = vet.getVectorElements().getListOfElements().get(0);
                if (sr instanceof SymbolRef) {
                    for (int k = 1; k <= len; k++) {
                        list.add(String.format("%s[%s%s] %s %s[%s]", idVect, index, k, assignSymbol, ((SymbolRef) sr).getSymbIdRef(), k));
                    }
                }
            } else {
                System.out.println("Vector " + idVect + " empty.");
                outDebug.println("Vector " + idVect + " empty.");
            }
        } else if (o.getAssign().getSymbRef() != null) {
            list.addAll(genVector(idVect, getParameter(o), len));
        }
        return list;
    }

    List<String> genMatrix(String idLeft, String idRigth, int len) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= len; i++) {
            for (int j = 1; j <= len; j++) {
                list.add(String.format("%s[%s,%s] %s %s[%s,%s]", idLeft, i, j, assignSymbol, idRigth, i, j));
            }
        }
        return list;
    }

    List<String> genVector(String idLeft, String idRigth, int len) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= len; i++) {
            list.add(String.format("%s[%s] %s %s[%s]", idLeft, i, assignSymbol, idRigth, i));
        }
        return list;
    }

    private List<String> getVectorElements(Vector o, String idVect) {
        List<String> list = new ArrayList<>();
        int i = 1;
        for (VectorValue ve : o.getVectorElements().getListOfElements()) {
            list.add(String.format("%s[%s] %s %s", idVect, i, assignSymbol, getVectorValue(ve), i));
            i++;
        }

        return list;
    }

    private boolean getVariability(ParameterRandomVariable o) {
        String symbol = o.getSymbId();
        LevelReference level_ref = ((ParameterRandomVariable) o).getVariabilityReference();
        VariabilityBlock vb = lexer.getVariabilityBlock(level_ref.getSymbRef());
        if (vb.isParameterVariability()) {
            return true;
        } else {
            return false;
        }
    }

    private String doMultivariateNormal(CommonParameter cp, List<String> lines) {
        String formatDist = "%s[%s1:%s] %s %s(%s[%s],%s[,])";
        ProbOnto value = (ProbOnto) Util.getDistribution(cp);
        String subIndex = "";
        if (cp instanceof ParameterRandomVariable) {
            ParameterRandomVariable prv = (ParameterRandomVariable) cp;
            if (getVariability(prv)) {
                subIndex = IND_S + ",";
            }
        }
        if (!subIndex.equals("")) {
            putVariableMap(cp.getSymbId(), cp.getSymbId() + IND_SUBJ);
        }
        String distName = value.getName().value();
        String id1 = cp.getSymbId() + "_mean";
        String id2 = cp.getSymbId() + "_prec";
        String par1 = "", par2 = "";
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        DistributionParameter covMat, precMat;
        int mDim;
        switch (distName) {
            case "MultivariateNormal1":
                covMat = value.getParameter(ParameterName.COVARIANCE_MATRIX);
                String covPar = getParameter(covMat);
                mDim = matrixMap.get(covPar)[0];
                lines.add(String.format(formatDist, cp.getSymbId(), subIndex, mDim, dist_symb, getDistName(value), id1, subIndex, id2));
                lines.addAll(getMeanVector(mean, id1, subIndex, mDim)); 
                lines.add(String.format("%s[1:%s,1:%s] %s inverse(%s[,])", id2, mDim, mDim, assignSymbol, covPar));
                break;
            case "MultivariateNormal2":
                precMat = value.getParameter(ParameterName.PRECISION_MATRIX);
                String precPar = getParameter(precMat);
                mDim = matrixMap.get(precPar)[0];
                lines.add(String.format(formatDist, cp.getSymbId(), subIndex, mDim, dist_symb, getDistName(value), id1, subIndex, precPar));
                lines.addAll(getMeanVector(mean, id1, subIndex, mDim));
                break;
        }
        return "";
    }

    private String doMultivariateTStudent(CommonParameter cp, List<String> lines) {
        String formatDist = "%s[%s1:%s] %s %s(%s[%s],%s[,],%s)";
        ProbOnto value = (ProbOnto) Util.getDistribution(cp);
        String subIndex = "";
        if (cp instanceof ParameterRandomVariable) {
            ParameterRandomVariable prv = (ParameterRandomVariable) cp;
            if (getVariability(prv)) {
                subIndex = IND_S + ",";
            }
        }
        if (!subIndex.equals("")) {
            putVariableMap(cp.getSymbId(), cp.getSymbId() + IND_SUBJ);
        }
        String distName = value.getName().value();
        String par1 = "", par2 = "";
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        DistributionParameter deg = value.getParameter(ParameterName.DEGREES_OF_FREEDOM);
        DistributionParameter covMat, precMat;
        String meanId = cp.getSymbId() + "_mean";
        String precId = cp.getSymbId() + "_prec";
        String degId = getParameter(deg);
        int mDim;
        switch (distName) {
            case "MultivariateStudentT1":
                covMat = value.getParameter(ParameterName.COVARIANCE_MATRIX);
                String covPar = getParameter(covMat);
                mDim = matrixMap.get(covPar)[0];
                lines.add(String.format(formatDist, cp.getSymbId(), subIndex, mDim, dist_symb, getDistName(value), meanId, subIndex, precId, degId));

                lines.addAll(getMeanVector(mean, meanId, subIndex, mDim)); 
                lines.add(String.format("%s[1:%s,1:%s] %s inverse(%s[,])", precId, mDim, mDim, assignSymbol, covPar));
                break;
            case "MultivariateStudentT2":
                precMat = value.getParameter(ParameterName.PRECISION_MATRIX);
                String precPar = getParameter(precMat);
                mDim = matrixMap.get(precPar)[0];
                lines.add(String.format(formatDist, cp.getSymbId(), subIndex, mDim, dist_symb, getDistName(value), meanId, subIndex, getParameter(precMat), degId));
                lines.addAll(getMeanVector(mean, meanId, subIndex, mDim));
                break;
        }
        return "";
    }

    private String doWeibull(CommonParameter context, List<String> lines) {
        String formatDist = "%s %s %s(%s,%s)";
        String formatPar1 = "lambda %s pow(%s,(-1/%s))";

        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();

        String id_shape = context.getSymbId() + "_shape";
        String id_lambda = context.getSymbId() + "_lambda";

        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_lambda, id_shape));
        if (distName.equals("Weibull1")) {
            lines.add(String.format("%s %s %s", assignSymbol, id_lambda, value.getParameter(ParameterName.LAMBDA)));
            lines.add(String.format("%s %s %s", assignSymbol, id_shape, value.getParameter(ParameterName.SHAPE)));
        } else {
            lines.add(String.format(formatPar1, assignSymbol, id_lambda, value.getParameter(ParameterName.SCALE)));
            lines.add(String.format("%s %s %s", assignSymbol, id_shape, value.getParameter(ParameterName.SHAPE)));

        }
        return distName + "(" + id_lambda + "," + id_shape + ")";
    }

    String doNormalProbOntoDist(ParameterRandomVariable context, List<String> lines) {
        String formatDistMeanValAssign = assignSymbol + " %s";
        String formatDistVarValAssign = assignSymbol + " (1/(%s))";
        String formatDistMeanVal = "%s";
        ProbOnto value
                = context.getDistribution().getProbOnto();
        String distName = getDistName(value);
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        DistributionParameter par2 = null;
        String varAss = "";
        switch (value.getName().value()) {
            case "Normal1":
                par2 = value.getParameter(ParameterName.STDEV);
                varAss = String.format("pow(%s,2)", parse(par2, lexer.getTreeMaker().newInstance(par2)));
                break;
            case "Normal2":
                par2 = value.getParameter(ParameterName.VAR);
                varAss = parse(par2, lexer.getTreeMaker().newInstance(par2));
                break;
            case "Normal3":
                par2 = value.getParameter(ParameterName.PRECISION);
                varAss = String.format(" (1/(%s))", parse(par2, lexer.getTreeMaker().newInstance(par2)));
                break;
        }
        SymbolRef srM = new SymbolRef();
        String meanAss = parse(mean, lexer.getTreeMaker().newInstance(mean));
        String meanAssign = null;
        if (mean.getAssign() != null) {
            srM.setSymbIdRef(meanAss);
            meanAssign = delimit(meanAss);
        }
        String varianceAssign = delimit(varAss);

        String rhsVarExpression = String.format(formatDistVarValAssign, varianceAssign);
        String rhsMeanExpression = String.format(formatDistMeanValAssign, meanAssign);
        String meanExpression = delimit(String.format(formatDistMeanVal, meanAssign));
        String brackets = getIndexes(rhsMeanExpression) == NO_INDEX ? "" : getBrackets(rhsMeanExpression);
        brackets = getIndexes(rhsVarExpression) == NO_INDEX ? "" : getBracketsGen(rhsVarExpression);
        String varTmpName = (context).getSymbId() + varianceSuffixLabel;
        String formatDistVarAssignement = varTmpName + "%s %s";
        String varAssignStmt = String.format(formatDistVarAssignement, brackets, rhsVarExpression);
        String varCompleteLabel = delimit(Util.clean(varTmpName + brackets));
        lines.add(varAssignStmt);
        String current_symbol = getSymbol(context);
        String statementDistFormat = "%s " + dist_symb + " %s(%s, %s)";
        return String.format(statementDistFormat, delimit(current_symbol), distName, meanExpression, varCompleteLabel);
    }

    private Object findElement(BinaryTree bk) {
        ScriptDefinition sd = lexer.getScriptDefinition();
        Map<Object, BinaryTree> stm = sd.getStatementsMap();
        Object[] elements = stm.keySet().toArray(new Object[0]);
        for (Object o : elements) {
            if (stm.get(o).equals(bk)) {
                return o;
            }
        }
        return null;
    }

    private String doCommonVariable(CommonVariableDefinition v) {
        return z.get(v.getSymbId());
    }

    private String doMatrixSelector(MatrixSelector ms) {
        if (variablesAssignementMap.get(delimit(ms.getSymbRef().getSymbIdRef())) != null) {
            return ms.getSymbRef().getSymbIdRef()
                    + "[" + IND_S + "," + ms.getCell().getRowIndex().getAssign().toMathExpression() + ","
                    + ms.getCell().getColumnIndex().getAssign().toMathExpression() + "]";
        } else {
            return ms.getSymbRef().getSymbIdRef()
                    + "[" + ms.getCell().getRowIndex().getAssign().toMathExpression() + ","
                    + ms.getCell().getColumnIndex().getAssign().toMathExpression() + "]";
        }
    }

    private String doMatrixUniOp(MatrixUniOp v) {
        return "@";
    }

    protected boolean isIndependentVariableSym(String symbol) {
        if (symbol != null) {
            IndependentVariable ip = lexer.getDom().getListOfIndependentVariable().get(0);
            if (ip != null) {

                String currentSymbol = ip.getSymbId();
                if (currentSymbol != null) {

                    if (currentSymbol.equals(symbol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updatePriorList(PharmMLRootType par) {
        if (priorLevel != null) {
            if (par instanceof PopulationParameter) {
                PopulationParameter p = (PopulationParameter) par;
                for (LevelReference vRef : p.getListOfVariabilityReference()) {
                    if (vRef.getSymbRef().getSymbIdRef().equals(priorLevel.getSymbId())) {
                        priorPar.put(par, p.getSymbId());
                    }
                }
            } else if (par instanceof IndividualParameter) {
                IndividualParameter p = (IndividualParameter) par;
                for (LevelReference vRef : p.getListOfVariabilityReference()) {
                    if (vRef.getSymbRef().getSymbIdRef().equals(priorLevel.getSymbId())) {
                        priorPar.put(par, p.getSymbId());
                    }
                }
            } else if (par instanceof CategoricalCovariate) {
                CategoricalCovariate p = (CategoricalCovariate) par;
                for (LevelReference vRef : p.getListOfVariabilityReference()) {
                    if (vRef.getSymbRef().getSymbIdRef().equals(priorLevel.getSymbId())) {
                        priorPar.put(par, p.getId());
                    }
                }
            } else if (par instanceof Correlation) {
                Correlation p = (Correlation) par;
                LevelReference vRef = p.getVariabilityReference();
                if (vRef.getSymbRef().getSymbIdRef().equals(priorLevel.getSymbId())) {
                    priorPar.put(par, p.getId());

                }
            } else if (par instanceof ParameterRandomVariable) {
                ParameterRandomVariable p = (ParameterRandomVariable) par;
                for (LevelReference vRef : p.getListOfVariabilityReference()) {
                    if (vRef.getSymbRef().getSymbIdRef().equals(priorLevel.getSymbId())) {
                        priorPar.put(par, p.getSymbId());
                    }
                }
            } else if (par instanceof GeneralObsError) {
                GeneralObsError p = (GeneralObsError) par;
                for (LevelReference vRef : p.getListOfVariabilityReference()) {
                    if (vRef.getSymbRef().getSymbIdRef().equals(priorLevel.getSymbId())) {
                        priorPar.put(par, p.getSymbId());
                    }
                }
            }
        }
    }

    protected List<String> getMatrixElementsAny(Matrix mat, String id) {
        List<String> lines = new ArrayList<>();
        List<PharmMLRootType> list = mat.getListOfMatrixElements();
        int i = 1, j = 1;
        for (PharmMLRootType el : list) {
            for (MatrixRowValue e : ((MatrixRow) el).getListOfValues()) {

                lines.add(String.format("%s[%d,%d] %s %s", id, i, j, assignSymbol, myParse(e)));
                j++;
            }
            i++;
            j = 1;
        }
        Integer dim[] = new Integer[2];
        dim[0] = i - 1;
        dim[1] = i - 1;
        matrixMap.put(id, dim);
        reUpdateMatrixMapWithAllMaps();
        return lines;
    }

    protected List<String> getMatrixElementsLT(Matrix mat, String id) {
        List<String> lines = new ArrayList<>();
        List<PharmMLRootType> list = mat.getListOfMatrixElements();
        int i = 1, j = 1, mDim = mat.getListOfMatrixElements().size();
        for (PharmMLRootType el : list) {
            j = 1;
            for (MatrixRowValue e : ((MatrixRow) el).getListOfValues()) {
                if (j < i) {
                    lines.add(String.format("%s[%d,%d] %s %s", id, j, i, assignSymbol, myParse(e)));
                }
                lines.add(String.format("%s[%d,%d] %s %s", id, i, j, assignSymbol, myParse(e)));
                j++;
            }
            i++;
        }

        Integer dim[] = new Integer[2];
        dim[0] = mDim;
        dim[1] = mDim;
        matrixMap.put(id, dim);
        reUpdateMatrixMapWithAllMaps();
        return lines;
    }

    protected List<String> getMatrixElementsUT(Matrix mat, String id) {
        List<String> lines = new ArrayList<>();
        List<PharmMLRootType> list = mat.getListOfMatrixElements();
        int i = 1, j = 1, mDim = mat.getListOfMatrixElements().size();
        for (PharmMLRootType el : list) {
            j = 1;
            for (MatrixRowValue e : ((MatrixRow) el).getListOfValues()) {
                if (j > i) {
                    lines.add(String.format("%s[%d,%d] %s %s", id, j, i, assignSymbol, myParse(e)));
                }
                lines.add(String.format("%s[%d,%d] %s %s", id, i, j, assignSymbol, myParse(e)));
                j++;
            }
            i++;
        }
        Integer dim[] = new Integer[2];
        dim[0] = i - 1;
        dim[1] = i - 1;
        matrixMap.put(id, dim);
        reUpdateMatrixMapWithAllMaps();
        return lines;
    }

    protected List<String> getMatrix(Matrix mat, String id) {
        List<String> lines = new ArrayList<>();
        switch (mat.getMatrixType()) {
            case "Any":
                lines.addAll(getMatrixElementsAny(mat, id));
                break;
            case "LowerTriangular":
                lines.addAll(getMatrixElementsLT(mat, id));
                break;
            case "UpperTriangular":
                lines.addAll(getMatrixElementsUT(mat, id));
                break;
        }
        return lines;
    }

    private void updateMatrixMap() {
        int nR, nC;
        Matrix mat = null;
        for (PopulationParameter p : getModelParameters()) {
            if (p.getAssign() != null) {
                if (p.getAssign().getMatrix() != null) {
                    mat = p.getAssign().getMatrix();
                } else if (p.getAssign().getMatrixUniop() != null) {
                    MatrixUniOp muo = p.getAssign().getMatrixUniop();
                    if (muo.getValue() instanceof Matrix) {
                        mat = (Matrix) muo.getValue();
                    }
                } else {
                    continue;
                }

                nR = mat.getListOfMatrixElements().size();
                nC = ((MatrixRow) mat.getListOfMatrixElements().get(0)).getListOfValues().size();
                Integer dim[] = new Integer[2];
                dim[0] = nR;
                dim[1] = nC;

                matrixMap.put(p.getSymbId(), dim);
                reUpdateMatrixMapWithAllMaps();
            }
        }
        reUpdateMatrixVectorMapsWithAllMaps();
    }

    private void updateVectorMap() {
        int nR;
        for (PopulationParameter p : getModelParameters()) {
            if (p.getAssign() != null && p.getAssign().getVector() != null) {
                Vector vet = p.getAssign().getVector();

                nR = vet.getVectorElements().getListOfElements().size();
                vectorMap.put(p.getSymbId(), nR);
            }
        }
        reUpdateMatrixVectorMapsWithAllMaps();

    }

    private boolean checkMatrix(PopulationParameter p, BinaryTree bt) {
        int nR = 0, nC = 0;
        String name = null, tmp = "";

        for (Node node : bt.nodes) {
            if (node.data instanceof MatrixSelector) {
                MatrixSelector ms = (MatrixSelector) node.data;
                String value = ms.getSymbRef().getSymbIdRef()
                        + "[" + ms.getCell().getRowIndex().getAssign().toMathExpression() + ","
                        + ms.getCell().getColumnIndex().getAssign().toMathExpression() + "]";
                String current_value = String.format("%s <- %s", delimit(p.getSymbId()), delimit(value));

                populationParametersMatrixLines.add(current_value);
                return true;
            } else if (node.data instanceof Matrix) {
                List<String> lines = new ArrayList<>();
                switch (((Matrix) node.data).getMatrixType()) {
                    case "Any":
                        lines.addAll(getMatrixElementsAny((Matrix) node.data, p.getSymbId()));
                        break;
                    case "LowerTriangular":
                        lines.addAll(getMatrixElementsLT((Matrix) node.data, p.getSymbId()));
                        break;
                    case "UpperTriangular":
                        lines.addAll(getMatrixElementsUT((Matrix) node.data, p.getSymbId()));
                        break;

                }
                populationParametersLines.addAll(lines);
                return true;
            } else if (node.data instanceof MatrixUniOp) {
                MatrixUniOp muo = ((MatrixUniOp) node.data);
                if (muo.getValue() instanceof Matrix) {
                    Matrix mat = (Matrix) muo.getValue();
                    name = p.getSymbId() + muo.getOp().value().substring(0, 3); 
                    Integer dim = matrixMap.get(p.getSymbId())[0];

                    List<String> lines = new ArrayList<>();
                    lines.addAll(getMatrix(mat, name));
                    matrixVectorLines.addAll(lines);
                    populationParametersMatrixLines.add(String.format("%s[1:%s,1:%s] %s %s(%s[,])",
                            p.getSymbId(), dim, dim,
                            assignSymbol,
                            muo.getOp().value(),
                            name));
                    String current_value = "";
                    if (p.getAssign() != null && p.getAssign().getMatrixUniop() != null) {
                        if (muo.getValue() instanceof SymbolRef) {
                            String value = delimit(muo.getOp().value() + "(" + ((SymbolRef) muo.getValue()).getSymbIdRef() + ")");
                            current_value = String.format("%s <- %s", name, value);

                        }
                        populationParametersMatrixLines.add(current_value); 
                    }
                } else if (muo.getValue() instanceof SymbolRef) {
                    PopulationParameter pp = null;
                    name = ((SymbolRef) (p.getAssign().getMatrixUniop().getValue())).getSymbIdRef();
                    Object o = lexer.getAccessor().fetchElement(name);
                    if (o instanceof PopulationParameter) {
                        pp = (PopulationParameter) o;

                        if (pp.getAssign() != null && pp.getAssign().getMatrix() != null) {
                            nR = pp.getAssign().getMatrix().getNumbRows(); 
                            nC = pp.getAssign().getMatrix().getNumbCols(); 
                            Integer dim[] = new Integer[2];
                            dim[0] = nR;
                            dim[1] = nC;
                            matrixMap.put(pp.getSymbId(), dim);
                            reUpdateMatrixMapWithAllMaps();
                            String value = delimit(muo.getOp().value() + "(" + ((SymbolRef) muo.getValue()).getSymbIdRef() + "[,])");
                            String current_value = String.format("%s[1:%s,1:%s] <- %s", p.getSymbId(), nR, nC, value);

                            populationParametersMatrixLines.add(current_value); 
                        } else if (pp.getAssign() != null && pp.getDistribution() != null) {
                            List<DistributionParameter> dps = pp.getDistribution().getProbOnto().getListOfParameter();
                            for (DistributionParameter par : dps) {
                                String distPar = getParameter(par);
                                PopulationParameter ppar = (PopulationParameter) lexer.getAccessor().fetchElement(distPar);
                                if (ppar.getAssign().getMatrix() != null) {
                                    addToMatrixMap(ppar.getAssign().getMatrix(), pp.getSymbId());
                                } else if (ppar.getAssign().getVector() != null) {
                                    addToVectorMap(ppar.getAssign().getVector(), pp.getSymbId());
                                }
                            }
                            Integer mDim[] = matrixMap.get(p.getSymbId());
                            populationParametersMatrixLines.add(String.format("%s[1:%s,1:%s] %s %s(%s[,])", p.getSymbId(), mDim[0], mDim[1], assignSymbol, muo.getOp().value(), name));
                        }
                    } else if (o instanceof SymbolRef) {
                        Object ob = lexer.getAccessor().fetchElement((SymbolRef) o).getId();
                        if (ob instanceof PopulationParameter) {
                            pp = (PopulationParameter) ob;

                            if (pp.getAssign() != null && pp.getAssign().getMatrix() != null) {
                                nR = pp.getAssign().getMatrix().getNumbRows(); //getRowNumber(name);
                                nC = pp.getAssign().getMatrix().getNumbCols(); // getColNumber(name);
                                Integer dim[] = new Integer[2];
                                dim[0] = nR;
                                dim[1] = nC;
                                matrixMap.put(pp.getSymbId(), dim);
                                reUpdateMatrixMapWithAllMaps();
                                String value = delimit(muo.getOp().value() + "(" + ((SymbolRef) muo.getValue()).getSymbIdRef() + "[,])");
                                String current_value = String.format("%s[1:%s,1:%s] <- %s", p.getSymbId(), nR, nC, value);

                                populationParametersMatrixLines.add(current_value); //                    populationParametersMatrixLines.add(String.format("%s[,] %s %s(%s[,],%s)", ((SymbolRef) muo.getValue()).getSymbIdRef(), dist_symb, "@", "@", "@"));
                            }
                        }

                    }
                } else {
                    String id = p.getSymbId();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private void addToMatrixMap(Matrix mat, String idVar) {
        int nR, nC;
        nR = mat.getListOfMatrixElements().size();
        nC = ((MatrixRow) (mat.getListOfMatrixElements().get(0))).getListOfValues().size();
        Integer dim[] = new Integer[2];
        dim[0] = nR;
        dim[1] = nC;
        matrixMap.put(idVar, dim);
        reUpdateMatrixVectorMapsWithAllMaps();

    }

    private void addToVectorMap(Vector vet, String idVar) {
        int nR;
        nR = vet.getListOfVectorCellAndSegment().size();
        vectorMap.put(idVar, nR);
        reUpdateMatrixVectorMapsWithAllMaps();
    }

    private void reUpdateMatrixMapWithMap(Map<String, List<SymbolRef>> map) {
        Set<String> es = map.keySet();
        for (String s : es) {
            List<SymbolRef> depV = map.get(s);
            for (String sv : Util.getList(depV)) {
                if (!matrixMap.containsKey(s) && matrixMap.get(sv) != null) {// 30 luglio
                    matrixMap.put(s, matrixMap.get(sv));
                    reUpdateMatrixMapWithAllMaps();
                }
            }
        }
    }

    private void reUpdateMatrixVectorMapsWithAllMaps() {
        reUpdateMatrixMapWithAllMaps();
        reUpdateVectorMapWithAllMaps();
    }

    private void reUpdateMatrixMapWithAllMaps() {
        reUpdateMatrixMapWithMap(parVariablesFromMap);
        reUpdateMatrixMapWithMap(randVariablesFromMap);

    }

    private void reUpdateVectorMapWithAllMaps() {
        reUpdateVectorMapWithMap(parVariablesFromMap);
        reUpdateVectorMapWithMap(randVariablesFromMap);

    }

    private void reUpdateVectorMapWithMap(Map<String, List<SymbolRef>> map) {
        Set<String> es = map.keySet();
        for (String s : es) {
            List<SymbolRef> depV = map.get(s);
            for (String sv : Util.getList(depV)) {
                if (!vectorMap.containsKey(s) && vectorMap.get(sv) != null) {// 30 luglio
                    vectorMap.put(s, vectorMap.get(sv));
                }
            }
        }
    }

    protected void managePopulationParameters() throws ParserConfigurationException, SAXException, IOException {
        variableCompleteList = loadXML(xmlFileName);
        String rightPart = "";
        updateMatrixMap();
        updateVectorMap();
        List<PopulationParameter> mpList = getModelParameters();
        for (PopulationParameter p : mpList) { // for (PopulationParameter p : sps) { CRI 0.7.3
            if (isInList(assignedSimpleParList, p.getSymbId())) {
                TreeMaker tm = lexer.getTreeMaker();
                BinaryTree bt = tm.newInstance(p);//isAssigned(assignedSimpleParList, p)) {
                if (checkMatrix(p, bt)) {
                    continue;
                }
                if (p.getAssign() != null) {
                    if (p.getAssign().getVector() != null) {
                        vectorMap.put(p.getSymbId(), p.getAssign().getVector().getVectorElements().getListOfElements().size());
                        populationParametersLines.addAll(getVectorElements(p.getAssign().getVector(), p.getSymbId()));
                    } else {
                        rightPart = parse(p, bt);
                        if (rightPart.trim().length() > 0) {
                            populationParametersLines.add(rightPart);
                        }
                    }
                }
            }
            updatePriorList(p);
        }

        BinaryTree bt;
        for (ObservationBlock ob : lexer.getScriptDefinition().getObservationBlocks()) {
            for (ObservationParameter sp : ob.getObservationParameters()) {
                if (isInList(assignedSimpleParList, extract(sp.getName()))) {
                    bt = lexer.getStatement(sp);
                    if (bt != null) {
                        rightPart = parse(sp, bt);
                    }
                    if (rightPart.trim().length() > 0) {
                        populationParametersLines.add(rightPart);
                    }
                }
            }
        }
    }

    List<String> parseScriptLines(List<String> lines) {
        List<String> toRemove = new ArrayList<>();
        List<String> output = new ArrayList<>();
        String oldTmp, tmp;
        String newLine;
        boolean modified = true;
        for (String l : lines) {
            modified = true;
            newLine = new String(l.trim());
            while (modified) {
                for (Map.Entry pairs : variablesAssignementMap.entrySet()) {
                    modified = false;
                    if (newLine.contains(pairs.getKey().toString().trim())) {
                        oldTmp = tmp = pairs.getValue().toString().trim();
                        if (oldTmp != null && variablesAssignementMap.get(oldTmp) != null) {// 18 luglio
                            while (tmp != null) {
                                oldTmp = tmp;
                                tmp = variablesAssignementMap.get(oldTmp);
                            }
                            newLine = newLine.replace(pairs.getKey().toString(), oldTmp);// 18 luglio
                        } else {
                            newLine = newLine.replace(pairs.getKey().toString(), pairs.getValue().toString());
                        }
                        newLine = adjustStatement(newLine);
                        if (!l.equals(newLine)) {
                            modified = true;
                        }
                        if (tmpVariablesAssignementMap.get(delimit(pairs.getKey().toString())) == null
                                && variablesAssignementMap.get(delimit(pairs.getKey().toString())) == null) {
                            tmpVariablesAssignementMap.put(delimit(pairs.getKey().toString()), delimit(getLeft(newLine)));
                        }
                    }
                }

                if (modified) {
                    newLine = adjustStatement(newLine);
                } else {
                    output.add(newLine);
                }
            }
        }
        while (!tmpVariablesAssignementMap.isEmpty()) {
            for (Map.Entry pairs : tmpVariablesAssignementMap.entrySet()) {
                String k = pairs.getKey().toString().trim();
                String v = tmpVariablesAssignementMap.get(k);
                variablesAssignementMap.put(delimit(k), delimit(v));
            }
            tmpVariablesAssignementMap.clear();
            updateVariableAssMap();
            output = parseScriptLines(output);
        }
        return output;
    }

    private void updateVariableAssMap() {
        HashMap<String, String> tmpMap = new HashMap<>();
        String k, v;
        for (Map.Entry pairs : variablesAssignementMap.entrySet()) {
            k = pairs.getKey().toString();
            v = pairs.getValue().toString();
            if (v.contains(IND_BOTH)) {
                tmpMap.put(v.replace(IND_BOTH, IND_SUBJ), v);
                tmpMap.put(v.replace(IND_BOTH, IND_TIME), v);
            }
        }
        for (Map.Entry pairs : tmpMap.entrySet()) {
            variablesAssignementMap.put(delimit(Util.clean(pairs.getKey().toString())), delimit(Util.clean(pairs.getValue().toString()))); // 18 luglio
        }
    }

    List<String> getValIndexes(String in) {
        List<String> list = new ArrayList<>();
        if (in.contains(IND_BOTH)) {
            list.add(IND_S);
            list.add(IND_T);
        } else if (in.contains(IND_T)) {
            list.add(IND_T);
        } else if (in.contains(IND_S)) {
            list.add(IND_S);
        }
        return list;
    }

    private void putVariableMap(String key, String newVal) {
        if (isPiecewiseVar(Util.clean(key))) {
            return;
        }
        String oldVal = variablesAssignementMap.get(key);
        if (oldVal != null) {
            oldVal = delimit(Util.clean(oldVal));
            newVal = delimit(Util.clean(newVal));
            if (getValIndexes(newVal).size() < getValIndexes(oldVal).size()) {
                variablesAssignementMap.put(newVal, oldVal);
                return;
            } else if (getValIndexes(newVal).size() > getValIndexes(oldVal).size()) {
                variablesAssignementMap.put(oldVal, newVal);
                return;
            }
        }

        if (!Util.clean(key).equals(Util.clean(newVal))) {
            variablesAssignementMap.put(delimit(removeIndexes(Util.clean(key))), newVal);
        }
    }

    private List<String> loadXML(File nameFile) throws ParserConfigurationException, SAXException, IOException {
        List<String> list = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(nameFile);

        assignedSimpleParList = Util.getObjectsList(doc, "PopulationParameter");
        if (DEBUG) {
            outDebug.println("assignedSimpleParList = " + assignedSimpleParList);
        }
        parametersList = Util.getObjectsList(doc, "Parameter");
        if (DEBUG) {
            outDebug.println("parametersList = " + parametersList);
        }
        return list;
    }

    private void getSimplePar(NodeList nList) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = (Node) nList.item(i);
            for (int temp = 0; temp < nList.getLength(); temp++) {
                org.w3c.dom.Node nNode = nList.item(temp);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    NodeList children = eElement.getChildNodes();
                    org.w3c.dom.Node child;
                    if (children.getLength() > 0) {
                        child = eElement.getFirstChild();
                        if (child != null && child.getNextSibling() != null) {
//                            }
                            if (child.getNextSibling().getNodeName().equals("ct:Assign")) {
                                if (!assignedSimpleParList.contains(eElement.getAttribute("symbId"))) {
                                    assignedSimpleParList.add(eElement.getAttribute("symbId"));
                                }
                                if (!list.contains(eElement.getAttribute("symbId"))) {
                                    list.add(eElement.getAttribute("symbId"));
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private static List<String> getUniqueNameList(List<String> list0) {
        List<String> list = new ArrayList<>();
        for (String s : list0) {
            if (!isInNameList(list, s)) {
                list.add(s);
            }
        }
        return list;
    }

    private String getTab(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) {
            s.append("  ");
        }
        return s.toString();
    }

    protected List<ParameterBlock> getParameterBlocks() {
        List<ParameterBlock> pbs;
        if (!lexer.getScriptDefinition().getParameterBlocks().isEmpty()) {
            return lexer.getScriptDefinition().getParameterBlocks();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected boolean isSupportedUnaryOp(String op) {
        boolean isSupported = false;

        if (op != null) {
            isSupported = winbugs_unary_operators_list.contains(op);
        }

        if (isSupported) {
            return isSupported;
        }
        return super.isSupportedUnaryOp(op); //To change body of generated methods, choose Tools | Templates.
    }

    protected void initSupportedWinBugsUnaryOperators() {
        try {
            winbugs_unary_operators_list = new ArrayList<>();
            winbugsUnaryOperatorsMap = new HashMap<>();
            Class cl = getClass();
            InputStream is = cl.getResourceAsStream("unary_operators_list.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader((is)));
            String line = null;
            ArrayList<String> lines = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
            in.close();
            in = null;

            for (String line_ : lines) {
                if (line_ == null) {
                    continue;
                }
                line_ = line_.trim();
                String[] uniOperator = line_.split(" ");
                if (uniOperator.length != 2) {
                    continue;
                }
                uniOperator[0] = uniOperator[0].trim();
                uniOperator[1] = uniOperator[1].trim();
                if (!winbugs_unary_operators_list.contains(uniOperator[0])) {
                    winbugs_unary_operators_list.add(uniOperator[0]);
                    winbugsUnaryOperatorsMap.put(uniOperator[0], uniOperator[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getUnaryOperator(String name) {
        String op = null;
        if (winbugsUnaryOperatorsMap.containsKey(name)) {
            op = winbugsUnaryOperatorsMap.get(name);
        }
        if (op != null) {
            return op;
        } else {
            return super.getUnaryOperator(name);
        }
    }

    protected String doDerivativeDependentVariable(VariableDefinition var) {
        String s = var.getSymbId() + upperSuffixDerDepLabel;
        return s;
    }

    protected String doAppendSuffix(SymbolRef sr) {
        String s = sr.getSymbIdRef() + upperSuffixDerDepLabel;
        return s;
    }

    private List<String> makeCompactDerivativeModelLines() {
        List<String> newLines = new ArrayList<>();
        for (String l : diffEqLines) {
            List<VariableDefinition> vars = lexer.getLocalVariables();
            for (VariableDefinition v : vars) {
                String fromLabel = derivativeDefFromMap.get(v.getSymbId()); // gets the variable assignement 
                String toLabel = doDerivativeDependentVariable(v);
                l = l.replace(fromLabel, toLabel);
            }
            newLines.add(l);
        }
        return newLines;
    }

    private List<String> adjustDerivativeModelLines() {
        List<String> newLines = new ArrayList<>();
        String toAdd = "";
        for (String l : diffEqLines) {
            if (l.contains(IND_S) && l.contains(IND_T)) {
                toAdd = IND_BOTH;
            } else if (l.contains(IND_S)) {
                toAdd = IND_SUBJ;
            } else if (l.contains(IND_T)) {
                toAdd = IND_TIME;
            }
            List<VariableDefinition> vars = lexer.getLocalVariables();
            for (VariableDefinition v : vars) {
                String fromLabel = doDerivativeDependentVariable(v); // gets the variable assignement 
                String toLabel = doDerivativeDependentVariable(v) + toAdd;
                l = l.replace(fromLabel, toLabel);
            }
            newLines.add(l);
        }
        return newLines;
    }

    private void doInterpLine(String oldSymbol, String newSymbol) {
        if (parVariablesFromMap.containsKey(oldSymbol)) {// lugio per sistemare UC2
            return;
        }
        covToNotRemove.add(oldSymbol);
        String format = "%s " + assignSymbol + " interp.function(%s[ind_subj,ind_t], %s_%s[ind_subj,1:%s%s[ind_subj]], %s[ind_subj,1:%s%s[ind_subj]])";
        variableLines.add(String.format(format, newSymbol, gridLabel, gridLabel, oldSymbol, NtNamePrefix, oldSymbol, oldSymbol, NtNamePrefix, oldSymbol));
    }

    private void doInterpLineCat(String oldSymbol, String newSymbol) {
        covToNotRemove.add(oldSymbol);

        String format = "%s " + assignSymbol + " interp.function.cost(%s[ind_subj,ind_t], %s_%s[ind_subj,1:%s%s[ind_subj]], %s[ind_subj,1:%s%s[ind_subj]])";
        variableLines.add(String.format(format, newSymbol, gridLabel, gridLabel, oldSymbol, NtNamePrefix, oldSymbol, oldSymbol, NtNamePrefix, oldSymbol));
    }

    private String doMeanTransformation(String errorTransformation, String meanTmpName, String rhsMeanExpression, String formatErrTransf) {
        if (errorTransformation.trim().length() > 0) {
            meanTmpName = errorTransformation + meanTmpName;
            rhsMeanExpression = String.format(formatErrTransf, errorTransformation, rhsMeanExpression.trim()); // aggiungo \n perchè prima faccio un trim()
        }
        String formatDistMeanAssignement = Util.clean(meanTmpName) + "%s %s %s";
        String brackets = getIndexes(rhsMeanExpression) == NO_INDEX ? "" : getBrackets(rhsMeanExpression);
        String meanAssignStmt = String.format(formatDistMeanAssignement, brackets, assignSymbol, rhsMeanExpression);
        return meanAssignStmt;
    }

    private String getVarAssign(NormalDistribution value) {
        String varianceAssign;
        if (value.getVariance() != null) {
            varianceAssign = getVariance(value.getVariance());
        } else {
            String std = "";
            if (value.getStddev().getPrVal() != null) {
                std = value.getStddev().getPrVal().toString();
            } else if (value.getStddev().getVar() != null) {
                std = delimit(value.getStddev().getVar().getVarId());
            }
            varianceAssign = String.format("pow(%s,2)", std);
        }
        return delimit(varianceAssign);
    }

    private String getLinkedVarAssign(NormalDistribution value) {
        String varianceAssign;
        if (value.getVariance() != null) {
            varianceAssign = getVariance(value.getVariance());
        } else {
            String std = "";
            if (value.getStddev().getPrVal() != null) {
                std = value.getStddev().getPrVal().toString();
            } else if (value.getStddev().getVar() != null) {
                std = delimit(value.getStddev().getVar().getVarId());
            }
            varianceAssign = String.format("pow(%s,2)", std);
        }
        return delimit(varianceAssign);
    }

    private String doMeanLabel(String errorTransformation, String rhsMeanExpression, String meanTmpName) {
        String brackets = getIndexes(rhsMeanExpression) == NO_INDEX ? "" : getBrackets(rhsMeanExpression);
        return errorTransformation + meanTmpName + brackets;
    }

    private String doPar1Label(String errorTransformation, String rhsMeanExpression, String meanTmpName) {
        String brackets = getIndexes(rhsMeanExpression) == NO_INDEX ? "" : getBrackets(rhsMeanExpression);
        return errorTransformation + meanTmpName + brackets;
    }

    private String checkTransformation(String errorTransformation) {
        if (errorTransformation.trim().length() > 0) {
            switch (errorTransformation) {
                case "identity":
                    return "";
                case "log":
                case "logit":
                    return errorTransformation;
                default:
                    throw new UnsupportedOperationException(errorTransformation.trim() + " transformation " + " not allowed");
            }
        }
        return "";
    }

    void updateUniopMap() {
        String oK, oV, nV;
        for (int i = 0; i < uniOpList.size(); i++) {
            oK = uniOpList.get(i);
            oV = unaryOpMap.get(oK);
            for (int j = i + 1; j < uniOpList.size(); j++) { // aggiorno la parte rimanente della mappa sostituendo nei valori gli elementi precendenti
                nV = unaryOpMap.get(uniOpList.get(j)).replace(oK, oV);
                unaryOpMap.put(uniOpList.get(j), nV);
            }
        }
    }

    void updateBinopMap(List<String> formalPar, List<String> actualPar) {
        Map<String, String> toAdd = new HashMap<>();
        for (Map.Entry pairs : binOpMap.entrySet()) {
            if (checkFormal(formalPar, pairs.getKey().toString())) {
                String k = pairs.getKey().toString();
                String v = pairs.getValue().toString();
                for (int j = 0; j < formalPar.size(); j++) {
                    k = k.replace(formalPar.get(j), actualPar.get(j));
                    v = v.replace(formalPar.get(j), actualPar.get(j));
                }
                toAdd.put(delimit(k), delimit(v));
            }

        }
        binOpMap.putAll(toAdd);
    }

    private boolean checkFormal(List<String> formalPar, String k) {
        for (String s : formalPar) {
            if (k.contains(s)) {
                return true;
            }
        }
        return false;
    }

    protected String pascalNamesTransform(String in) {
        if (in.trim().length() == 0) {
            return "";
        }
        String out = delimit(removeIndexes(in));
        String newId;
        for (int i = uniOpList.size() - 1; i >= 0; i--) {
            if (out.contains(delimit(uniOpList.get(i)))) {
                out = out.replace(delimit(uniOpList.get(i)), delimit(unaryOpMap.get(uniOpList.get(i))));
            }
        }

        if (out.contains(delimit(gridLabel))) {
            if (hasDiffEquations && !inPW) {
                out = out.replace(gridLabel, "t");
            } else {
                out = out.replace(gridLabel, "T");
            }
        }
        for (Map.Entry pairs : binOpMap.entrySet()) {
            if (out.contains(delimit(pairs.getKey().toString()))) {
                out = out.replace(delimit(pairs.getKey().toString()), delimit(pairs.getValue().toString()));
            }
        }

        for (Map.Entry pairs : unaryOpMap.entrySet()) {
            if (out.contains(delimit(pairs.getKey().toString()))) {
                out = out.replace(delimit(pairs.getKey().toString()), delimit(pairs.getValue().toString()));
            }
        }

        for (SymbolRef s : stateV_Parameters) {
            if (!isIn(s, theta_Parameters) && !isCovariate(s)
                    && out.contains(delimit(s.getSymbIdRef()))) {
                if (parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                    if (!isDerivativeVar(s.getSymbIdRef())) {
                        newId = delimit(s.getSymbIdRef() + upperSuffixDerDepLabel);
                        out = out.replaceAll(delimit(s.getSymbIdRef()), delimit(newId));
                    }
                }
            }
        }

        for (SymbolRef s : leafPiecewiseParameters) {
            if (parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                if (!isDerivativeVar(s.getSymbIdRef())) {
                    newId = delimit(s.getSymbIdRef() + upperSuffixDerDepLabel);
                    out = out.replaceAll(delimit(s.getSymbIdRef()), delimit(newId));
                } else {
                    newId = derivativeMapNew.get(s.getSymbIdRef());
                    out = out.replaceAll(delimit(s.getSymbIdRef() + upperSuffixDerDepLabel), delimit(newId));
                    out = out.replaceAll(delimit(s.getSymbIdRef()), delimit(newId));
                }
            }
        }

        for (SymbolRef s : indivV_Parameters) {
            if (!isIn(s, theta_Parameters) && !isCovariate(s)) {
                if (out.contains(delimit(s.getSymbIdRef()))
                        && parVariablesFromMap.containsKey(s.getSymbIdRef())
                        && !isPiecewiseVar(s.getSymbIdRef())) {
                    out = out.replaceAll(delimit(s.getSymbIdRef()), delimit(s.getSymbIdRef() + upperSuffixDerDepLabel));
                }
            }
        }

        for (Map.Entry pairs : derivativeMapNew.entrySet()) {
            if (out.contains(delimit(pairs.getKey().toString()))) {
                if (this instanceof PascalParser1) {
                    out = out.replace(delimit(pairs.getKey().toString()), delimit(derivativeMap.get(pairs.getKey()).toString()));
                } else {
                    out = out.replace(delimit(pairs.getKey().toString()), delimit(pairs.getValue().toString()));
                }
            }
        }
        for (SymbolRef s : leafOdeParameters) {
            if (!isIn(s, theta_Parameters)
                    && !isCovariate(s)) {
                if (!isPiecewiseVar(s.getSymbIdRef())) {
                    out = out.replaceAll(delimit(s.getSymbIdRef()), doAppendSuffix(s));
                } else {
                    out = out.replaceAll(delimit(s.getSymbIdRef()), piecewiseSuffix + "_" + s.getSymbIdRef());
                }
            } else {
                if (!isIndependentVariableSym(s.getSymbIdRef()) || !hasDiffEquations) {
                    out = out.replaceAll(delimit(s.getSymbIdRef()), s.getSymbIdRef());
                }
            }
        }

        out = out.replaceAll(interpSuffix, "");
        out = out.replaceAll("_" + upperSuffixDerDepLabel, "");
        if (parVariablesFromMap.containsKey(Util.clean(out))) {
            out = delimit(out.replace(delimit(Util.clean(out)), delimit(Util.clean(out) + upperSuffixDerDepLabel)));
        }
        for (String dv : doseVar) {
            if (out.contains(delimit(dv + "[1]"))) {
                out = out.replace(delimit(dv + "[1]"), delimit(dv));
            }
        }
        return removeIndexes(out);
    }

    public List<String> removeIndexes(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s.trim().length() > 0) {
                out.add(removeIndexes(s));
            }
        }
        return out;
    }

    protected String removeIndexesNew(String in) {
        String out = in;
        while (out.contains(leftArrayBracket) && out.contains(rightArrayBracket)) {
            out = out.substring(0, out.indexOf(leftArrayBracket)) + out.substring(out.indexOf(rightArrayBracket) + 1);
        }
        return out;
    }

    protected String removeIndexes(String in) {
        String out = in;
        if (!out.contains(leftArrayBracket)) {
            return out;
        }
        if (out.contains(IND_BOTH)
                || out.contains(IND_BOTH2)) {
            out = out.replace(IND_BOTH, "");
            out = out.replace(IND_BOTH2, "");
        }
        if (out.contains(IND_TIME)
                || in.contains(IND_TIME2)) {
            out = out.replace(IND_TIME, "");
            out = out.replace(IND_TIME2, "");
        }
        if (out.contains(IND_SUBJ)) {
            out = out.replace(IND_SUBJ, "");
        }
        out = out.replace(IND_S + ",", "");
        return out;
    }

    protected String getModelFunctionName(StructuralBlock sb) {
        String format = lexer.getDom().getName().getValue();
        return String.format(format);

    }

    protected String concat(List<String> lines, String tabs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + tabs);
        for (String s : lines) {
            sb.append(s);
            sb.append(tabs);
        }
        return sb.toString();
    }

    protected String concat(String sep, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s);
            sb.append(sep);
        }
        return sb.toString();
    }

    protected String concat(Map<String, String> lines, String tabs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + tabs);
        for (Map.Entry<String, String> s : lines.entrySet()) {
            sb.append(s.getValue());
            sb.append(tabs);
        }
        return sb.toString();
    }

    protected String concat(List<SymbolRef> lines) {
        List<String> names = new ArrayList();

        for (SymbolRef s : lines) {
            names.add(s.getSymbIdRef());
        }
        Arrays.sort(names.toArray());
        return concat(names, " ");
    }

    protected String concatVars(List<VariableDefinition> lines) {
        List<String> names = new ArrayList();

        for (VariableDefinition s : lines) {
            names.add(s.getSymbId());
        }
        Arrays.sort(names.toArray());
        return concat(names, " ");
    }

    protected String concatSblocks(List<StructuralBlock> lines) {
        List<String> names = new ArrayList();

        for (StructuralBlock s : lines) {
            for (VariableDefinition s0 : s.getLocalVariables()) {
                names.add(s0.getSymbId());
            }
        }
        Arrays.sort(names.toArray());
        return concat(names, " ");
    }

    private void addVar(String[] c, List<List<String>> g) {
        boolean inserted = false;
        for (List<String> l : g) {
            if (isInList(l, c[0])) {
                if (!isInList(l, c[1])) {
                    l.add(c[1]);
                }
                inserted = true;
                break;
            }
            if (isInList(l, c[1])) {
                if (!isInList(l, c[0])) {
                    l.add(c[0]);
                }
                inserted = true;
                break;
            }
        }
        if (!inserted) {
            List<String> nl = new ArrayList();
            nl.add(c[0]);
            nl.add(c[1]);
            g.add(nl);
        }
    }

    private void manageCorrelations(ParameterBlock pb) {
        List<CorrelationRef> corr = pb.getCorrelations();
        List<String> vars = new ArrayList<>();
        String id;
        for (CorrelationRef cr : corr) {
            id = cr.rnd1.getSymbId();
            if (!vars.contains(id)) {
                vars.add(id);
            }
            id = cr.rnd2.getSymbId();
            if (!vars.contains(id)) {
                vars.add(id);
            }
        }
        int dim = vars.size();
        String matrix[][] = new String[dim][dim];

        for (int i = 0; i < dim; i++) {
            for (int j = i + 1; j < dim; j++) {
                matrix[i][j] = matrix[j][i] = getCorr(vars.get(i), vars.get(j), corr);
            }
        }
        for (int i = 0; i < dim; i++) {
            matrix[i][i] = Util.clean(getRandomVarVariance((vars.get(i))));
        }
        List<List<String>> groups = new ArrayList<>();
        String couple[] = new String[2];
        for (CorrelationRef cr : corr) {
            couple[0] = cr.rnd1.getSymbId();
            couple[1] = cr.rnd2.getSymbId();
            addVar(couple, groups);
        }

        for (List<String> l : groups) {
            l = Util.getUniqueString(l);
        }

        groups = mergeGroups(groups);
        for (List<String> l : groups) {
            l = Util.getUniqueString(l);
        }

        generateEtaOmegaMatrix(vars, matrix, groups);
    }

    private List<List<String>> mergeGroups(List<List<String>> g) {
        List<List<String>> tmp = new ArrayList<>();
        List<List<String>> tmp0 = new ArrayList<>();
        List<List<String>> toRemove = new ArrayList<>();
        tmp0.addAll(g);
        for (int i = 0; i < g.size(); i++) {
            for (int j = i + 1; j < g.size(); j++) {
                if (intersection(g.get(i), g.get(j))) {
                    tmp.add(Util.mergeStrings(g.get(i), g.get(j)));
                    toRemove.add(g.get(i));
                    toRemove.add(g.get(j));
                }
            }
        }
        tmp0.removeAll(toRemove);
        for (List<String> s : tmp0) {
            tmp.add(s);
        }
        return tmp;
    }

    private boolean intersection(List<String> a1, List<String> a2) {
        boolean flag = false;
        for (String s : a1) {
            if (isInList(a2, s)) {
                return true;
            }
        }
        return false;
    }

    private void generateEtaOmegaMatrix(List<String> vars, String[][] matrix, List<List<String>> groups) {
        int num, j;
        for (int i = 1; i <= groups.size(); i++) {
            List<String> cor = groups.get(i - 1);
            j = 1;
            num = cor.size();
            correlationLines.add(String.format("%s[%s,1:%s] %s dmnorm(%s[], %s[,])",
                    etaName + i, IND_S, num, dist_symb, muName + i, tName + i));
            correlationLines.add(String.format("%s[1:%s,1:%s] %s inverse(%s[,])",
                    tName + i, num, num, assignSymbol, omegaName + i));
            for (String s : cor) {
                String left, right;
                left = delimit(String.format("%s[%s]", s, IND_S));
                right = delimit(String.format("%s[%s,%s]", etaName + i,
                        IND_S, "" + j));
                correlationLines.add(String.format("%s %s %s", delimit(left), assignSymbol, right));
                variablesAssignementMap.put(delimit(s), delimit(left));
                double mean = getRandomVarMean(s);
                correlationLines.add(String.format("%s[%s] %s %s", muName + i, "" + j, assignSymbol, "" + mean));
                j++;
            }
            generateOmega(matrix, omegaName + i, cor, vars);
        }

    }

    private double getRandomVarMean(String id) { // 0.7.3
        ParameterRandomVariable context = getLinkedRandomVar(id);
        if (context.getDistribution().getUncertML() != null) {
            PharmMLElement distribution = Util.getDistributionType(context);
            if (distribution instanceof NormalDistribution) {
                if (((NormalDistribution) distribution).getMean() != null) {
                    return ((NormalDistribution) distribution).getMean().getRVal();
                }
            }
        } else if (context.getDistribution().getProbOnto() != null) {
            DistributionParameter dp = context.getDistribution().getProbOnto().getParameter(ParameterName.MEAN);
            return Double.parseDouble(dp.getAssign().getScalar().valueToString());
        }
        return -99999;
    }

    private String getRandomVarVariance(String id) {
        ParameterRandomVariable context = getLinkedRandomVar(id);
        String varianceAssign = "";
        PharmMLElement distribution = Util.getDistributionType(context);
        if (distribution instanceof NormalDistribution && ((NormalDistribution) distribution).getVariance() != null) {
            BinaryTree bt = lexer.getStatement(((NormalDistribution) distribution).getVariance());
            varianceAssign = parse(((NormalDistribution) distribution).getVariance(), bt);
        } else if (distribution instanceof NormalDistribution && ((NormalDistribution) distribution).getStddev() != null) {
            BinaryTree bt = lexer.getStatement(((NormalDistribution) distribution).getStddev());
            varianceAssign = parse(((NormalDistribution) distribution).getStddev(), bt);
            varianceAssign = "pow(" + varianceAssign + ",2)";
        } else if (distribution instanceof ProbOnto) {
            ProbOnto pb = ((ProbOnto) distribution);
            DistributionParameter dp = null;

            if (pb.getName().value().equals("Normal1")) {
                dp = pb.getParameter(ParameterName.STDEV);
                varianceAssign = delimit(String.format("pow(%s,2)", getParameter(dp)));
            } else if (pb.getName().value().equals("Normal2")) {
                dp = pb.getParameter(ParameterName.VAR);
                varianceAssign = delimit(getParameter(dp));
            } else if (pb.getName().value().equals("Normal3")) {
                dp = pb.getParameter(ParameterName.PRECISION);
                varianceAssign = delimit(String.format("(1/(%s))", getParameter(dp)));
            } else {
                throw new RuntimeException("Distribution not supported");
            }
        }
        return varianceAssign;
    }

    private void generateOmega(String matrix[][], String omegaN, List<String> group, List<String> vars) {
        int i = 1;
        int j = 1;
        int num = group.size();
        for (i = 1; i <= num; i++) {
            for (j = 1; j <= num; j++) {
                int im = Util.getCorrVarIndex(group.get(i - 1), vars);
                int jm = Util.getCorrVarIndex(group.get(j - 1), vars);
                correlationLines.add(String.format("%s[%s,%s] %s %s",
                        omegaN, "" + i, "" + j, assignSymbol,
                        matrix[im][jm]));
            }
        }
    }

    private ParameterRandomVariable getLinkedRandomVar(String symbol) {
        if (symbol != null) {
            List<ParameterBlock> pbs = getParameterBlocks();
            for (ParameterBlock pb : pbs) {
                List<ParameterRandomVariable> rvs = pb.getLinkedRandomVariables();

                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return rv;
                    }
                }
            }
            List<ObservationBlock> obs = lexer.getObservationBlocks();
            for (ObservationBlock ob : obs) {
                List<ParameterRandomVariable> rvs = ob.getRandomVariables();
                for (ParameterRandomVariable rv : rvs) {
                    if (symbol.equals(rv.getSymbId())) {
                        return rv;
                    }
                }
            }
        }
        return null;
    }

    private VariableDefinition getVariable(String symbol) {
        if (symbol != null) {
            if (!lexer.getStructuralBlocks().isEmpty()) {
                List<StructuralBlock> sbs = lexer.getStructuralBlocks();
                for (StructuralBlock sb : sbs) {
                    if (sb != null) {
                        List<VariableDefinition> ips = sb.getLocalVariables();
                        for (VariableDefinition ip : ips) {
                            if (ip == null) {
                                continue;
                            }
                            String currentSymbol = ip.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol)) {
                                return ip;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private PopulationParameter getPopulationParameter(String symbol) {
        if (symbol != null) {
            if (!lexer.getStructuralBlocks().isEmpty()) {
                ParameterBlock sb = lexer.getParameterBlock();//                for (ParameterBlock sb : sbs) {
                if (sb != null) {
                    List<PopulationParameter> ips = sb.getParameters();
                    for (PopulationParameter ip : ips) {
                        if (ip == null) {
                            continue;
                        }
                        String currentSymbol = ip.getSymbId();
                        if (currentSymbol == null) {
                            continue;
                        }
                        if (currentSymbol.equals(symbol)) {
                            return ip;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected Piecewise getPiecewise(String symbol) {
        if (symbol != null) {
            if (!lexer.getStructuralBlocks().isEmpty()) {
                List<StructuralBlock> sbs = lexer.getStructuralBlocks();
                for (StructuralBlock sb : sbs) {
                    if (sb != null) {
                        List<VariableDefinition> ips = sb.getLocalVariables();
                        for (VariableDefinition ip : ips) {
                            if (ip == null) {
                                continue;
                            }
                            String currentSymbol = ip.getSymbId();
                            if (currentSymbol == null) {
                                continue;
                            }
                            if (currentSymbol.equals(symbol)) {
                                if (ip.getAssign().getPiecewise() != null) {
                                    return ip.getAssign().getPiecewise();
                                }
                            }
                        }
                    }
                }
            }

            if (!lexer.getStructuralBlocks().isEmpty()) {
                ParameterBlock sb = lexer.getParameterBlock();
                if (sb != null) {
                    List<PopulationParameter> ips = sb.getParameters();
                    for (PopulationParameter ip : ips) {
                        if (ip == null) {
                            continue;
                        }
                        String currentSymbol = ip.getSymbId();
                        if (currentSymbol == null) {
                            continue;
                        }
                        if (currentSymbol.equals(symbol)) {
                            if (ip.getAssign().getPiecewise() != null) {
                                return ip.getAssign().getPiecewise();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getCorr(String get, String get0, List<CorrelationRef> corr) {
        String c = "0";
        for (CorrelationRef cr : corr) {
            if (cr.rnd1.getSymbId().equals(get) && cr.rnd2.getSymbId().equals(get0)
                    || cr.rnd1.getSymbId().equals(get0) && cr.rnd2.getSymbId().equals(get)) {
                if (cr.correlationCoefficient != null) {
                    BinaryTree bt = lexer.getTreeMaker().newInstance(cr.correlationCoefficient);
                    c = parse(cr.correlationCoefficient, bt) + "*" + getStdDev(cr.rnd1) + "*" + getStdDev(cr.rnd2);
                } else if (cr.covariance != null) {
                    BinaryTree bt = lexer.getTreeMaker().newInstance(cr.covariance);
                    c = parse(cr.covariance, bt);
                }
            }
        }
        return c;
    }

    String getStdDev(ParameterRandomVariable v) {
        String std = "";
        BinaryTree tmpstmt = lexer.getStatement(v);

        if (v.getDistribution().getUncertML() != null
                && v.getDistribution().getUncertML().getAbstractContinuousUnivariateDistribution() != null) {
            AbstractContinuousUnivariateDistributionType val = v.getDistribution().getUncertML().getAbstractContinuousUnivariateDistribution().getValue();
            if (val instanceof NormalDistribution) {
                NormalDistribution nDist = (NormalDistribution) val;
                if (nDist.getStddev() != null) {
                    if (nDist.getStddev().getPrVal() != null) {
                        std = nDist.getStddev().getPrVal().toString();
                    } else if (nDist.getStddev().getVar() != null) {
                        std = delimit(nDist.getStddev().getVar().getVarId());
                    }
                } else if (nDist.getVariance() != null) {
                    if (nDist.getVariance().getPrVal() != null) {
                        std = String.format("sqrt(%s)", nDist.getVariance().getPrVal().toString());
                    } else if (nDist.getVariance().getVar() != null) {
                        std = String.format("sqrt(%s)", nDist.getVariance().getVar().getVarId());
                    }
                }
            }
        } else if (v.getDistribution().getProbOnto() != null) {
            ProbOnto dist = v.getDistribution().getProbOnto();
            String name = dist.getName().value();
            DistributionParameter dp = null;
            if (name.equals("Normal1")) {
                dp = dist.getParameter(ParameterName.STDEV);
                std = delimit(getParameter(dp));
            } else if (name.equals("Normal2")) {
                dp = dist.getParameter(ParameterName.VAR);
                std = delimit(String.format("sqrt(%s)", getParameter(dp)));

            } else if (name.equals("Normal3")) {
                dp = dist.getParameter(ParameterName.PRECISION);
                std = delimit(String.format("1/sqrt(%s)", getParameter(dp)));
            } else {
                throw new RuntimeException("Distribution not supported");
            }
        }
        return std;
    }

    private String getParameter(DistributionParameter dp) {
        String tmp = "";
        if (dp.getAssign().getVector() != null) {
            Vector v = dp.getAssign().getVector();
            int i = 1;
            String id = ((CommonParameter) dp.getParent().getParent().getParent()).getSymbId();
            for (VectorValue el : v.getVectorElements().getListOfElements()) {
                matrixVectorLines.add(String.format("%s_%s[%s] %s %s", id, dp.getName(), i, assignSymbol, el.asString()));
            }
            tmp = id + "_" + dp.getName();
        } else if (dp.getAssign().getMatrix() != null) {
            Matrix v = dp.getAssign().getMatrix();
            String type = v.getMatrixType();
            String id = ((CommonParameter) dp.getParent().getParent().getParent()).getSymbId();
            tmp = id + "_" + dp.getName().value();

            switch (type) {
                case "Any":
                    matrixVectorLines.addAll(getMatrixElementsAny(v, tmp));
                    break;
                case "LowerTriangular":
                    matrixVectorLines.addAll(getMatrixElementsLT(v, tmp));
                    break;
                case "UpperTriangular":
                    matrixVectorLines.addAll(getMatrixElementsUT(v, tmp));
                    break;
            }
            matrixMap.put(id, matrixMap.get(tmp));
        } else {
            tmp = Util.clean(removeIndexesNew(parse(dp, lexer.getTreeMaker().newInstance(dp))));

        }
        return tmp;
    }

    private String getVectorValue(VectorValue dp) {
        return parse(dp, lexer.getTreeMaker().newInstance(dp));
    }

    protected String myParse(Object o) {
        return parse(o, lexer.getTreeMaker().newInstance(o));
    }

    private void manageInitialEstimates() {
        EstimationStep es = lexer.getEstimationStep();
        List<FixedParameter> fpList = es.getFixedParameters();
        for (FixedParameter fp : fpList) {
            BinaryTree bt = lexer.getTreeMaker().newInstance(fp.pe.getInitialEstimate());
            initialEstimatesLines.add(String.format("%s %s %s\n", fp.pe.getSymbRef().getSymbIdRef(), assignSymbol, parse(fp.pe.getInitialEstimate(), bt)));
        }
    }

    private String getPiecewiseName(String pid) {
        String format = "%s%s[%s,%s]";
        String pid1 = removeIndexes(pid);
        String tmp = "";
        int ind = 1;
        tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(Util.clean(removeIndexes(pid))), IND_S, IND_T);
        return tmp;
    }

    protected BinaryTree doPutCategory(BinaryTree bt) {
        SymbolRef sr = null;
        CovariateDefinition cd;
        boolean isCategorical = false;
        for (Node node : bt.nodes) {
            if (node.data instanceof SymbolRef) {
                sr = (SymbolRef) node.data;
                if ((cd = getCovariate(sr.getSymbIdRef())) != null) {
                    if (cd.getCategorical() != null) {
                        isCategorical = true;
                        break;
                    }
                }
            }
        }
        if (sr != null && isCategorical) {
            List<CategoryRef_> lc = categoricalMap.get(sr.getSymbIdRef());
            if (lc != null) {
                CategoryRef cr;
                for (Node node : bt.nodes) {
                    if (node.data instanceof CategoryRef) {
                        cr = (CategoryRef) node.data;
                        for (CategoryRef_ val : lc) {
                            if (val.getModelSymbol().equals(cr.getCatIdRef())) {
                                node.data = val.getDataSymbol();
                            }
                        }
                    }
                }
            }
        }
        return bt;
    }

    private static SymbolRef symbolRef(PharmMLRootType o, Accessor a) {
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
        } else if (PharmMLTypeChecker.isCovariate(o)) {
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
        } else if (PharmMLTypeChecker.isContinuousCovariate(o)) {
            ContinuousCovariate ccov = (ContinuousCovariate) o;
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

    protected static void assign(Rhs rhs, Object o, Accessor a) {
        if (rhs == null || o == null) {
            return;
        }

        if (isConstant(o)) {
            rhs.setConstant((Constant) o);
        } else if (isLocalVariable(o)) {
            rhs.setSymbRef(symbolRef((VariableDefinition) o, a));
        } else if (PharmMLTypeChecker.isPopulationParameter(o)) {
            rhs.setSymbRef(symbolRef((PopulationParameter) o, a));
        } else if (isBinaryOperation(o)) {
            rhs.setBinop((Binop) o);
        } else if (isDerivative(o)) {
            rhs.setSymbRef(symbolRef((DerivativeVariable) o, a));
        } else if (isString_(o)) {
            rhs.setScalar(new StringValue((String) o));
        } else if (isBoolean(o)) {
            Boolean b = (Boolean) o;
            if (b.booleanValue()) {
                rhs.setScalar(new TrueBoolean());
            } else {
                rhs.setScalar(new FalseBoolean());
            }
        } else if (isInteger(o)) {
            rhs.setScalar(new IntValue((Integer) o));
        } else if (isBigInteger(o)) {
            BigInteger v = (BigInteger) o;
            rhs.setScalar(new IntValue(v.intValue()));
        } else if (isDouble(o)) {
            rhs.setScalar(new RealValue((Double) o));
        } else if (isUnaryOperation(o)) {
            rhs.setUniop((Uniop) o);
        } else if (isIndependentVariable(o)) {
            rhs.setSymbRef(symbolRef((IndependentVariable) o, a));
        } else if (isIndividualParameter(o)) {
            rhs.setSymbRef(symbolRef((IndividualParameter) o, a));
        } else if (isFunctionCall(o)) {
            rhs.setFunctionCall((FunctionCallType) o);
        } else if (isScalarInterface(o)) {
            rhs.setScalar((Scalar) o);
        } else if (isPiecewise(o)) {
            rhs.setPiecewise((Piecewise) o);
        } else if (isSymbolReference(o)) {
            rhs.setSymbRef((SymbolRef) o);
        } else if (isDelay(o)) {
            rhs.setDelay((Delay) o);
        } else if (isSum(o)) {
            rhs.setSum((Sum) o);
        } else if (isJAXBElement(o)) {
            JAXBElement<?> element = (JAXBElement<?>) o;
            assign(rhs, element.getValue(), a);
        } else if (isMatrix(o)) {
            rhs.setMatrix((Matrix) o);
        } else if (isProduct(o)) {
            rhs.setProduct((Product) o);
        } else if (isSequence(o)) {
            rhs.setSequence((Sequence) o);
        } else if (isVector(o)) {
            rhs.setVector((Vector) o);
        } else if (isInterval(o)) {
            rhs.setInterval((Interval) o);
        } else if (isInterpolation(o)) {
            rhs.setInterpolation((Interpolation) o);
        } else if (isProbability(o)) {
            rhs.setProbability((Probability) o);
        } else if (isMatrixUnaryOperation(o)) {
            rhs.setMatrixUniop((MatrixUniOp) o);
        } else if (isMatrixSelector(o)) {
            rhs.setMatrixSelector((MatrixSelector) o);
        } else if (isVectorSelector(o)) {
            rhs.setVectorSelector((VectorSelector) o);
        } else if (isRhs(o)) {
            Rhs assign = (Rhs) o;
            assign(rhs, assign.getContent(), a);
        } else {
            throw new UnsupportedOperationException("Unsupported Expression Term (value='" + o + "')");
        }
    }

    protected static Rhs rhs(Object o, Accessor a) {
        if (o == null) {
            throw new NullPointerException("Expression Class NULL");
        }
        Rhs rhs = new Rhs();
        assign(rhs, o, a);
        return rhs;
    }

    protected String generatePiecewiseBlock(Piecewise pw, String symbId) {
        String symbol = unassigned_symbol;

        List<Piece> pieces = pw.getListOfPiece();
        Piece else_block = null;
        BinaryTree[] assignment_trees = new BinaryTree[pieces.size()];
        BinaryTree[] conditional_trees = new BinaryTree[pieces.size()];
        String[] conditional_stmts = new String[pieces.size()];
        String[] assignment_stmts = new String[pieces.size()];

        TreeMaker tm = lexer.getTreeMaker();
        Accessor a = lexer.getAccessor();

        int assignment_count = 0, else_index = -1;
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece != null) {
                Condition cond = piece.getCondition();
                if (cond != null) {
                    conditional_trees[i] = tm.newInstance(piece.getCondition());
                    conditional_trees[i] = doPutCategory(conditional_trees[i]);
                    if (cond.getOtherwise() != null) {
                        else_block = piece;
                        else_index = i;
                    }
                }

                BinaryTree assignment_tree = null;
                ExpressionValue expr = piece.getValue();
                if (isBinaryOperation(expr)) {

                    assignment_tree = tm.newInstance(rhs((Binop) expr, a));
                    assignment_count++;
                } else if (isConstant(expr)) {
                    assignment_tree = tm.newInstance(expr);
                    assignment_count++;
                } else if (isFunctionCall(expr)) {
                    assignment_tree = tm.newInstance(rhs((FunctionCallType) expr, a));
                    assignment_count++;
                } else if (isJAXBElement(expr)) {
                    assignment_tree = tm.newInstance(rhs((JAXBElement<?>) expr, a));
                    assignment_count++;
                } else if (isSymbolReference(expr)) {
                    assignment_tree = tm.newInstance(expr);
                    assignment_count++;
                } else if (isScalarInterface(expr)) {
                    assignment_tree = tm.newInstance(expr);
                    assignment_count++;
                } else {
                    throw new IllegalStateException("Piecewise assignment failed (expr='" + expr + "')");
                }

                if (assignment_tree != null) {
                    assignment_trees[i] = assignment_tree;
                }
            }
        }

        if (assignment_count == 0) {
            throw new IllegalStateException("A piecewise block has no assignment statements.");
        }

        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);

            if (conditional_trees[i] != null && assignment_trees[i] != null) {
                // Logical condition
                if (!piece.equals(else_block)) {
                    conditional_stmts[i] = parse(piece, conditional_trees[i]);
                    conditional_stmts[i] = pascalNamesTransform(conditional_stmts[i]); // tolto Util.clean
                }
                assignment_stmts[i] = parse(new Object(), assignment_trees[i]);
                assignment_stmts[i] = pascalNamesTransform(assignment_stmts[i]);
            }
        }

        int block_assignment = 0;
        StringBuilder block = new StringBuilder("");
        field_tag = "value";
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece == null) {
                continue;
            } else if (piece.equals(else_block)) {
                continue;
            }

            if (!(conditional_stmts[i] != null && assignment_stmts[i] != null)) {
                continue;
            }
            String operator = "IF", format = "%s %s THEN;\n\t\t\t%s := %s; \n";
            if (block_assignment > 0) {
                operator = "ELSIF ";
                format = "\t\t%s (%s) THEN; \n\t\t\t%s := %s; \n";
            }

            if (inPW) {
                block.append(String.format(format, operator, conditional_stmts[i], field_tag, assignment_stmts[i]));
            } else {
                block.append(String.format(format, operator, conditional_stmts[i], piecewiseSuffix + "_" + symbId, assignment_stmts[i]));
            }
            block_assignment++;
        }

        if (else_block != null && else_index >= 0) {
            block.append("\t\tELSE;\n");
            String format = "\t\t\t%s := %s;\n";
            if (inPW) {
                block.append(String.format(format, field_tag, assignment_stmts[else_index]));
            } else {
                block.append(String.format(format, piecewiseSuffix + "_" + symbId, assignment_stmts[else_index]));
            }
        }
        block.append("\t\tEND;\n");
        if (assignment_count == 0) {
            throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
        }
        symbol = block.toString();
        symbol = pascalNamesTransform(symbol);
        return symbol;
    }

    String getName(String in) {

        if (in.endsWith(upperSuffixDerDepLabel)) {
            return in.substring(0, in.indexOf(upperSuffixDerDepLabel));
        } else if (in.startsWith(piecePrefix)) {
            return in.substring(piecePrefix.length());
        } else if (in.endsWith(interpSuffix)) {
            return in.substring(0, in.indexOf(interpSuffix));
        } else {
            return in;
        }
    }

    protected List<String> getSortedOde() {
        List<String> sortedIds = new ArrayList<>();
        sortedIds.addAll(Util.getList(theta_Parameters));
        sortedIds.addAll(derivativeMapNew.values());
        sortedIds.addAll(Util.getList(usedOdeCatCovNames));
        sortedIds.addAll(Util.getList(usedOdeContCovNames));
        return sortedIds;
    }

    protected List<String> getSortedPiecewise(String pwId) {
        List<String> sortedIds = new ArrayList<>();

        for (Map.Entry<Integer, String> s : piecewiseVarToPascal.get(pwId).entrySet()) {
            sortedIds.add(getName(s.getValue()));
        }

        return sortedIds;
    }

    protected Map<String, String> checkEquations(Map<String, String> eqLines, List<String> sortedIds) {
        Map<String, String> tmpEq = new HashMap<>();
        for (String var : eqLines.keySet()) {
            if (parVariablesFromMap.get(var) != null) {
                List<SymbolRef> depV = parVariablesFromMap.get(var);
                for (SymbolRef v : depV) {
                    if (eqLines.keySet().contains(v.getSymbIdRef())
                            || tmpEq.keySet().contains(v.getSymbIdRef())
                            || parVariablesFromMap.get(v.getSymbIdRef()) == null
                            || isInList(Util.getList(theta_Parameters), v.getSymbIdRef())) {
                        continue;
                    } else {
                        if (isContinuousCovariate(v) || isTransformedCovariate(v)) {
                            String id, eq;
                            id = v.getSymbIdRef();
                            Object o = lexer.getAccessor().fetchElement(id);
                            if (o instanceof TransformedCovariate) {
                                TransformedCovariate tc = (TransformedCovariate) o;
                                eq = String.format("%s := %s;\n", id + upperSuffixDerDepLabel, pascalNamesTransform(myParse(tc.getParent())));
                                tmpEq.put(id, eq);
                            } else {
                                CovariateDefinition tc = (CovariateDefinition) o;
                                eq = String.format("%s := %s;\n", id + upperSuffixDerDepLabel, pascalNamesTransform(myParse(tc)));
                                tmpEq.put(id, eq);
                            }

                        }
                    }
                }
            }
        }
        eqLines.putAll(tmpEq);
        return eqLines;
    }

    protected List<String> sortVarLines(Map<String, String> list, List<String> sortedIds) {
        list = checkEquations(list, sortedIds);
        List<String> toRemove = new ArrayList<>();

        for (String key : list.keySet()) {
            if (sortedIds.contains(key)) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            list.remove(key);
        }
        toRemove.clear();
        List<String> sortedEq = new ArrayList<>();
        String var;

        while (!list.isEmpty()) {
            toRemove.clear();
            for (Map.Entry<String, String> pair : list.entrySet()) {
                var = getName(pair.getKey());
                if (!sortedIds.contains(pair.getKey())) {
                    if (!parVariablesFromMap.containsKey(var)) {
                        sortedEq.add(pair.getValue());
                        sortedIds.add(var);
                        toRemove.add(pair.getKey());
                    } else {
                        List<String> lf = Util.getList(parVariablesFromMap.get(var));
                        int i = 0;
                        for (String s : lf) {
                            if (sortedIds.contains(s) || isIndependentVariableSym(s) || isDerivativeVar(s)) {
                                i++;
                            }
                        }
                        if (i == lf.size()) {
                            sortedEq.add(pair.getValue());
                            sortedIds.add(var);
                            toRemove.add(pair.getKey());
                        }
                    }
                }
            }
            boolean removed = false;
            for (String el : toRemove) {
                list.remove(el);
                removed = true;
            }
            if (!removed) {
                throw new UnsupportedOperationException("Equations loop!");
            }
        }
        return sortedEq;
    }

    protected List<String> sortVarLines(Map<String, String> list) {
        List<String> out = new ArrayList<>();
        List<String> sortedIds = new ArrayList<>();
        Set<String> inputId = list.keySet();
        List<String> sortedEq = new ArrayList<>();
        boolean cont = true;
        boolean ended = true;
        int n, i;
        String var;
        while (ended) {
            ended = false;
            for (Map.Entry<String, String> pair : list.entrySet()) {
                var = getName(pair.getKey());
                if (!sortedIds.contains(var)) {
                    if (!parVariablesFromMap.containsKey(var)) {
                        sortedEq.add(pair.getValue());
                        sortedIds.add(var);
                    } else {

                        for (SymbolRef leaf : parVariablesFromMap.get(var)) {
                            if (!parVariablesFromMap.containsKey(var)) {
                                sortedEq.add(pair.getValue());
                                sortedIds.add(var);
                            }
                        }

                    }
                }
            }
            if (sortedEq.size() != list.entrySet().size()) {
                ended = true;
            }
        }
        for (String s : sortedEq) {
            out.add(list.get(s));
        }
        return out;
    }

    protected List<String> sortVarLinesOld(Map<String, String> list) {
        List<String> toRemove = new ArrayList<>();
        List<String> sortedIds = new ArrayList<>();
        sortedIds.addAll(Util.getList(theta_Parameters));
        sortedIds.addAll(derivativeMapNew.values());
        sortedIds.addAll(Util.getList(usedOdeCatCovNames));
        sortedIds.addAll(Util.getList(usedOdeContCovNames));

        Set<String> inputId = list.keySet();
        for (String key : list.keySet()) {
            if (sortedIds.contains(key)) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            list.remove(key);
        }
        toRemove.clear();
        List<String> sortedEq = new ArrayList<>();
        String var;

        while (!list.isEmpty()) {
            toRemove.clear();
            for (Map.Entry<String, String> pair : list.entrySet()) {
                var = getName(pair.getKey());
                if (!sortedIds.contains(pair.getKey())) {
                    if (!parVariablesFromMap.containsKey(var)) {
                        sortedEq.add(pair.getValue());
                        sortedIds.add(var);
                        toRemove.add(pair.getKey());
                    } else {
                        List<String> lf = Util.getList(parVariablesFromMap.get(var));
                        int i = 0;
                        for (String s : lf) {
                            if (sortedIds.contains(s) || isIndependentVariableSym(s) || isDerivativeVar(s)) {
                                i++;
                            }
                        }
                        if (i == lf.size()) {
                            sortedEq.add(pair.getValue());
                            sortedIds.add(var);
                            toRemove.add(pair.getKey());
                        }
                    }
                }
            }
            boolean removed = false;
            for (String el : toRemove) {
                list.remove(el);
                removed = true;
            }
            if (!removed) {
                throw new UnsupportedOperationException("Equations loop!");
            }
        }
        return sortedEq;
    }

    protected List<String> sortVarLinesOLD(Map<String, String> list, Set<String> otherVar) {
        List<String> out = new ArrayList<>();
        List<String> sortedIds = new ArrayList<>();
        Set<String> inputId = list.keySet();
        List<String> sortedEq = new ArrayList<>();
        boolean cont = true;
        boolean flag = true;
        int n, i;
        while (flag) {
            flag = false;
            for (String s : inputId) {
                if (!sortedIds.contains(s)) {
                    if (!parVariablesFromMap.containsKey(s)) {
                        sortedEq.add(s);
                        sortedIds.add(s);
                    } else {
                        n = parVariablesFromMap.get(s).size();
                        i = 0;
                        for (String s0 : Util.getList(parVariablesFromMap.get(s))) {
                            if ((sortedIds.contains(s0)) //                                    || otherVar.contains(covNames)
                                    ) {
                                i++;
                            } else if (!inputId.contains(s0) //                                    || sortedIds.contains(covNames)
                                    ) {
                                sortedIds.add(s0);
                                i++;
                            }
                        }
                        if (i == parVariablesFromMap.get(s).size()) {
                            sortedEq.add(s);
                            sortedIds.add(s);
                        }
                    }
                }
            }
            if (sortedEq.size() != list.entrySet().size()) {
                flag = true;
            }
        }
        for (String s : sortedEq) {
            out.add(list.get(s));
        }
        return out;
    }

    protected List<String> pascalIndivEqGeneration(List<SymbolRef> calcVars) {
        String varLinesEq;
        List<String> varLines = new ArrayList<>();
        String equation = "";
        String current_symbol = "", operand = "";
        String format = "%s " + pascalAssignSymbol + " %s;\n";
        List<String> eqList = Util.mergeStrings(winbugsIndivLines, variableLines);
        for (String s : eqList) {
            if (s != null && s.length() > 0) {

                current_symbol = delimit(s.substring(0, s.indexOf(assignSymbol)).trim());
                if (current_symbol.contains("probit")) {
                    operand = "probit";
                } else if (current_symbol.contains("logit")) {
                    operand = "logit";
                } else if (current_symbol.contains("log")) {
                    operand = "log";
                } else {
                    operand = "";
                }

                if (transfMap.containsKey(current_symbol)) {
                    current_symbol = transfMap.get(current_symbol); // recupero la variabile assegnata
                }
                equation = unaryOperandEqMap.get(current_symbol); // recupero l'equazione
                String symbol = getName(Util.clean(removeIndexes(current_symbol)).trim());
                if (!Util.getList(calcVars).contains(getName(symbol))) {
                    continue;
                }
                String pascal_symbol = delimit(getName(symbol) + upperSuffixDerDepLabel);
                String pascal_equation = "";
                String pascal_operand = "";
                // the derivative variable usedOdeContCovNames are saved in calcVars list to be used in the method winbugsDerivativeDepVarAssignement()
                // derivativeSymbols.add(getDerivativeSymbol((DerivativeVariable) context));
                // the line has to be transformed to substitute true usedOdeContCovNames with vector elements

                String tmp = "";
                tmp = new String(removeIndexes(s.substring(s.indexOf(assignSymbol) + assignSymbol.length(), s.length())));
                if (equation != null) {
                    if (operand.length() > 0) {
                        pascal_equation = doInverseTransformation(operand, pascalIndivMap.get(current_symbol));
                    } else {
                        pascal_equation = pascalIndivMap.get(current_symbol);
                    }
                    // sostituire nome state var
                    pascal_equation = removeIndexes(pascal_equation);
                    if (unaryOperandEqMap.containsKey(current_symbol)) {
                    } else {
                        varLines.add(String.format(format, current_symbol, tmp.trim()));
                    }
                    String eq = String.format("%s " + pascalAssignSymbol + " %s;", pascal_symbol, pascal_equation);
                    pascalIndivLines.add(eq);
                    pascalAssignIndivEqLinesMap.put(Util.clean(removeIndexes(current_symbol)), eq);
                } else {
                    if (pascalIndivMap.get(symbol) != null) {
                        pascal_equation = removeIndexes(pascalIndivMap.get(symbol));
                        if (piecewiseParameters.get(symbol) != null && (isInList(Util.getList(odeParameters), symbol)
                                || isInList(Util.getList(piecewiseParameters.get(symbol)), symbol))) {
                            String eq = String.format("%s " + pascalAssignSymbol + " %s;", pascal_symbol, pascal_equation.trim());
                            pascalIndivLines.add(eq);
                            pascalAssignIndivEqLinesMap.put(Util.clean(removeIndexes(current_symbol)), eq);
                        }
                    } else {
                        PharmMLRootType rt = lexer.getAccessor().fetchElement(symbol);
                        if (rt instanceof CovariateDefinition) {
                            CovariateDefinition covT = (CovariateDefinition) rt;
                            String covTeq = covariateTransformationMap.get(covT);
                            if (covTeq != null) {
                                pascal_equation = removeIndexes(covTeq);
                                if (piecewiseParameters.get(symbol) != null && (isInList(Util.getList(odeParameters), symbol)
                                        || isInList(Util.getList(piecewiseParameters.get(symbol)), symbol))) {
                                    String eq = String.format("%s " + pascalAssignSymbol + " %s;", pascal_symbol, pascal_equation.trim());
                                    pascalIndivLines.add(eq);
                                    pascalAssignIndivEqLinesMap.put(Util.clean(removeIndexes(current_symbol)), eq);
                                }
                            }
                        } else if (rt instanceof TransformedCovariate) {
                            for (Map.Entry<Object, String> ct : covariateTransformationMap.entrySet()) {
                                if (((CovariateTransformation) ct.getKey()).getTransformedCovariate().getSymbId().equals(symbol)) {
                                    pascal_equation = pascalNamesTransform(ct.getValue());
                                }
                            }
                            String eq = String.format("%s " + pascalAssignSymbol + " %s;", pascal_symbol, pascal_equation.trim());
                            pascalIndivLines.add(eq);
                            pascalAssignIndivEqLinesMap.put(Util.clean(removeIndexes(current_symbol)), eq);
                        }
                    }
                }
            }
        }
        return varLines;
    }

    protected String generatePiecewiseBlocks(String pwId) {// 8 aprile 2016
        Map<String, String> completeVarAssignEq = new HashMap<>();
        Set<String> otherVar = new HashSet<>();
        String format = "%s := func.arguments[0][%s].Value();\n\t\t";
        StringBuilder sb1 = new StringBuilder("");
        StringBuilder[] s2 = new StringBuilder[1];
        StringBuilder sb3 = new StringBuilder();
        String varEq = "";

        List<SymbolRef> vars = new ArrayList<>();
// ta make recursive
        vars = parVariablesFromMap.get(pwId);
        List<SymbolRef> a = getPiecewiseParameters_Pascal(vars);
        a = completeList(a);
        pascalIndivEqGeneration(a);

        vars = new ArrayList<>();
        vars = parVariablesFromMap.get(pwId);

        List<SymbolRef> completeVarList = getPiecewiseParameters_Pascal(vars);

        // aggiunto controllo 1 agosto fatto in generatePiecewiseVarDeclaration 
        for (String line : Util.getUniqueString(pascalIndivLines)) {
            String name = getName(Util.clean(line.substring(0, line.indexOf(pascalAssignSymbol))).trim());
            if (isInList(Util.getList(completeVarList), name)) {
                completeVarAssignEq.put(Util.clean(name), line);
            }
        }

        int ind = 0;

        Map<Integer, String> tmp = piecewiseVarToPascal.get(pwId);
        if (tmp != null) {
            for (int i = 1; i <= tmp.size(); i++) {
                sb1.append(String.format(format, tmp.get(i), ind++));
            }

            String line = null;
            for (SymbolRef par : a) {
                String id = par.getSymbIdRef();
                if (id != null && parVariablesFromMap.containsKey(id)) {
                    if (pascalAssignIndivEqLines.get(id) != null) {
                        line = Util.clean(pascalAssignIndivEqLines.get(id));
                        completeVarAssignEq.put(id, line);
                    } else if (pascalAssignVarEqLines.get(id) != null) {
                        line = Util.clean(pascalAssignVarEqLines.get(id));
                        completeVarAssignEq.put(id, line);
                    }

                }
            }

            otherVar = completeVarAssignEq.keySet();

            varEq = cleanPascal(concat(sortVarLines(completeVarAssignEq, getSortedPiecewise(pwId)), "\n\t\t"));
            s2 = generateIFELSEBlocks();
            pascalAssignIndivEq = Util.clean(concat(filterBy(pascalAssignIndivEqLines, pwId), "\t\t"));
            if (!pascalAssignIndivEq.trim().isEmpty()) {
                sb3.append(pascalAssignIndivEq);
            }
        }
        return sb1.toString() + varEq + sb3.toString() + s2[piecewiseIndexMap.get(pwId) - 1];
    }

    protected StringBuilder[] generateIFELSEBlocks() {
        StringBuilder sb2[] = new StringBuilder[piecewiseCompleteList.size()];
        for (int i = 0; i < sb2.length; i++) {
            sb2[i] = new StringBuilder();
        }
        for (Map.Entry<String, Integer> pw : piecewiseIndexMap.entrySet()) {

            sb2[pw.getValue() - 1].append(generatePiecewiseBlock(getPiecewise(pw.getKey()), pw.getKey()));
        }

        return sb2;
    }

    protected List<String> filterBy(Map<String, String> map, String id) {
        List<String> out = new ArrayList<>();
        List<SymbolRef> list = parVariablesFromMap.get(id);
        for (SymbolRef s : list) {
            if (map.get(s.getId()) != null) {
                out.add(map.get(s.getId()));
            }
        }
        return out;
    }

    private void managePiecewiseVariables() {
        List<VariableDefinition> pwv = piecewiseVariables;
        List<String> pwLines = new ArrayList<>();
        for (VariableDefinition var : pwv) {
            piecewiseODE.put(var.getSymbId(), false);
            if (Util.getList(odeParameters).contains(var.getSymbId())) {
                piecewiseODE.put(var.getSymbId(), true);
            }
            piecewiseVariablesId.add(var.getSymbId());
            BinaryTree bt = lexer.getTreeMaker().newInstance(var);
            if (var.getAssign().getPiecewise() != null) {
                String pwblock = generatePiecewiseBlock(var.getAssign().getPiecewise(), var.getSymbId());
                pwLines.add(pwblock);
            } else {
                derivativeDefFromMap.put(var.getSymbId(),
                        String.format("%s%s(%s%s[%s,%s,])",
                                piecewiseFunctionSuffix,
                                piecewiseIndexMap.get(var.getSymbId()),
                                piecewiseSymb,
                                piecewiseIndexMap.get(var.getSymbId()),
                                IND_S, IND_T2));
            }
        }

        for (PopulationParameter var : piecewisePopPars) {
            piecewiseODE.put(var.getSymbId(), false);
            if (Util.getList(odeParameters).contains(var.getSymbId())) {
                piecewiseODE.put(var.getSymbId(), true);
                continue;
            }
            piecewiseVariablesId.add(var.getSymbId());
            BinaryTree bt = lexer.getTreeMaker().newInstance(var);
            String pwblock = parse(pwv, bt);
            pwLines.add(pwblock);
            derivativeDefFromMap.put(var.getSymbId(),
                    String.format("%s%s(%s%s[%s,%s,])",
                            piecewiseFunctionSuffix,
                            piecewiseIndexMap.get(var.getSymbId()),
                            piecewiseSymb,
                            piecewiseIndexMap.get(var.getSymbId()),
                            IND_S, IND_T2));
        }

        int n = 0;
        for (SymbolRef sr : leafPiecewiseParameters) {
            pascalVarMap.put(sr.getSymbIdRef(), n++);
        }
        // assigning the index to thetaparameters
        for (SymbolRef sr : theta_Parameters) { // 6 aprile 2016
            if (!pascalVarMap.containsKey(sr.getSymbIdRef())) {// 170516
                pascalVarMap.put(sr.getSymbIdRef(), n++);
            }
        }
        if (!hasDiffEquations) {

            String format = "%s%s[%s,%s,%s] %s %s\n\t\t";
            String tmp;
            int ind = 1;
            for (VariableDefinition var : piecewiseVariables) {
                String pid = var.getSymbId();
                generateWinbugsPWLine(pid, ind, format);
                ind++;
            }

            for (PopulationParameter var : piecewisePopPars) {
                String pid = var.getSymbId();
                generateWinbugsPWLine(pid, ind, format);
                ind++;
            }

        } else {
            String format = "%s%s[%s,%s,%s] %s %s\n\t\t";
            for (VariableDefinition var : piecewiseVariables) {
                String pwId = var.getSymbId();
                String tmp;
                int ind = 1;
                // se la variabile di stato dipende da una pw 
                if (Util.getList(odeParameters).contains(pwId)) {// 07/06/2016
                    continue;
                } else {// 07/06/2016
                    generateWinbugsPWLine(pwId, ind, format);
                    ind++;
                }

                for (Map.Entry<String, Object> pw : pascalVarMap.entrySet()) {
                    String id = pw.getKey();
                    if (piecewiseParameters.get(pwId) != null // da vedere per bug equazione usecase1 KA_UNIPV 
                            && isInList(Util.getList(piecewiseParameters.get(pwId)), id) && !pwId.equals(id)) {
                        List<SymbolRef> vars = parVariablesFromMap.get(id);
                        if (vars != null) {
                            for (SymbolRef sr : vars) {
                                String id1 = sr.getSymbIdRef();
                                if (variablesAssignementMap.get(delimit(id1)) != null) {
                                    id = delimit(variablesAssignementMap.get(delimit(id1)));
                                } else if (variablesAssignementMap.get(delimit(id)) != null) {
                                    id = delimit(variablesAssignementMap.get(delimit(id)));
                                } else {
                                    id = delimit(id);
                                }
                                if (!isDerivativeVar(Util.clean(removeIndexes(id)))) {
                                    tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pwId), IND_S, IND_T, ind++, assignSymbol, delimit(id1));
                                    winbugsPiecewiseLines.add(tmp);
                                } else {
                                    tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pwId), IND_S, IND_T2, ind++, assignSymbol, delimit(id));
                                    tmp = adjustStatement(tmp);
                                    winbugsPiecewiseDerivDepLines.add(tmp);
                                }

                            }

                        } else {
                            tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pwId), IND_S, IND_T, ind++, assignSymbol, delimit(id));
                            winbugsPiecewiseLines.add(tmp);
                        }
                    }
                }
            }

            for (PopulationParameter var : piecewisePopPars) {
                String pid = var.getSymbId();
                String tmp;
                int ind = 1;

                if (!Util.getList(odeParameters).contains(pid) // 1 agosto
                        || piecewiseMap.containsKey(delimit(variablesAssignementMap.get(delimit(pid))))) {
                    generateWinbugsPWLine(pid, piecewiseIndexMap.get(pid), format);
                    ind++;
                }

                for (Map.Entry<String, Object> pw : pascalVarMap.entrySet()) {
                    String id = pw.getKey();
                    if (piecewiseParameters.get(id) != null
                            && isInList(Util.getList(piecewiseParameters.get(id)), id)
                            && !pid.equals(id)) {
                        List<SymbolRef> vars = parVariablesFromMap.get(id);
                        if (vars != null) {
                            for (SymbolRef sr : vars) {
                                String id1 = sr.getSymbIdRef();
                                if (variablesAssignementMap.get(delimit(id1)) != null) {
                                    id = delimit(variablesAssignementMap.get(delimit(id1)));
                                } else if (variablesAssignementMap.get(delimit(id)) != null) {
                                    id = delimit(variablesAssignementMap.get(delimit(id)));
                                } else {
                                    id = delimit(id);
                                }
                                if (!isDerivativeVar(Util.clean(removeIndexes(id)))) {
                                    tmp = String.format(format,
                                            piecewiseSymb,
                                            piecewiseIndexMap.get(pid),
                                            IND_S,
                                            IND_T,
                                            piecewiseIndexMap.get(Util.clean(removeIndexes(id))),//ind++, 
                                            assignSymbol, delimit(id1));
                                    winbugsPiecewiseLines.add(tmp);
                                } else {
                                    tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pid), IND_S, IND_T2, ind++, assignSymbol, delimit(id));
                                    tmp = adjustStatement(tmp);
                                    winbugsPiecewiseDerivDepLines.add(tmp);
                                }

                            }

                        } else {
                            tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pid), IND_S, IND_T, ind++, assignSymbol, delimit(id));
                            winbugsPiecewiseLines.add(tmp);
                        }
                    }
                }
            }
        }

    }

    private void generateWinbugsPWLine(String pid, int ind, String format) {
        String tmp;
        Set<SymbolRef> tmpList = new HashSet<>();
        pwBugsList.add(pid);
        winbugsPiecewiseLines.add(adjustStatement(String.format("%s_%s %s %s%s(%s%s[%s,%s,])",
                piecewiseSuffix,
                pid,
                assignSymbol,
                piecewiseFunctionSuffix,
                ind,
                piecewiseSymb,
                ind,
                IND_S,
                IND_T)));
        Set<SymbolRef> tmpSet = new HashSet<>(parVariablesFromMap.get(pid));
        Set<SymbolRef> toAdd = new HashSet<>();
        Set<SymbolRef> toRemove = new HashSet<>();
        boolean end = false;
        while (!end) {
            end = true;
            for (SymbolRef tl : tmpSet) {
                if (!tmpList.contains(tl) && parVariablesFromMap.get(tl.getSymbIdRef()) != null) {
                    toAdd.addAll(parVariablesFromMap.get(tl.getSymbIdRef()));
                    toRemove.add(tl);
                    end = false;
                }
            }
            tmpSet.addAll(toAdd);
            tmpSet.removeAll(toRemove);
            toAdd.clear();
            toRemove.clear();
        }

        tmpList.addAll(tmpSet);
        int iv = 1;
        Map<Integer, String> tmpMap = new HashMap<>();
        for (SymbolRef sr : tmpList) {
            tmpMap.put(iv, sr.getSymbIdRef());
            tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pid), IND_S, IND_T, iv++, assignSymbol, delimit(sr.getSymbIdRef()));
            winbugsPiecewiseLines.add(adjustStatement(tmp));
        }
        piecewiseVarToPascal.put(pid, tmpMap);
    }

    private void generateWinbugsCovLine(String pid, int ind, String format) {
        String tmp;
        Set<SymbolRef> tmpList = new HashSet<>();
        winbugsCovariateLines.add(adjustStatement(String.format("%s_%s %s %s%s(%s%s[%s,%s,])",
                categSuffix,
                pid,
                assignSymbol,
                categFunctionSuffix,
                ind,
                pid + "",
                ind,
                IND_S,
                IND_T)));
        Set<SymbolRef> tmpSet = new HashSet<>(parVariablesFromMap.get(pid));
        Set<SymbolRef> toAdd = new HashSet<>();
        Set<SymbolRef> toRemove = new HashSet<>();
        boolean end = false;
        while (!end) {
            end = true;
            for (SymbolRef tl : tmpSet) {
                if (!tmpList.contains(tl) && parVariablesFromMap.get(tl.getSymbIdRef()) != null) {
                    toAdd.addAll(parVariablesFromMap.get(tl.getSymbIdRef()));
                    toRemove.add(tl);
                    end = false;
                }
            }
            tmpSet.addAll(toAdd);
            tmpSet.removeAll(toRemove);
            toAdd.clear();
            toRemove.clear();
        }

        tmpList.addAll(tmpSet);
        int iv = 1;
        Map<Integer, String> tmpMap = new HashMap<>();
        for (SymbolRef sr : tmpList) {
            tmpMap.put(iv, sr.getSymbIdRef());
            tmp = String.format(format, piecewiseSymb, piecewiseIndexMap.get(pid), IND_S, IND_T, iv++, assignSymbol, delimit(sr.getSymbIdRef()));
            winbugsPiecewiseLines.add(adjustStatement(tmp));
        }
        piecewiseVarToPascal.put(pid, tmpMap);
    }

    private String addSuffix(String symbol, String suffix) {
        if (symbol.contains(leftArrayBracket)) {
            int pos = symbol.indexOf(leftArrayBracket.charAt(0));
            return symbol.substring(0, pos) + suffix + symbol.substring(pos);
        } else {
            return symbol;
        }
    }

    private String doString(String string) {
        return string;
    }

    private String doReal(RealValue realValue) {
        return realValue.valueToString();
    }

    private String doStringValue(StringValue stringValue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doInt(IntValue intValue) {
        return intValue.valueToString();
    }

    protected String doPiecewise(Piecewise pw) {
        incrementPiecewiseIndent();
        String symbol = unassigned_symbol;

        List<Piece> pieces = pw.getListOfPiece();
        Piece else_block = null;
        BinaryTree[] assignment_trees = new BinaryTree[pieces.size()];
        BinaryTree[] conditional_trees = new BinaryTree[pieces.size()];
        String[] conditional_stmts = new String[pieces.size()];
        String[] assignment_stmts = new String[pieces.size()];

        TreeMaker tm = lexer.getTreeMaker();
        int assignment_count = 0, else_index = -1;
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece != null) {
                // Logical blocks
                Condition cond = piece.getCondition();
                if (cond != null) {
                    conditional_trees[i] = tm.newInstance(piece.getCondition());
                    if (cond.getOtherwise() != null) {
                        else_block = piece;
                        else_index = i;
                    }
                }
                BinaryTree assignment_tree = tm.newInstance(piece);
                lexer.updateNestedTrees();
                if (assignment_tree != null) {
                    assignment_trees[i] = assignment_tree;
                    assignment_count++;
                }
            }
        }

        if (assignment_count == 0) {
            throw new IllegalStateException("A piecewise block has no assignment statements.");
        }

        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);

            if (conditional_trees[i] != null && assignment_trees[i] != null) {
                if (!piece.equals(else_block)) {
                    conditional_stmts[i] = parse(piece, conditional_trees[i]);
                }
                assignment_stmts[i] = parse(new Object(), assignment_trees[i]);
            }
        }

        int block_assignment = 0;
        StringBuilder block = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece == null) {
                continue;
            } else if (piece.equals(else_block)) {
                continue;
            }

            if (!(conditional_stmts[i] != null && assignment_stmts[i] != null)) {
                continue;
            }

            String operator = "if";
            if (block_assignment > 0) {
                operator = "elseif";
            }

            String format = "%s%s%s %s\n%s%s = %s;\n";
            block.append(String.format(format, "", indent(getLogicalStatementIndent()), operator, conditional_stmts[i], indent(getAssignmentStatementIndent()), field_tag, assignment_stmts[i]));
            block_assignment++;
        }
        if (else_block != null && else_index >= 0) {
            block.append(indent(getLogicalStatementIndent()) + "else\n");
            if (isNestedPiecewise(else_block)) {
                String format = "%s%s\n";
                block.append(String.format(format, indent(getLogicalStatementIndent()), assignment_stmts[else_index]));
            } else {
                String format = "%s%s = %s;\n";
                block.append(String.format(format, indent(getAssignmentStatementIndent()), field_tag, assignment_stmts[else_index]));
            }
        }
        block.append(indent(getLogicalStatementIndent()) + "end");
        if (assignment_count == 0) {
            throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
        }
        symbol = block.toString();

        decrementPiecewiseIndent();

        return symbol;
    }

    private String doLogicalBinaryOperator(LogicBinOp logicBinOp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doLogicalUnaryOperator(LogicUniOp logicUniOp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doParameterEstimate(ParameterEstimate parameterEstimate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean isActivityDoseAmountBlock(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doElement(JAXBElement<?> jaxbElement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doUnivariateDistribution(AbstractContinuousUnivariateDistributionType abstractContinuousUnivariateDistributionType) {
        return abstractContinuousUnivariateDistributionType.getDefinition(); //NEW
    }

    private String doProbonto(ProbOnto prob) {
        return prob.getName().value();
    }

    private String doVarRef(VarRefType varRefType) {
        return varRefType.getVarId();
    }

    private boolean isReplicateVariable(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doCorrelation(CorrelationRef correlationRef) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doBigInteger(BigInteger bigInteger) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doFixedParameter(FixedParameter fixedParameter) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String doParameterAssignmentFromEstimation(ParameterAssignmentFromEstimation parameterAssignmentFromEstimation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean isRandomVariableRef(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean isReplicateLinkedParameter(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLexer(ILexer lexer) {
        if (lexer == null) {
            throw new NullPointerException("The lexer is NULL.");
        }
        this.lexer = lexer;
        z = new SymbolReader(lexer);
    }

    private String doSimulationOutput(SimulationOutput simulationOutput) { // CCoPIMono da BasePArser
        String symbol = unassigned_symbol;

        if (simulationOutput == null) {
            throw new NullPointerException("Simulation output reference is NULL");
        }
        if (isLocalVariable(simulationOutput.v)) {
            VariableDefinition v = (VariableDefinition) simulationOutput.v;
            String format = "%s";
            symbol = String.format(format, z.get(v));
        } else if (isDerivative(simulationOutput.v)) {
            DerivativeVariable dv = (DerivativeVariable) simulationOutput.v;
            String format = "%s";
            symbol = String.format(format, z.get(dv));
        } else {
            throw new UnsupportedOperationException("Simulation output type not supported (ref='" + simulationOutput.v + "')");
        }

        return symbol;

    }

    private static Object getDist(Distribution dist) {
        if (dist == null) {
            return null;
        }

        ProbOnto probOnto = dist.getProbOnto();
        if (probOnto != null) {
            return probOnto;
        }

        UncertML uncert = dist.getUncertML();
        if (uncert == null) {
            return null;
        }

        JAXBElement<? extends AbstractContinuousUnivariateDistributionType> tag = uncert.getAbstractContinuousUnivariateDistribution();
        AbstractContinuousUnivariateDistributionType d = null;
        if (tag != null) {
            d = tag.getValue();
        }

        return d;
    }

    private static Object getDist(ParameterRandomVariable rv) {
        return getDist(rv.getDistribution());
    }

    private boolean isFixedParameter(Object o) {
        return o instanceof FixedParameter;
    }

    private boolean isObjectiveFunctionParameter(Object o) {
        return o instanceof ParameterAssignmentFromEstimation;
    }

    private boolean isIndividualParameterAssignment(Object o) {
        return o instanceof IndividualParameterAssignment;
    }

    private boolean isContinuous(Object context) {
        return context instanceof Continuous;
    }

    private List<String> genPrior() {
        List<String> priorLines = new ArrayList<>();
        for (Map.Entry<PharmMLRootType, String> p : priorPar.entrySet()) {
            if (p.getKey() instanceof PopulationParameter
                    || p.getKey() instanceof ParameterRandomVariable) {
                priorLines.addAll(doProbOntoDistributionPrior(p.getKey()));
            }
        }
        return priorLines;
    }

    String doNormal(CommonParameter context, List<String> lines) {

        String formatDist = "%s %s %s(%s,%s)";
        String formatPar1 = "%s %s";
        ProbOnto value = (ProbOnto) Util.getDistribution(context);
        String distName = value.getName().value();
        DistributionParameter mean = value.getParameter(ParameterName.MEAN);
        String id_mean = context.getSymbId() + "_mean";
        String id_prec = context.getSymbId() + "_prec";
        lines.add(String.format(formatDist, context.getSymbId(), dist_symb, getDistName(getProbOntoDistributionType(context)), id_mean, id_prec));
        lines.add(String.format(formatPar1 + " %s", id_mean, assignSymbol, getParameter(value.getParameter(ParameterName.MEAN))));
        if (distName.equals("Normal2")) {
            lines.add(String.format(formatPar1 + "1/%s", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.VAR))));
        } else if (distName.equals("Normal1")) {
            lines.add(String.format(formatPar1 + "1/pow(%s,2)", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.STDEV))));
        } else if (distName.equals("Normal3")) {
            lines.add(String.format("%s %s %s", id_prec, assignSymbol, getParameter(value.getParameter(ParameterName.PRECISION))));
        }
        return distName + "(" + id_mean + "," + id_prec + ")";
    }

    private List<String> doNormalPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doNormal((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doGammaPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doGamma((CommonParameter) elem, lines);
        return lines;

    }

    private List<String> doExponentialPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doExponential((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doBetaPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doBeta((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doStudentPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doStudent((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doWeibullPriors(PharmMLRootType elem) {

        List<String> lines = new ArrayList<>();
        doWeibull((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doWishartPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doWishart((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doInverseWishartPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doInverseWishart((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doMultivariateNormalPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doMultivariateNormal((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doMultivariateStudentTPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doMultivariateTStudent((CommonParameter) elem, lines);
        return lines;
    }

    private List<String> doInverseGammaPriors(PharmMLRootType elem) {
        List<String> lines = new ArrayList<>();
        doInverseGamma((CommonParameter) elem, lines);
        return lines;
    }

    private List<OperationProperty> getOperation(String type) {
        ModellingSteps steps = lexer.getDom().getModellingSteps();
        List<EstimationOperation> operation = ((Estimation) steps.getCommonModellingStep().get(0).getValue()).getOperation();
        for (EstimationOperation op : operation) {
            if (op.getOpType().equals("BUGS")) {
                return op.getProperty();
            }
        }
        return null;
    }

    void removeNotUsedFromDataFile() throws FileNotFoundException, IOException {
        String dataFileName = jobDir + "/" + "data_BUGS.txt";
        BufferedReader in = new BufferedReader(new FileReader(new File(dataFileName)));
        List<PharmMLRootType> ext = lexer.getScriptDefinition().getTrialDesignBlock().getModel().getListOfExternalDataSet().get(0).getListOfColumnMappingOrColumnTransformationOrMultipleDVMapping();
        List<Piece> pi = null;
        for (PharmMLRootType el : ext) {
            if (el instanceof ColumnMapping) {
                ColumnMapping cm = (ColumnMapping) el;
                if (cm.getColumnRef().getColumnIdRef().equals("AMT")) {
                    doseTargetMap.put(cm.getColumnRef().getColumnIdRef(), cm.getPiecewise().getListOfPiece());
                    pi = cm.getPiecewise().getListOfPiece();
                }
            } else if (el instanceof MultipleDVMapping) {
                MultipleDVMapping cm = (MultipleDVMapping) el;
                if (cm.getColumnRef().getColumnIdRef().equals("AMT")) {
                    doseTargetMap.put(cm.getColumnRef().getColumnIdRef(), cm.getPiecewise().getListOfPiece());
                    pi = cm.getPiecewise().getListOfPiece();
                }
            }
        }
        if (pi == null) {
            return;
        }
        outDebug.println("-- Dose mapping --");
        for (Piece p : pi) {
            String id = ((SymbolRef) p.getValue()).getSymbIdRef();
            String number = getCmtFromCondition(p.getCondition());
            cmtMap.put(number, id);
            outDebug.println(id + " - " + number);
            outDebug.println((getStateVarIndex(id) + 1) + " <-- " + number);

        }
        String dataFile = getStringFile(new BufferedReader(new FileReader(new File(dataFileName))));
        String remappedFile = remap(dataFile);
        output(remappedFile, new File(dataFileName), inPW);
    }

    private static String extractCovName(String in) {
        return in.substring(in.lastIndexOf(",") + 1).trim();
    }

    protected String loadVarNamesFromDataFile(String dataFile, Map<Integer, String> names) {
        String piece;
        int k = 0;
        String[] tokens = dataFile.split("=");
        names.put(k++, tokens[0].substring(tokens[0].lastIndexOf("(") + 1).trim());
        for (int i = 1; i < tokens.length - 1; i++) {
            piece = new String(extractCovName(tokens[i].trim()));
            if (!piece.startsWith("stru") && !piece.startsWith(".Dim")) {
                names.put(k++, piece);

            }
        }
        return tokens[0].substring(0, tokens[0].indexOf(names.get(0)));
    }

    protected void loadVarsFromDataFile(Map<String, String> varsOfDataFile, Map<Integer, String> names, String tmp) {
        for (String n : names.values()) {
            varsOfDataFile.put(n, Util.getDataFromDataFile(tmp, n));
        }
        return;
    }

    protected String loadDataFile(String dataFileName, Map<String, String> varsOfDataFile, Map<Integer, String> names) throws FileNotFoundException, IOException {
        BufferedReader in = new BufferedReader(new FileReader(new File(dataFileName)));
        String tmp = getStringFile(in);

        String firstPiece = loadVarNamesFromDataFile(tmp, names);
        loadVarsFromDataFile(varsOfDataFile, names, tmp);
        return firstPiece;
    }

    public int removeNotUsedCovariatesFromDataFile(boolean writeDataFile) throws FileNotFoundException, IOException {
        String dataFileName = jobDir + "/" + "data_BUGS.txt";
        String firstPiece = "";
        Map<Integer, String> names = new TreeMap<>();
        Map<String, String> varsOfDataFile = new TreeMap<>();
        firstPiece = loadDataFile(dataFileName, varsOfDataFile, names);
        int actualNum1 = cleanDataFile(names, varsOfDataFile, firstPiece, dataFileName, writeDataFile);
        return actualNum1;
    }

    private int get_max_m_num(String v, Map<String, String> varsOfDataFile) {
        int num = 0;
        if (varsOfDataFile.get(maxNamePrefix + v) != null) {
            num = Integer.parseInt(varsOfDataFile.get(maxNamePrefix + v));
        }
        return num;
    }

    private int cleanDataFile(Map<Integer, String> names, Map<String, String> varsOfDataFile, String firstPiece, String dataFileName, boolean writeFile) throws FileNotFoundException {

        outDebug.println("-- Data File cleaning --");
        int nCont = 0, nCat = 0, oldCont = 0, oldCat = 0;
        int numToDecrease = 0, actualNum1, tmp = 0, ncov = this.lexer.getCovariates().size(); // verificare 27 luglio
        for (CovariateDefinition var : this.lexer.getCovariates()) {
            tmp += 2 * get_max_m_num(var.getSymbId(), varsOfDataFile);
        }
        if (varsOfDataFile.get(nCovCatName) != null) {
            oldCat = nCat = Integer.parseInt(varsOfDataFile.get(nCovCatName));
        }
        if (varsOfDataFile.get(nCovContName) != null) {
            oldCont = nCont = Integer.parseInt(varsOfDataFile.get(nCovContName));
        }
        actualNum1 = 1 * (oldCont > 0 ? 1 : 0) + 1 * (oldCat > 0 ? 1 : 0) + 2 * ncov + tmp;
        List<String> toRemoveNames = getToRemoveCovariatesNames();
        for (String v : toRemoveNames) {// 1 agosto
            if (((CovariateDefinition) lexer.getAccessor().fetchElement(v)).getCategorical() != null) {
                nCat--;
            } else if (((CovariateDefinition) lexer.getAccessor().fetchElement(v)).getContinuous() != null) {
                nCont--;
            }
            numToDecrease += 2 + 2 * get_max_m_num(v, varsOfDataFile);
        }
        if (oldCat > 0 && nCat <= 0) {
            numToDecrease++;
        }
        if (oldCont > 0 && nCont <= 0) {
            numToDecrease++;
        }
        actualNum1 -= numToDecrease;
        if (numToDecrease == 0) {
            outDebug.println("No data removed");
        }
        if (!writeFile) {
            return actualNum1;
        }
        Map<String, String> varsToWrite = new HashMap<>();
        for (Map.Entry<String, String> var : varsOfDataFile.entrySet()) {
            if (!dataToRemove.contains(var.getKey())) {
                varsToWrite.put(var.getKey(), var.getValue());
            } else {
                outDebug.println("WARNING: variable " + var.getKey() + " removed");
            }
        }
        if (nCont <= 0 && varsToWrite.remove(nCovContName) != null) {
            outDebug.println("WARNING: variable " + nCovContName + " removed");
        } else {
            if (oldCont != nCont) {
                varsToWrite.put(nCovContName, "" + nCont);
                outDebug.println("WARNING: variable " + nCovContName + " updated");
            }
        }
        if (nCat <= 0 && varsToWrite.remove(nCovCatName) != null) {
            outDebug.println("WARNING: variable " + nCovCatName + " removed");

        } else {
            if (oldCat != nCat) {
                varsToWrite.put(nCovCatName, "" + nCat);
                outDebug.println("WARNING: variable " + nCovCatName + " updated");
            }
        }

        StringBuffer sb = new StringBuffer(firstPiece);
        String id, data;
        for (Map.Entry<Integer, String> name : names.entrySet()) {
            if (varsToWrite.get(name.getValue()) != null) {
                id = name.getValue();
                data = varsToWrite.get(id);
                sb.append(id).append(" = ").append(data).append(", ");
            }
        }
        String out = sb.substring(0, sb.length() - 2).toString() + ")";

        if (writeFile) {
            PrintStream outF = new PrintStream(new File(dataFileName));
            outF.println(out);
            outF.close();
        }
        return actualNum1;
    }

    public List<String> getToRemoveCovariatesNames() {
        List<String> list = new ArrayList<>();
        List<String> covariates = new ArrayList<>();
        for (CovariateDefinition cov : lexer.getCovariates()) {
            covariates.add(cov.getSymbId());
        }
        List<String> pwVars = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, String>> pw : piecewiseVarToPascal.entrySet()) {
            pwVars.addAll(pw.getValue().values());
        }
        pwVars.addAll(covToNotRemove);
        for (String id : covariates) { //1 agosto
            if (!Util.getList(usedOdeCatCovNames).contains(id)
                    && !Util.getList(usedOdeContCovNames).contains(id)
                    && !pwVars.contains(id)) {
                list.add(id);
            }
        }
        return list;
    }

    public List<String> toRemoveCovariates() {
        List<String> list = getToRemoveCovariatesNames();
        for (String var : list) {
            for (String v : dataCovKeywords) {
                dataToRemove.add(v + var);
            }
            dataToRemove.add(var);
        }
        if (dataToRemove.isEmpty()) {
            outDebug.println("No data to remove from data file");
        }
        return list;
    }

    protected void adjustDataFile() throws FileNotFoundException, IOException {
        String dataFileName = jobDir + "/" + "data_BUGS.txt";
        BufferedReader in = new BufferedReader(new FileReader(new File(dataFileName)));
        List<PharmMLRootType> ext = lexer.getScriptDefinition().getTrialDesignBlock().getModel().getListOfExternalDataSet().get(0).getListOfColumnMappingOrColumnTransformationOrMultipleDVMapping();
        List<Piece> pi = null;
        for (PharmMLRootType el : ext) {
            if (el instanceof ColumnMapping) {
                ColumnMapping cm = (ColumnMapping) el;
                if (cm.getColumnRef().getColumnIdRef().equals("AMT")) {
                    doseTargetMap.put(cm.getColumnRef().getColumnIdRef(), cm.getPiecewise().getListOfPiece());
                    pi = cm.getPiecewise().getListOfPiece();
                }
            } else if (el instanceof MultipleDVMapping) {
                MultipleDVMapping cm = (MultipleDVMapping) el;
                if (cm.getColumnRef().getColumnIdRef().equals("AMT")) {
                    doseTargetMap.put(cm.getColumnRef().getColumnIdRef(), cm.getPiecewise().getListOfPiece());
                    pi = cm.getPiecewise().getListOfPiece();
                }
            }
        }
        if (pi == null) {
            return;
        }
        outDebug.println("-- Dose mapping --");
        for (Piece p : pi) {
            String id = ((SymbolRef) p.getValue()).getSymbIdRef();
            String number = getCmtFromCondition(p.getCondition());
            cmtMap.put(number, id);
            outDebug.println(id + " - " + number);
            outDebug.println((getStateVarIndex(id) + 1) + " <-- " + number);

        }
        String dataFile = getStringFile(new BufferedReader(new FileReader(new File(dataFileName))));
        String remappedFile = remap(dataFile);
        output(remappedFile, new File(dataFileName), inPW);

    }

    private String remap(String data) {
        String first, last, middle;
        String start = "cmt = structure(.Data = c(";
        int pos = data.indexOf(start);
        if (pos == -1) {
            throw new RuntimeException("Data file does not contain CMT values.");
        }
        String toRemap = data.substring(pos + start.length(), pos + data.substring(pos).indexOf(")"));
        String vals[] = toRemap.split(",");
        if (cmtMap.entrySet().size() == 1) {
            String id = cmtMap.values().iterator().next();
            vals = putVal(vals, "" + (getStateVarIndex(id) + 1));
        } else {
            for (Map.Entry<String, String> e : cmtMap.entrySet()) {
                String newV = "" + (getStateVarIndex(e.getValue()) + 1);
                vals = remap(vals, e.getKey(), newV);
            }
        }
        first = data.substring(0, pos + start.length());
        last = data.substring(pos + start.length() + toRemap.length());
        outDebug.println("toRemap = " + toRemap);
        middle = Util.createList(vals, ", ");
        outDebug.println("mapped = " + middle);
        return first + middle + last;
    }

    private String[] putVal(String[] vet, String newV) {
        String[] newVals = new String[vet.length];
        int num = 0;
        for (int i = 0; i < vet.length; i++) {
            newVals[i] = new String(newV.trim());
            num++;
        }
        outDebug.println(num + " substitution");
        return newVals;
    }

    private String[] remap(String[] vet, String oldV, String newV) {
        String[] newVals = new String[vet.length];
        for (int i = 0; i < vet.length; i++) {
            newVals[i] = new String(vet[i]);
        }
        System.out.println(oldV + " --> " + newV);
        int num = 0;
        for (int i = 0; i < vet.length; i++) {
            if (vet[i].trim().equals(oldV.trim())) {
                newVals[i] = new String(newV.trim());
                num++;
            } else {
                newVals[i] = vet[i].trim();
            }
        }
        outDebug.println(num + " substitution");
        return newVals;
    }

    private String getCmtFromCondition(Condition condition) {
        if (condition.getLogicBinop() != null) {
            return getCondValue(condition.getLogicBinop().getContent());
        }
        return null;
    }

    private String getCondValue(List<JAXBElement<?>> vv) {
        for (JAXBElement<?> v : vv) {
            if (v.getValue() != null && v.getValue() instanceof LogicBinOp) {
                return getCondValue(((LogicBinOp) v.getValue()).getContent());
            } else if (v.getValue() != null && v.getValue() instanceof ColumnReference) {
                if (((ColumnReference) v.getValue()).getColumnIdRef().equals("CMT")) {
                    continue;
                }
            } else if (v.getValue() != null && v.getValue() instanceof IntValue) {
                return ((IntValue) (v.getValue())).asString();
            }
        }
        return null;
    }

    @Override
    public String getDefaultPiecewiseAssignmentValue(Object o, Object o1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String indent(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
