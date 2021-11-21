### Version History


#### v0.1.7
- Reorganized some classes, i.e., classes `Score`, `ScoreNote`, `ScorePage` moved from package `mpmToolbox.gui.score` to package `mpmToolbox.projectData.score`.
- Added "Hide Overlay" button to the score widget (classes `mpmToolbox.gui.score.ScoreDocumentData` and `ScoreDisplayPanel`) that allows to show the score without the overlays. 
- Addition to the spectrogram context menu to switch between normalized and non-normalized display.
- New method `getPart()` in classes `mpmToolbox.gui.msmTree.MsmTreeNode` and `mpmToolbox.gui.mpmTree.MpmTreeNode` to retrieve the MSM `part` element that the node belongs to.
- New package `mpmToolbox.supplementary.avlTree` that implements AVL Tree data structure.
- New package `mpmToolbox.projectData.alignment` with several new classes that serve to associate measurements in audio recordings with MSM data.
- Added class `mpmToolbox.gui.audio.PianoRollPanel` which is also the basis for the classes `WaveformPanel` and `SpectrogramPanel` in the same package.
- Several optimizations when editing `Performance` names, adding and removing `Performance` or `Audio` objects from and to the project in order to reduce update traffic between the widgets and the re-rendering of performances for overlay display in the audio widget.


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
