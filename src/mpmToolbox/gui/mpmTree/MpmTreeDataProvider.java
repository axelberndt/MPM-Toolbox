package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import com.alee.extended.tree.AbstractExTreeDataProvider;
import meico.mpm.Mpm;
import meico.mpm.elements.*;
import meico.mpm.elements.maps.*;
import meico.mpm.elements.metadata.Author;
import meico.mpm.elements.metadata.Comment;
import meico.mpm.elements.metadata.Metadata;
import meico.mpm.elements.metadata.RelatedResource;
import meico.mpm.elements.styles.*;
import meico.mpm.elements.styles.defs.*;
import mpmToolbox.ProjectData;
import nu.xom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TreeDataProvider for MPM tree
 * @author Axel Berndt
 */
public class MpmTreeDataProvider extends AbstractExTreeDataProvider<MpmTreeNode> {
    @NotNull private final ProjectData project;

    /**
     * constructor
     * @param project
     */
    public MpmTreeDataProvider(ProjectData project) {
        this.project = project;
    }

    /**
     * get the root node of the tree
     * @return
     */
    @Override
    public MpmTreeNode getRoot() {
        return new MpmTreeNode(this.project.getMpm(), this.project);
    }

    /**
     * this method is used to buffer the tree nodes
     * @param parent
     * @return
     */
    @Override
    public List<MpmTreeNode> getChildren(MpmTreeNode parent) {
        ArrayList<MpmTreeNode> childNodes = new ArrayList<>();  // fill this list with child nodes of the specified parent

        // parse the MPM data structure
        switch (parent.type) {
            case mpm:
                Mpm mpm = (Mpm) parent.getUserObject();
                if (mpm.getMetadata() != null)
                    childNodes.add(new MpmTreeNode(mpm.getMetadata(), this.project));
                for (Performance performance : mpm.getAllPerformances())
                    childNodes.add(new MpmTreeNode(performance, this.project));
                break;

            case metadata:
                Metadata metadata = (Metadata) parent.getUserObject();
                for (Author author : metadata.getAuthors())
                    childNodes.add(new MpmTreeNode(author, this.project));
                for (Comment comment : metadata.getComments())
                    childNodes.add(new MpmTreeNode(comment, this.project));
                childNodes.add(new MpmTreeNode(new MpmRelatedResources(metadata.getRelatedResources()), this.project));
                break;

            case author:
                break;

            case comment:
                break;

            case relatedResources:
                for (RelatedResource resource : ((MpmRelatedResources) parent.getUserObject()).relatedResources)
                    childNodes.add(new MpmTreeNode(resource, this.project));
                break;

            case relatedResource:
                break;

            case performance:
                Performance performance = (Performance) parent.getUserObject();
                childNodes.add(new MpmTreeNode(performance.getGlobal(), this.project));
                for (Part part : performance.getAllParts())
                    childNodes.add(new MpmTreeNode(part, this.project));
                break;

            case global:
                Global global = (Global) parent.getUserObject();
                childNodes.add(new MpmTreeNode(global.getHeader(), this.project));
                childNodes.add(new MpmTreeNode(global.getDated(), this.project));
                break;

            case part:
                Part part = (Part) parent.getUserObject();
                childNodes.add(new MpmTreeNode(part.getHeader(), this.project));
                childNodes.add(new MpmTreeNode(part.getDated(), this.project));
                break;

            case header:
                Header header = (Header) parent.getUserObject();
                HashMap<String, HashMap<String, GenericStyle>> styleCollections = header.getAllStyleTypes();
                for (Map.Entry<String, HashMap<String, GenericStyle>> entry : styleCollections.entrySet())
                    childNodes.add(new MpmTreeNode(new MpmStyleCollection(entry.getKey(), entry.getValue()), this.project));
                break;

            case styleCollection:
                MpmStyleCollection styleCollection = (MpmStyleCollection) parent.getUserObject();
                for (Map.Entry<String, GenericStyle> style : styleCollection.collection.entrySet())
                    childNodes.add(new MpmTreeNode(style.getValue(), this.project));
                break;

            case articulationStyle:
                ArticulationStyle articulationStyle = (ArticulationStyle) parent.getUserObject();
                for (Map.Entry<String, ArticulationDef> entry : articulationStyle.getAllDefs().entrySet())
                    childNodes.add(new MpmTreeNode(entry.getValue(), this.project));
                break;

            case articulationDef:
                break;

            case dynamicsStyle:
                DynamicsStyle dynamicsStyle = (DynamicsStyle) parent.getUserObject();
                for (Map.Entry<String, DynamicsDef> entry : dynamicsStyle.getAllDefs().entrySet())
                    childNodes.add(new MpmTreeNode(entry.getValue(), this.project));
                break;

            case dynamicsDef:
                break;

            case genericStyle:
                GenericStyle genericStyle = (GenericStyle) parent.getUserObject();
                for (Element e : genericStyle.getXml().getChildElements())
                    childNodes.add(new MpmTreeNode(e, this.project));
                break;

            case metricalAccentuationStyle:
                MetricalAccentuationStyle metricalAccentuationStyle = (MetricalAccentuationStyle) parent.getUserObject();
                for (Map.Entry<String, AccentuationPatternDef> entry : metricalAccentuationStyle.getAllDefs().entrySet())
                    childNodes.add(new MpmTreeNode(entry.getValue(), this.project));
                break;

            case accentuationPatternDef:
                AccentuationPatternDef accentuationPatternDef = (AccentuationPatternDef) parent.getUserObject();
                for (int i = 0; i < accentuationPatternDef.size(); ++i)
                    childNodes.add(new MpmTreeNode(accentuationPatternDef.getAccentuationXml(i), this.project));
                break;

            case rubatoStyle:
                RubatoStyle rubatoStyle = (RubatoStyle) parent.getUserObject();
                for (Map.Entry<String, RubatoDef> entry : rubatoStyle.getAllDefs().entrySet())
                    childNodes.add(new MpmTreeNode(entry.getValue(), this.project));
                break;

            case rubatoDef:
                break;

            case tempoStyle:
                TempoStyle tempoStyle = (TempoStyle) parent.getUserObject();
                for (Map.Entry<String, TempoDef> entry : tempoStyle.getAllDefs().entrySet())
                    childNodes.add(new MpmTreeNode(entry.getValue(), this.project));
                break;

            case tempoDef:
                break;

            case dated:
                Dated dated = (Dated) parent.getUserObject();
                HashMap<String, GenericMap> maps = dated.getAllMaps();
                for (Map.Entry<String, GenericMap> entry : maps.entrySet())
                    childNodes.add(new MpmTreeNode(entry.getValue(), this.project));
                break;

            case articulationMap:
                ArticulationMap articulationMap = (ArticulationMap) parent.getUserObject();
                for (int i = 0; i < articulationMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(articulationMap.getElement(i), this.project));
                break;

            case articulation:
                break;

            case asynchronyMap:
                AsynchronyMap asynchronyMap = (AsynchronyMap) parent.getUserObject();
                for (int i = 0; i < asynchronyMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(asynchronyMap.getElement(i), this.project));
                break;

            case asynchrony:
                break;

            case dynamicsMap:
                DynamicsMap dynamicsMap = (DynamicsMap) parent.getUserObject();
                for (int i = 0; i < dynamicsMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(dynamicsMap.getElement(i), this.project));
                break;

            case dynamics:
                break;

            case genericMap:
                GenericMap genericMap = (GenericMap) parent.getUserObject();
                for (int i = 0; i < genericMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(genericMap.getElement(i), this.project));
                break;

            case imprecisionMap:
                ImprecisionMap imprecisionMap = (ImprecisionMap) parent.getUserObject();
                for (int i = 0; i < imprecisionMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(imprecisionMap.getElement(i), this.project));
                break;

            case distributionUniform:
            case distributionGaussian:
            case distributionTriangular:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
                break;

            case distributionList:
                for (Element child : ((Element) parent.getUserObject()).getChildElements())
                    childNodes.add(new MpmTreeNode(child, this.project));
                break;

            case metricalAccentuationMap:
                MetricalAccentuationMap metricalAccentuationMap = (MetricalAccentuationMap) parent.getUserObject();
                for (int i = 0; i < metricalAccentuationMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(metricalAccentuationMap.getElement(i), this.project));
                break;

            case accentuationPattern:
                break;

            case ornamentationMap:
                OrnamentationMap ornamentationMap = (OrnamentationMap) parent.getUserObject();
                for (int i = 0; i < ornamentationMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(ornamentationMap.getElement(i), this.project));
                break;

            case rubatoMap:
                RubatoMap rubatoMap = (RubatoMap) parent.getUserObject();
                for (int i = 0; i < rubatoMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(rubatoMap.getElement(i), this.project));
                break;

            case rubato:
                break;

            case tempoMap:
                TempoMap tempoMap = (TempoMap) parent.getUserObject();
                for (int i = 0; i < tempoMap.size(); ++i)
                    childNodes.add(new MpmTreeNode(tempoMap.getElement(i), this.project));
                break;

            case tempo:
                break;

            case style:
                break;

            case xmlElement:
                for (Element child : ((Element) parent.getUserObject()).getChildElements())
                    childNodes.add(new MpmTreeNode(child, this.project));
                break;

            case unknown:
            default:
                break;
        }

        return childNodes;
    }
}
