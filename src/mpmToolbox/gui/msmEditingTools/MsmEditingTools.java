package mpmToolbox.gui.msmEditingTools;

import com.alee.api.annotations.NotNull;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import meico.mei.Helper;
import meico.mpm.elements.Part;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.ArticulationMap;
import meico.mpm.elements.maps.GenericMap;
import meico.msm.Msm;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.msmTree.MsmTree;
import mpmToolbox.gui.msmTree.MsmTreeNode;
import mpmToolbox.projectData.score.ScorePage;
import nu.xom.Element;
import nu.xom.Elements;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Helper class for MsmTreeNode. It generates the context menu that appears when right clicking
 * in the MSM tree and all functionality required for it.
 * @author Axel Berndt
 */public class MsmEditingTools {
    public static WebPopupMenu makeMsmTreeContextMenu(@NotNull MsmTreeNode forThisNode, @NotNull MsmTree inThisMsmTree) {
        MsmTreeNode self = forThisNode;
        MsmTree msmTree = inThisMsmTree;

//        System.out.println("Creating popup menu for " + self.name);
        WebPopupMenu menu = new WebPopupMenu();

        switch (self.getType()) {
            case msm:
                // resolve all sequencingMaps
                WebMenuItem expandRepetitions = new WebMenuItem("Resolve all sequencingMaps");
                expandRepetitions.setToolTipText("<html>Creates \"through-composed\" MSM and performances.<br>Deletes all sequencingMaps afterwards. If you plan to use<br>this functionality, do it before making entries in the score!</html>");
                expandRepetitions.addActionListener(actionEvent -> MsmEditingTools.resolveAllSequencingMaps(msmTree));
                menu.add(expandRepetitions);
                break;

            case attribute:
                break;

            case global:
                break;

            case part:
                break;

            case header:
                break;

            case dated:
                break;

            case score:
                break;

            case sequencingMap:
                break;

            case note:
                break;

            case rest:
                break;

            case lyrics:
                break;

            case element:
            default:
                break;
        }

        return menu;

    }

    /**
     * Invoke this method to immediately open the editor dialog of MSM nodes ... at least those that have
     * an editor dialog and are leaf nodes in the MSM tree. Non-leaf nodes expand on double click and that
     * is what this method is meant to be used for, open the editor on double click.
     * @param forThisNode
     * @param inThisMsmTree
     */
    public static void quickOpenEditor(@NotNull MsmTreeNode forThisNode, @NotNull MsmTree inThisMsmTree) {
        MsmTreeNode self = forThisNode;
        MsmTree msmTree = inThisMsmTree;

        switch (self.getType()) {
            // TODO: any editing of the MSM data?
            default:
                break;
        }
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
     * apply all sequencingMaps to the MSM tree and the MPM performances, remove the sequencingMaps from the MSM
     * @param msmTree
     */
    private static void resolveAllSequencingMaps(@NotNull MsmTree msmTree) {
        Msm msm = msmTree.getProjectPane().getMsm();
        MpmTree mpmTree = msmTree.getProjectPane().getMpmTree();

        ArrayList<GenericMap> articulationMaps = new ArrayList<>();

        // apply the sequencingMaps to MPM data first
        Element globalSequencingMap = msm.getRootElement().getFirstChildElement("global").getFirstChildElement("dated").getFirstChildElement("sequencingMap");
        for (Performance performance : mpmTree.getProjectPane().getMpm().getAllPerformances()) {
            if (globalSequencingMap != null) {
                HashMap<String, GenericMap> maps = performance.getGlobal().getDated().getAllMaps();
                for (GenericMap map : maps.values()) {
                    map.applySequencingMap(globalSequencingMap);
                    if (map instanceof ArticulationMap)     // in articulationMaps the elements have notid attribute that has to be updated after resolving the sequencingmaps in MSM
                        articulationMaps.add(map);          // so keep the articulationMaps for later reference
                }
            }
            Elements msmParts = msm.getParts();
            ArrayList<Part> mpmParts = performance.getAllParts();
            for (int pa=0; pa < performance.size(); ++pa) {
                Element msmPart = msmParts.get(pa);
                Element sequencingMap = msmPart.getFirstChildElement("dated").getFirstChildElement("sequencingMap");
                if (sequencingMap == null) {
                    sequencingMap = globalSequencingMap;
                    if (sequencingMap == null)
                        continue;
                }
                for (GenericMap map : mpmParts.get(pa).getDated().getAllMaps().values()) {
                    map.applySequencingMap(sequencingMap);
                    if (map instanceof ArticulationMap)     // in articulationMaps the elements have notid attribute that has to be updated after resolving the sequencingmaps in MSM
                        articulationMaps.add(map);          // so keep the articulationMaps for later reference
                }
            }
        }

        // apply the sequencingMaps to MSM data, this will also delete the sequencingMaps
        HashMap<String, String> repetitionIDs = msm.resolveSequencingMaps();

        // update the articulationMap's elements' noteid attributes
        for (GenericMap map : articulationMaps)
            Helper.updateMpmNoteidsAfterResolvingRepetitions(map, repetitionIDs);

        // reload the nodes whose content changed
        msmTree.reloadNode(msmTree.getRootNode());
        mpmTree.reloadNode(mpmTree.getRootNode());
        msmTree.getProjectPane().getScore().cleanupDeadNodes();
    }
}
