#pragma once

#include <JuceHeader.h>
#include "OrchidAudioProcessor.h"

class KairosAudioProcessorEditor : public juce::AudioProcessorEditor
{
public:
    explicit KairosAudioProcessorEditor(KairosAudioProcessor&);
    ~KairosAudioProcessorEditor() override;

    void paint(juce::Graphics&) override;
    void resized() override;

private:
    KairosAudioProcessor& audioProcessor;
    
    // GUI Components
    juce::Slider osc1LevelSlider;
    juce::Slider osc2LevelSlider;
    juce::Slider osc3LevelSlider;
    juce::Slider osc4LevelSlider;
    
    juce::ComboBox osc1WaveformCombo;
    juce::ComboBox osc2WaveformCombo;
    juce::ComboBox osc3WaveformCombo;
    juce::ComboBox osc4WaveformCombo;
    
    juce::Slider filterCutoffSlider;
    juce::Slider filterResonanceSlider;
    
    juce::Slider attackSlider;
    juce::Slider decaySlider;
    juce::Slider sustainSlider;
    juce::Slider releaseSlider;
    
    juce::Slider reverbLevelSlider;
    juce::Slider chorusLevelSlider;
    juce::Slider delayLevelSlider;
    
    // Labels
    juce::Label osc1LevelLabel;
    juce::Label osc2LevelLabel;
    juce::Label osc3LevelLabel;
    juce::Label osc4LevelLabel;
    
    juce::Label osc1WaveformLabel;
    juce::Label osc2WaveformLabel;
    juce::Label osc3WaveformLabel;
    juce::Label osc4WaveformLabel;
    
    juce::Label filterCutoffLabel;
    juce::Label filterResonanceLabel;
    
    juce::Label attackLabel;
    juce::Label decayLabel;
    juce::Label sustainLabel;
    juce::Label releaseLabel;
    
    juce::Label reverbLevelLabel;
    juce::Label chorusLevelLabel;
    juce::Label delayLevelLabel;
    
    // Parameter attachments
    std::vector<std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>> sliderAttachments;
    std::vector<std::unique_ptr<juce::AudioProcessorValueTreeState::ComboBoxAttachment>> comboAttachments;

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR(KairosAudioProcessorEditor)
}; 