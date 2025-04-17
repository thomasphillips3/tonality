cmake_minimum_required(VERSION 3.15)

project(Kairos VERSION 1.0.0)

# Use the shared JUCE installation
set(JUCE_DIR "/Users/thomasphillips/workspace/JUCE" CACHE PATH "Path to JUCE")
add_subdirectory(${JUCE_DIR} JUCE_BUILD)

# Initialize JUCE
juce_add_plugin(Kairos
    VERSION 1.0.0
    COMPANY_NAME "BombestStudios"
    BUNDLE_ID "com.bombeststudios.kairos"
    IS_SYNTH TRUE
    NEEDS_MIDI_INPUT TRUE
    NEEDS_MIDI_OUTPUT FALSE
    IS_MIDI_EFFECT FALSE
    EDITOR_WANTS_KEYBOARD_FOCUS TRUE
    COPY_PLUGIN_AFTER_BUILD TRUE
    PLUGIN_MANUFACTURER_CODE Bomb
    PLUGIN_CODE Kair
    FORMATS AU VST3 VST2 AAX Standalone
    PRODUCT_NAME "Kairos")

# Add source files
target_sources(Kairos
    PRIVATE
        Source/OrchidAudioProcessor.cpp
        Source/OrchidAudioProcessorEditor.cpp)

# Set C++ standard
target_compile_features(Kairos PRIVATE cxx_std_17)

# Link with JUCE modules
target_link_libraries(Kairos
    PRIVATE
        juce::juce_audio_utils
        juce::juce_audio_processors
        juce::juce_core
        juce::juce_data_structures
        juce::juce_events
        juce::juce_graphics
        juce::juce_gui_basics
        juce::juce_gui_extra
        juce::juce_opengl
        juce::juce_recommended_config_flags
        juce::juce_recommended_lto_flags
        juce::juce_recommended_warning_flags)

# Set include directories
target_include_directories(Kairos
    PRIVATE
        Source
        ${JUCE_DIR}/modules)

# Generate JuceHeader.h
juce_generate_juce_header(Kairos)

# Set compiler flags
if(MSVC)
    target_compile_options(Kairos PRIVATE /W4)
else()
    target_compile_options(Kairos PRIVATE -Wall -Wextra -Wpedantic)
endif()

# iOS specific settings
if(IOS)
    set_target_properties(Kairos PROPERTIES
        XCODE_ATTRIBUTE_TARGETED_DEVICE_FAMILY "1,2"  # iPhone and iPad
        XCODE_ATTRIBUTE_IPHONEOS_DEPLOYMENT_TARGET "12.0"
        XCODE_ATTRIBUTE_CODE_SIGN_IDENTITY "Apple Development"
        XCODE_ATTRIBUTE_CODE_SIGN_STYLE "Automatic"
        XCODE_ATTRIBUTE_PROVISIONING_PROFILE_SPECIFIER ""
        XCODE_ATTRIBUTE_DEVELOPMENT_TEAM ""
    )
    
    # Add iOS-specific frameworks
    target_link_libraries(Kairos PRIVATE
        "-framework AudioToolbox"
        "-framework AVFoundation"
        "-framework CoreAudio"
        "-framework CoreMIDI"
    )
endif()

# AAX specific settings
if(AAX_SDK_PATH)
    set_target_properties(Kairos PROPERTIES
        AAX_SDK_PATH "${AAX_SDK_PATH}"
    )
endif() 