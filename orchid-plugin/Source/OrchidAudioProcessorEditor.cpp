#include "OrchidAudioProcessorEditor.h"

KairosAudioProcessorEditor::KairosAudioProcessorEditor(KairosAudioProcessor& p)
    : AudioProcessorEditor(&p), audioProcessor(p)
{
    // Set up sliders
    auto setupSlider = [this](juce::Slider& slider, juce::Label& label, const juce::String& labelText)
    {
        addAndMakeVisible(slider);
        addAndMakeVisible(label);
        label.setText(labelText, juce::dontSendNotification);
        label.attachToComponent(&slider, true);
        slider.setSliderStyle(juce::Slider::RotaryHorizontalVerticalDrag);
        slider.setTextBoxStyle(juce::Slider::TextBoxBelow, false, 80, 20);
    };

    // Set up combo boxes
    auto setupComboBox = [this](juce::ComboBox& combo, juce::Label& label, const juce::String& labelText)
    {
        addAndMakeVisible(combo);
        addAndMakeVisible(label);
        label.setText(labelText, juce::dontSendNotification);
        label.attachToComponent(&combo, true);
        combo.addItem("Sine", 1);
        combo.addItem("Saw", 2);
        combo.addItem("Square", 3);
        combo.addItem("Triangle", 4);
    };

    // Oscillator 1
    setupSlider(osc1LevelSlider, osc1LevelLabel, "Osc 1 Level");
    setupComboBox(osc1WaveformCombo, osc1WaveformLabel, "Osc 1 Waveform");

    // Oscillator 2
    setupSlider(osc2LevelSlider, osc2LevelLabel, "Osc 2 Level");
    setupComboBox(osc2WaveformCombo, osc2WaveformLabel, "Osc 2 Waveform");

    // Filter
    setupSlider(filterCutoffSlider, filterCutoffLabel, "Cutoff");
    setupSlider(filterResonanceSlider, filterResonanceLabel, "Resonance");

    // ADSR
    setupSlider(attackSlider, attackLabel, "Attack");
    setupSlider(decaySlider, decayLabel, "Decay");
    setupSlider(sustainSlider, sustainLabel, "Sustain");
    setupSlider(releaseSlider, releaseLabel, "Release");

    // Effects
    setupSlider(reverbLevelSlider, reverbLevelLabel, "Reverb");
    setupSlider(delayLevelSlider, delayLevelLabel, "Delay");

    // Create parameter attachments
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "osc1Level", osc1LevelSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "osc2Level", osc2LevelSlider));
    comboBoxAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::ComboBoxAttachment>(
        audioProcessor.apvts, "osc1Waveform", osc1WaveformCombo));
    comboBoxAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::ComboBoxAttachment>(
        audioProcessor.apvts, "osc2Waveform", osc2WaveformCombo));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "filterCutoff", filterCutoffSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "filterResonance", filterResonanceSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "attack", attackSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "decay", decaySlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "sustain", sustainSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "release", releaseSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "reverbLevel", reverbLevelSlider));
    sliderAttachments.push_back(std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment>(
        audioProcessor.apvts, "delayLevel", delayLevelSlider));

    setSize(800, 600);
}

KairosAudioProcessorEditor::~KairosAudioProcessorEditor()
{
}

void KairosAudioProcessorEditor::paint(juce::Graphics& g)
{
    g.fillAll(getLookAndFeel().findColour(juce::ResizableWindow::backgroundColourId));
}

void KairosAudioProcessorEditor::resized()
{
    auto bounds = getLocalBounds().reduced(20);
    
    // Oscillators section
    auto oscSection = bounds.removeFromTop(bounds.getHeight() * 0.25);
    auto osc1Area = oscSection.removeFromLeft(oscSection.getWidth() * 0.5);
    auto osc2Area = oscSection;
    
    // Oscillator 1 controls
    osc1WaveformCombo.setBounds(osc1Area.removeFromTop(osc1Area.getHeight() * 0.4));
    osc1LevelSlider.setBounds(osc1Area);
    
    // Oscillator 2 controls
    osc2WaveformCombo.setBounds(osc2Area.removeFromTop(osc2Area.getHeight() * 0.4));
    osc2LevelSlider.setBounds(osc2Area);
    
    // Filter section
    auto filterSection = bounds.removeFromTop(bounds.getHeight() * 0.25);
    filterCutoffSlider.setBounds(filterSection.removeFromLeft(filterSection.getWidth() * 0.5));
    filterResonanceSlider.setBounds(filterSection);
    
    // ADSR section
    auto adsrSection = bounds.removeFromTop(bounds.getHeight() * 0.25);
    auto adsrWidth = adsrSection.getWidth() / 4;
    attackSlider.setBounds(adsrSection.removeFromLeft(adsrWidth));
    decaySlider.setBounds(adsrSection.removeFromLeft(adsrWidth));
    sustainSlider.setBounds(adsrSection.removeFromLeft(adsrWidth));
    releaseSlider.setBounds(adsrSection);
    
    // Effects section
    auto effectsSection = bounds;
    reverbLevelSlider.setBounds(effectsSection.removeFromLeft(effectsSection.getWidth() * 0.5));
    delayLevelSlider.setBounds(effectsSection);
} 