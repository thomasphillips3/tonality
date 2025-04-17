#pragma once

#include <JuceHeader.h>

class KairosAudioProcessor : public juce::AudioProcessor
{
public:
    KairosAudioProcessor();
    ~KairosAudioProcessor() override;

    void prepareToPlay (double sampleRate, int samplesPerBlock) override;
    void releaseResources() override;
    bool isBusesLayoutSupported (const BusesLayout& layouts) const override;
    void processBlock (juce::AudioBuffer<float>&, juce::MidiBuffer&) override;

    juce::AudioProcessorEditor* createEditor() override;
    bool hasEditor() const override { return true; }

    const juce::String getName() const override { return JucePlugin_Name; }
    bool acceptsMidi() const override { return true; }
    bool producesMidi() const override { return false; }
    bool isMidiEffect() const override { return false; }
    double getTailLengthSeconds() const override { return 0.0; }

    int getNumPrograms() override { return 1; }
    int getCurrentProgram() override { return 0; }
    void setCurrentProgram(int) override {}
    const juce::String getProgramName(int) override { return {}; }
    void changeProgramName(int, const juce::String&) override {}

    void getStateInformation (juce::MemoryBlock& destData) override;
    void setStateInformation (const void* data, int sizeInBytes) override;

    juce::AudioProcessorValueTreeState apvts;

private:
    // Synthesis components
    struct SynthVoice : public juce::SynthesiserVoice
    {
        bool canPlaySound(juce::SynthesiserSound* sound) override;
        void startNote(int midiNoteNumber, float velocity, juce::SynthesiserSound*, int currentPitchWheelPosition) override;
        void stopNote(float /*velocity*/, bool allowTailOff) override;
        void controllerMoved(int controllerNumber, int value) override;
        void pitchWheelMoved(int newValue) override;
        void renderNextBlock(juce::AudioBuffer<float>&, int startSample, int numSamples) override;

        double frequency = 0.0;
        double level = 0.0;
        double tailOff = 0.0;
        double phase = 0.0;
    };

    struct SynthSound : public juce::SynthesiserSound
    {
        bool appliesToNote(int /*midiNoteNumber*/) override { return true; }
        bool appliesToChannel(int /*midiChannel*/) override { return true; }
    };

    juce::Synthesiser synth;
    
    // Parameters
    juce::AudioParameterFloat* osc1Level;
    juce::AudioParameterFloat* osc2Level;
    juce::AudioParameterFloat* osc3Level;
    juce::AudioParameterFloat* osc4Level;
    
    juce::AudioParameterChoice* osc1Waveform;
    juce::AudioParameterChoice* osc2Waveform;
    juce::AudioParameterChoice* osc3Waveform;
    juce::AudioParameterChoice* osc4Waveform;
    
    juce::AudioParameterFloat* filterCutoff;
    juce::AudioParameterFloat* filterResonance;
    
    juce::AudioParameterFloat* attack;
    juce::AudioParameterFloat* decay;
    juce::AudioParameterFloat* sustain;
    juce::AudioParameterFloat* release;
    
    juce::AudioParameterFloat* reverbLevel;
    juce::AudioParameterFloat* chorusLevel;
    juce::AudioParameterFloat* delayLevel;
    
    juce::AudioProcessorValueTreeState::ParameterLayout createParameters();

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (KairosAudioProcessor)
}; 