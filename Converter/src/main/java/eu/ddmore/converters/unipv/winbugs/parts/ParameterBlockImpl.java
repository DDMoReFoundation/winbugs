/* ----------------------------------------------------------------------------
 * This file is part of the CCoPI-Mono Toolkit. 
 *
 * Copyright (C) 2016 jointly by the following organizations:
 * 1. BMS - University of Pavia, Italy
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation. A copy of the license agreement is provided
 * in the file named "LICENSE.txt" included with this software distribution.
 * ----------------------------------------------------------------------------
 */
package eu.ddmore.converters.unipv.winbugs.parts;

import static crx.converter.engine.PharmMLTypeChecker.*;
import crx.converter.engine.assoc.*;
import crx.converter.tree.*;
import crx.converter.engine.common.*;
import crx.converter.spi.blocks.*;
import crx.converter.spi.*;

//import static crx.converter.engine.PharmMLTypeChecker.isCorrelation;
//import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
//import static crx.converter.engine.PharmMLTypeChecker.isPopulationParameter;
//import static crx.converter.engine.PharmMLTypeChecker.isRandomVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//import crx.converter.engine.assoc.Cluster;
//import crx.converter.engine.common.CorrelationRef;
//import crx.converter.engine.common.ParameterEvent;
//import unipv.converters.winbugs.utils.ILexer;
//import crx.converter.spi.blocks.OrderableBlock;
//import crx.converter.spi.blocks.ParameterBlock;
//import crx.converter.tree.BinaryTree;
//import crx.converter.tree.TreeMaker;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLElement;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.modeldefn.CommonParameter;
import eu.ddmore.libpharmml.dom.modeldefn.Correlation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterModel;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
//import unipv.converters.winbugs.utils.BinaryTree;
//import unipv.converters.winbugs.utils.Cluster;
//import unipv.converters.winbugs.utils.CorrelationRef;
//import unipv.converters.winbugs.utils.ParameterEvent;
//import static unipv.converters.winbugs.utils.PharmMLTypeChecker.*;
//import unipv.converters.winbugs.utils.TreeMaker;

/**
 * A class representing the code for a parameter model.
 */
public class ParameterBlockImpl extends BaseRandomVariableBlockImpl implements OrderableBlock, ParameterBlock {

    private List<PharmMLRootType> cached_declaration_list = new ArrayList<PharmMLRootType>();
    private List<ParameterEvent> events = new ArrayList<ParameterEvent>();
    private List<PharmMLElement> objects_to_remove = new ArrayList<PharmMLElement>();
    private HashMap<CommonParameter, Integer> param_map_idx = new HashMap<CommonParameter, Integer>();
    private HashMap<String, CommonParameter> param_map_name = new HashMap<String, CommonParameter>();
    private ArrayList<PopulationParameter> params = new ArrayList<PopulationParameter>();
    private ParameterModel pm = null;

    /**
     * Constructor
     *
     * @param pm_ Parameter Model
     * @param c_ Converter Instance
     */
    public ParameterBlockImpl(ParameterModel pm_, ILexer c_) {
        if (pm_ == null) {
            throw new NullPointerException("ParameterModel is NULL.");
        }
        pm = pm_;

        if (pm.getBlkId() == null) {
            throw new IllegalStateException("Parameter model must have a BLK ID");
        }

        lexer = c_;

        int i = 1, indiv_i = 1;
        for (PharmMLElement o : pm.getListOfParameterModelElements()) {
            if (isPopulationParameter(o)) {
                PopulationParameter p = (PopulationParameter) o;
                String symbolId = p.getSymbId();
                if (!isSymbolIdOkay(symbolId)) {
                    throw new IllegalStateException("The node symbolId (" + symbolId + ") is not okay.");
                }
                Integer idx = i++;
                params.add(p);
                param_map_idx.put(p, idx);
                param_map_name.put(symbolId, p);
                checkAssignment(p);
            } else if (isIndividualParameter(o)) {
                IndividualParameter ip = (IndividualParameter) o;
                String symbolId = ip.getSymbId();
                if (!isSymbolIdOkay(symbolId)) {
                    throw new IllegalStateException("The node symbolId (" + symbolId + ") is not okay.");
                }
                Integer idx = indiv_i++;
                param_map_idx.put(ip, idx);
                indiv_params.add(ip);
                param_map_name.put(symbolId, ip);
            } else if (isRandomVariable(o)) {
                ParameterRandomVariable rv = (ParameterRandomVariable) o;
                String symbolId = rv.getSymbId();
                if (!isSymbolIdOkay(symbolId)) {
                    throw new IllegalStateException("The node symbolId (" + symbolId + ") is not okay.");
                }
                param_map_name.put(symbolId, rv);
                rvs.add(rv);
            } else if (isCorrelation(o)) {
                register((Correlation) o);
            } else {
                new UnsupportedOperationException("PharmML type not supported yet (type=" + o + ")");
            }
        }

        removeMatrixParameters();
    }

    public boolean addCluster(Cluster cluster) {
        throw new UnsupportedOperationException();
    }

    private void buildMatrixDeclarationTrees() {
        if (has_matrices.size() == 0) {
            return;
        }
        TreeMaker tm = lexer.getTreeMaker();

        for (Object o : has_matrices) {
            lexer.addStatement(o, tm.newInstance(o));
            lexer.updateNestedTrees();
        }
    }

    @Override
    public void buildTrees() {
        TreeMaker tm = lexer.getTreeMaker();
        for (PopulationParameter p : params) {
            if (lexer.isUsePiecewiseAsEvents()) {
                if (hasPiecewise(p)) {
                    ParameterEvent evt = new ParameterEvent(p, lexer);
                    evt.buildTrees();
                    events.add(evt);
                    lexer.addStatement(p, evt.getDefaultTree());
                } else {
                    BinaryTree bt = tm.newInstance(p);
                    lexer.addStatement(p, bt);
                    lexer.updateNestedTrees();
                }
            } else {
                BinaryTree bt = tm.newInstance(p);
                lexer.addStatement(p, bt);
                lexer.updateNestedTrees();
            }
        }

        for (ParameterRandomVariable rv : rvs) {
            lexer.addStatement(rv, tm.newInstance(rv));
            lexer.updateNestedTrees();
        }

        for (CorrelationRef corr : correlations) {
            buildTrees(corr);
        }

        buildIndividualParameterTrees();
        buildMatrixDeclarationTrees();
    }

    private void clearAllParameterLists() {
        params.clear();
        rvs.clear();
        indiv_params.clear();
        linked_rvs.clear();
    }

    /**
     * Check if this parameter block contains this element.
     *
     * @param v Model Element
     * @return boolean
     */
    public boolean contains(PharmMLRootType v) {
        if (isPopulationParameter(v)) {
            for (PopulationParameter p : params) {
                if (p == null) {
                    continue;
                }
                if (p.equals(v)) {
                    return true;
                }
            }
        } else if (isIndividualParameter(v)) {
            for (IndividualParameter ip : indiv_params) {
                if (ip == null) {
                    continue;
                }
                if (ip.equals(v)) {
                    return true;
                }
            }
        } else if (isRandomVariable(v)) {
            for (ParameterRandomVariable rv : rvs) {
                if (rv == null) {
                    continue;
                }
                if (rv.equals(v)) {
                    return true;
                }
            }
            for (ParameterRandomVariable rv : linked_rvs) {
                if (rv == null) {
                    continue;
                }
                if (rv.equals(v)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<Cluster> getClusters() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the parameter assignment events linked to global parameter model.
     *
     * @return java.util.List<Event>
     * @see ParameterBlockImpl#hasEvents()
     */
    public List<ParameterEvent> getEvents() {
        return events;
    }

    /**
     * All of the declared variables in the parameter model. This method returns
     * the cache declaration list if the variable order already assigned by a
     * converter instance.
     *
     * @return List<PharmMLRootType>
     */
    public List<PharmMLRootType> getListOfDeclarations() {
        if (!cached_declaration_list.isEmpty()) {
            return cached_declaration_list;
        }

        List<PharmMLRootType> params = new ArrayList<PharmMLRootType>();

        params.addAll(getParameters());
        params.addAll(getRandomVariables());
        params.addAll(getIndividualParameters());
        params.addAll(getLinkedRandomVariables());

        return params;
    }

    /**
     * Get the parameter model.
     *
     * @return eu.ddmore.libpharmml.dom.modeldefn.ParameterModel
     */
    public ParameterModel getModel() {
        return pm;
    }

    @Override
    public String getName() {
        String blkId = "__RUBBISH_DEFAULT_VALUE_";
        if (pm != null) {
            if (pm.getBlkId() != null) {
                blkId = pm.getBlkId();
            }
        }
        return blkId;
    }

    /**
     * Get the index of a parameter in the numeric parameter array passed to a
     * model function.
     *
     * @param name Parameter Name
     * @return java.lang.Integer
     */
    public Integer getParameterIndex(String name) {
        Integer idx = -1;
        if (param_map_name.containsKey(name)) {
            CommonParameter p = (param_map_name.get(name));
            if (p != null) {
                if (param_map_idx.containsKey(p)) {
                    idx = param_map_idx.get(p);
                    if (lexer.isIndexFromZero()) {
                        idx--;
                    }
                }
            }
        }

        return idx;
    }

    /**
     * Get the index of a parameter in the numeric parameter array passed to a
     * model function.
     *
     * @param ref Reference to the parameter
     * @return java.lang.Integer
     */
    public Integer getParameterIndex(SymbolRef ref) {
        Integer idx = -1;

        if (ref != null) {
            PharmMLRootType p = lexer.getAccessor().fetchElement(ref);
            if (p != null) {
                if (param_map_idx.containsKey(p)) {
                    idx = param_map_idx.get(p);
                }
            }
        }

        return idx;
    }

    /**
     * Get a list of numeric parameters.
     *
     * @return
     * java.util.List<eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter>
     */
    public List<PopulationParameter> getParameters() {
        return params;
    }

    /**
     * Get a list of random variables.
     *
     * @return
     * java.util.List<eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable>
     */
    public List<ParameterRandomVariable> getRandomVariables() {
        return rvs;
    }

    @Override
    public List<String> getSymbolIds() {
        ArrayList<String> ids = new ArrayList<String>();
        for (PopulationParameter p : params) {
            if (p == null) {
                continue;
            }
            if (p.getSymbId() != null) {
                ids.add(p.getSymbId());
            }
        }
        return ids;
    }

    /**
     * Flag if the parameter model has events. Event is an piecewise assignment
     * cued on the IDV or a derivative.
     *
     * @return boolean
     * @see ParameterEvent
     */
    public boolean hasEvents() {
        return !events.isEmpty();
    }

    /**
     * Flag if the parameter model has matrix assigned parameters.
     *
     * @return boolean
     */
    public boolean hasMatrixAssignedParameters() {
        return has_matrices.size() > 0;
    }

    private boolean hasPiecewise(PopulationParameter p) {
        if (p == null) {
            return false;
        }
        Rhs rhs = p.getAssign();
        if (rhs == null) {
            return false;
        }
        return rhs.getPiecewise() != null;
    }

    @Override
    public boolean hasSymbolId(String name) {
        boolean has = false;
        if (name != null) {
            for (PopulationParameter s : params) {
                String v_name = s.getSymbId();
                if (v_name != null) {
                    if (v_name.equals(name)) {
                        has = true;
                        break;
                    }
                }
            }
        }
        return has;
    }

    /**
     * Flag if a random variable is correlated, i.e. linked.<br/>
     * Acts as a filter flag to avoid duplicated random variable assignment
     * blocks.
     *
     * @param rv
     * @return boolean
     */
    public boolean isLinkedRandomVariable(ParameterRandomVariable rv) {
        if (rv != null) {
            return linked_rvs.contains(rv);
        } else {
            return false;
        }
    }

    // Remove so do not show up in the numeric parameter vector but as part of a matrix assignment block.
    private void removeMatrixParameters() {
        if (objects_to_remove.size() == 0) {
            return;
        }
        pm.getListOfParameterModelElements().removeAll(objects_to_remove);
    }

    /**
     * Set the ordered parameter list within the parameter block.<br/>
     * This is set outside of the ParameterBlock, hence this accessor function.
     *
     * @param ordered_variables Ordered parameter List.
     */
    public void setOrderedVariableList(List<PharmMLElement> ordered_variables) {
        if (ordered_variables == null) {
            return;
        }
        if (ordered_variables.isEmpty()) {
            return;
        }

        List<PopulationParameter> old_params = new ArrayList<PopulationParameter>();
        List<ParameterRandomVariable> old_rvs = new ArrayList<ParameterRandomVariable>();
        List<IndividualParameter> old_indiv_params = new ArrayList<IndividualParameter>();
        List<ParameterRandomVariable> old_linked_rvs = new ArrayList<ParameterRandomVariable>();

        old_params.addAll(getParameters());
        old_rvs.addAll(getRandomVariables());
        old_indiv_params.addAll(getIndividualParameters());
        old_linked_rvs.addAll(getLinkedRandomVariables());

        clearAllParameterLists();

        for (PharmMLElement ordered_variable : ordered_variables) {
            if (isPopulationParameter(ordered_variable)) {
                PopulationParameter p = (PopulationParameter) ordered_variable;
                if (old_params.contains(p)) {
                    params.add(p);
                }
            } else if (isIndividualParameter(ordered_variable)) {
                IndividualParameter ip = (IndividualParameter) ordered_variable;
                if (old_indiv_params.contains(ip)) {
                    indiv_params.add(ip);
                }
            } else if (isRandomVariable(ordered_variable)) {
                ParameterRandomVariable rv = (ParameterRandomVariable) ordered_variable;
                if (old_rvs.contains(rv)) {
                    rvs.add(rv);
                } else if (old_linked_rvs.contains(rv)) {
                    linked_rvs.add(rv);
                }
            }
        }

        cached_declaration_list.clear();
        for (PharmMLElement o : ordered_variables) {
            if (isPopulationParameter(o) || isIndividualParameter(o) || isRandomVariable(o)) {
                cached_declaration_list.add((PharmMLRootType) o);
            }
        }
    }
}
