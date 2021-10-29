package mpmToolbox.gui.score;

import com.alee.api.annotations.NotNull;
import com.alee.extended.button.WebSplitButton;
import com.alee.extended.tab.DocumentData;
import com.alee.laf.button.WebButton;
import com.alee.laf.grouping.GroupPane;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.PopupMenuWay;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmTree.MpmTreeNode;
import mpmToolbox.gui.msmTree.MsmTreeNode;
import mpmToolbox.projectData.score.ScorePage;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * A custom DocumentData object for the score display component.
 * @author Axel Berndt
 */
public class ScoreDocumentData extends DocumentData<WebPanel> implements ActionListener {
    protected final ProjectPane parent;
    private final WebPanel scorePanel = new WebPanel();
    private ScoreDisplayPanel scoreDisplay = null;                                                              // this displays the score
    private final WebLabel placeholder = new WebLabel("Place score images here.", WebLabel.CENTER);
    private final WebPopupMenu scorePagesPopupMenu = new WebPopupMenu();                                        // this is filled with the file names of the score pages, then used by the WebSplitButton for pages selection
    private final WebSplitButton interactionMode = new WebSplitButton();                                        // with this button we switch between interaction modes
    protected InteractionMode currentInteractionMode = InteractionMode.panAndZoom;                              // this is set to the current interaction mode whenever the interactionMode split button is set
    protected final WebSpinner annotationSizeSpinner = new WebSpinner(new SpinnerNumberModel(0, -999, 999, 1)); // this spinner allows scaling the size of overlay elements in the score display
    boolean hideScore = false;
    boolean hideOverlay = false;

    /**
     * constructor
     * @param parent
     */
    public ScoreDocumentData(@NotNull ProjectPane parent) {
        super("Score", "Score", null);

        this.setComponent(this.scorePanel);
        this.setClosable(false);
        this.parent = parent;
        this.draw();
    }

    /**
     * Get the ProjectPane object that this belongs to.
     * @return
     */
    public ProjectPane getParent() {
        return this.parent;
    }

    /**
     * this draws the content of the score frame
     */
    private void draw() {
        // previous page button
//        Icon previousIcon = IconFontSwing.buildIcon(Entypo.LEFT_OPEN, Settings.getDefaultFontSize()*3, this.getForeground());
//        WebButton previousButton = new WebButton("<html><font size=\"+2\"><b>&#10092;</b></font></html>"/*previousIcon*/);
        final WebButton previousButton = new WebButton("<html><b>&#8249;</b></html>"/*previousIcon*/);
        previousButton.setFontSize(Settings.getDefaultFontSize() + 10);
        previousButton.setPadding(Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2);
        previousButton.setActionCommand("previousPage");
        previousButton.addActionListener(this);
        previousButton.setEnabled(true);
        previousButton.setToolTip("previous page");
//        previousButton.addHotkey(this.scorePanel, Hotkey.LEFT);      // deactivated because handled in the scorePanel's keyboard listener
//        this.scoreDockFrame.add(previousButton, BorderLayout.WEST);

        // next page button
//        Icon nextIcon = IconFontSwing.buildIcon(Entypo.RIGHT_OPEN, Settings.getDefaultFontSize()*3, this.getForeground());
//        WebButton nextButton = new WebButton("<html><font size=\"+2\"><b>&#10093;</b></font></html>"/*nextIcon*/);
        final WebButton nextButton = new WebButton("<html><b>&#8250;</b></html>"/*nextIcon*/);
        nextButton.setFontSize(Settings.getDefaultFontSize() + 10);
        nextButton.setPadding(Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2);
        nextButton.setActionCommand("nextPage");
        nextButton.addActionListener(this);
        nextButton.setEnabled(true);
        nextButton.setToolTip("next page");
//        nextButton.addHotkey(this.scorePanel, Hotkey.RIGHT);         // deactivated because handled in the scorePanel's keyboard listener

        // delete page button
//        Icon deleteIcon = IconFontSwing.buildIcon(Entypo.CANCEL, Settings.getDefaultFontSize()*2.5f, this.getForeground());
        final WebButton deleteButton = new WebButton("<html><b>&times;</b></html>"/*deleteIcon*/);
        deleteButton.setFontSize(Settings.getDefaultFontSize() + 5);
        deleteButton.setPadding(Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2);
        deleteButton.setActionCommand("deletePage");
        deleteButton.addActionListener(this);
        deleteButton.setEnabled(true);
        deleteButton.setToolTip("delete page");
//        nextButton.addHotkey(Hotkey.DELETE);         // deactivated because handled in the scorePanel's keyboard listener

        // the hide overlay button
        final WebButton hideOverlayButton = new WebButton(this.hideOverlay ? "Show Overlay" : "Hide Overlay");
        hideOverlayButton.setToolTip("hide/show the overlay on the score image");
        hideOverlayButton.setPadding(Settings.paddingInDialogs);
        hideOverlayButton.addActionListener(actionEvent -> {
            if (this.scoreDisplay == null)
                return;
            this.hideOverlay = !this.hideOverlay;
            this.scoreDisplay.repaint();
            hideOverlayButton.setText(this.hideOverlay ? "Show Overlay" : "Hide Overlay");
        });

        // the hide score button
        final WebButton hideScoreButton = new WebButton(this.hideScore ? "Show Score" : "Hide Score");
        hideScoreButton.setToolTip("hide/show the score image");
        hideScoreButton.setPadding(Settings.paddingInDialogs);
        hideScoreButton.addActionListener(actionEvent -> {
            if (this.scoreDisplay == null)
                return;
            this.hideScore = !this.hideScore;
            this.scoreDisplay.repaint();
            hideScoreButton.setText(this.hideScore ? "Show Score" : "Hide Score");
        });

        // make page selection and interaction mode selection buttons
        final WebSplitButton pageSelectButton = this.makePageSelectButton();  // page select popup menu button
        this.initInteractionModeButton();           // the split button to choose the interaction mode

        // overlay elements size
        this.annotationSizeSpinner.setValue(this.parent.getScore().getOverlayElementSize());
        this.annotationSizeSpinner.addChangeListener(changeEvent -> {
            this.parent.getScore().setOverlayElementSize((int) this.annotationSizeSpinner.getValue());
            this.scoreDisplay.updateOverlayElementsScaleFactor();
        });
        this.annotationSizeSpinner.setToolTipText("set the size of overlay elements, e.g. notes");
//        this.annotationSizeSpinner.setToolTip("set the size of overlay elements, e.g. notes");  // TODO: this method doesn't work on WebSpinners so we have to show the standard tooltip here

        // bottom pane with buttons
        GridBagLayout scoreButtonPanelLayout = new GridBagLayout();
        final WebPanel scoreButtonPanel = new WebPanel(scoreButtonPanelLayout);
        scoreButtonPanel.setPadding(Settings.paddingInDialogs);
        final GroupPane buttonGroup = new GroupPane(GroupPane.CENTER, previousButton, deleteButton, hideScoreButton, hideOverlayButton, pageSelectButton, this.interactionMode, nextButton);    // the GroupPane groups the buttons
        Tools.addComponentToGridBagLayout(scoreButtonPanel, scoreButtonPanelLayout, buttonGroup, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        Tools.addComponentToGridBagLayout(scoreButtonPanel, scoreButtonPanelLayout, this.annotationSizeSpinner, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        GridBagLayout gridBagLayout = new GridBagLayout();
        this.scorePanel.setLayout(gridBagLayout);
        Tools.addComponentToGridBagLayout(this.scorePanel, gridBagLayout, scoreButtonPanel, 0, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.SOUTH);

        // the score page display
        if (!this.parent.getScore().isEmpty()) {                                    // if the project has no score images
            this.scoreDisplay = new ScoreDisplayPanel(this);
            Tools.addComponentToGridBagLayout(this.scorePanel, gridBagLayout, this.scoreDisplay, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        } else {
            Tools.addComponentToGridBagLayout(this.scorePanel, gridBagLayout, this.placeholder, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        }
    }

    /**
     * initialize the page selection button
     * @return the page selection button
     */
    private WebSplitButton makePageSelectButton() {
        for (ScorePage page : this.parent.getScore().getAllPages()) {                   // first we generate the popup menu that writes down all score page file names, hence, we parse all score files
            WebMenuItem menuItem = new WebMenuItem(page.getFile().getAbsolutePath().trim());   // the popup menu item gets the file path
            this.scorePagesPopupMenu.add(menuItem);                                     // add the item to the popup menu
            // and set the behaviour when clicked
            menuItem.addActionListener(actionEvent -> {
//                    pageSelectButton.setText(menuItem.getText());
                int pageIndex = this.parent.getScore().getPageIndex(new File(menuItem.getText()));
                if (pageIndex > -1) {
                    this.scoreDisplay.showPage(pageIndex);
                }
            });
        }

        WebSplitButton pageSelectButton = new WebSplitButton("Select Page");
        pageSelectButton.setPadding(Settings.paddingInDialogs);
        pageSelectButton.setPopupMenuWay(PopupMenuWay.aboveEnd);
        pageSelectButton.setPopupMenu(this.scorePagesPopupMenu);
        pageSelectButton.setToolTip("go to page");
        return pageSelectButton;
    }

    /**
     * this method initializes the split button that triggers the interaction modes
     */
    private void initInteractionModeButton() {
        WebPopupMenu interactionModePopup = new WebPopupMenu();

        WebMenuItem menuItem = new WebMenuItem("Pan & Zoom");
        menuItem.addActionListener(actionEvent -> {
            this.currentInteractionMode = InteractionMode.panAndZoom;
            this.interactionMode.setText("Pan & Zoom");
            this.interactionMode.setForeground(Settings.foregroundColor);
//            interactionMode.setToolTip("interaction mode: pan and zoom");
        });
        menuItem.setToolTipText("pan and zoom interaction mode");
        interactionModePopup.add(menuItem);

        menuItem = new WebMenuItem("Mark Notes");
        menuItem.addActionListener(actionEvent -> {
            this.currentInteractionMode = InteractionMode.markNotes;
            MsmTreeNode node = this.parent.getMsmTree().getSelectedNode();              // get the currently selected node
            if ((node == null) || (node.getType() != MsmTreeNode.XmlNodeType.note))     // if none is selected or it is not of type note
                this.parent.getMsmTree().gotoFirstNoteNode();                           // select the first note node in the tree
            this.interactionMode.setText("Mark Notes");
            this.interactionMode.setForeground(Color.GREEN);
//            interactionMode.setToolTip("<html><center>Interaction mode: Place notes from Musical Sequence Markup on the score.<br>Be sure to select the respective note in the Musical Sequence Markup.<br>Left click places a note, right click deletes a note from the score page.</center></html>");
        });
        menuItem.setToolTipText("<html><center>Place notes from Musical Sequence Markup on the score.<br>Be sure to select the respective note in the Musical Sequence Markup.<br>Left click places a note, right click deletes a note from the score page.</center></html>");
        interactionModePopup.add(menuItem);

        menuItem = new WebMenuItem("Add/Place Performance");
        menuItem.addActionListener(actionEvent -> {
            this.currentInteractionMode = InteractionMode.editPerformance;
            if (this.parent.getMpmTree() != null) {
                MpmTreeNode node = this.parent.getMpmTree().getSelectedNode();
                if ((node == null) || !node.isMapEntryType())
                    this.parent.getMpmTree().gotoFirstMapEntryNode();
            }
            this.interactionMode.setText("Add/Place Performance");
//            interactionMode.setToolTip("interaction mode: edit performance data");
            this.interactionMode.setForeground(Color.CYAN);
        });
        menuItem.setToolTipText("add or place performance data");
        interactionModePopup.add(menuItem);

        this.interactionMode.setPopupMenu(interactionModePopup);
        this.interactionMode.setPopupMenuWay(PopupMenuWay.aboveEnd);
        this.currentInteractionMode = InteractionMode.panAndZoom;
        this.interactionMode.setText("Pan & Zoom");
        this.interactionMode.setPadding(Settings.paddingInDialogs);
        this.interactionMode.setToolTip("select interaction mode");
    }

    /**
     * a getter for the score display panel
     * @return
     */
    public ScoreDisplayPanel getScoreDisplay() {
        return this.scoreDisplay;
    }

    /**
     * This adds a score page to this frame's GUI data.
     * This method is meant to be invoked AFTER ProjectData.addScorePage(file). It keeps the GUI up to date with the data.
     * @param file
     */
    public void addScorePage(File file) {
        if (this.scoreDisplay == null) {
            this.scoreDisplay = new ScoreDisplayPanel(this);
            this.getComponent().remove(this.placeholder);
            Tools.addComponentToGridBagLayout(this.getComponent(), (GridBagLayout) this.getComponent().getLayout(), this.scoreDisplay, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        }

        WebMenuItem menuItem = new WebMenuItem(file.getAbsolutePath());
        menuItem.addActionListener(actionEvent -> {
            int pageIndex = this.parent.getScore().getPageIndex(new File(menuItem.getText()));
            if (pageIndex > -1) {
                this.scoreDisplay.showPage(pageIndex);
            }
        });
        this.scorePagesPopupMenu.add(menuItem);
    }

    /**
     * This removes the score page at the specified index from the popup menu.
     * Basically, this is a method that updates the GUI and must be invoked AFTER ProjectData.removeScorePage(index).
     * @param index
     */
    public void removeScorePage(int index) {
        this.scorePagesPopupMenu.remove(index);

        if (this.parent.getScore().isEmpty()) {                     // if no score page is left, close the frame and delete the score panel
            this.getComponent().remove(this.scoreDisplay);
            this.scoreDisplay = null;
            Tools.addComponentToGridBagLayout(this.scorePanel, (GridBagLayout) this.scorePanel.getLayout(), this.placeholder, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            this.getComponent().repaint();
        }
        else if (this.scoreDisplay.getPageIndex() >= index) {       // if the currently displayed page was deleted or a page before it
            this.scoreDisplay.showPage(Math.max(0, index - 1));     // go to the previous page index or index 0 if the first page was deleted
        }

    }

    /**
     * Actions that can be triggered by interaction.
     * @param actionEvent
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        switch (actionEvent.getActionCommand()) {
            case "previousPage":                    // score page navigation
                if (this.scoreDisplay != null)
                    this.scoreDisplay.previousPage();
                break;
            case "nextPage":                        // score page navigation
                if (this.scoreDisplay != null)
                    this.scoreDisplay.nextPage();
                break;
            case "deletePage":                      // delete score page
                if (this.scoreDisplay != null) {
                    this.parent.removeScorePage(this.scoreDisplay.getPageIndex());  // delete the file from the Score data structure

                    // update the MSM tree as some green points might have to disappear
                    ArrayList<MsmTreeNode> scoreNodesInMsm = this.parent.getMsmTree().getAllNodesOfType(this.parent.getMsmTree().getRootNode(), MsmTreeNode.XmlNodeType.note);
                    for (MsmTreeNode n : scoreNodesInMsm) {
                        this.parent.getMsmTree().updateNode(n);
                    }

                    // update the MPM tree as some blue points might have to disappear
                    ArrayList<MpmTreeNode> scoreNodesInMpm = this.parent.getMpmTree().getAllMapEntryNodes(this.parent.getMpmTree().getRootNode());
                    for (MpmTreeNode n : scoreNodesInMpm) {
                        this.parent.getMpmTree().updateNode(n);
                    }
                }
                break;
            default:
        }
    }

    /**
     * This enumerates the score interaction modes.
     */
    public enum InteractionMode {
        panAndZoom,
        markNotes,
        editPerformance
    }
}
