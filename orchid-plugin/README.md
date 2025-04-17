# Kairos Synthesizer Plugin

Kairos is a powerful synthesizer plugin developed by Bombest Studios, featuring four oscillators, filter controls, ADSR envelope, and onboard effects.

## Features

- **Four Oscillators**: Each with level control and waveform selection (Sine, Saw, Square, Triangle)
- **Filter Section**: Adjustable cutoff and resonance
- **ADSR Envelope**: Full control over the sound's envelope
- **Effects**: Reverb, Chorus, and Delay
- **MIDI Input**: Compatible with any MIDI controller or keyboard

## Supported Platforms

Kairos is available for all major platforms and DAWs:

### macOS
- Audio Unit (AU) - Compatible with Logic Pro, GarageBand, MainStage
- VST3 - Compatible with Ableton Live, FL Studio, Cubase, Reaper
- VST2 - Compatible with older DAWs
- AAX - Compatible with Pro Tools
- Standalone application

### Windows
- VST3 - Compatible with Ableton Live, FL Studio, Cubase, Reaper
- VST2 - Compatible with older DAWs
- AAX - Compatible with Pro Tools
- Standalone application

### Linux
- VST3 - Compatible with REAPER, Bitwig Studio, Ardour
- VST2 - Compatible with older DAWs
- Standalone application

### iOS
- AUv3 - Compatible with GarageBand, AUM, Cubasis, BeatMaker 3

## Building the Plugin

### Prerequisites

- CMake 3.15 or higher
- C++17 compatible compiler
- JUCE framework
- For AAX: Avid AAX SDK
- For iOS: Xcode 12 or higher

### Building for macOS

```bash
mkdir build_mac
cd build_mac
cmake .. -G "Xcode" -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### Building for Windows

```bash
mkdir build_windows
cd build_windows
cmake .. -G "Visual Studio 17 2022" -A x64 -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### Building for Linux

```bash
mkdir build_linux
cd build_linux
cmake .. -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

### Building for iOS

```bash
mkdir build_ios
cd build_ios
cmake .. -G "Xcode" -DCMAKE_TOOLCHAIN_FILE=../CMakeLists.iOS.txt -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### Building for All Platforms

You can use the provided build script to build for all platforms at once:

```bash
./build_all.sh
```

## Installation

### macOS
- AU: Copy the `.component` file to `/Library/Audio/Plug-Ins/Components/`
- VST3: Copy the `.vst3` file to `/Library/Audio/Plug-Ins/VST3/`
- VST2: Copy the `.vst` file to `/Library/Audio/Plug-Ins/VST/`
- AAX: Copy the `.aaxplugin` file to `/Library/Application Support/Avid/Audio/Plug-Ins/`
- Standalone: Copy the `.app` file to your Applications folder

### Windows
- VST3: Copy the `.vst3` file to `C:\Program Files\Common Files\VST3\`
- VST2: Copy the `.dll` file to `C:\Program Files\VSTPlugins\`
- AAX: Copy the `.aaxplugin` file to `C:\Program Files\Common Files\Avid\Audio\Plug-Ins\`
- Standalone: Copy the `.exe` file to your desired location

### Linux
- VST3: Copy the `.so` file to `~/.vst3/`
- VST2: Copy the `.so` file to `~/.vst/`
- Standalone: Copy the executable to your desired location

### iOS
- AUv3: Install through the App Store or TestFlight

## Usage

1. Load Kairos in your preferred DAW
2. Adjust the oscillator levels and waveforms to create your base sound
3. Use the filter to shape the tone
4. Modify the ADSR envelope to control how the sound evolves over time
5. Add effects to enhance your sound

## License

Copyright © 2023 Bombest Studios. All rights reserved.

## Support

For support, please visit our website or contact us at support@bombeststudios.com 