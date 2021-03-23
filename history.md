### Version History


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
