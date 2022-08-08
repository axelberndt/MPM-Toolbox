### Version History


#### v0.1.16
- Addition to method `mpmToolbox.gui.score.ScoreDisplayPanel.paintComponent()`. Performance squares in the score will now contain a symbol indicating the type of the performance instruction.
- Dragging a note in the piano roll is now only possible when it is an audio alignment. Performance renderings no longer allow note dragging but should be altered by performance instructions.
- The hop size parameter in the spectrogram specification dialog (class `mpmToolbox.gui.audio.utilities.SpectrogramSpecs`) should not have values greater than the window length. This is now ensured via an additional listener.
- The spectrogram is now displayed with a horizontal offset of half the window length, so it aligns better with the waveform.
- Added another panel to the audio analysis tab, the tempomap panel.
  - New classes were added, `mpmToolbox.gui.audio.TempoMapPanel` and `mpmToolbox.gui.audio.utilities.CursorPositions`.
  - The tempomap panel visualizes its data on the symbolic time axis. Class `CursorPositions` does the corresponding conversions to place playback and mouse cursor in all panels at their correct position.
  - Further conversion methods have also been added to classes `mpmToolbox.projectData.alignment.Alignment` and `mpmToolbox.projectData.alignment.Part`. Class `mpmToolbox.projectData.alignment.Note` is also slightly expanded.
  - The tempomap panel is not yet interactive, though.

#### v0.1.15
- Bugfix in method `mpmToolbox.gui.mpmEditingTools.editorDialogs.DistributionEditor.switchDistributionTypeTo()` that crashed when attribute `seed` was activated.
- Meico update v0.8.37 solves issues with negative event timing from asynchrony and timing imprecision.
- The save file dialog in method `mpmToolbox.gui.MpmToolbox.saveProjectAs()` will now make sure that the project file will always have extension `.mpr`.
- Bugfix in constructor `mpmToolbox.projectData.score.Score.Score(@NotNull ProjectData project)` that ran into a `NullPointerException` when loading a project with image data but no MPM data.
- Bugfix in method `mpmToolbox.gui.mpmTree.MpmTree.gotoFirstMapEntryNode()` that caused an exception when the project's MPM contained no maps.
- Added a note ID check to method `mpmToolbox.gui.mpmEditingTools.editorDialogs.ArticulationEditor.edit()` to ensure that, if an initial note ID is set before displaying the dialog, the ID is tinted correctly.
- Added method `mpmToolbox.supplementary.Tools.uniformPath()` to handle differences in path separators on the different operating systems.
- Added more parsing of ornamentation data for a proper display in the MPM tree widget. Ornamentation support has been added to class `mpmToolbox.gui.mpmEditingTools.MpmEditingTools`. New classes `OrnamentEditor` and `OrnamentDefEditor` have been added to package `mpmToolbox.gui.mpmEditingTools.editDialogs`, not yet functional, though.
- Little changes in method `mpmToolbox.gui.MpmToolbox.openProject()`. The "Close current project with or without saving?" dialog needed some adjustment.


#### v0.1.14
- Meico update v0.8.36.


#### v0.1.13
- Meico update to v0.8.35 with added support for MEI element `arpeg` for arpeggios and new MPM features for ornamentation. These, however, are not yet editable in MPM Toolbox. Full integration of MPM ornamentation features is pending. The update further contains a little enhancement in meico's MEI to MSM and MPM export, namely that IDs of MEI `staffDef` elements will recur in MSM and MPM `part` elements.
- Enhancement in the constructor of class `mpmToolbox.projectData.score.Score` that parses a project file's XML to generate its data structure. Now it is able to handle occurrences of a note or performance instruction on multiple score pages.
- Java compatibility tests and according `README.md` update.


#### v0.1.12
- In the MPM tree, the context menus of `articulationStyles`, `dynamicsStyles` and `tempoStyles` have an additional option to create default styles. These serve as convenient starter styles to be further refined.


#### v0.1.11
- New method `mpmToolbox.projectData.alignment.Alignment.getPart(Note note)` to retrieve the part that contains the given note.
- Class `mpmToolbox.projectData.alignment.Note` has new getters for the MIDI tick date and duration, and MSM part. 
- New class `mpmToolbox.gui.audio.utilities.ArticulationMenu` that creates a submenu in the piano roll context menu to articulate a note.


#### v0.1.10
- As of meico v0.8.33, audio export (WAV, MP3) will now use the currently loaded soundfont.
- The window icon will be displayed also in the title bar of dialog windows.
- The root element in MPR files was renamed to `mpmToolboxProject` (in method `mpmToolbox.projectData.ProjectData.saveProjectAs()`). This change is backward compatible, the old `mpmToolkitProject` will also work.
- New method `mpmToolbox.gui.audio.PianoRollPanel.drawMouseCursor()` that simplifies code in the child classes `SpectrogramPanel` and `WaveformPanel`.
- New method `getMillisecondsLength()` in classes `mpmToolbox.projectData.alignment.Alignment` and `Part`. Furthermore, an optimization has been applied to both classes to more quickly retrieve the last note sounding.
- Added a playback position indicator to the audio visualizations.
- In method `mpmToolbox.projectData.alignment.Alignment.getExpressiveMsm()` some cleanup has been added to remove all maps but the `score` elements, as their contents are not affected by the alignment's timing and, after expressive MIDI export, appear at wrong positions.
- The runtime performance of waveform image computation has been significantly increased by method `mpmToolbox.projectData.audio.Audio.convertWaveform2Image()` and its new helper class `mpmToolbox.projectData.audio.PeakList`.


#### v0.1.9
- Added a MIDI Master Volume slider to the SyncPlayer. It is relevant when audio recording and MIDI performance are played synchronously and the MIDI part is too loud. However, Master Volume Control is a MIDI SysEx message and not all devices support it. MPM Toolbox's default synthesizer, Gervill, fortunately does.
- Bugfix in method `mpmToolbox.gui.mpmEditingTools.CommentEditor.makeContentPanel()`. It was unable to query the font metrics for proper initialization.
- All editing functionality in classes `mpmToolbox.gui.mpmEditingTools.MpmEditingTools` and `mpmToolbox.gui.mpmEditingTools.PlaceAndCreateContextMenu` has been extended, so the piano roll visualization in the audio frame (class `mpmToolbox.gui.audio.AudioDocumentData`) is kept up to date.
- Audio-to-MSM alignments are initially scaled to fit the length of the audio. however, whenever the user edits the alignment, the initial scaling gets lost. Therefore, the last sounding note is now set fixed (pinned to its position after scaling).
- The notes in the piano roll are displayed with a slight alpha fade so that onsets are easier to see when subsequent notes of the same pitch are displayed.
- Enhancement in `mpmToolbox.projectData.alignment.Alignment`. 
  - Method `updateTiming()` has been simplified and code was moved to method `updateTimingTransformation()` as this functionality is also required aside from rendering.
  - The last step in method `updateTimingTransformation()` is a rather dull way to handle the timing after the last fixed note. The enhancement will now continue with the tempo from before the last fixed note.
  - New functionality in class `mpmToolbox.gui.audio.AudioDocumentData` and `mpmToolbox.projectData.alignment.Alignment`, resp., to transform an alignment to an `meico.mpm.elements.Performance` and vice versa.
  - In the process of the aforementioned addition, corresponding new functionality has been added via methods `exportTiming()` and `exportArticulation`. These transform the timing data of the alignment to an MPM tempo, asynchrony and articulation map.


#### v0.1.8
- Meico update with new functionality to add a timing offset to `Midi` instances and improved audio waveform image rendering.
- So far, the SyncPlayer (class `mpmToolbox.gui.syncPlayer.SyncPlayer` and its inner class `PlaybackRunnable`) allowed only positive audio offsets. Now it can also handle negative offsets. Instead of adding the offset time to the audio data it will be removed from the MIDI sequence.
- Spectrogram computation is put into new class `mpmToolbox.gui.audio.utilities.SpectrogramComputation` which visualizes the progress in a dialog with progress bar. The process of spectrogram computation is now done in a separate thread via a `SwingWorker` derivative called `mpmToolbox.gui.audio.utilities.SpectrogramComputationWorker` that informs the progress bar and can be cancelled.


#### v0.1.7
- Little edit in method `mpmToolbox.gui.score.ScoreDisplayPanel.mouseMoved()` that changes the mouse cursor to hand symbol when moved to a clickable overlay element.
- Fixed potential division-by-zero bug in `mpmToolbox.gui.syncPlayer.SyncPlayer.PlaybackRunnable.run()`.
- Reorganized some classes, i.e., classes `Score`, `ScoreNote`, `ScorePage` moved from package `mpmToolbox.gui.score` to package `mpmToolbox.projectData.score`.
- Added "Hide Overlay" button to the score widget (classes `mpmToolbox.gui.score.ScoreDocumentData` and `ScoreDisplayPanel`) that allows to show the score without the overlays. 
- Addition to the spectrogram context menu to switch between normalized and non-normalized display.
- New method `getPart()` in classes `mpmToolbox.gui.msmTree.MsmTreeNode` and `mpmToolbox.gui.mpmTree.MpmTreeNode` to retrieve the MSM `part` element that the node belongs to.
- New package `mpmToolbox.supplementary.avlTree` that implements the AVL Tree data structure.
- New package `mpmToolbox.projectData.alignment` with several new classes that serve to associate measurements in audio recordings with MSM data, display them as piano roll and interact with it.
- Alignment data is stored in `mpr` project files.
- Added class `mpmToolbox.gui.audio.PianoRollPanel` which is also the basis for the classes `WaveformPanel` and `SpectrogramPanel` in the same package.
- Several optimizations when editing `Performance` names, adding and removing `Performance` or `Audio` objects from and to the project in order to reduce update traffic between the widgets and the re-rendering of performances for overlay display in the audio widget.
- Added button "Align Frequencies with MIDI Pitches" to the spectrogram specs. These set the min. and max frequency of the spectrogram. Use these to align it vertically with the piano roll.
- Added combobox and sub-class `PartChooserItem` to class `mpmToolbox.gui.audio.AudioDocumentData` to choose the musical part or select all parts to be displayed by the piano roll overlay.


#### v0.1.6
- Class `mpmToolbox.ProjectData` moved into the newly added package `mpmToolbox.projectData`.
- New Class `mpmToolbox.projectData.Audio`. It extends the eponymous class from meico and replaces all its occurrences in the code.
- Communication of classes `mpmToolbox.gui.audio.AudioDocumentData`, `WaveformPanel` and `SpectrogramPanel` has been redone. 
- The spectrogram display is now properly synchronized to the waveform display and optimized.
- Added a GUI in the spectrogram panel to specify the parameters for the spectrogram computation.
- Expanded the SyncPlayer's `triggeredPlayback()` methods to jump to specific playback positions. With this we can start playback from within the waveform and spectrogram panel (via right click).


#### v0.1.5
- Code simplification in class `mpmToolbox.gui.syncPlayer`.
- Updated meico to v0.8.26.
- Removed `rsyntaxtextarea-3.1.1.jar` from the externals. It was never used so far.
- Class `mpmToolbox.gui.msmTree.MsmTree` has been expanded. It now provides its own instance of a `WebDockableFrame`. A simple getter method makes it easier to be used in class `mpmToolbox.gui.ProjectPane`. Thus, some code optimizations have been applied there.
- New class `mpmToolbox.gui.mpmTree.MpmDockableFrame` has been added. This simplifies some code in class `mpmToolbox.gui.ProjectPane`.
- New method `toXml()` in class `mpmToolbox.projectData.score.ScorePage`. It exports the image data and concordances that are stored in MPM Toolbox's project files (`.mpr`).
    - The same code was previously performed by method `mpmToolbox.projectData.score.Score.toXml()`. It has been simplified accordingly and invokes the `ScorePage`'s `toXml()` now instead.
    - Two new attributes were added to the generated `page` element, `width.pixels` and `height.pixels`. If the page image gets replaced by another with a different resolution, the coordinates of the concordances can be scaled on this basis.
- Added another constructor to class `mpmToolbox.projectData.score.ScorePage` to be used by the constructor of class `mpmToolbox.projectData.score.Score` that reads the data from a project file (`.mpr`). The new `ScorePage` constructor checks if the image resolution changed and, if so, scales the concordance coordinates accordingly.
- Some first preparations for the audio analysis component.
    - Interactive waveform and CQT spectrogram display, incl. 
        - resize, 
        - zoom with mouse wheel,
        - pan by mouse drag,
        - precise mouse click to sample index mapping, 
        - context menu to switch between displaying all audio channels or only one.
    - All interactions are performed synchronous on both, waveform and spectrogram. However, the spectrogram display is not yet properly aligned with the waveform!
- Bugfix in classes `mpmToolbox.ProjectData`, `mpmToolbox.score.Score`: Path separators encoded as `"\\"` were replaced by `File.separator` to function on all operating systems. Thanks to [pfefferniels](https://github.com/pfefferniels) for this bug report! The same "inter-OS-operability" has been added to the parsing of the `.mpr` project files in class `ProjectData`.


#### v0.1.4
- Double-click on MPM elements ...
    - in the MPM tree widget will immediately open its editor dialog, provided it has one. This function is added to leaf nodes only and not to nodes that expand by double click. Element `distribution.list` is an exception to this rule, so this functionality can also be used in the score display, see next bullet point.
    - in the score display widget does the same as the above. It immediately opens the corresponding editor dialog.
- Bugfix: The SyncPlayer's performance chooser did not update when the MPM file is changes. This is fixed.
- Enhancement in the score display: The zoom factor of the score image now initializes so that it matches the size of the panel.


#### v0.1.3
- Added new operations to map elements in the MPM tree widget. Maps can now be moved and copied from one `dated` environment to another - effectively to another `part` or `global` environment in the same or any other `performance`.
  - When copied, the ID of each map element will be changed in the copy to ensure unique IDs throughout the MPM document.
  - It is not valid to have two maps of the same type within one and the same `dated` environment. Hence, if the target environment has already a map of the same type as the origin map, the contents will be merged into it.
- The same functionality is also added to individual MPM map entries.
- Bugfix in method `mpmToolbox.gui.mpmEditingTools.editDialogs.TempoEditor.edit()`. When editing a tempo transition with a numeric value for `transition.to` the `meanTempoAt`was not initialized from the source.
- Enhancement of method `mpmToolbox.projectData.score.ScorePage.addEntry()`. If an element to be added has no `xml:id` attribute, which is required to be linked to a score position, the method generates a random ID and adds the attribute to the element.
- Layout related fix in method `mpmToolbox.gui.mpmEditingTools.editDialogs.EditDialog.addIdInput()` to ensure that the ID input field has a fixed width and does not break the layout.


#### v0.1.2
- Some refactoring in package `mpmToolbox.gui.syncPlayer`.
- The SyncPlayer's MIDI output can now be streamed to any other MIDI port available on the host system. Thus, users are no longer limited to the internal Gervill synthesizer and the soundbank loaded into it.


#### v0.1.1
- PDF score import added. The pages of a PDF file are extracted and stored as PNG files with 300dpi (following the [DFG Practical Guidelines on Digitisation](https://www.dfg.de/formulare/12_151/12_151_en.pdf)) in a subfolder of the source directory.
- The window title will now print the title of the currently opened project.


#### v0.1.0
- Added menu bar.
- Redefined keyboard shortcuts and integrated them into the menu bar.
- Added some more tooltips to the SyncPlayer.
- Added Soundfont support.
- Added MIDI and audio export of performance renderings.
- Reworked the application icon. Added several resolution variants.
- Bugfix in method `mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.DistributionVisualizer.scale()` to prevent division by zero.
- Redone the welcome message.


#### v0.0.2
- Meico update to v0.8.22.
- Updated behaviour of all sliders. When clicking on a slider, its thumb will jump to that position. Previous behaviour was that the thumb did a step towards the click position. Therefore, method `makeSliderSetToClickPosition()` was added to class `mpmToolbox.supplementary.Tools`.
- The SyncPlayer's playback slider is now fully functional.
- Added SyncPlayer input to set a certain milliseconds time to be skipped in audio playback. This is required because, while MIDI play starts immediately with the first note, music recordings usually start a bit later. Now we can synchronize audio and MIDI start.
- Added keyboard shortcut `[SPACE]` to method `mpmToolbox.supplementary.Tools.initKeyboardShortcuts()` for triggering the SyncPlayer's playback.
- Added Comboboxes to SyncPlayer to choose the performance and audio to be played back.


#### v0.0.1
- Initial commit.
- This version has already most of its core features.
  - New project instantiation from MEI, MIDI and MSM.
  - Further import data: MPM, MP3, WAV, JPG, GIF, PNG, and BMP.
  - Project data is stored in MPR files, a proprietary XML format for MPM Toolbox projects.
  - Tree display of MSM and MPM data; the latter is fully interactive.
  - Score display for sheet music autographs with interactive overlays of notes and performance instructions.
  - Lots of editor dialogs for customizing performance features.
  - A basic player for audio data and expressive MIDI renderings; not fully functional, yet.
