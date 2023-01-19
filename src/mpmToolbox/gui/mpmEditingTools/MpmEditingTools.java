package mpmToolbox.gui.mpmEditingTools;

import com.alee.api.annotations.NotNull;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import meico.audio.Audio;
import meico.mei.Helper;
import meico.mpm.Mpm;
import meico.mpm.elements.*;
import meico.mpm.elements.maps.*;
import meico.mpm.elements.maps.data.*;
import meico.mpm.elements.metadata.Author;
import meico.mpm.elements.metadata.Comment;
import meico.mpm.elements.metadata.Metadata;
import meico.mpm.elements.metadata.RelatedResource;
import meico.mpm.elements.styles.*;
import meico.mpm.elements.styles.defs.*;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import mpmToolbox.projectData.ProjectData;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.mpmEditingTools.editDialogs.*;
import mpmToolbox.gui.mpmTree.MpmStyleCollection;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.mpmTree.MpmTreeNode;
import mpmToolbox.gui.msmTree.MsmTree;
import mpmToolbox.gui.msmTree.MsmTreeNode;
import mpmToolbox.projectData.score.Score;
import mpmToolbox.projectData.score.ScoreNode;
import mpmToolbox.projectData.score.ScorePage;
import nu.xom.Attribute;
import nu.xom.Element;

import java.io.File;
import java.util.*;

/**
 * Helper class for MpmTreeNode. It generates the context menu that appears when right clicking
 * in the MPM tree and all functionality required for it.
 * @author Axel Berndt
 */
public class MpmEditingTools {
    /**
     * This method creates the context menu when a node in an MpmTree is right-clicked.
     * @param forThisNode
     * @param inThisMpmTree
     * @return
     */
    public static WebPopupMenu makeMpmTreeContextMenu(@NotNull MpmTreeNode forThisNode, @NotNull MpmTree inThisMpmTree) {
        MpmTreeNode self = forThisNode;
        MpmTree mpmTree = inThisMpmTree;

//        System.out.println("Creating popup menu for " + self.name);
        WebPopupMenu menu = new WebPopupMenu();

        switch (self.getType()) {
            case mpm:
                // add metadata
                if (((Mpm) self.getUserObject()).getMetadata() == null) {                                               // if the MPM does already have a metadata element, we cannot add another one; thus we check here if we can offer the user the according context menu item
                    WebMenuItem addMetadata = new WebMenuItem("Add Metadata");
                    addMetadata.addActionListener(actionEvent -> MpmEditingTools.addMetadata(self, mpmTree));           // with method addMetadata() we initialize a default metadata entry and add it to the MPM
                    menu.add(addMetadata);
                }

                // add performance
                WebMenuItem addPerformance = new WebMenuItem("Add Performance");
                addPerformance.addActionListener(actionEvent -> MpmEditingTools.addPerformance(self, mpmTree));     // specify the action to be performed when this menu item is clicked
                menu.add(addPerformance);

                // remove mpm from project
                WebMenuItem removeMpm = new WebMenuItem("Remove MPM from Project");
                removeMpm.addActionListener(actionEvent -> MpmEditingTools.removeMpm(mpmTree.getProjectPane()));
                menu.add(removeMpm);
                break;

            case metadata:
                // add author
                WebMenuItem addAuthor = new WebMenuItem("Add Author");
                addAuthor.addActionListener(actionEvent -> MpmEditingTools.addAuthor(self, mpmTree));
                menu.add(addAuthor);

                // add comment
                WebMenuItem addComment = new WebMenuItem("Add Comment");
                addComment.addActionListener(actionEvent -> MpmEditingTools.addComment(self, mpmTree));
                menu.add(addComment);

                // delete metadata
                WebMenuItem deleteMetadata = new WebMenuItem("Delete");
                deleteMetadata.addActionListener(actionEvent -> MpmEditingTools.deleteMetadata(self, mpmTree));
                menu.add(deleteMetadata);
                break;

            case author:
                // edit author
                WebMenuItem editAuthor = new WebMenuItem("Edit");
                editAuthor.addActionListener(actionEvent -> MpmEditingTools.editAuthor(self, mpmTree));
                menu.add(editAuthor);

                // delete author
                WebMenuItem deleteAuthor = new WebMenuItem("Delete");
                deleteAuthor.addActionListener(actionEvent -> MpmEditingTools.deleteAuthor(self, mpmTree));
                menu.add(deleteAuthor);
                break;

            case comment:
                // edit comment
                WebMenuItem editComment = new WebMenuItem("Edit");
                editComment.addActionListener(actionEvent -> MpmEditingTools.editComment(self, mpmTree));
                menu.add(editComment);

                // delete comment
                WebMenuItem deleteComment = new WebMenuItem("Delete");
                deleteComment.addActionListener(actionEvent -> MpmEditingTools.deleteComment(self, mpmTree));
                menu.add(deleteComment);
                break;

            case relatedResources:
                // add resource
                WebMenuItem addResource = new WebMenuItem("Add Resource");
                addResource.addActionListener(actionEvent -> MpmEditingTools.addResource(self, mpmTree));
                menu.add(addResource);

                // delete all resources
                if (!((Metadata) self.getParent().getUserObject()).getRelatedResources().isEmpty()) {
                    WebMenuItem deleteAllResources = new WebMenuItem("Delete All Resources");
                    deleteAllResources.addActionListener(actionEvent -> MpmEditingTools.deleteAllResources(self, mpmTree));
                    menu.add(deleteAllResources);
                }
                break;

            case relatedResource:
                // edit resource
                WebMenuItem editResource = new WebMenuItem("Edit");
                editResource.addActionListener(actionEvent -> MpmEditingTools.editResource(self, mpmTree));
                menu.add(editResource);

                // delete resource
                WebMenuItem deleteResource = new WebMenuItem("Delete");
                deleteResource.addActionListener(actionEvent -> MpmEditingTools.deleteResource(self, mpmTree));
                menu.add(deleteResource);
                break;

            case performance:
                // add part
                WebMenuItem addPart = new WebMenuItem("Add New Part");
                addPart.addActionListener(actionEvent -> MpmEditingTools.addPart(self, mpmTree));
                menu.add(addPart);

                // import part from MSM
                WebMenu importPartsFromMpm = MpmEditingTools.importPartsFromMpm(self, mpmTree);
                if (importPartsFromMpm != null)
                    menu.add(importPartsFromMpm);

                // edit performance
                WebMenuItem editPerformance = new WebMenuItem("Edit");
                editPerformance.addActionListener(actionEvent -> MpmEditingTools.editPerformance(self, mpmTree));   // specify the action to be performed when this menu item is clicked
                menu.add(editPerformance);

                // delete performance
                WebMenuItem deletePerformance = new WebMenuItem("Delete");
                if (((Mpm) self.getParent().getUserObject()).size() < 2) {
                    deletePerformance.setEnabled(false);
                    deletePerformance.setToolTipText("An MPM must contain at least one performance. Thus, you cannot delete this last one.");
                } else {
                    deletePerformance.addActionListener(actionEvent -> MpmEditingTools.deletePerformance(self, mpmTree));
                }
                menu.add(deletePerformance);
                break;

            case global:
                break;

            case part:
                // edit part
                WebMenuItem editPart = new WebMenuItem("Edit");
                editPart.addActionListener(actionEvent -> MpmEditingTools.editPart(self, mpmTree));
                menu.add(editPart);

                // delete part
                WebMenuItem deletePart = new WebMenuItem("Delete");
                deletePart.addActionListener(actionEvent -> MpmEditingTools.deletePart(self, mpmTree));
                menu.add(deletePart);
                break;

            case header:
                // add style collections
                HashMap<String, HashMap<String, GenericStyle>> allStyleCollections = ((Header) self.getUserObject()).getAllStyleTypes();
                if (!allStyleCollections.containsKey(Mpm.ARTICULATION_STYLE)) {
                    WebMenuItem addArticulationStyleCollection = new WebMenuItem("Add Articulation Style Collection");
                    addArticulationStyleCollection.addActionListener(actionEvent -> MpmEditingTools.addStyleCollection(Mpm.ARTICULATION_STYLE, self, mpmTree));
                    menu.add(addArticulationStyleCollection);
                }
                if (!allStyleCollections.containsKey(Mpm.DYNAMICS_STYLE)) {
                    WebMenuItem addDynamicsStyleCollection = new WebMenuItem("Add Dynamics Style Collection");
                    addDynamicsStyleCollection.addActionListener(actionEvent -> MpmEditingTools.addStyleCollection(Mpm.DYNAMICS_STYLE, self, mpmTree));
                    menu.add(addDynamicsStyleCollection);
                }
                if (!allStyleCollections.containsKey(Mpm.METRICAL_ACCENTUATION_STYLE)) {
                    WebMenuItem addMetricalAccentuationStyleCollection = new WebMenuItem("Add Metrical Accentuation Style Collection");
                    addMetricalAccentuationStyleCollection.addActionListener(actionEvent -> MpmEditingTools.addStyleCollection(Mpm.METRICAL_ACCENTUATION_STYLE, self, mpmTree));
                    menu.add(addMetricalAccentuationStyleCollection);
                }
                if (!allStyleCollections.containsKey(Mpm.ORNAMENTATION_STYLE)) {
                    WebMenuItem addOrnamentationStyleCollection = new WebMenuItem("Add Ornamentation Style Collection");
                    addOrnamentationStyleCollection.addActionListener(actionEvent -> MpmEditingTools.addStyleCollection(Mpm.ORNAMENTATION_STYLE, self, mpmTree));
                    menu.add(addOrnamentationStyleCollection);
                }
                if (!allStyleCollections.containsKey(Mpm.RUBATO_STYLE)) {
                    WebMenuItem addRubatoStyleCollection = new WebMenuItem("Add Rubato Style Collection");
                    addRubatoStyleCollection.addActionListener(actionEvent -> MpmEditingTools.addStyleCollection(Mpm.RUBATO_STYLE, self, mpmTree));
                    menu.add(addRubatoStyleCollection);
                }
                if (!allStyleCollections.containsKey(Mpm.TEMPO_STYLE)) {
                    WebMenuItem addTempoStyleCollection = new WebMenuItem("Add Tempo Style Collection");
                    addTempoStyleCollection.addActionListener(actionEvent -> MpmEditingTools.addStyleCollection(Mpm.TEMPO_STYLE, self, mpmTree));
                    menu.add(addTempoStyleCollection);
                }
                // delete all style collections
                WebMenuItem deleteAllStyleCollections = new WebMenuItem("Delete All Style Collections");
                deleteAllStyleCollections.addActionListener(actionEvent -> MpmEditingTools.deleteAllStyleCollections(self, mpmTree));
                menu.add(deleteAllStyleCollections);
                break;

            case dated:
                // add maps
                HashMap<String, GenericMap> allMaps = ((Dated) self.getUserObject()).getAllMaps();
                if (!allMaps.containsKey(Mpm.ARTICULATION_MAP)) {
                    WebMenuItem addArticulationMap = new WebMenuItem("Add Articulation Map");
                    addArticulationMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.ARTICULATION_MAP, self, mpmTree));
                    menu.add(addArticulationMap);
                }
                if (!allMaps.containsKey(Mpm.ASYNCHRONY_MAP)) {
                    WebMenuItem addAsynchronyMap = new WebMenuItem("Add Asynchrony Map");
                    addAsynchronyMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.ASYNCHRONY_MAP, self, mpmTree));
                    menu.add(addAsynchronyMap);
                }
                if (!allMaps.containsKey(Mpm.DYNAMICS_MAP)) {
                    WebMenuItem addDynamicsMap = new WebMenuItem("Add Dynamics Map");
                    addDynamicsMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.DYNAMICS_MAP, self, mpmTree));
                    menu.add(addDynamicsMap);
                }
                if (!allMaps.containsKey(Mpm.IMPRECISION_MAP_DYNAMICS)) {
                    WebMenuItem addImprecisionMapDynamics = new WebMenuItem("Add Imprecision Map for Dynamics");
                    addImprecisionMapDynamics.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.IMPRECISION_MAP_DYNAMICS, self, mpmTree));
                    menu.add(addImprecisionMapDynamics);
                }
                if (!allMaps.containsKey(Mpm.IMPRECISION_MAP_TIMING)) {
                    WebMenuItem addImprecisionMapTiming = new WebMenuItem("Add Imprecision Map for Timing");
                    addImprecisionMapTiming.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.IMPRECISION_MAP_TIMING, self, mpmTree));
                    menu.add(addImprecisionMapTiming);
                }
                if (!allMaps.containsKey(Mpm.IMPRECISION_MAP_TONEDURATION)) {
                    WebMenuItem addImprecisionMapToneduration = new WebMenuItem("Add Imprecision Map for Tone Duration");
                    addImprecisionMapToneduration.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.IMPRECISION_MAP_TONEDURATION, self, mpmTree));
                    menu.add(addImprecisionMapToneduration);
                }
                if (!allMaps.containsKey(Mpm.IMPRECISION_MAP_TUNING)) {
                    WebMenu addImprecisionMapTuning = new WebMenu("Add Imprecision Map for Tuning");
                    WebMenuItem detuneCents = new WebMenuItem("in Cents");
                    detuneCents.addActionListener(actionEvent -> MpmEditingTools.addImprecisionMapTuning("cents", self, mpmTree));
                    addImprecisionMapTuning.add(detuneCents);
                    WebMenuItem detuneHertz = new WebMenuItem("in Hertz");
                    detuneHertz.addActionListener(actionEvent -> MpmEditingTools.addImprecisionMapTuning("Hz", self, mpmTree));
                    addImprecisionMapTuning.add(detuneHertz);
                    menu.add(addImprecisionMapTuning);
                }
//                if (!allMaps.containsKey(Mpm.IMPRECISION_MAP)) {
//                    WebMenuItem addImprecisionMap = new WebMenuItem("Add Imprecision Map for Unspecified Domain");
//                    addImprecisionMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.IMPRECISION_MAP, self, mpmTree));
//                    menu.add(addImprecisionMap);
//                }
                if (!allMaps.containsKey(Mpm.METRICAL_ACCENTUATION_MAP)) {
                    WebMenuItem addMetricalAccentuationMap = new WebMenuItem("Add Metrical Accentuation Map");
                    addMetricalAccentuationMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.METRICAL_ACCENTUATION_MAP, self, mpmTree));
                    menu.add(addMetricalAccentuationMap);
                }
                if (!allMaps.containsKey(Mpm.ORNAMENTATION_MAP)) {
                    WebMenuItem addOrnamentationMap = new WebMenuItem("Add Ornamentation Map");
                    addOrnamentationMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.ORNAMENTATION_MAP, self, mpmTree));
                    menu.add(addOrnamentationMap);
                }
                if (!allMaps.containsKey(Mpm.RUBATO_MAP)) {
                    WebMenuItem addRubatoMap = new WebMenuItem("Add Rubato Map");
                    addRubatoMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.RUBATO_MAP, self, mpmTree));
                    menu.add(addRubatoMap);
                }
                if (!allMaps.containsKey(Mpm.TEMPO_MAP)) {
                    WebMenuItem addTempoMap = new WebMenuItem("Add Tempo Map");
                    addTempoMap.addActionListener(actionEvent -> MpmEditingTools.addMap(Mpm.TEMPO_MAP, self, mpmTree));
                    menu.add(addTempoMap);
                }

                // delete all maps
                WebMenuItem deleteAllMaps = new WebMenuItem("Delete All Maps");
                deleteAllMaps.addActionListener(actionEvent -> MpmEditingTools.deleteAllMaps(self, mpmTree));
                menu.add(deleteAllMaps);
                break;

            case styleCollection:
                // add style
                WebMenuItem addStyle = new WebMenuItem("Add Style Definition");
                addStyle.addActionListener(actionEvent -> MpmEditingTools.addStyleDef(self, mpmTree));
                menu.add(addStyle);

                // add default styles
                switch (((MpmStyleCollection) self.getUserObject()).getType()) {
                    case "articulationStyles":
                        WebMenuItem addDefaultArticulationStyle = new WebMenuItem("Add Default Articulation Style");
                        addDefaultArticulationStyle.addActionListener(actionEvent -> MpmEditingTools.addDefaultArticulationStyle(self, mpmTree));
                        menu.add(addDefaultArticulationStyle);
                        break;
                    case "dynamicsStyles":
                        WebMenuItem addDefaultDynamicsStyle = new WebMenuItem("Add Default Dynamics Style");
                        addDefaultDynamicsStyle.addActionListener(actionEvent -> MpmEditingTools.addDefaultDynamicsStyle(self, mpmTree));
                        menu.add(addDefaultDynamicsStyle);
                        break;
                    case "tempoStyles":
                        WebMenuItem addDefaultTempoStyle = new WebMenuItem("Add Default Tempo Style");
                        addDefaultTempoStyle.addActionListener(actionEvent -> MpmEditingTools.addDefaultTempoStyle(self, mpmTree));
                        menu.add(addDefaultTempoStyle);
                        break;
                    case "ornamentationStyles":
                        WebMenuItem addDefaultOrnamentationStyle = new WebMenuItem("Add Default Ornamentation Style");
                        addDefaultOrnamentationStyle.addActionListener(actionEvent -> MpmEditingTools.addDefaultOrnamentationStyle(self, mpmTree));
                        menu.add(addDefaultOrnamentationStyle);
                        break;
                }

                // delete all style definitions
                WebMenuItem deleteAllStyleDefs = new WebMenuItem("Delete All Style Definitions");
                deleteAllStyleDefs.addActionListener(actionEvent -> MpmEditingTools.deleteAllStyleDefs(self, mpmTree));
                menu.add(deleteAllStyleDefs);

                // delete collection
                WebMenuItem deleteStyleCollection = new WebMenuItem("Delete");
                deleteStyleCollection.addActionListener(actionEvent -> MpmEditingTools.deleteStyleCollection(self, mpmTree));
                menu.add(deleteStyleCollection);
                break;

            case articulationStyle:
            case metricalAccentuationStyle:
            case dynamicsStyle:
            case genericStyle:
            case ornamentationStyle:
            case rubatoStyle:
            case tempoStyle:
                // add definition to styleDef
                WebMenuItem addDefinition = new WebMenuItem("Add Definition to Style");
                addDefinition.addActionListener(actionEvent -> MpmEditingTools.addDefinition(self, mpmTree));
                menu.add(addDefinition);

                // edit styleDef
                WebMenuItem editStyleDef = new WebMenuItem("Edit");
                editStyleDef.addActionListener(actionEvent -> MpmEditingTools.editStyleDef(self, mpmTree));
                menu.add(editStyleDef);

                // copy style
                // TODO ...

                // move style
                // TODO ...

                // delete styleDef
                WebMenuItem deleteStyleDef = new WebMenuItem("Delete");
                deleteStyleDef.addActionListener(actionEvent -> MpmEditingTools.deleteStyleDef(self, mpmTree));
                menu.add(deleteStyleDef);
                break;

            case articulationMap:
                // add articulation
                WebMenuItem addArticulation = new WebMenuItem("Add Articulation");
                addArticulation.addActionListener(actionEvent -> MpmEditingTools.addArticulation(self, mpmTree));
                menu.add(addArticulation);

                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case asynchronyMap:
                // add asynchrony
                WebMenuItem addAsynchrony = new WebMenuItem("Add Asynchrony");
                addAsynchrony.addActionListener(actionEvent -> MpmEditingTools.addAsynchrony(self, mpmTree));
                menu.add(addAsynchrony);

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case metricalAccentuationMap:
                // add accentuationPattern
                WebMenuItem addAccentuationPattern = new WebMenuItem("Add Accentuation Pattern");
                addAccentuationPattern.addActionListener(actionEvent -> MpmEditingTools.addAccentuationPattern(self, mpmTree));
                menu.add(addAccentuationPattern);

                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case dynamicsMap:
                // add dynamics
                WebMenuItem addDynamics = new WebMenuItem("Add Dynamics");
                addDynamics.addActionListener(actionEvent -> MpmEditingTools.addDynamics(self, mpmTree));
                menu.add(addDynamics);

                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case ornamentationMap:
                // add ornament
                WebMenuItem addOrnament = new WebMenuItem("Add Ornament");
                addOrnament.addActionListener(actionEvent -> MpmEditingTools.addOrnament(self, mpmTree));
                menu.add(addOrnament);

                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case rubatoMap:
                // add rubato
                WebMenuItem addRubato = new WebMenuItem("Add Rubato");
                addRubato.addActionListener(actionEvent -> MpmEditingTools.addRubato(self, mpmTree));
                menu.add(addRubato);

                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case tempoMap:
                // add tempo
                WebMenuItem addTempo = new WebMenuItem("Add Tempo");
                addTempo.addActionListener(actionEvent -> MpmEditingTools.addTempo(self, mpmTree));
                menu.add(addTempo);

                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case genericMap:
                // add style switch
                menu.add(MpmEditingTools.makeAddStyleSwitchMenuEntry(self, mpmTree));

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;


            case imprecisionMap:
                // add distribution
                WebMenuItem addDistribution = new WebMenuItem("Add Distribution");
                addDistribution.addActionListener(actionEvent -> MpmEditingTools.addDistribution(self, mpmTree));
                menu.add(addDistribution);

                // set the domain
                ImprecisionMap imprecisionMap = (ImprecisionMap) self.getUserObject();
                if (imprecisionMap.getDomain().equals("tuning")) {
                    String detuneUnit = imprecisionMap.getDetuneUnit();
                    if (!detuneUnit.equals("Hz")) {
                        WebMenuItem setDetuneUniteToHz = new WebMenuItem("Change Detune Unit to Hertz");
                        setDetuneUniteToHz.addActionListener(actionEvent -> MpmEditingTools.setDetuneUnite("Hz", self, mpmTree));
                        menu.add(setDetuneUniteToHz);
                    }
                    if (!detuneUnit.equals("cents")) {
                        WebMenuItem setDetuneUniteToCents = new WebMenuItem("Change Detune Unit to Cents");
                        setDetuneUniteToCents.addActionListener(actionEvent -> MpmEditingTools.setDetuneUnite("cents", self, mpmTree));
                        menu.add(setDetuneUniteToCents);
                    }
                }

                // move/merge into another part or global
                menu.add(MpmEditingTools.makeMoveMergeMapEntry(self, mpmTree));

                // copy/merge into another part or global
                menu.add(MpmEditingTools.makeCopyMergeMapEntry(self, mpmTree));

                // delete map
                menu.add(MpmEditingTools.makeDeleteMapMenuEntry(self, mpmTree));
                break;

            case articulationDef:
            case accentuationPatternDef:
            case dynamicsDef:
            case ornamentDef:
            case rubatoDef:
            case tempoDef:
                // edit def
                WebMenuItem editDef = new WebMenuItem("Edit");
                editDef.addActionListener(actionEvent -> MpmEditingTools.editDef(self, mpmTree));
                menu.add(editDef);

                // copy def
                // TODO ...

                // move def
                // TODO

                // delete def
                WebMenuItem deleteDef = new WebMenuItem("Delete");
                deleteDef.addActionListener(actionEvent -> MpmEditingTools.deleteDef(self, mpmTree));
                menu.add(deleteDef);
                break;

            case articulation:
                // edit articulation instruction
                WebMenuItem editArticulation = new WebMenuItem("Edit Articulation");
                editArticulation.addActionListener(actionEvent -> MpmEditingTools.editArticulation(self, mpmTree));
                menu.add(editArticulation);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case accentuationPattern:
                // edit accentuationPattern instruction
                WebMenuItem editAccentuationPattern = new WebMenuItem("Edit Accentuation Pattern");
                editAccentuationPattern.addActionListener(actionEvent -> MpmEditingTools.editAccentuationPattern(self, mpmTree));
                menu.add(editAccentuationPattern);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case accentuation:
                // edit the accentuationPatternDef instead of this element
                break;

            case asynchrony:
                // edit asynchrony
                WebMenuItem editAsyncrony = new WebMenuItem("Edit Asynchrony");
                editAsyncrony.addActionListener(actionEvent -> MpmEditingTools.editAsynchrony(self, mpmTree));
                menu.add(editAsyncrony);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case dynamics:
                // edit dynamics
                WebMenuItem editDynamics = new WebMenuItem("Edit Dynamics");
                editDynamics.addActionListener(actionEvent -> MpmEditingTools.editDynamics(self, mpmTree));
                menu.add(editDynamics);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case distributionUniform:
            case distributionGaussian:
            case distributionTriangular:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
            case distributionList:
                // edit distribution
                WebMenuItem editDistribution = new WebMenuItem("Edit Distribution");
                editDistribution.addActionListener(actionEvent -> MpmEditingTools.editDistribution(self, mpmTree));
                menu.add(editDistribution);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case ornament:
                // edit ornament
                WebMenuItem editOrnament = new WebMenuItem("Edit Ornament");
                editOrnament.addActionListener(actionEvent -> MpmEditingTools.editOrnament(self, mpmTree));
                menu.add(editOrnament);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case dynamicsGradient:
            case temporalSpread:
                // edit ornamentDef instead
                break;

            case rubato:
                // edit rubato
                WebMenuItem editRubato = new WebMenuItem("Edit Rubato");
                editRubato.addActionListener(actionEvent -> MpmEditingTools.editRubato(self, mpmTree));
                menu.add(editRubato);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case tempo:
                // edit tempo
                WebMenuItem editTempo = new WebMenuItem("Edit Tempo");
                editTempo.addActionListener(actionEvent -> MpmEditingTools.editTempo(self, mpmTree));
                menu.add(editTempo);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete entry
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));

                // replace tempo instruction by continuous tempo transition from the preceding to the succeeding instruction
                WebMenuItem replaceByContinuous = new WebMenuItem("Replace with Continuous Transition");
                menu.add(replaceByContinuous);
                replaceByContinuous.setToolTipText("<html><center>Removes this segment from the tempoMap.<br>The preceding tempo instruction is adapted to keep up the timing.<br>This operation is not applicable to the first and last tempo instruction in the map.</center></html>");

                MpmTreeNode prev = self.getPreviousSibling();
                while ((prev != null) && (prev.getType() != MpmTreeNode.MpmNodeType.tempo))
                    prev = prev.getPreviousSibling();
                if (prev == null) {                             // this operation is not applicable to the first tempo instruction in the map
                    replaceByContinuous.setEnabled(false);
                } else {
                    MpmTreeNode next = self.getNextSibling();
                    while ((next != null) && (next.getType() != MpmTreeNode.MpmNodeType.tempo))
                        next = next.getNextSibling();
                    if (next == null) {                         // this operation is not applicable to the last tempo instruction in the map
                        replaceByContinuous.setEnabled(false);
                    } else {                                    // here we can enable this instruction
                        replaceByContinuous.addActionListener(actionEvent -> MpmEditingTools.replaceByContinuousTempo(self, mpmTree));
                    }
                }
                break;


            case style:
                // edit style switch
                WebMenuItem editStyleSwitch = new WebMenuItem("Edit Style Switch");
                editStyleSwitch.addActionListener(actionEvent -> MpmEditingTools.editStyleSwitch(self, mpmTree));
                menu.add(editStyleSwitch);

                // move entry
                menu.add(MpmEditingTools.makeMoveMapEntry(self, mpmTree));

                // copy entry
                menu.add(MpmEditingTools.makeCopyMapEntry(self, mpmTree));

                // delete style switch
                menu.add(MpmEditingTools.makeDeleteMapEntryMenuItem(self, mpmTree));
                break;

            case xmlElement:
            case unknown:
            default:
                break;
        }

        return menu;
    }

    /**
     * Invoke this method to immediately open the editor dialog of MPM nodes ... at least those that have
     * an editor dialog and are leaf nodes in the MPM tree. Non-leaf nodes expand on double click and that
     * is what this method is meant to be used for, open the editor on double click.
     * @param forThisNode
     * @param inThisMpmTree
     */
    public static void quickOpenEditor(@NotNull MpmTreeNode forThisNode, @NotNull MpmTree inThisMpmTree) {
        MpmTreeNode self = forThisNode;
        MpmTree mpmTree = inThisMpmTree;

        switch (self.getType()) {
            case mpm:
                break;
            case metadata:
                break;
            case author:
                MpmEditingTools.editAuthor(self, mpmTree);
                break;
            case comment:
                MpmEditingTools.editComment(self, mpmTree);
                break;
            case relatedResources:
                break;
            case relatedResource:
                MpmEditingTools.editResource(self, mpmTree);
                break;
            case performance:
//                MpmEditingTools.editPerformance(self, mpmTree);
                break;
            case global:
                break;
            case part:
//                MpmEditingTools.editPart(self, mpmTree);
                break;
            case header:
                break;
            case dated:
                break;
            case styleCollection:
                break;
            case articulationStyle:
            case metricalAccentuationStyle:
            case dynamicsStyle:
            case genericStyle:
            case ornamentationStyle:
            case rubatoStyle:
            case tempoStyle:
//                MpmEditingTools.editStyleDef(self, mpmTree);
                break;
            case articulationMap:
            case asynchronyMap:
            case metricalAccentuationMap:
            case dynamicsMap:
            case ornamentationMap:
            case rubatoMap:
            case tempoMap:
            case genericMap:
            case imprecisionMap:
                break;
            case accentuationPatternDef:
//                MpmEditingTools.editDef(self, mpmTree);
                break;
            case ornamentDef:
//                MpmEditingTools.editDef(self, mpmTree);
                break;
            case dynamicsGradient:
                // edit the ornamentDef instead of this element
                break;
            case temporalSpread:
                // edit the ornamentDef instead of this element
                break;
            case articulationDef:
            case dynamicsDef:
            case rubatoDef:
            case tempoDef:
                MpmEditingTools.editDef(self, mpmTree);
                break;
            case articulation:
                MpmEditingTools.editArticulation(self, mpmTree);
                break;
            case accentuationPattern:
                MpmEditingTools.editAccentuationPattern(self, mpmTree);
                break;
            case accentuation:
                // edit the accentuationPatternDef instead of this element
                break;
            case asynchrony:
                MpmEditingTools.editAsynchrony(self, mpmTree);
                break;
            case dynamics:
                MpmEditingTools.editDynamics(self, mpmTree);
                break;
            case distributionUniform:
            case distributionGaussian:
            case distributionTriangular:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
            case distributionList:
                MpmEditingTools.editDistribution(self, mpmTree);
                break;
            case ornament:
                MpmEditingTools.editOrnament(self, mpmTree);
                break;
            case rubato:
                MpmEditingTools.editRubato(self, mpmTree);
                break;
            case tempo:
                MpmEditingTools.editTempo(self, mpmTree);
                break;
            case style:
                MpmEditingTools.editStyleSwitch(self, mpmTree);
                break;
            case xmlElement:
            case unknown:
            default:
                break;
        }
    }

    /**
     * This creates the context menu in the ScoreDisplayPanel when an MPM object is right-clicked.
     * @return
     */
    public static WebPopupMenu makeScoreContextMenu(@NotNull MpmTreeNode mpmTreeNode, @NotNull MpmTree mpmTree, @NotNull ScorePage scorePage) {
        WebMenuItem deleteFromScore = new WebMenuItem("Remove from Score");
        deleteFromScore.addActionListener(actionEvent -> {
            scorePage.removeEntry((Element) mpmTreeNode.getUserObject());   // remove the performance instruction from the score page graph structure
            mpmTree.updateNode(mpmTreeNode);                                // update the MpmTree
        });

        WebPopupMenu menu = MpmEditingTools.makeMpmTreeContextMenu(mpmTreeNode, mpmTree);
        menu.add(deleteFromScore);

        return menu;
    }

    /**
     * This creates the context menu in the ScoreDisplayPanel when an MSM object is right-clicked.
     * @return
     */
    public static WebPopupMenu makeScoreContextMenu(@NotNull MsmTreeNode msmTreeNode, @NotNull MsmTree msmTree, @NotNull ScorePage scorePage) {
        WebMenuItem deleteFromScore = new WebMenuItem("Remove from Score");
        deleteFromScore.addActionListener(actionEvent -> {
            scorePage.removeEntry((Element) msmTreeNode.getUserObject());   // remove the note from the score page graph structure
            msmTree.updateNode(msmTreeNode);                                // update the MsmTree
        });

        WebPopupMenu menu = new WebPopupMenu();
        menu.add(deleteFromScore);
        return menu;
    }

    /**
     * changes in the performance should also be visible in the alignment that is currently shown in the audio frame
     * @param performance the performance where the changes were made
     * @param projectPane the project pane from which we get access to all other components, particularly the SyncPlayer and the audio frame/AudioDocumentData object
     * @param updateTempomapPanel set true if timing was affected and the tempomap panel in the audio tab might be updated
     * @return true if the currently displayed alignment in the audio frame has been updated, otherwise false
     */
    protected static boolean updateAudioAlignment(Performance performance, ProjectPane projectPane, boolean updateTempomapPanel) {
        if (performance == null)
            return false;

        Performance selectedPerformance = projectPane.getSyncPlayer().getSelectedPerformance();    // get the performance that is currently selected in the SyncPlayer

        if ((selectedPerformance == null) || (selectedPerformance != performance))
            return false;

        projectPane.getAudioFrame().updateAlignment(true);      // update the alignment visualization in the audio frame

        if (updateTempomapPanel)
            projectPane.getAudioFrame().updateTempomapPanel();  // update the tempomap visualization in the audio frame

        return true;
    }

    /**
     * the procedure to remove the complete MPM document from the project
     * @param projectPane
     */
    private static void removeMpm(@NotNull ProjectPane projectPane) {
        for (Performance performance : projectPane.getMpm().getAllPerformances())
            projectPane.getSyncPlayer().removePerformance(performance);

        projectPane.removeMpm();
    }

    /**
     * the procedure to add Metadata to an mpm node
     * @param mpmNode
     * @param mpmTree
     */
    private static void addMetadata(@NotNull MpmTreeNode mpmNode, @NotNull MpmTree mpmTree) {
        ProjectData project = mpmTree.getProjectPane().getProjectData();
        ArrayList<RelatedResource> relatedResources = new ArrayList<>();

        // by default we can add the MSM to the related resources so we have some data to initialize a non-empty Metadata instance
        relatedResources.add(RelatedResource.createRelatedResource(project.getMsm().getFile().getName(), "msm"));

        // add the score image files to the related resources
        if (!project.getScore().isEmpty()) {
            for (File scorePage : project.getScore().getFiles()) {
                String filename = scorePage.getName();
                String extension = filename.substring(filename.lastIndexOf(".") + 1);
                if (extension.isEmpty())
                    extension = "image";
                relatedResources.add(RelatedResource.createRelatedResource(filename, extension));
            }
        }

        // add the audio files to the related resources
        if (!project.getAudio().isEmpty()) {
            for (Audio audio : project.getAudio()) {
                String filename = audio.getFile().getName();
                String extension = filename.substring(filename.lastIndexOf(".") + 1);
                if (extension.isEmpty())
                    extension = "audio";
                relatedResources.add(RelatedResource.createRelatedResource(filename, extension));
            }
        }

        ((Mpm) mpmNode.getUserObject()).addMetadata(null, null, relatedResources); // add the metadata to the MPM
        mpmTree.reloadNode(mpmNode);                                               // reload the tree node so we can see the changes in the tree
    }

    /**
     * the procedure to delete all metadata from the MPM
     * @param metadataNode
     * @param mpmTree
     */
    private static void deleteMetadata(@NotNull MpmTreeNode metadataNode, @NotNull MpmTree mpmTree) {
        ((Mpm) metadataNode.getParent().getUserObject()).removeMetadata();
        mpmTree.reloadNode(metadataNode.getParent());
    }

    /**
     * the procedure to add a new author to a metadata node
     * @param metadataNode
     * @param mpmTree
     */
    private static void addAuthor(@NotNull MpmTreeNode metadataNode, @NotNull MpmTree mpmTree) {
        AuthorEditor authorEditor = new AuthorEditor();
        Author author = authorEditor.create();
        Metadata metadata = (Metadata) metadataNode.getUserObject();
        if (metadata.addAuthor(author) > -1)            // if the index of the author just added is equal to the number of authors before adding, we have succeeded adding the author
            mpmTree.reloadNode(metadataNode);           // reload the MPM node to see the result
    }

    /**
     * the procedure to edit an author
     * @param authorNode
     * @param mpmTree
     */
    private static void editAuthor(@NotNull MpmTreeNode authorNode, @NotNull MpmTree mpmTree) {
        AuthorEditor authorEditor = new AuthorEditor();
        authorEditor.edit((Author) authorNode.getUserObject());
        mpmTree.updateNode(authorNode);
    }

    /**
     * the procedure to delete an author from the MPM
     * @param authorNode
     * @param mpmTree
     */
    private static void deleteAuthor(@NotNull MpmTreeNode authorNode, @NotNull MpmTree mpmTree) {
        ((Metadata) authorNode.getParent().getUserObject()).removeAuthor(((Author) authorNode.getUserObject()).getName()); // delete the author from the metadata
        mpmTree.reloadNode(authorNode.getParent());                             // update the mpm tree
    }

    /**
     * the procedure to add a new comment to a metadata node
     * @param metadataNode
     * @param mpmTree
     */
    private static void addComment(@NotNull MpmTreeNode metadataNode, @NotNull MpmTree mpmTree) {
        CommentEditor commentEditor = new CommentEditor();
        Comment comment = commentEditor.create();
        Metadata metadata = (Metadata) metadataNode.getUserObject();
        if (metadata.addComment(comment) > -1)          // if the index of the comment just added is equal to the number of comment before adding, we have succeeded adding the comment
            mpmTree.reloadNode(metadataNode);           // reload the MPM node to see the result
    }

    /**
     * the procedure to edit a comment
     * @param commentNode
     * @param mpmTree
     */
    private static void editComment(@NotNull MpmTreeNode commentNode, @NotNull MpmTree mpmTree) {
        CommentEditor commentEditor = new CommentEditor();
        commentEditor.edit((Comment) commentNode.getUserObject());
        mpmTree.updateNode(commentNode);
    }

    /**
     * the procedure to delete a comment from the MPM
     * @param commentNode
     * @param mpmTree
     */
    private static void deleteComment(@NotNull MpmTreeNode commentNode, @NotNull MpmTree mpmTree) {
        ((Metadata) commentNode.getParent().getUserObject()).removeComment((Comment) commentNode.getUserObject()); // delete the comment from the metadata
        mpmTree.reloadNode(commentNode.getParent());                             // update the mpm tree
    }

    /**
     * the procedure to add a related resource
     * @param relatedResourcesNode
     * @param mpmTree
     */
    private static void addResource(@NotNull MpmTreeNode relatedResourcesNode, @NotNull MpmTree mpmTree) {
        ResourceEditor resourceEditor = new ResourceEditor();
        RelatedResource resource = resourceEditor.create();
        Metadata metadata = (Metadata) relatedResourcesNode.getParent().getUserObject();
        if (metadata.addRelatedResource(resource) > -1)
            mpmTree.reloadNode(relatedResourcesNode);
    }

    /**
     * this removes all entries in the relatedResources node
     * @param relatedResourcesNode
     * @param mpmTree
     */
    private static void deleteAllResources(@NotNull MpmTreeNode relatedResourcesNode, @NotNull MpmTree mpmTree) {
        Metadata metadata = (Metadata) relatedResourcesNode.getParent().getUserObject();
        while (!metadata.getRelatedResources().isEmpty())
            metadata.removeRelatedResource(0);
        mpmTree.reloadNode(relatedResourcesNode);
    }

    /**
     * open the resource editor dialog for the specified node
     * @param relatedResourceNode
     * @param mpmTree
     */
    private static void editResource(@NotNull MpmTreeNode relatedResourceNode, @NotNull MpmTree mpmTree) {
        ResourceEditor resourceEditor = new ResourceEditor();
        resourceEditor.edit((RelatedResource) relatedResourceNode.getUserObject());
        mpmTree.updateNode(relatedResourceNode);
    }

    /**
     * this remove the specified related resource from the MPM
     * @param relatedResourceNode
     * @param mpmTree
     */
    private static void deleteResource(@NotNull MpmTreeNode relatedResourceNode, @NotNull MpmTree mpmTree) {
        Metadata metadata = (Metadata) relatedResourceNode.getParent().getParent().getUserObject();
        metadata.removeRelatedResource((RelatedResource) relatedResourceNode.getUserObject());
        mpmTree.reloadNode(relatedResourceNode.getParent());
    }

    /**
     * this method triggers the dialog for performance creation and returns the performance newly created or null (cancel)
     * @return
     */
    public static Performance createPerformanceDialog(Msm msm) {
        PerformanceEditor performanceEditor = new PerformanceEditor();              // create performance edit dialog
        performanceEditor.setPpq(msm.getPPQ());                                     // preset the pulses per quarter value
        return performanceEditor.create();                                          // open it to create a performance
    }

    /**
     * the procedure to add a new performance to an mpm node
     * @param mpmNode
     * @param mpmTree
     */
    private static void addPerformance(@NotNull MpmTreeNode mpmNode, @NotNull MpmTree mpmTree) {
        Performance performance = MpmEditingTools.createPerformanceDialog(mpmTree.getProjectPane().getMsm());   // show the performance edit dialog and get the performance it generates, or null
        if (((Mpm) mpmNode.getUserObject()).addPerformance(performance)) {                  // if the performance was successfully added to the MPM
            mpmTree.reloadNode(mpmNode);                                                    // reload the MPM node to see the result
            mpmTree.getProjectPane().getSyncPlayer().addPerformance(performance, false);    // the SyncPlayer must update its performance chooser
        }
    }

    /**
     * the procedure to edit a performance
     * @param performanceNode
     * @param mpmTree
     */
    private static void editPerformance(@NotNull MpmTreeNode performanceNode, @NotNull MpmTree mpmTree) {
        Performance performance = (Performance) performanceNode.getUserObject();
        PerformanceEditor performanceEditor = new PerformanceEditor();          // open performance edit dialog
        performanceEditor.edit(performance);  // open it to edit the performance
        mpmTree.updateNode(performanceNode);
        mpmTree.getProjectPane().getSyncPlayer().updatePerformance(performance);
    }

    /**
     * the procedure to delete a performance from the MPM
     * @param performanceNode
     * @param mpmTree
     */
    private static void deletePerformance(@NotNull MpmTreeNode performanceNode, @NotNull MpmTree mpmTree) {
        Performance performance = (Performance) performanceNode.getUserObject();
        ((Mpm) performanceNode.getParent().getUserObject()).removePerformance(performance); // delete the performance from the MPM
        mpmTree.getProjectPane().getScore().cleanupDeadNodes();             // remove all entries in the score that are associated with elements in this performance
        mpmTree.reloadNode(performanceNode.getParent());                    // update the mpm tree
        mpmTree.getProjectPane().getSyncPlayer().removePerformance(performance);    // the SyncPlayer must update its performance chooser
    }

    /**
     * open the dialog for creating a new part in the specified performance
     * @param performanceNode
     * @param mpmTree
     */
    private static void addPart(@NotNull MpmTreeNode performanceNode, @NotNull MpmTree mpmTree) {
        PartEditor partEditor = new PartEditor();
        Part part = partEditor.create();
        if(((Performance) performanceNode.getUserObject()).addPart(part))
            mpmTree.reloadNode(performanceNode);
    }

    /**
     * Look in the MSM, any parts not yet in the MPM can be added via the menu that this method creates.
     * @param performanceNode
     * @param mpmTree
     * @return
     */
    private static WebMenu importPartsFromMpm(MpmTreeNode performanceNode, MpmTree mpmTree) {
        Performance performance = (Performance) performanceNode.getUserObject();
        WebMenu importParts = new WebMenu("Import Part from MSM");

        for (Element part : mpmTree.getProjectPane().getMsm().getParts()) {
            String name = Helper.getAttributeValue("name", part);
            int number = Integer.parseInt(Helper.getAttributeValue("number", part));
            int midiChannel = Integer.parseInt(Helper.getAttributeValue("midi.channel", part));
            int midiPort = Integer.parseInt(Helper.getAttributeValue("midi.port", part));

            Part mpmPart = performance.getPart(number);
            if ((mpmPart != null) && mpmPart.getName().equals(name) && (mpmPart.getMidiChannel() == midiChannel) && (mpmPart.getMidiPort() == midiPort)) // if there is already a part in the MPM with identical attributes
                continue;                                                                           // we do not allow creating another one

            WebMenuItem importPart = new WebMenuItem(number + " " + name);
            importPart.addActionListener(actionEvent -> {
                String id = Helper.getAttributeValue("id", part);
                performance.addPart(Part.createPart(name, number, midiChannel, midiPort, id.isEmpty() ? null : id));
                mpmTree.reloadNode(performanceNode);
            });
            importParts.add(importPart);
        }
        if (importParts.getItemCount() == 0)
            return null;

        return importParts;
    }

    /**
     * run the part editor
     * @param partNode
     * @param mpmTree
     */
    private static void editPart(@NotNull MpmTreeNode partNode, @NotNull MpmTree mpmTree) {
        PartEditor partEditor = new PartEditor();
        partEditor.edit((Part) partNode.getUserObject());
        mpmTree.updateNode(partNode);
    }

    /**
     * remove the part from the MPM
     * @param partNode
     * @param mpmTree
     */
    private static void deletePart(@NotNull MpmTreeNode partNode, @NotNull MpmTree mpmTree) {
        Performance performance = partNode.getPerformance();
        MpmTreeNode performanceNode = partNode.getParent();
        ((Performance) performanceNode.getUserObject()).removePart((Part) partNode.getUserObject());
        mpmTree.getProjectPane().getScore().cleanupDeadNodes();
        mpmTree.reloadNode(performanceNode);
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), true);
    }

    /**
     * the procedure to add a style collection to an MPM header
     * @param type
     * @param headerNode
     * @param mpmTree
     */
    private static void addStyleCollection(@NotNull String type, @NotNull MpmTreeNode headerNode, @NotNull MpmTree mpmTree) {
        HashMap<String, GenericStyle> styleCollection = ((Header) headerNode.getUserObject()).addStyleType(type);
        if (styleCollection != null)
            mpmTree.reloadNode(headerNode);
    }

    /**
     * the procedure to empty an MPM header
     * @param headerNode
     * @param mpmTree
     */
    private static void deleteAllStyleCollections(@NotNull MpmTreeNode headerNode, @NotNull MpmTree mpmTree) {
        Performance performance = headerNode.getPerformance();
        Header header = (Header) headerNode.getUserObject();
        header.clear();
        mpmTree.reloadNode(headerNode);
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), true);
    }

    /**
     * empty the style collection
     * @param styleCollectionNode
     * @param mpmTree
     */
    private static void deleteAllStyleDefs(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        Performance performance = styleCollectionNode.getPerformance();
        MpmStyleCollection collection = (MpmStyleCollection) styleCollectionNode.getUserObject();
        Header header = (Header) styleCollectionNode.getParent().getUserObject();
        ArrayList<String> names = new ArrayList<>();
        for (Map.Entry<String, GenericStyle> entry : header.getAllStyleDefs(collection.getType()).entrySet())
            names.add(entry.getKey());
        for (String name : names)
            header.removeStyleDef(collection.getType(), name);
        mpmTree.reloadNode(styleCollectionNode);
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), collection.getType().equals("tempoStyles"));
    }

    /**
     * remove the style collection from its header environment
     * @param styleCollectionNode
     * @param mpmTree
     */
    private static void deleteStyleCollection(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        Performance performance = styleCollectionNode.getPerformance();
        MpmStyleCollection collection = (MpmStyleCollection) styleCollectionNode.getUserObject();
        Header header = (Header) styleCollectionNode.getParent().getUserObject();
        header.removeStyleType(collection.getType());
        mpmTree.reloadNode(styleCollectionNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), collection.getType().equals("tempoStyles"));
    }

    /**
     * create a new MPM styleDef and add it to the style collection node
     * @param styleCollectionNode
     * @param mpmTree
     */
    private static void addStyleDef(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        MpmStyleCollection collection = (MpmStyleCollection) styleCollectionNode.getUserObject();
        Header header = (Header) styleCollectionNode.getParent().getUserObject();
        StyleDefEditor styleDefEditor = new StyleDefEditor(collection.getType(), header);
        header.addStyleDef(collection.getType(), (GenericStyle) styleDefEditor.create());
        mpmTree.reloadNode(styleCollectionNode);
    }

    /**
     * create a default articulation style and add it to the style collection node
     * @param styleCollectionNode this node must be a collection of articulation styles!
     * @param mpmTree
     */
    private static void addDefaultArticulationStyle(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        Header header = (Header) styleCollectionNode.getParent().getUserObject();

        String styleName = "Default Articulations";
        if (header.getStyleDef(Mpm.ARTICULATION_STYLE, styleName) != null) {    // if there is already a style with the same name, add a number to it
            int styleNumber = 1;
            while (header.getStyleDef(Mpm.ARTICULATION_STYLE, styleName.concat(" " + styleNumber)) != null)
                styleNumber++;
            styleName = styleName.concat(" " + styleNumber);
        }

        // create and fill the default style
        ArticulationStyle articulationStyle = (ArticulationStyle) header.addStyleDef(Mpm.ARTICULATION_STYLE, styleName);
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("accent"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("breath"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("legatissimo"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("legato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("legatostop"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("marcato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("nonlegato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("pizzicato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("portato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("sforzato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("snap pizzicato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("spiccato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("staccato"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("staccatissimo"));
        articulationStyle.addDef(ArticulationDef.createDefaultArticulationDef("tenuto"));

        mpmTree.reloadNode(styleCollectionNode);

        Performance performance = styleCollectionNode.getPerformance();
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);
    }

    /**
     * create a default dynamics style and add it to the style collection node
     * @param styleCollectionNode this node must be a collection of dynamics styles!
     * @param mpmTree
     */
    private static void addDefaultDynamicsStyle(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        Header header = (Header) styleCollectionNode.getParent().getUserObject();

        String styleName = "Default Dynamics";
        if (header.getStyleDef(Mpm.DYNAMICS_STYLE, styleName) != null) {    // if there is already a style with the same name, add a number to it
            int styleNumber = 1;
            while (header.getStyleDef(Mpm.DYNAMICS_STYLE, styleName.concat(" " + styleNumber)) != null)
                styleNumber++;
            styleName = styleName.concat(" " + styleNumber);
        }

        // create and fill the default style
        DynamicsStyle dynamicsStyle = (DynamicsStyle) header.addStyleDef(Mpm.DYNAMICS_STYLE, styleName);
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("pppp"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("ppp"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("pp"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("p"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("mp"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("mf"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("f"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("ff"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("fff"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("ffff"));
        dynamicsStyle.addDef(DynamicsDef.createDefaultDynamicsDef("sforzato"));

        mpmTree.reloadNode(styleCollectionNode);

        Performance performance = styleCollectionNode.getPerformance();
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);
    }

    /**
     * create a default tempo style and add it to the style collection node
     * @param styleCollectionNode this node must be a collection of tempo styles!
     * @param mpmTree
     */
    private static void addDefaultTempoStyle(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        Header header = (Header) styleCollectionNode.getParent().getUserObject();

        String styleName = "Default Tempo";
        if (header.getStyleDef(Mpm.TEMPO_STYLE, styleName) != null) {    // if there is already a style with the same name, add a number to it
            int styleNumber = 1;
            while (header.getStyleDef(Mpm.TEMPO_STYLE, styleName.concat(" " + styleNumber)) != null)
                styleNumber++;
            styleName = styleName.concat(" " + styleNumber);
        }

        // create and fill the default style
        TempoStyle tempoStyle = (TempoStyle) header.addStyleDef(Mpm.TEMPO_STYLE, styleName);
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("grave"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("largo"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("lento"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("adagio"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("larghetto"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("adagietto"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("andante"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("andantino"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("maestoso"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("moderato"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("allegretto"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("animato"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("allegro"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("assai"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("vivace"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("presto"));
        tempoStyle.addDef(TempoDef.createDefaultTempoDef("prestissimo"));

        mpmTree.reloadNode(styleCollectionNode);

        Performance performance = styleCollectionNode.getPerformance();
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), true);
    }

    /**
     * create a default ornamentation style and add it to the style collection node
     * @param styleCollectionNode this node must be a collection of ornamentation styles!
     * @param mpmTree
     */
    private static void addDefaultOrnamentationStyle(@NotNull MpmTreeNode styleCollectionNode, @NotNull MpmTree mpmTree) {
        Header header = (Header) styleCollectionNode.getParent().getUserObject();

        String styleName = "Default Ornamentation";
        if (header.getStyleDef(Mpm.ORNAMENTATION_STYLE, styleName) != null) {    // if there is already a style with the same name, add a number to it
            int styleNumber = 1;
            while (header.getStyleDef(Mpm.ORNAMENTATION_STYLE, styleName.concat(" " + styleNumber)) != null)
                styleNumber++;
            styleName = styleName.concat(" " + styleNumber);
        }

        // create and fill the default style
        OrnamentationStyle ornamentationStyle = (OrnamentationStyle) header.addStyleDef(Mpm.ORNAMENTATION_STYLE, styleName);
        ornamentationStyle.addDef(OrnamentDef.createDefaultOrnamentDef("arpeggio"));

        mpmTree.reloadNode(styleCollectionNode);

        Performance performance = styleCollectionNode.getPerformance();
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);
    }

    /**
     * edit a styleDef
     * @param styleDefNode
     * @param mpmTree
     */
    private static void editStyleDef(@NotNull MpmTreeNode styleDefNode, @NotNull MpmTree mpmTree) {
        Performance performance = styleDefNode.getPerformance();
        MpmStyleCollection collection = (MpmStyleCollection) styleDefNode.getParent().getUserObject();
        Header header = (Header) styleDefNode.getParent().getParent().getUserObject();
        GenericStyle styleDef = (GenericStyle) styleDefNode.getUserObject();
        StyleDefEditor styleDefEditor = new StyleDefEditor(collection.getType(), header);
        styleDefEditor.edit(styleDef);
        mpmTree.reloadNode(styleDefNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), collection.getType().equals("tempoStyles"));
    }

    /**
     * delete the styleDef from the MPM
     * @param styleDefNode
     * @param mpmTree
     */
    private static void deleteStyleDef(@NotNull MpmTreeNode styleDefNode, @NotNull MpmTree mpmTree) {
        Performance performance = styleDefNode.getPerformance();
        GenericStyle styleDef = (GenericStyle) styleDefNode.getUserObject();
        MpmStyleCollection collection = (MpmStyleCollection) styleDefNode.getParent().getUserObject();
        Header header = (Header) styleDefNode.getParent().getParent().getUserObject();
        header.removeStyleDef(collection.getType(), styleDef.getName());
        mpmTree.reloadNode(styleDefNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), collection.getType().equals("tempoStyles"));
    }

    /**
     * add a definition to styleDef
     * @param styleDefNode
     * @param mpmTree
     */
    private static void addDefinition(@NotNull MpmTreeNode styleDefNode, @NotNull MpmTree mpmTree) {
        Performance performance = styleDefNode.getPerformance();
        boolean updateTempomapPanel = false;

        if (styleDefNode.getUserObject() instanceof ArticulationStyle) {
            ArticulationStyle styleDef = (ArticulationStyle) styleDefNode.getUserObject();
            styleDef.addDef((new ArticulationDefEditor(styleDef)).create());
        } else if (styleDefNode.getUserObject() instanceof DynamicsStyle) {
            DynamicsStyle styleDef = (DynamicsStyle) styleDefNode.getUserObject();
            styleDef.addDef((new DynamicsDefEditor(styleDef)).create());
        } else if (styleDefNode.getUserObject() instanceof MetricalAccentuationStyle) {
            MetricalAccentuationStyle styleDef = (MetricalAccentuationStyle) styleDefNode.getUserObject();
            styleDef.addDef((new AccentuationPatternDefEditor(styleDef)).create());
        } else if (styleDefNode.getUserObject() instanceof OrnamentationStyle) {
            OrnamentationStyle styleDef = (OrnamentationStyle) styleDefNode.getUserObject();
            styleDef.addDef((new OrnamentDefEditor(styleDef)).create());
        } else if (styleDefNode.getUserObject() instanceof RubatoStyle) {
            RubatoStyle styleDef = (RubatoStyle) styleDefNode.getUserObject();
            styleDef.addDef((new RubatoDefEditor(styleDef)).create());
        } else if (styleDefNode.getUserObject() instanceof TempoStyle) {
            TempoStyle styleDef = (TempoStyle) styleDefNode.getUserObject();
            TempoDef tempoDef = (new TempoDefEditor(styleDef)).create();
            styleDef.addDef(tempoDef);
            updateTempomapPanel = tempoDef != null;
        } else {
            return;
        }
        mpmTree.reloadNode(styleDefNode);
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), updateTempomapPanel);
    }

    /**
     * edit an MPM ...Def element
     * @param defNode
     * @param mpmTree
     */
    private static void editDef(@NotNull MpmTreeNode defNode, @NotNull MpmTree mpmTree) {
        Performance performance = defNode.getPerformance();
        boolean updateTempomapPanel = false;

        if (defNode.getUserObject() instanceof ArticulationDef) {
            ArticulationDefEditor editor = new ArticulationDefEditor((ArticulationStyle) defNode.getParent().getUserObject());
            editor.edit((ArticulationDef) defNode.getUserObject());
        } else if (defNode.getUserObject() instanceof DynamicsDef) {
            DynamicsDefEditor editor = new DynamicsDefEditor((DynamicsStyle) defNode.getParent().getUserObject());
            editor.edit((DynamicsDef) defNode.getUserObject());
        } else if (defNode.getUserObject() instanceof AccentuationPatternDef) {
            AccentuationPatternDefEditor editor = new AccentuationPatternDefEditor((MetricalAccentuationStyle) defNode.getParent().getUserObject());
            editor.edit((AccentuationPatternDef) defNode.getUserObject());
        } else if (defNode.getUserObject() instanceof OrnamentDef) {
            OrnamentDefEditor editor = new OrnamentDefEditor((OrnamentationStyle) defNode.getParent().getUserObject());
            editor.edit((OrnamentDef) defNode.getUserObject());
        } else if (defNode.getUserObject() instanceof RubatoDef) {
            RubatoDefEditor editor = new RubatoDefEditor((RubatoStyle) defNode.getParent().getUserObject());
            editor.edit((RubatoDef) defNode.getUserObject());
        } else if (defNode.getUserObject() instanceof TempoDef) {
            TempoDefEditor editor = new TempoDefEditor((TempoStyle) defNode.getParent().getUserObject());
            editor.edit((TempoDef) defNode.getUserObject());
            updateTempomapPanel = true;
        }
        mpmTree.reloadNode(defNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), updateTempomapPanel);
    }

    /**
     * delete a def from a styleDef
     * @param defNode
     * @param mpmTree
     */
    private static void deleteDef(@NotNull MpmTreeNode defNode, @NotNull MpmTree mpmTree) {
        Performance performance = defNode.getPerformance();
        GenericStyle styleDef = (GenericStyle) defNode.getParent().getUserObject();
        styleDef.removeDef(((AbstractDef) defNode.getUserObject()).getName());
        mpmTree.reloadNode(defNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), styleDef instanceof TempoStyle);
    }

    /**
     * add a map to an MPM dated environment
     * @param datedNode
     * @param mpmTree
     * @param type
     */
    private static void addMap(String type, @NotNull MpmTreeNode datedNode, @NotNull MpmTree mpmTree) {
        GenericMap map = ((Dated) datedNode.getUserObject()).addMap(type);
        if (map != null) {
            Performance performance = datedNode.getPerformance();
            mpmTree.reloadNode(datedNode);
            MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), type.equals(Mpm.TEMPO_MAP));
        }
    }

    /**
     * this is a special case of method addMap() especially for adding tuning imprecision maps
     * @param detuneUnit
     * @param datedNode
     * @param mpmTree
     */
    private static void addImprecisionMapTuning(String detuneUnit, @NotNull MpmTreeNode datedNode, @NotNull MpmTree mpmTree) {
        ImprecisionMap map = (ImprecisionMap) ((Dated) datedNode.getUserObject()).addMap(Mpm.IMPRECISION_MAP_TUNING);
        if (map != null) {
            Performance performance = datedNode.getPerformance();
            map.setDetuneUnit(detuneUnit);
            mpmTree.reloadNode(datedNode);
            MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);
        }
    }

    /**
     * the procedure to remove all maps from an MPM dated environment
     * @param datedNode
     * @param mpmTree
     */
    private static void deleteAllMaps(@NotNull MpmTreeNode datedNode, @NotNull MpmTree mpmTree) {
        Performance performance = datedNode.getPerformance();
        Dated dated = (Dated) datedNode.getUserObject();
        boolean updateTempoMapPanel = dated.getMap(Mpm.TEMPO_STYLE) != null;
        dated.clear();
        mpmTree.getProjectPane().getScore().cleanupDeadNodes();
        mpmTree.reloadNode(datedNode);
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), updateTempoMapPanel);
    }

    /**
     * delete a map from the MPM tree;
     * as this functionality (cluding the creation of the WebMenuItem) is used with several elements (all the maps), this method is a shorthand for it
     * @param mapNode
     * @param mpmTree
     * @return
     */
    private static WebMenuItem makeDeleteMapMenuEntry(MpmTreeNode mapNode, MpmTree mpmTree) {
        WebMenuItem deleteMap = new WebMenuItem("Delete");

        deleteMap.addActionListener(actionEvent -> {
            Performance performance = mapNode.getPerformance();
            Dated dated = (Dated) mapNode.getParent().getUserObject();
            String mapType = ((GenericMap) mapNode.getUserObject()).getType();
            dated.removeMap(mapType);
            mpmTree.getProjectPane().getScore().cleanupDeadNodes();
            mpmTree.reloadNode(mapNode.getParent());
            MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), mapType.equals(Mpm.TEMPO_MAP));
        });

        return deleteMap;
    }

    /**
     * The context menu entry for moving/merging an MPM map into another part's or the global dated environment.
     * @param mapNode
     * @param mpmTree
     * @return
     */
    private static WebMenu makeMoveMergeMapEntry(MpmTreeNode mapNode, MpmTree mpmTree) {
        WebMenu moveMergeMap = new WebMenu("Move Map & Merge into");                                // the context menu sub menu

        // if there is no other place to move/merge this map to, disable this context menu entry
        if ((mapNode.getParent().getParent().getParent().getChildCount() < 2)                       // no other part or global
                && (mpmTree.getProjectPane().getMpm().getAllPerformances().size() < 2)) {           // no other performance
            moveMergeMap.setEnabled(false);
            moveMergeMap.setToolTipText("No other performance, part or global element to move to.");
            return moveMergeMap;
        }

        GenericMap map = (GenericMap) mapNode.getUserObject();                                      // get the handle to the MPM map
        Performance originPerformance = mapNode.getPerformance();

        for (Performance performance : mpmTree.getProjectPane().getMpm().getAllPerformances()) {
            WebMenu performanceMenu = new WebMenu(performance.getName());                           // the performance choice submenu
            MpmTreeNode performanceNode = mpmTree.findNode(performance, false);                     // get the performance's corresponding tree node
            Enumeration<MpmTreeNode> partNodes = performanceNode.children();                        // get all the performance node's children (global and parts)
            ArrayList<WebMenuItem> items = new ArrayList<>();

            while (partNodes.hasMoreElements()) {                                                   // iterate through global and all parts
                MpmTreeNode partNode = partNodes.nextElement();                                     // get the part or global node
                if (partNode == mapNode.getParent().getParent())                                    // if the part/global element is the environment of the input map
                    continue;                                                                       // done, we do not intend to move and merge the map into itself

                WebMenuItem item = new WebMenuItem(partNode.getText(null));                         // create a context menu item for this part/global from the node's text
                item.addActionListener(actionEvent -> {                                             // define the action
                    Dated dated = (partNode.getUserObject() instanceof Global) ? ((Global) partNode.getUserObject()).getDated() : ((Part) partNode.getUserObject()).getDated(); // get the global/part's dated environment
                    GenericMap targetMap = dated.getMap(map.getType());                             // if there is already a map of the input map's type, get it; that is the target map into which we merge the contents
                    if (targetMap == null)                                                          // if there is no such map
                        targetMap = dated.addMap(map.getType());                                    // create it

                    for (KeyValue<Double, Element> e : map.getAllElements()) {                      // for each element in the input map
                        Element copy = e.getValue().copy();                                         // copy it
                        targetMap.addElement(copy);                                                 // add it to the target map
                        MpmEditingTools.handOverScorePosition(e.getValue(), copy, mpmTree.getProjectPane().getScore()); // if the origin element is linked in the score, we have to associate the new one now with that score position
                    }

                    ((Dated) mapNode.getParent().getUserObject()).removeMap(map.getType());         // remove the input map from its dated environment

                    mpmTree.reloadNode(performanceNode.findChildNode(dated, false));                // update the dated node in the target environment so the new map contents are displayed
                    mpmTree.reloadNode(mapNode.getParent());                                        // update the dated node where we removed the map

                    // update the alignment visualization in the audio frame; we have to check the performance that gained the map as well as the performance that lost it
                    boolean updateTempomapPanel = map.getType().equals(Mpm.TEMPO_MAP);
                    if (!MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), updateTempomapPanel))       // if the performance that gained the map was not the one to be updated in the audio frame, it could be the performance that lost the map
                        MpmEditingTools.updateAudioAlignment(originPerformance, mpmTree.getProjectPane(), updateTempomapPanel);  // try to update the alignment of the origin performance of the map
                });
                items.add(item);
            }

            if (items.isEmpty())                                                                    // if there is no place in this performance where the map can be moved
                continue;                                                                           // done, do not add the performance to the context menu

            for (WebMenuItem item : items)                                                          // for each menu item we created
                performanceMenu.add(item);                                                          // add it to the performance submenu

            moveMergeMap.add(performanceMenu);                                                      // add the performance submenu to the context menu
        }

        return moveMergeMap;                                                                        // return the context sub-menu
    }

    /**
     * The context menu entry for copying/merging an MPM map into another part's or the global dated environment.
     * @param mapNode
     * @param mpmTree
     * @return
     */
    private static WebMenu makeCopyMergeMapEntry(MpmTreeNode mapNode, MpmTree mpmTree) {
        WebMenu copyMergeMap = new WebMenu("Copy Map & Merge into");                                // the context menu sub menu

        // if there is no other place to copy/merge this map to, disable this context menu entry
        if ((mapNode.getParent().getParent().getParent().getChildCount() < 2)                       // no other part or global
                && (mpmTree.getProjectPane().getMpm().getAllPerformances().size() < 2)) {           // no other performance
            copyMergeMap.setEnabled(false);
            copyMergeMap.setToolTipText("No other part or global element available to copy into.");
            return copyMergeMap;
        }

        copyMergeMap.setToolTipText("The copies will get unique IDs.");                             // set a tooltip text

        GenericMap map = (GenericMap) mapNode.getUserObject();                                      // get the handle to the MPM map

        for (Performance performance : mpmTree.getProjectPane().getMpm().getAllPerformances()) {
            WebMenu performanceMenu = new WebMenu(performance.getName());                           // the performance choice submenu
            MpmTreeNode performanceNode = mpmTree.findNode(performance, false);                     // get the performance's corresponding tree node
            Enumeration<MpmTreeNode> partNodes = performanceNode.children();                        // get all the performance node's children (global and parts)
            ArrayList<WebMenuItem> items = new ArrayList<>();

            while (partNodes.hasMoreElements()) {                                                   // iterate through global and all parts
                MpmTreeNode partNode = partNodes.nextElement();                                     // get the part or global node
                if (partNode == mapNode.getParent().getParent())                                    // if the part/global element is the environment of the input map
                    continue;                                                                       // done, we do not intend to copy and merge the map into itself

                WebMenuItem item = new WebMenuItem(partNode.getText(null));                         // create a context menu item for this part/global from the node's text
                item.addActionListener(actionEvent -> {                                             // define the action
                    Dated dated = (partNode.getUserObject() instanceof Global) ? ((Global) partNode.getUserObject()).getDated() : ((Part) partNode.getUserObject()).getDated(); // get the global/part's dated environment
                    GenericMap targetMap = dated.getMap(map.getType());                             // if there is already a map of the input map's type, get it; that is the target map into which we merge the contents
                    if (targetMap == null)                                                          // if there is no such map
                        targetMap = dated.addMap(map.getType());                                    // create it

                    for (KeyValue<Double, Element> e : map.getAllElements()) {                      // for each element in the input map
                        Element elt = e.getValue().copy();                                          // copy it
                        Attribute id = elt.getAttribute("id", "http://www.w3.org/XML/1998/namespace");  // get its XML ID
                        if (id != null)                                                             // if it has one, we have to change it to ensure that it is unique
                            id.setValue(id.getValue() + "_mpmToolbox-copy_" + UUID.randomUUID());   // create and set a unique ID
                        targetMap.addElement(elt);                                                  // add the copied element to the target map
                    }

                    mpmTree.reloadNode(performanceNode.findChildNode(dated, false));                // update the dated node in the target environment so the new map contents are displayed
                    MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), map.getType().equals(Mpm.TEMPO_MAP));    // update the alignment visualization in the audio frame
                });
                items.add(item);                                                                    // add the menu item to the context menu
            }

            if (items.isEmpty())                                                                    // if there is no place in this performance where the map can be moved
                continue;                                                                           // done, do not add the performance to the context menu

            for (WebMenuItem item : items)                                                          // for each menu item we created
                performanceMenu.add(item);                                                          // add it to the performance submenu

            copyMergeMap.add(performanceMenu);                                                      // add the performance submenu to the context menu
        }

        return copyMergeMap;                                                                        // return the context sub-menu
    }

    /**
     * move the map entry to a map in another global or part environment
     * @param mapEntryNode
     * @param mpmTree
     * @return
     */
    private static WebMenu makeMoveMapEntry(MpmTreeNode mapEntryNode, MpmTree mpmTree) {
        WebMenu moveMapEntry = new WebMenu("Move to");                                              // the context menu sub menu

        // if there is no other place to move this entry to, disable this context menu entry
        if ((mapEntryNode.getParent().getParent().getParent().getParent().getChildCount() < 2)      // no other part or global
                && (mpmTree.getProjectPane().getMpm().getAllPerformances().size() < 2)) {           // no other performance
            moveMapEntry.setEnabled(false);
            moveMapEntry.setToolTipText("No other performance, part or global element to move to.");
            return moveMapEntry;
        }

        MpmTreeNode mapNode = mapEntryNode.getParent();                                             // get a handle to the map node
        GenericMap map = (GenericMap) mapNode.getUserObject();                                      // get the handle to the MPM map
        Performance originPerformance = mapNode.getPerformance();

        for (Performance performance : mpmTree.getProjectPane().getMpm().getAllPerformances()) {
            WebMenu performanceMenu = new WebMenu(performance.getName());                           // the performance choice submenu
            MpmTreeNode performanceNode = mpmTree.findNode(performance, false);                     // get the performance's corresponding tree node
            Enumeration<MpmTreeNode> partNodes = performanceNode.children();                        // get all the performance node's children (global and parts)
            ArrayList<WebMenuItem> items = new ArrayList<>();

            while (partNodes.hasMoreElements()) {                                                   // iterate through global and all parts
                MpmTreeNode partNode = partNodes.nextElement();                                     // get the part or global node
                if (partNode == mapNode.getParent().getParent())                                    // if the part/global element is the environment of the input map
                    continue;                                                                       // done, we do not intend to move and merge the map into itself

                WebMenuItem item = new WebMenuItem(partNode.getText(null));                         // create a context menu item for this part/global from the node's text
                item.addActionListener(actionEvent -> {                                             // define the action
                    Dated dated = (partNode.getUserObject() instanceof Global) ? ((Global) partNode.getUserObject()).getDated() : ((Part) partNode.getUserObject()).getDated(); // get the global/part's dated environment
                    MpmTreeNode datedNode = partNode.findChildNode(dated, false);
                    GenericMap targetMap = dated.getMap(map.getType());                             // if there is already a map of the input map's type, get it; that is the target map into which we merge the contents
                    if (targetMap == null) {                                                        // if there is no such map
                        targetMap = dated.addMap(map.getType());                                    // create it
                        mpmTree.reloadNode(datedNode);                                              // update the dated node in the target environment so the new map gets displayed
                    }

                    Element entry = (Element) mapEntryNode.getUserObject();                         // get the original entry data
                    Element copy = entry.copy();                                                    // make a copy
                    targetMap.addElement(copy);                                                     // add the copy to the target map
                    MpmEditingTools.handOverScorePosition(entry, copy, mpmTree.getProjectPane().getScore());    // if the origin element is linked in the score, we have to associate the new one now with that score position
                    map.removeElement(entry);                                                       // remove the original entry

                    mpmTree.reloadNode(datedNode.findChildNode(targetMap, false));                  // update the target map node so the new entry is displayed
                    mpmTree.reloadNode(mapNode);                                                    // update the map node where we removed the entry

                    // update the alignment visualization in the audio frame; we have to check the performance that gained the map as well as the performance that lost it
                    boolean updateTempomapPanel = map.getType().equals(Mpm.TEMPO_MAP);
                    if (!MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), updateTempomapPanel))       // if the performance that gained the map was not the one to be updated in the audio frame, it could be the performance that lost the map
                        MpmEditingTools.updateAudioAlignment(originPerformance, mpmTree.getProjectPane(), updateTempomapPanel);  // try to update the alignment of the origin performance of the map
                });
                items.add(item);
            }

            if (items.isEmpty())                                                                    // if there is no place in this performance where the entry can be moved
                continue;                                                                           // done, do not add the performance to the context menu

            for (WebMenuItem item : items)                                                          // for each menu item we created
                performanceMenu.add(item);                                                          // add it to the performance submenu

            moveMapEntry.add(performanceMenu);                                                      // add the performance submenu to the context menu
        }

        return moveMapEntry;                                                                        // return the context sub-menu
    }

    /**
     * copy the map entry to a map in another global or part environment
     * @param mapEntryNode
     * @param mpmTree
     * @return
     */
    private static WebMenu makeCopyMapEntry(MpmTreeNode mapEntryNode, MpmTree mpmTree) {
        WebMenu copyMapEntry = new WebMenu("Copy to");                                              // the context menu sub menu

        // if there is no other place to copy this entry to, disable this context menu entry
        if ((mapEntryNode.getParent().getParent().getParent().getParent().getChildCount() < 2)      // no other part or global
                && (mpmTree.getProjectPane().getMpm().getAllPerformances().size() < 2)) {           // no other performance
            copyMapEntry.setEnabled(false);
            copyMapEntry.setToolTipText("No other performance, part or global element to copy to.");
            return copyMapEntry;
        }

        copyMapEntry.setToolTipText("The copy will get a unique ID.");                              // set a tooltip text

        MpmTreeNode mapNode = mapEntryNode.getParent();                                             // get a handle to the map node
        GenericMap map = (GenericMap) mapNode.getUserObject();                                      // get the handle to the MPM map

        for (Performance performance : mpmTree.getProjectPane().getMpm().getAllPerformances()) {
            WebMenu performanceMenu = new WebMenu(performance.getName());                           // the performance choice submenu
            MpmTreeNode performanceNode = mpmTree.findNode(performance, false);                     // get the performance's corresponding tree node
            Enumeration<MpmTreeNode> partNodes = performanceNode.children();                        // get all the performance node's children (global and parts)
            ArrayList<WebMenuItem> items = new ArrayList<>();

            while (partNodes.hasMoreElements()) {                                                   // iterate through global and all parts
                MpmTreeNode partNode = partNodes.nextElement();                                     // get the part or global node
                if (partNode == mapNode.getParent().getParent())                                    // if the part/global element is the environment of the input map
                    continue;                                                                       // done, we do not intend to move and merge the map into itself

                WebMenuItem item = new WebMenuItem(partNode.getText(null));                         // create a context menu item for this part/global from the node's text
                item.addActionListener(actionEvent -> {                                             // define the action
                    Dated dated = (partNode.getUserObject() instanceof Global) ? ((Global) partNode.getUserObject()).getDated() : ((Part) partNode.getUserObject()).getDated(); // get the global/part's dated environment
                    MpmTreeNode datedNode = partNode.findChildNode(dated, false);
                    GenericMap targetMap = dated.getMap(map.getType());                             // if there is already a map of the input map's type, get it; that is the target map into which we merge the contents
                    if (targetMap == null) {                                                        // if there is no such map
                        targetMap = dated.addMap(map.getType());                                    // create it
                        mpmTree.reloadNode(datedNode);                                              // update the dated node in the target environment so the new map gets displayed
                    }

                    Element entry = (Element) mapEntryNode.getUserObject();                         // get the original entry data
                    Element copy = entry.copy();                                                    // make a copy

                    Attribute id = copy.getAttribute("id", "http://www.w3.org/XML/1998/namespace"); // get its XML ID
                    if (id != null)                                                                 // if it has one, we have to change it to ensure that it is unique
                        id.setValue(id.getValue() + "_mpmToolbox-copy_" + UUID.randomUUID());       // create and set a unique ID

                    targetMap.addElement(copy);                                                     // add the copy to the target map

                    mpmTree.reloadNode(datedNode.findChildNode(targetMap, false));                  // update the target map node so the new entry is displayed
                    MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), map.getType().equals(Mpm.TEMPO_MAP));    // update the alignment visualization in the audio frame
                });
                items.add(item);
            }

            if (items.isEmpty())                                                                    // if there is no place in this performance where the entry can be copied
                continue;                                                                           // done, do not add the performance to the context menu

            for (WebMenuItem item : items)                                                          // for each menu item we created
                performanceMenu.add(item);                                                          // add it to the performance submenu

            copyMapEntry.add(performanceMenu);                                                      // add the performance submenu to the context menu
        }

        return copyMapEntry;                                                                        // return the context sub-menu
    }

    /**
     * Menu item and functionality for adding a style switch to an MPM map
     * @param mapNode
     * @param mpmTree
     * @return
     */
    private static WebMenuItem makeAddStyleSwitchMenuEntry(MpmTreeNode mapNode, MpmTree mpmTree) {
        WebMenuItem addStyleSwitch = new WebMenuItem("Add Style Switch");

        addStyleSwitch.addActionListener(actionEvent -> {
            GenericMap map = (GenericMap) mapNode.getUserObject();
            StyleSwitchEditor.StyleSwitchData style = (new StyleSwitchEditor(map)).create();

            if (style == null)
                return;

            Performance performance = mapNode.getPerformance();

            if (map instanceof ArticulationMap)
                ((ArticulationMap) map).addStyleSwitch(style.date, style.styleName, style.defaultArticulation, style.id);
            else
                map.addStyleSwitch(style.date, style.styleName, style.id);

            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), map.getType().equals(Mpm.TEMPO_MAP));    // update the alignment visualization in the audio frame
        });

        return addStyleSwitch;
    }

    /**
     * edit a style switch
     * @param styleSwitchNode
     * @param mpmTree
     */
    private static void editStyleSwitch(MpmTreeNode styleSwitchNode, MpmTree mpmTree) {
        GenericMap map = (GenericMap) styleSwitchNode.getParent().getUserObject();

        Element styleElement = (Element) styleSwitchNode.getUserObject();
        StyleSwitchEditor.StyleSwitchData style = new StyleSwitchEditor.StyleSwitchData(styleElement);

        // create and start editor
        StyleSwitchEditor editor = new StyleSwitchEditor(map);
        StyleSwitchEditor.StyleSwitchData newStyle = editor.edit(style);

        if (style == newStyle)  // no change
            return;             // no need to do anything

        Performance performance = styleSwitchNode.getPerformance();
        map.removeElement(styleElement);    // remove the old style switch from the map

        // add the new style switch to the map
        int index;
        if (map instanceof ArticulationMap)
            index = ((ArticulationMap) map).addStyleSwitch(newStyle.date, newStyle.styleName, newStyle.defaultArticulation, newStyle.id);
        else
            index = map.addStyleSwitch(newStyle.date, newStyle.styleName, newStyle.id);

        MpmEditingTools.handOverScorePosition(styleElement, map.getElement(index), mpmTree.getProjectPane().getScore());    // if the old style switch is linked in the score, we have to associate the new switch now with that score position
        mpmTree.reloadNode(styleSwitchNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), map.getType().equals(Mpm.TEMPO_MAP));    // update the alignment visualization in the audio frame
    }

    /**
     * delete a map entry
     * @param mapEntryNode
     * @param mpmTree
     * @return menu item
     */
    private static WebMenuItem makeDeleteMapEntryMenuItem(MpmTreeNode mapEntryNode, MpmTree mpmTree) {
        WebMenuItem deleteMapEntry = new WebMenuItem("Delete");

        deleteMapEntry.addActionListener(actionEvent -> {
            Performance performance = mapEntryNode.getPerformance();
            GenericMap map = (GenericMap) mapEntryNode.getParent().getUserObject();
            map.removeElement((Element) mapEntryNode.getUserObject());
            mpmTree.getProjectPane().getScore().cleanupDeadNodes();
            mpmTree.reloadNode(mapEntryNode.getParent());
            MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), map.getType().equals(Mpm.TEMPO_MAP));    // update the alignment visualization in the audio frame
        });

        return deleteMapEntry;

    }

    /**
     * set the detune unit of a tuning imprecision map
     * @param unit
     * @param imprecisionMapTuningNode
     * @param mpmTree
     */
    private static void setDetuneUnite(@NotNull String unit, @NotNull MpmTreeNode imprecisionMapTuningNode, @NotNull MpmTree mpmTree) {
        ((ImprecisionMap) imprecisionMapTuningNode.getUserObject()).setDetuneUnit(unit);
        mpmTree.updateNode(imprecisionMapTuningNode);
        MpmEditingTools.updateAudioAlignment(imprecisionMapTuningNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create an asynchrony instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addAsynchrony(MpmTreeNode mapNode, MpmTree mpmTree) {
        AsynchronyEditor editor = new AsynchronyEditor();
        AsynchronyEditor.AsynchronyData asynchrony = editor.create();
        if (asynchrony != null) {
            AsynchronyMap map = (AsynchronyMap) mapNode.getUserObject();
            int index = map.addAsynchrony(asynchrony.date, asynchrony.millisecondsOffset);

            if (asynchrony.id != null)
                map.getElement(index).addAttribute(new Attribute("xml:id", "http://www.w3.org/XML/1998/namespace", asynchrony.id));

            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM asynchrony instruction
     * @param asynchronyNode
     * @param mpmTree
     */
    private static void editAsynchrony(MpmTreeNode asynchronyNode, MpmTree mpmTree) {
        AsynchronyMap map = (AsynchronyMap) asynchronyNode.getParent().getUserObject();

        Element asynchronyElement = (Element) asynchronyNode.getUserObject();
        AsynchronyEditor.AsynchronyData asynchrony = new AsynchronyEditor.AsynchronyData(asynchronyElement);

        // create and start editor
        AsynchronyEditor editor = new AsynchronyEditor();
        AsynchronyEditor.AsynchronyData newAsynchrony = editor.edit(asynchrony);

        if (asynchrony == newAsynchrony)        // no change
            return;                             // no need to do anything

        Performance performance = asynchronyNode.getPerformance();
        map.removeElement(asynchronyElement);   // remove the old instruction from the map

        // add the new instruction to the map
        int index = map.addAsynchrony(newAsynchrony.date, newAsynchrony.millisecondsOffset);
        if (newAsynchrony.id != null)
            map.getElement(index).addAttribute(new Attribute("xml:id", "http://www.w3.org/XML/1998/namespace", newAsynchrony.id));

        MpmEditingTools.handOverScorePosition(asynchronyElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(asynchronyNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create an accentuationPattern instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addAccentuationPattern(MpmTreeNode mapNode, MpmTree mpmTree) {
        MetricalAccentuationMap map = (MetricalAccentuationMap) mapNode.getUserObject();

        AccentuationPatternEditor editor = new AccentuationPatternEditor(map);
        MetricalAccentuationData accentuationPattern = editor.create();
        if (accentuationPattern != null) {
            int index = map.addAccentuationPattern(accentuationPattern.startDate, accentuationPattern.accentuationPatternDefName, accentuationPattern.scale, accentuationPattern.loop, accentuationPattern.stickToMeasures);

            if (accentuationPattern.xmlId != null)
                map.getElement(index).addAttribute(new Attribute("xml:id", "http://www.w3.org/XML/1998/namespace", accentuationPattern.xmlId));

            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM accentuationPattern instruction
     * @param accentuationPatternNode
     * @param mpmTree
     */
    private static void editAccentuationPattern(MpmTreeNode accentuationPatternNode, MpmTree mpmTree) {
        MetricalAccentuationMap map = (MetricalAccentuationMap) accentuationPatternNode.getParent().getUserObject();

        Element accentuationPatternElement = (Element) accentuationPatternNode.getUserObject();
        MetricalAccentuationData accentuationPattern = new MetricalAccentuationData(accentuationPatternElement);

        // create and start editor
        AccentuationPatternEditor editor = new AccentuationPatternEditor(map);
        MetricalAccentuationData newAccentuationPattern = editor.edit(accentuationPattern);

        if (accentuationPattern == newAccentuationPattern)              // no change
            return;                                                     // no need to do anything

        Performance performance = accentuationPatternNode.getPerformance();
        map.removeElement(accentuationPatternElement);                  // remove the old instruction from the map
        int index = map.addAccentuationPattern(newAccentuationPattern); // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(accentuationPatternElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(accentuationPatternNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create an articulation instrucEletion
     * @param mapNode
     * @param mpmTree
     */
    private static void addArticulation(MpmTreeNode mapNode, MpmTree mpmTree) {
        ArticulationMap map = (ArticulationMap) mapNode.getUserObject();
        Msm msm = mpmTree.getProjectPane().getMsm();

        ArticulationEditor editor = new ArticulationEditor(map, msm, mapNode.getPerformance());
        ArticulationData articulation = editor.create();
        if (articulation != null) {
            map.addArticulation(articulation);
            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM articulation instruction
     * @param articulationNode
     * @param mpmTree
     */
    private static void editArticulation(MpmTreeNode articulationNode, MpmTree mpmTree) {
        ArticulationMap map = (ArticulationMap) articulationNode.getParent().getUserObject();
        Msm msm = mpmTree.getProjectPane().getMsm();

        Element articulationElement = (Element) articulationNode.getUserObject();
        ArticulationData articulation = new ArticulationData(articulationElement);

        // create and start editor
        ArticulationEditor editor = new ArticulationEditor(map, msm, articulationNode.getPerformance());
        ArticulationData newArticulation = editor.edit(articulation);

        if (articulation == newArticulation)                // no change
            return;                                         // no need to do anything

        Performance performance = articulationNode.getPerformance();
        map.removeElement(articulationElement);             // remove the old instruction from the map
        int index = map.addArticulation(newArticulation);   // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(articulationElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(articulationNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create an ornament instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addOrnament(MpmTreeNode mapNode, MpmTree mpmTree) {
        OrnamentationMap map = (OrnamentationMap) mapNode.getUserObject();

        OrnamentEditor editor = new OrnamentEditor(map, mpmTree.getProjectPane(), mapNode.getPerformance());
        OrnamentData ornament = editor.create();
        if (ornament != null) {
            map.addOrnament(ornament);
            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM ornament instruction
     * @param ornamentNode
     * @param mpmTree
     */
    private static void editOrnament(MpmTreeNode ornamentNode, MpmTree mpmTree) {
        OrnamentationMap map = (OrnamentationMap) ornamentNode.getParent().getUserObject();

        Element ornamentElement = (Element) ornamentNode.getUserObject();
        OrnamentData ornament = new OrnamentData(ornamentElement);

        // create and start editor
        OrnamentEditor editor = new OrnamentEditor(map, mpmTree.getProjectPane(), ornamentNode.getPerformance());
        OrnamentData newOrnament = editor.edit(ornament);

        if (ornament == newOrnament)                                // no change
            return;                                                 // no need to do anything

        Performance performance = ornamentNode.getPerformance();
        map.removeElement(ornamentElement);                         // remove the old instruction from the map
        int index = map.addOrnament(newOrnament);                   // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(ornamentElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(ornamentNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create an rubato instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addRubato(MpmTreeNode mapNode, MpmTree mpmTree) {
        RubatoMap map = (RubatoMap) mapNode.getUserObject();

        RubatoEditor editor = new RubatoEditor(map);
        RubatoData rubato = editor.create();
        if (rubato != null) {
            map.addRubato(rubato);
            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM rubato instruction
     * @param rubatoNode
     * @param mpmTree
     */
    private static void editRubato(MpmTreeNode rubatoNode, MpmTree mpmTree) {
        RubatoMap map = (RubatoMap) rubatoNode.getParent().getUserObject();

        Element rubatoElement = (Element) rubatoNode.getUserObject();
        RubatoData rubato = new RubatoData(rubatoElement);

        // create and start editor
        RubatoEditor editor = new RubatoEditor(map);
        RubatoData newRubato = editor.edit(rubato);

        if (rubato == newRubato)                                    // no change
            return;                                                 // no need to do anything

        Performance performance = rubatoNode.getPerformance();
        map.removeElement(rubatoElement);                           // remove the old instruction from the map
        int index = map.addRubato(newRubato);                       // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(rubatoElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(rubatoNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create a dynamics instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addDynamics(MpmTreeNode mapNode, MpmTree mpmTree) {
        DynamicsMap map = (DynamicsMap) mapNode.getUserObject();

        DynamicsEditor editor = new DynamicsEditor(map);
        DynamicsData dynamics = editor.create();
        if (dynamics != null) {
            map.addDynamics(dynamics);
            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM dynamics node
     * @param dynamicsNode
     * @param mpmTree
     */
    private static void editDynamics(MpmTreeNode dynamicsNode, MpmTree mpmTree) {
        DynamicsMap map = (DynamicsMap) dynamicsNode.getParent().getUserObject();

        Element dynamicsElement = (Element) dynamicsNode.getUserObject();
        DynamicsData dynamics = new DynamicsData(dynamicsElement);

        // create and start editor
        DynamicsEditor editor = new DynamicsEditor(map);
        DynamicsData newDynamics = editor.edit(dynamics);

        if (dynamics == newDynamics)                                // no change
            return;                                                 // no need to do anything

        Performance performance = dynamicsNode.getPerformance();
        map.removeElement(dynamicsElement);                         // remove the old instruction from the map
        int index = map.addDynamics(newDynamics);                   // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(dynamicsElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(dynamicsNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * create a tempo instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addTempo(MpmTreeNode mapNode, MpmTree mpmTree) {
        TempoMap map = (TempoMap) mapNode.getUserObject();

        TempoEditor editor = new TempoEditor(map);
        TempoData tempo = editor.create();
        if (tempo != null) {
            map.addTempo(tempo);
            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), true);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM tempo node
     * @param tempoNode
     * @param mpmTree
     */
    private static void editTempo(MpmTreeNode tempoNode, MpmTree mpmTree) {
        TempoMap map = (TempoMap) tempoNode.getParent().getUserObject();

        Element tempoElement = (Element) tempoNode.getUserObject();
        TempoData tempo = new TempoData(tempoElement);

        // create and start editor
        TempoEditor editor = new TempoEditor(map);
        TempoData newTempo = editor.edit(tempo);

        if (tempo == newTempo)                                      // no change
            return;                                                 // no need to do anything

        Performance performance = tempoNode.getPerformance();
        map.removeElement(tempoElement);                            // remove the old instruction from the map
        int index = map.addTempo(newTempo);                         // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(tempoElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(tempoNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), true);    // update the alignment visualization in the audio frame
    }

    /**
     * replace tempo instruction by continuous tempo transition from the preceding to the succeeding instruction
     * @param tempoNode
     * @param mpmTree
     */
    private static void replaceByContinuousTempo(MpmTreeNode tempoNode, MpmTree mpmTree) {
        TempoMap map = (TempoMap) tempoNode.getParent().getUserObject();

        MpmTreeNode prev = tempoNode.getPreviousSibling();
        while ((prev != null) && (prev.getType() != MpmTreeNode.MpmNodeType.tempo))
            prev = prev.getPreviousSibling();

        if (prev == null)
            return;

        MpmTreeNode next = tempoNode.getNextSibling();
        while ((next != null) && (next.getType() != MpmTreeNode.MpmNodeType.tempo))
            next = next.getNextSibling();

        if (next == null)
            return;

        Element tempoElement = (Element) tempoNode.getUserObject();
        TempoData tempoData = map.getTempoDataOf(map.getElementIndexOf(tempoElement));

        Element prevElement = (Element) prev.getUserObject();
        int prevIndex = map.getElementIndexOf(prevElement);
        TempoData prevData = map.getTempoDataOf(prevIndex);

        Element nextElement = (Element) next.getUserObject();
        TempoData nextData = map.getTempoDataOf(map.getElementIndexOf(nextElement));

        Performance performance = tempoNode.getPerformance();

        map.simplify(new ArrayList<>(Arrays.asList(prevData, tempoData, nextData)), false, tempoNode.getPerformance().getPPQ());

        MpmEditingTools.handOverScorePosition(prevElement, map.getElement(prevIndex), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(prev.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), true);    // update the alignment visualization in the audio frame
    }

    /**
     * create a distribution instruction
     * @param mapNode
     * @param mpmTree
     */
    private static void addDistribution(MpmTreeNode mapNode, MpmTree mpmTree) {
        ImprecisionMap map = (ImprecisionMap) mapNode.getUserObject();

        DistributionEditor editor = new DistributionEditor();
        DistributionData distribution = editor.create();
        if (distribution != null) {
            map.addDistribution(distribution);
            mpmTree.reloadNode(mapNode);
            MpmEditingTools.updateAudioAlignment(mapNode.getPerformance(), mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
        }
    }

    /**
     * edit an MPM distribution node
     * @param distributionNode
     * @param mpmTree
     */
    private static void editDistribution(MpmTreeNode distributionNode, MpmTree mpmTree) {
        ImprecisionMap map = (ImprecisionMap) distributionNode.getParent().getUserObject();

        Element distributionElement = (Element) distributionNode.getUserObject();
        DistributionData distribution = new DistributionData(distributionElement);

        // create and start editor
        DistributionEditor editor = new DistributionEditor();
        DistributionData newDistribution = editor.edit(distribution);

        if (distribution == newDistribution)                        // no change
            return;                                                 // no need to do anything

        Performance performance = distributionNode.getPerformance();
        map.removeElement(distributionElement);                     // remove the old instruction from the map
        int index = map.addDistribution(newDistribution);           // add the new instruction to the map
        MpmEditingTools.handOverScorePosition(distributionElement, map.getElement(index), mpmTree.getProjectPane().getScore());   // if the old instruction is linked in the score, we have to associate the new now with that score position
        mpmTree.reloadNode(distributionNode.getParent());
        MpmEditingTools.updateAudioAlignment(performance, mpmTree.getProjectPane(), false);    // update the alignment visualization in the audio frame
    }

    /**
     * This is a helper function.
     * If the old map entry is linked in the score, we have to associate the new now with that score position.
     * @param prevMapElement the old map entry
     * @param newMapElement the new map entry
     * @param score the score data
     */
    private static void handOverScorePosition(Element prevMapElement, Element newMapElement, Score score) {
        for (ScorePage page : score.getAllPages()) {                // check every score page
            ScoreNode node = page.getNode(prevMapElement);          // is the previous element linked on this page
            if (node == null)                                       // if not linked on the page
                continue;                                           // go on with the next
            page.addEntry(node.getX(), node.getY(), newMapElement); // add the new element at the same position on that page
            page.removeEntry(prevMapElement);                       // remove the old one
        }
    }
}
