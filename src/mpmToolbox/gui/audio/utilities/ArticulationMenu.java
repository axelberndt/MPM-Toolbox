package mpmToolbox.gui.audio.utilities;

import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import meico.mei.Helper;
import meico.mpm.Mpm;
import meico.mpm.elements.Part;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.ArticulationMap;
import meico.mpm.elements.maps.GenericMap;
import meico.mpm.elements.maps.data.ArticulationData;
import meico.mpm.elements.styles.GenericStyle;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.mpmEditingTools.editDialogs.ArticulationEditor;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.mpmTree.MpmTreeNode;
import mpmToolbox.projectData.alignment.Note;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * This class creates a submenu in the piano roll context menu to articulate a note.
 */
public class ArticulationMenu extends WebMenu {
    private final Note note;
    private final Performance performance;
    private final MpmTree mpmTree;

    /**
     * constructor
     * @param note The note is from an Alignment that was computed from the performance as given subsequently! This is important as the note's date will be converted to the MSM PPQ here.
     * @param performance
     * @param mpmTree
     */
    public ArticulationMenu(Note note, Performance performance, MpmTree mpmTree) {
        super("Articulate");

        this.performance = performance;
        this.note = note;
        this.mpmTree = mpmTree;

        if (this.performance == null) {    // we have no performance to create an articulation
            this.setEnabled(false);
            this.setToolTipText("Articulations can only be added to performances, not to audio alignment.");  // explain by tooltip
            return;
        }

//        this.setEnabled(true);

        // get the dates of the note according to PPQ in the performance and in MSM
//        double msmDate = this.note.getDate();
        int ppqMsm = mpmTree.getProjectPane().getMsm().getPPQ();
        int ppqMpm = performance.getPPQ();
//        double mpmPerformanceDate = (msmDate * ppqMpm) / ppqMsm;
        double mpmPerformanceDate = this.note.getDate();
        double msmDate = (mpmPerformanceDate * ppqMsm) / ppqMpm;

        // submenu to create a global articulation
        WebMenu globalArticulations = (WebMenu) this.add(new WebMenu("Global"));
        GenericStyle style;
        GenericMap globalMap = performance.getGlobal().getDated().getMap(Mpm.ARTICULATION_MAP);
        if (globalMap != null) {
            style = globalMap.getStyleAt(mpmPerformanceDate, Mpm.ARTICULATION_STYLE);
            if (style != null) {
                for (Object def : style.getAllDefs().keySet()) {
                    String defName = (String) def;
                    globalArticulations.add(defName).addActionListener(actionEvent -> {
                        Element articulationElement = globalMap.getElement(((ArticulationMap) globalMap).addArticulation(mpmPerformanceDate, defName, "#" + this.note.getId(), null));
                        MpmTreeNode mapNode = this.mpmTree.findNode(globalMap, false);
                        this.updateMpmTreeAndAlignment(mapNode, articulationElement);
                    });
                }
            }
        }
        // Edit Articulation Dialog
        globalArticulations.add("New Articulation").addActionListener(actionEvent -> {
           ArticulationMap map = (ArticulationMap) ((globalMap != null) ? globalMap : this.performance.getGlobal().getDated().addMap(Mpm.ARTICULATION_MAP));
            ArticulationEditor editor = new ArticulationEditor(map, mpmTree.getProjectPane().getMsm(), this.performance);
            editor.setNoteId(this.note.getId());

            editor.setDate(mpmPerformanceDate);

            ArticulationData articulation = editor.create();
            if (articulation != null) {
                Element articulationElement = map.getElement(map.addArticulation(articulation));
                MpmTreeNode mapNode = this.mpmTree.findNode((globalMap == null) ? performance.getGlobal().getDated() : map, false);
                this.updateMpmTreeAndAlignment(mapNode, articulationElement);
            } else if (globalMap == null) {
                this.performance.getGlobal().getDated().removeMap(Mpm.ARTICULATION_MAP);
            }
        });

        // submenu to create a local articulation
        Element msmPart = this.note.getMsmPart();
        int partNumber = Integer.parseInt(Helper.getAttributeValue("number", msmPart));
        WebMenu localArticulations = (WebMenu) this.add(new WebMenu("Part " + partNumber + " " + Helper.getAttributeValue("name", msmPart)));
        Part mpmPart = performance.getPart(partNumber);
        GenericMap localMap = (mpmPart == null) ? null : mpmPart.getDated().getMap(Mpm.ARTICULATION_MAP);
        if (localMap != null) {
            style = localMap.getStyleAt(mpmPerformanceDate, Mpm.ARTICULATION_STYLE);
            if (style != null) {
                for (Object def : style.getAllDefs().keySet()) {
                    String defName = (String) def;
                    localArticulations.add(defName).addActionListener(actionEvent -> {
                        Element articulationElement = localMap.getElement(((ArticulationMap) localMap).addArticulation(mpmPerformanceDate, defName, "#" + this.note.getId(), null));
                        MpmTreeNode mapNode = this.mpmTree.findNode(localMap, false);
                        this.updateMpmTreeAndAlignment(mapNode, articulationElement);
                    });
                }
            }
        }
        // Edit Articulation Dialog
        localArticulations.add("New Articulation").addActionListener(actionEvent -> {
            Part tempPart = mpmPart;
            if (mpmPart == null) {
                tempPart = Part.createPart(Helper.getAttributeValue("name", msmPart), partNumber, Integer.parseInt(Helper.getAttributeValue("midi.channel", msmPart)), Integer.parseInt(Helper.getAttributeValue("midi.port", msmPart)), msmPart.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace"));
                this.performance.addPart(tempPart);
            }
            ArticulationMap map = (ArticulationMap) ((localMap != null) ? localMap : tempPart.getDated().addMap(Mpm.ARTICULATION_MAP));
            ArticulationEditor editor = new ArticulationEditor(map, mpmTree.getProjectPane().getMsm(), this.performance);
            editor.setNoteId(this.note.getId());

            editor.setDate(mpmPerformanceDate);

            ArticulationData articulation = editor.create();
            if (articulation != null) {
                Element articulationElement = map.getElement(map.addArticulation(articulation));
                MpmTreeNode mapNode = this.mpmTree.findNode((mpmPart == null) ? this.performance : ((localMap == null) ? mpmPart.getDated() : localMap), false);
                this.updateMpmTreeAndAlignment(mapNode, articulationElement);
            } else if (mpmPart == null) {
                this.performance.removePart(tempPart);
            } else if (localMap == null) {
                tempPart.getDated().removeMap(Mpm.ARTICULATION_MAP);
            }
        });

        // remove all articulations
        WebMenuItem removeArticulations = (WebMenuItem) this.add("Remove All Articulations");
        removeArticulations.setToolTipText("<html><center>Clear the note from all articulations associated to its ID.<br>Default articulation and other articulations at the note's date stay intact.</center></html>");
        removeArticulations.addActionListener(actionEvent -> {
            String noteId = "#" + this.note.getId();
            boolean updateAlignment = false;
            if (globalMap != null) {
                boolean update = false;
                for (KeyValue<Double, Element> artic : globalMap.getAllElementsAt(mpmPerformanceDate)) {
                    Attribute idAtt = artic.getValue().getAttribute("noteid");
                    if ((idAtt != null) && (idAtt.getValue().equals(noteId))) {
                        globalMap.removeElement(artic.getValue());
                        update = true;
                    }
                }
                if (update) {
                    this.mpmTree.reloadNode(this.mpmTree.findNode(globalMap, false));
                    updateAlignment = true;
                }
            }
            if (localMap != null) {
                boolean update = false;
                for (KeyValue<Double, Element> artic : localMap.getAllElementsAt(mpmPerformanceDate)) {
                    Attribute idAtt = artic.getValue().getAttribute("noteid");
                    if ((idAtt != null) && (idAtt.getValue().equals(noteId))) {
                        localMap.removeElement(artic.getValue());
                        update = true;
                    }
                }
                if (update) {
                    this.mpmTree.reloadNode(this.mpmTree.findNode(localMap, false));
                    updateAlignment = true;
                }
            }
            if (updateAlignment)
                this.mpmTree.getProjectPane().getAudioFrame().updateAlignment(true);
        });
    }

    /**
     * when an articulation was added, it should be shown in the MPM tree
     * and rendered into the alignment shown in the audio frame
     * @param mapNode the MPM tree node of the articulation map
     * @param selectThisUserObject the user object of the node to be selected, or null
     */
    private void updateMpmTreeAndAlignment(MpmTreeNode mapNode, Element selectThisUserObject) {
        this.mpmTree.reloadNode(mapNode);
        this.mpmTree.getProjectPane().getAudioFrame().updateAlignment(true);     // update the alignment visualization in the audio frame;

        if (selectThisUserObject != null)
            this.mpmTree.setSelectedNode(mapNode.findChildNode(selectThisUserObject, false));
    }
}


/////////// dead code /////////////////////////////
//                // submenu to create a global articulation
//                this.add(this.makeArticulations("Global", this.performance.getGlobal().getDated(), false));
//
//                // submenu to create a local articulation
//                Element msmPart = this.note.getMsmPart();
//                int partNumber = Integer.parseInt(Helper.getAttributeValue("number", msmPart));
//                Part mpmPart = this.performance.getPart(partNumber);
//                boolean deletePartOnCancel = false;
//                if (mpmPart == null) {
//                String name = Helper.getAttributeValue("name", msmPart);
//                int number = Integer.parseInt(Helper.getAttributeValue("number", msmPart));
//                int midiChannel = Integer.parseInt(Helper.getAttributeValue("midi.channel", msmPart));
//                int midiPort = Integer.parseInt(Helper.getAttributeValue("midi.port", msmPart));
//                String id = msmPart.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
//                mpmPart = Part.createPart(name, number, midiChannel, midiPort, id);
//                this.performance.addPart(mpmPart);
////            this.mpmTree.reloadNode(this.mpmTree.getRootNode().findChildNode(this.performance, false));
//                deletePartOnCancel = true;
//                }
//                String label = "Part " + partNumber + " " + Helper.getAttributeValue("name", msmPart);
//                this.add(this.makeArticulations(label, mpmPart.getDated(), deletePartOnCancel));







// for each articulation in the articulation styles (global, local) add a quick option here
//    Element msmPart = note.getMsmPart();
//
//    Part mpmPart = performance.getPart(Integer.parseInt(Helper.getAttributeValue("number", msmPart)));
//    ArticulationMap map;
//
//    boolean deletePartOnCancel = false;
//    boolean deleteMapOnCancel = false;
//
//        if (mpmPart == null) {
//                String name = Helper.getAttributeValue("name", msmPart);
//                int number = Integer.parseInt(Helper.getAttributeValue("number", msmPart));
//                int midiChannel = Integer.parseInt(Helper.getAttributeValue("midi.channel", msmPart));
//                int midiPort = Integer.parseInt(Helper.getAttributeValue("midi.port", msmPart));
//                String id = msmPart.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
//                mpmPart = Part.createPart(name, number, midiChannel, midiPort, id);
//                performance.addPart(mpmPart);
//                this.parent.getParent().getMpmTree().reloadNode(this.parent.getParent().getMpmTree().getRootNode().findChildNode(performance, false));
//
//                map = (ArticulationMap) mpmPart.getDated().addMap(Mpm.ARTICULATION_MAP);
//
//                deletePartOnCancel = true;
//                deleteMapOnCancel = true;
//                } else {
//                map = (ArticulationMap) mpmPart.getDated().getMap(Mpm.ARTICULATION_MAP);
//                if (map == null) {
//                map = (ArticulationMap) mpmPart.getDated().addMap(Mpm.ARTICULATION_MAP);
//                deleteMapOnCancel = true;
//                }
//                }
//
//        // open articulation editor
//        WebMenuItem newArticulation = new WebMenuItem("New Articulation");
//// TODO               newArticulation.addActionListener(actionEvent -> PlaceAndCreateContextMenu.addArticulation(datedNode, mpmTree, anchor, this.mousePosInImage, this));
//        articulate.add(newArticulation);
