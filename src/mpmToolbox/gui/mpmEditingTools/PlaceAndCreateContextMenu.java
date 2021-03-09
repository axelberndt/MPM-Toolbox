package mpmToolbox.gui.mpmEditingTools;

import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import meico.mei.Helper;
import meico.mpm.Mpm;
import meico.mpm.elements.Dated;
import meico.mpm.elements.Part;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.*;
import meico.mpm.elements.maps.data.*;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.mpmEditingTools.editDialogs.*;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.mpmTree.MpmTreeNode;
import mpmToolbox.gui.score.ScoreDisplayPanel;
import mpmToolbox.gui.score.ScoreNode;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * A popup menu to be used in class ScoreDisplayPanel.
 * The menu is used to create and/or place performance instructions
 * via clicking on the score page.
 * @author Axel Berndt
 */
public class PlaceAndCreateContextMenu extends WebPopupMenu {
    protected final ScoreDisplayPanel parent;   // the parent interface widget
    private final Point mousePosInImage;        // the pixel position in the score page that corresponds to the mouse event

    /**
     * constructor
     * @param mousePosInImage
     */
    public PlaceAndCreateContextMenu(Point mousePosInImage, ScoreDisplayPanel parent) {
        super();

        this.parent = parent;
        this.mousePosInImage = mousePosInImage;

        this.add(this.createPerformanceInstructionPopupSubmenu());      // create performance instruction in the score
        this.add(this.repositionPerformanceInstructionPopupSubmenu());  // place a performance instruction on the score
    }

    /**
     * This is the popup submenu to create performance instructions via the score.
     * @return
     */
    private WebMenu createPerformanceInstructionPopupSubmenu() {
        MpmTree mpmTree = this.parent.getParentScoreDocumentData().getParent().getMpmTree();
        WebMenu creationMenu = new WebMenu("New Performance Instruction in");

        // Which date should the instruction be associated to? Here we choose the date of the anchor node or 0.0 if there is no anchor node.
        final ScoreNode anchor = this.parent.getAnchorNode();

        // choose the performance where the instruction should be added
        for (Performance performance : this.parent.getParentScoreDocumentData().getParent().getMpm().getAllPerformances()) {
            WebMenu performanceMenu = new WebMenu(performance.getName());
            MpmTreeNode performanceNode = mpmTree.findNode(performance, false);

            // choose global or the part where the instruction should be added
            ArrayList<KeyValue<WebMenu, MpmTreeNode>> datedMenus = new ArrayList<>();
            datedMenus.add(new KeyValue<>(new WebMenu("Global"), performanceNode.findChildNode(performance.getGlobal().getDated(), false)));
            for (Part part : performance.getAllParts()) {
                MpmTreeNode partNode = performanceNode.findChildNode(part, false);
                datedMenus.add(new KeyValue<>(new WebMenu("Part " + part.getNumber() + " " + part.getName()), partNode.findChildNode(part.getDated(), false)));
            }

            // make the submenus with the instructions
            for (KeyValue<WebMenu, MpmTreeNode> datedMenu : datedMenus) {
                performanceMenu.add(datedMenu.getKey());
                MpmTreeNode datedNode = datedMenu.getValue();

                // choose instruction type and specify the action when it is clicked
                WebMenuItem articulationItem = new WebMenuItem("Articulation");
                articulationItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addArticulation(datedNode, mpmTree, anchor, this.mousePosInImage, this));
                datedMenu.getKey().add(articulationItem);

                WebMenuItem asynchronyItem = new WebMenuItem("Asynchrony");
                asynchronyItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addAsynchrony(datedNode, mpmTree, anchor, this.mousePosInImage, this));
                datedMenu.getKey().add(asynchronyItem);

                WebMenuItem dynamicsItem = new WebMenuItem("Dynamics");
                dynamicsItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addDynamics(datedNode, mpmTree, anchor, this.mousePosInImage, this));
                datedMenu.getKey().add(dynamicsItem);

                WebMenuItem metricalAccentuationItem = new WebMenuItem("Metrical Accentuation");
                metricalAccentuationItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addAccentuationPattern(datedNode, mpmTree, anchor, this.mousePosInImage, this));
                datedMenu.getKey().add(metricalAccentuationItem);

                WebMenuItem rubatoItem = new WebMenuItem("Rubato");
                rubatoItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addRubato(datedNode, mpmTree, anchor, this.mousePosInImage, this));
                datedMenu.getKey().add(rubatoItem);

                WebMenuItem tempoItem = new WebMenuItem("Tempo");
                tempoItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addTempo(datedNode, mpmTree, anchor, this.mousePosInImage, this));
                datedMenu.getKey().add(tempoItem);

                WebMenu randomizationItem = new WebMenu("Randomization");
                WebMenuItem randomDynamicsItem = new WebMenuItem("Dynamics");
                randomDynamicsItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addDistribution(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.IMPRECISION_MAP_DYNAMICS));
                randomizationItem.add(randomDynamicsItem);
                WebMenuItem randomTimingItem = new WebMenuItem("Timing");
                randomTimingItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addDistribution(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.IMPRECISION_MAP_TIMING));
                randomizationItem.add(randomTimingItem);
                WebMenuItem randomTondedurationItem = new WebMenuItem("Tone Duration");
                randomTondedurationItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addDistribution(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.IMPRECISION_MAP_TONEDURATION));
                randomizationItem.add(randomTondedurationItem);
                WebMenuItem randomTuningItem = new WebMenuItem("Tuning");
                randomTuningItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addDistribution(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.IMPRECISION_MAP_TUNING));
                randomizationItem.add(randomTuningItem);
                datedMenu.getKey().add(randomizationItem);

                WebMenu styleSwitchItem = new WebMenu("Style Switch");
                WebMenuItem articulationStyleSwitchItem = new WebMenuItem("Articulation Style");
                articulationStyleSwitchItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addStyleSwitch(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.ARTICULATION_MAP));
                styleSwitchItem.add(articulationStyleSwitchItem);
                WebMenuItem dynamicsStyleSwitchItem = new WebMenuItem("Dynamics Style");
                dynamicsStyleSwitchItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addStyleSwitch(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.DYNAMICS_MAP));
                styleSwitchItem.add(dynamicsStyleSwitchItem);
                WebMenuItem metricalAccentuationStyleSwitchItem = new WebMenuItem("Metrical Accentuation Style");
                metricalAccentuationStyleSwitchItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addStyleSwitch(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.METRICAL_ACCENTUATION_MAP));
                styleSwitchItem.add(metricalAccentuationStyleSwitchItem);
                WebMenuItem rubatoStyleSwitchItem = new WebMenuItem("Rubato Style");
                rubatoStyleSwitchItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addStyleSwitch(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.RUBATO_MAP));
                styleSwitchItem.add(rubatoStyleSwitchItem);
                WebMenuItem tempoStyleSwitchItem = new WebMenuItem("Tempo Style");
                tempoStyleSwitchItem.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addStyleSwitch(datedNode, mpmTree, anchor, this.mousePosInImage, this, Mpm.TEMPO_MAP));
                styleSwitchItem.add(tempoStyleSwitchItem);
                datedMenu.getKey().add(styleSwitchItem);
            }

            creationMenu.add(performanceMenu);
        }

        return  creationMenu;
    }

    /**
     * This generates and returns the submenu that deals with
     * the (re-)positioning of performance instructions that exist already in the MPM.
     * @return
     */
    private JMenuItem repositionPerformanceInstructionPopupSubmenu() {
        MpmTreeNode currentNode = this.parent.getParentScoreDocumentData().getParent().getMpmTree().getSelectedNode();            // get the currently selected node

        if ((currentNode == null) || !currentNode.isMapEntryType()) {
            WebMenuItem placeInstructionHere = new WebMenuItem("Place instruction here");
            placeInstructionHere.setEnabled(false);
            placeInstructionHere.setToolTipText("No performance instruction selected.");
            return placeInstructionHere;
        }

        String instructionType;
        switch (currentNode.getType()) {
            case accentuationPattern:
                instructionType = "Metrical Accentuation";
                break;
            case articulation:
                instructionType = "Articulation";
                break;
            case asynchrony:
                instructionType = "Asynchrony";
                break;
            case distributionCorrelatedBrownianNoise:
                instructionType = "Brownian Noise Distribution";
                break;
            case distributionCorrelatedCompensatingTriangle:
                instructionType = "Compensating Triangle Distribution";
                break;
            case distributionGaussian:
                instructionType = "Gaussian Distribution";
                break;
            case distributionList:
                instructionType = "Distribution List";
                break;
            case distributionTriangular:
                instructionType = "Triangular Distribution";
                break;
            case distributionUniform:
                instructionType = "Uniform Distribution";
                break;
            case dynamics:
                instructionType = "Dynamics";
                break;
            case rubato:
                instructionType = "Rubato";
                break;
            case style:
                instructionType = "Style Switch";
                break;
            case tempo:
                instructionType = "Tempo";
                break;
            default:
                instructionType = "instruction";
        }

        if (this.parent.getAnchorNode() == null) {        // if there is no anchor node from which we might get a date
            WebMenuItem placeInstructionHere = new WebMenuItem("Place " + instructionType + " here");
            placeInstructionHere.addActionListener(actionEvent -> repositionPerformanceInstruction(currentNode, mousePosInImage, this, true));  // just position the object in the score
            return placeInstructionHere;
        }

        // there is an anchor node and we have to offer the choice to keep the instruction's date or assign the anchor node's date to the instruction
        WebMenu placeInstructionHere = new WebMenu("Place " + instructionType + " here");

        WebMenuItem placeAndKeepDate = new WebMenuItem("Keep its date " + Helper.getAttributeValue("date", (Element) currentNode.getUserObject()));
        placeAndKeepDate.addActionListener(actionEvent -> repositionPerformanceInstruction(currentNode, mousePosInImage, this, true));  // position the object in the score
        placeAndKeepDate.setToolTipText("The date can be edited later on.");
        placeInstructionHere.add(placeAndKeepDate);

        for (Element e : this.parent.getAnchorNode().getAssociatedElements()) {              // for each element that is associated with this ONGNode
            String date = Helper.getAttributeValue("date", e);
            WebMenuItem placeAndChangeDate = new WebMenuItem("Set its date to " + date);
            placeAndChangeDate.addActionListener(actionEvent -> {
                repositionPerformanceInstruction(currentNode, mousePosInImage, this, true);         // position the object in the score
                currentNode.setDate(Double.parseDouble(date));                          // set the new date and reorder the parent map accordingly
                parent.getParentScoreDocumentData().getParent().getMpmTree().reloadNode(currentNode.getParent());  // update MPM subtree
            });
            placeAndChangeDate.setToolTipText("The date can be edited later on.");
            placeInstructionHere.add(placeAndChangeDate);
        }

        return placeInstructionHere;
    }

    /**
     * Position or reposition a the currently selected MPM performance instruction in the score image.
     * If the instruction has no xml:id, it will be generated and added.
     * @param currentNode
     * @param position
     * @param self a reference to the popup menu
     */
    private static void repositionPerformanceInstruction(MpmTreeNode currentNode, Point position, PlaceAndCreateContextMenu self, boolean incrementTreeCursor) {
        // update the associated data in the project data structure
        Element object = (Element) currentNode.getUserObject();                 // get the element
        ScoreNode objectNode = self.parent.getScorePage().addEntry(position.getX(), position.getY(), object);
        self.parent.getParentScoreDocumentData().getParent().getMpmTree().updateNode(currentNode);          // update the indication that the instruction is associated to a pixel position now

        // if the cursor in the MPM tree should not increment, i.e. select the next map node, it should at leas select the current node
        if (!incrementTreeCursor) {
            self.parent.getParentScoreDocumentData().getParent().getMpmTree().setSelectedNode(currentNode);                   // select it
            self.parent.getParentScoreDocumentData().getParent().getMpmTree().scrollPathToVisible(currentNode.getTreePath()); // scroll the tree so the node is visible
            return;
        }

        // in the MPM tree find and select the next node
        for (MpmTreeNode nextNode = currentNode.getNextNode(); nextNode != null; nextNode = nextNode.getNextNode()) {
            if (nextNode.isMapEntryType()) {                                                                // if the node is an entry in an MPM map
                self.parent.getParentScoreDocumentData().getParent().getMpmTree().setSelectedNode(nextNode);                   // select it
                self.parent.getParentScoreDocumentData().getParent().getMpmTree().scrollPathToVisible(nextNode.getTreePath()); // scroll the tree so the node is visible
                return;
            }
        }
        self.parent.getParentScoreDocumentData().getParent().getMpmTree().clearSelection();                 // no node was found (null because end of tree), clear the selection so the next click won't overwrite the last node's coordinates
    }

    /**
     * create an articulation instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     */
    private static void addArticulation(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self) {
        boolean deleteMapOnCancel = false;  // if there is no map of the desired type in the dated environment and we have to create a new map but cancel the editor dialog, we should remove the map as well; this flag will be set true for this situation

        // get the map or create one if there is none of the desired type
        ArticulationMap map = (ArticulationMap) ((Dated) datedNode.getUserObject()).getMap(Mpm.ARTICULATION_MAP);
        if (map == null) {
            map = (ArticulationMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.ARTICULATION_MAP);
            mpmTree.reloadNode(datedNode);  // update the MPM tree display to show the newly created map
            deleteMapOnCancel = true;
        }

        Msm msm = mpmTree.getProjectPane().getMsm();
        ArticulationEditor editor = new ArticulationEditor(map, msm);   // initialize the editor dialog
//        double date = (anchor != null) ? Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))) : 0.0;

        // if there is an anchor node, we set its date in the editor dialog
        if (anchor != null) {
            Element anchorElement = anchor.getAssociatedElements().get(0);

            // if the anchor is of type note, we can associate the articulation via its ID with it
            for (Element e : anchor.getAssociatedElements()) {
                if (!e.getLocalName().equals("note"))
                    continue;
                Attribute idAtt = e.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
                if (idAtt == null)
                    continue;
                editor.setNoteId(idAtt.getValue());
                anchorElement = e;
                break;
            }

            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchorElement)));
        }

        ArticulationData articulation = editor.create();

        MpmTreeNode mapNode = datedNode.findChildNode(map, false);
        if (articulation != null) {
            int index = map.addArticulation(articulation);                              // add the instruction to the map
            mpmTree.reloadNode(mapNode);                                                // update the MPM tree
            MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);  // get a handle to the MPM tree node of the instruction just added
            PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);  // the its position on the score page
        } else if (deleteMapOnCancel) {                                                 // cancel
            ((Dated) datedNode.getUserObject()).removeMap(Mpm.ARTICULATION_MAP);
            mpmTree.reloadNode(mapNode.getParent());
        }
    }

    /**
     * create an asynchrony instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     */
    private static void addAsynchrony(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self) {
        AsynchronyEditor editor = new AsynchronyEditor();   // initialize the editor dialog

        // if there is an anchor node, we set its date in the editor dialog
        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        AsynchronyEditor.AsynchronyData asynchrony = editor.create();

        if (asynchrony == null)
            return;

        // get the map or create one if there is none of the desired type
        AsynchronyMap map = (AsynchronyMap) ((Dated) datedNode.getUserObject()).getMap(Mpm.ASYNCHRONY_MAP);
        if (map == null)
            map = (AsynchronyMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.ASYNCHRONY_MAP);

        // add the new instruction to the map, then update MPM tree and score
        int index = map.addAsynchrony(asynchrony.date, asynchrony.millisecondsOffset);          // add the instruction to the map
        mpmTree.reloadNode(datedNode);                                                          // update the MPM tree display to show the newly created map
        MpmTreeNode mapNode = datedNode.findChildNode(map, false);                              // get a handle to the map node in the MPM tree
        MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);              // get a handle to the MPM tree node of the instruction just added
        PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);    // the its position on the score page
    }

    /**
     * create an dynamics instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     */
    private static void addDynamics(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self) {
        boolean deleteMapOnCancel = false;  // if there is no map of the desired type in the dated environment and we have to create a new map but cancel the editor dialog, we should remove the map as well; this flag will be set true for this situation

        // get the map or create one if there is none of the desired type
        DynamicsMap map = (DynamicsMap) ((Dated) datedNode.getUserObject()).getMap(Mpm.DYNAMICS_MAP);
        if (map == null) {
            map = (DynamicsMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.DYNAMICS_MAP);
            mpmTree.reloadNode(datedNode);  // update the MPM tree display to show the newly created map
            deleteMapOnCancel = true;
        }

        DynamicsEditor editor = new DynamicsEditor(map);   // initialize the editor dialog

        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        DynamicsData dynamics = editor.create();

        MpmTreeNode mapNode = datedNode.findChildNode(map, false);
        if (dynamics != null) {
            int index = map.addDynamics(dynamics);                                      // add the instruction to the map
            mpmTree.reloadNode(mapNode);                                                // update the MPM tree
            MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);  // get a handle to the MPM tree node of the instruction just added
            PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);  // the its position on the score page
        } else if (deleteMapOnCancel) {                                                 // cancel
            ((Dated) datedNode.getUserObject()).removeMap(Mpm.DYNAMICS_MAP);
            mpmTree.reloadNode(mapNode.getParent());
        }
    }

    /**
     * create an accentuationPattern instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     */
    private static void addAccentuationPattern(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self) {
        boolean deleteMapOnCancel = false;  // if there is no map of the desired type in the dated environment and we have to create a new map but cancel the editor dialog, we should remove the map as well; this flag will be set true for this situation

        // get the map or create one if there is none of the desired type
        MetricalAccentuationMap map = (MetricalAccentuationMap) ((Dated) datedNode.getUserObject()).getMap(Mpm.METRICAL_ACCENTUATION_MAP);
        if (map == null) {
            map = (MetricalAccentuationMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.METRICAL_ACCENTUATION_MAP);
            mpmTree.reloadNode(datedNode);  // update the MPM tree display to show the newly created map
            deleteMapOnCancel = true;
        }

        AccentuationPatternEditor editor = new AccentuationPatternEditor(map);   // initialize the editor dialog

        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        MetricalAccentuationData accentuationPattern = editor.create();

        MpmTreeNode mapNode = datedNode.findChildNode(map, false);
        if (accentuationPattern != null) {
            int index = map.addAccentuationPattern(accentuationPattern);                // add the instruction to the map
            mpmTree.reloadNode(mapNode);                                                // update the MPM tree
            MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);  // get a handle to the MPM tree node of the instruction just added
            PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);  // the its position on the score page
        } else if (deleteMapOnCancel) {                                                 // cancel
            ((Dated) datedNode.getUserObject()).removeMap(Mpm.METRICAL_ACCENTUATION_MAP);
            mpmTree.reloadNode(mapNode.getParent());
        }
    }

    /**
     * create an rubato instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     */
    private static void addRubato(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self) {
        boolean deleteMapOnCancel = false;  // if there is no map of the desired type in the dated environment and we have to create a new map but cancel the editor dialog, we should remove the map as well; this flag will be set true for this situation

        // get the map or create one if there is none of the desired type
        RubatoMap map = (RubatoMap) ((Dated) datedNode.getUserObject()).getMap(Mpm.RUBATO_MAP);
        if (map == null) {
            map = (RubatoMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.RUBATO_MAP);
            mpmTree.reloadNode(datedNode);  // update the MPM tree display to show the newly created map
            deleteMapOnCancel = true;
        }

        RubatoEditor editor = new RubatoEditor(map);   // initialize the editor dialog

        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        RubatoData rubato = editor.create();

        MpmTreeNode mapNode = datedNode.findChildNode(map, false);
        if (rubato != null) {
            int index = map.addRubato(rubato);                                          // add the instruction to the map
            mpmTree.reloadNode(mapNode);                                                // update the MPM tree
            MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);  // get a handle to the MPM tree node of the instruction just added
            PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);  // the its position on the score page
        } else if (deleteMapOnCancel) {                                                 // cancel
            ((Dated) datedNode.getUserObject()).removeMap(Mpm.RUBATO_MAP);
            mpmTree.reloadNode(mapNode.getParent());
        }
    }

    /**
     * create an tempo instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     */
    private static void addTempo(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self) {
        boolean deleteMapOnCancel = false;  // if there is no map of the desired type in the dated environment and we have to create a new map but cancel the editor dialog, we should remove the map as well; this flag will be set true for this situation

        // get the map or create one if there is none of the desired type
        TempoMap map = (TempoMap) ((Dated) datedNode.getUserObject()).getMap(Mpm.TEMPO_MAP);
        if (map == null) {
            map = (TempoMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.TEMPO_MAP);
            mpmTree.reloadNode(datedNode);  // update the MPM tree display to show the newly created map
            deleteMapOnCancel = true;
        }

        TempoEditor editor = new TempoEditor(map);   // initialize the editor dialog

        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        TempoData tempo = editor.create();

        MpmTreeNode mapNode = datedNode.findChildNode(map, false);
        if (tempo != null) {
            int index = map.addTempo(tempo);                                            // add the instruction to the map
            mpmTree.reloadNode(mapNode);                                                // update the MPM tree
            MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);  // get a handle to the MPM tree node of the instruction just added
            PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);  // the its position on the score page
        } else if (deleteMapOnCancel) {                                                 // cancel
            ((Dated) datedNode.getUserObject()).removeMap(Mpm.TEMPO_MAP);
            mpmTree.reloadNode(mapNode.getParent());
        }
    }

    /**
     * create an distribution instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     * @param imprecisionMapType use the constants in class Mpm (IMPRECISION_MAP_...)
     */
    private static void addDistribution(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self, String imprecisionMapType) {
        DistributionEditor editor = new DistributionEditor();   // initialize the editor dialog

        // if there is an anchor node, we set its date in the editor dialog
        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        DistributionData distribution = editor.create();

        if (distribution == null)
            return;

        // get the map or create one if there is none of the desired type
        ImprecisionMap map = (ImprecisionMap) ((Dated) datedNode.getUserObject()).getMap(imprecisionMapType);
        if (map == null)
            map = (ImprecisionMap) ((Dated) datedNode.getUserObject()).addMap(imprecisionMapType);

        // add the new instruction to the map, then update MPM tree and score
        int index = map.addDistribution(distribution);                                          // add the instruction to the map
        mpmTree.reloadNode(datedNode);                                                          // update the MPM tree display to show the newly created map
        MpmTreeNode mapNode = datedNode.findChildNode(map, false);                              // get a handle to the map node in the MPM tree
        MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);              // get a handle to the MPM tree node of the instruction just added
        PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);    // the its position on the score page
    }

    /**
     * create an stale switch instruction on the score
     * @param datedNode
     * @param mpmTree
     * @param anchor
     * @param position position of mouse click on the score page
     * @param self a reference to the popup menu
     * @param mapType use the constants in class Mpm (TEMPO_MAP, DYNAMICS_MAP ...)
     */
    private static void addStyleSwitch(MpmTreeNode datedNode, MpmTree mpmTree, ScoreNode anchor, Point position, PlaceAndCreateContextMenu self, String mapType) {
        boolean deleteMapOnCancel = false;  // if there is no map of the desired type in the dated environment and we have to create a new map but cancel the editor dialog, we should remove the map as well; this flag will be set true for this situation

        // get the map or create one if there is none of the desired type
        GenericMap map = ((Dated) datedNode.getUserObject()).getMap(mapType);
        if (map == null) {
            map = ((Dated) datedNode.getUserObject()).addMap(mapType);
            mpmTree.reloadNode(datedNode);  // update the MPM tree display to show the newly created map
            deleteMapOnCancel = true;
        }

        StyleSwitchEditor editor = new StyleSwitchEditor(map);   // initialize the editor dialog

        if (anchor != null)
            editor.setDate(Double.parseDouble(Helper.getAttributeValue("date", anchor.getAssociatedElements().get(0))));

        StyleSwitchEditor.StyleSwitchData style = editor.create();

        MpmTreeNode mapNode = datedNode.findChildNode(map, false);
        if (style != null) {
            int index;
            if (map instanceof ArticulationMap)
                index = ((ArticulationMap) map).addStyleSwitch(style.date, style.styleName, style.defaultArticulation, style.id);
            else
                index = map.addStyleSwitch(style.date, style.styleName, style.id);

            mpmTree.reloadNode(mapNode);                                                // update the MPM tree
            MpmTreeNode newNode = mapNode.findChildNode(map.getElement(index), false);  // get a handle to the MPM tree node of the instruction just added
            PlaceAndCreateContextMenu.repositionPerformanceInstruction(newNode, position, self, false);  // the its position on the score page
        } else if (deleteMapOnCancel) {                                                 // cancel
            ((Dated) datedNode.getUserObject()).removeMap(mapType);
            mpmTree.reloadNode(mapNode.getParent());
        }
    }
}
