#include "OrchidAudioProcessor.h"
#include "OrchidAudioProcessorEditor.h"

KairosAudioProcessor::KairosAudioProcessor()
    : AudioProcessor(BusesProperties()
        .withOutput("Output", juce::AudioChannelSet::stereo(), true)),
      apvts(*this, nullptr, "Parameters", createParameters())
{
    // Initialize parameters
    addParameter(osc1Level = new juce::AudioParameterFloat("osc1level", "Oscillator 1 Level", 0.0f, 1.0f, 0.5f));
    addParameter(osc2Level = new juce::AudioParameterFloat("osc2level", "Oscillator 2 Level", 0.0f, 1.0f, 0.5f));
    addParameter(osc3Level = new juce::AudioParameterFloat("osc3level", "Oscillator 3 Level", 0.0f, 1.0f, 0.5f));
    addParameter(osc4Level = new juce::AudioParameterFloat("osc4level", "Oscillator 4 Level", 0.0f, 1.0f, 0.5f));
    
    juce::StringArray waveforms = {"Sine", "Saw", "Square", "Triangle"};
    addParameter(osc1Waveform = new juce::AudioParameterChoice("osc1wave", "Oscillator 1 Waveform", waveforms, 0));
    addParameter(osc2Waveform = new juce::AudioParameterChoice("osc2wave", "Oscillator 2 Waveform", waveforms, 0));
    addParameter(osc3Waveform = new juce::AudioParameterChoice("osc3wave", "Oscillator 3 Waveform", waveforms, 0));
    addParameter(osc4Waveform = new juce::AudioParameterChoice("osc4wave", "Oscillator 4 Waveform", waveforms, 0));
    
    addParameter(filterCutoff = new juce::AudioParameterFloat("cutoff", "Filter Cutoff", 20.0f, 20000.0f, 1000.0f));
    addParameter(filterResonance = new juce::AudioParameterFloat("resonance", "Filter Resonance", 0.1f, 10.0f, 1.0f));
    
    addParameter(attack = new juce::AudioParameterFloat("attack", "Attack", 0.001f, 5.0f, 0.1f));
    addParameter(decay = new juce::AudioParameterFloat("decay", "Decay", 0.001f, 5.0f, 0.1f));
    addParameter(sustain = new juce::AudioParameterFloat("sustain", "Sustain", 0.0f, 1.0f, 0.8f));
    addParameter(release = new juce::AudioParameterFloat("release", "Release", 0.001f, 5.0f, 0.1f));
    
    addParameter(reverbLevel = new juce::AudioParameterFloat("reverb", "Reverb", 0.0f, 1.0f, 0.0f));
    addParameter(chorusLevel = new juce::AudioParameterFloat("chorus", "Chorus", 0.0f, 1.0f, 0.0f));
    addParameter(delayLevel = new juce::AudioParameterFloat("delay", "Delay", 0.0f, 1.0f, 0.0f));
    
    // Initialize synthesizer
    for (int i = 0; i < 16; ++i)
        synth.addVoice(new SynthVoice());
    
    synth.addSound(new SynthSound());
}

KairosAudioProcessor::~KairosAudioProcessor()
{
}

void KairosAudioProcessor::prepareToPlay(double sampleRate, int /*samplesPerBlock*/)
{
    synth.setCurrentPlaybackSampleRate(sampleRate);
}

void KairosAudioProcessor::releaseResources()
{
}

void KairosAudioProcessor::processBlock(juce::AudioBuffer<float>& buffer, juce::MidiBuffer& midiMessages)
{
    buffer.clear();
    synth.renderNextBlock(buffer, midiMessages, 0, buffer.getNumSamples());
}

juce::AudioProcessorEditor* KairosAudioProcessor::createEditor()
{
    return new OrchidAudioProcessorEditor(*this);
}

void KairosAudioProcessor::getStateInformation(juce::MemoryBlock& destData)
{
    auto state = apvts.copyState();
    std::unique_ptr<juce::XmlElement> xml(state.createXml());
    copyXmlToBinary(*xml, destData);
}

void KairosAudioProcessor::setStateInformation(const void* data, int sizeInBytes)
{
    std::unique_ptr<juce::XmlElement> xmlState(getXmlFromBinary(data, sizeInBytes));
    if (xmlState.get() != nullptr)
        if (xmlState->hasTagName(apvts.state.getType()))
            apvts.replaceState(juce::ValueTree::fromXml(*xmlState));
}

// SynthVoice implementation
bool KairosAudioProcessor::SynthVoice::canPlaySound(juce::SynthesiserSound* sound)
{
    return dynamic_cast<SynthSound*>(sound) != nullptr;
}

void KairosAudioProcessor::SynthVoice::startNote(int midiNoteNumber, float velocity, juce::SynthesiserSound*, int)
{
    frequency = juce::MidiMessage::getMidiNoteInHertz(midiNoteNumber);
    level = velocity;
    tailOff = 0.0;
}

void KairosAudioProcessor::SynthVoice::stopNote(float /*velocity*/, bool allowTailOff)
{
    if (allowTailOff)
    {
        if (tailOff == 0.0)
            tailOff = 1.0;
    }
    else
    {
        clearCurrentNote();
    }
}

void KairosAudioProcessor::SynthVoice::renderNextBlock(juce::AudioBuffer<float>& outputBuffer, int startSample, int numSamples)
{
    if (getCurrentlyPlayingSound() == nullptr)
        return;
        
    // Basic sine wave synthesis for now - will be expanded with more sophisticated synthesis
    for (int sample = 0; sample < numSamples; ++sample)
    {
        const float currentSample = std::sin(2.0 * juce::MathConstants<double>::pi * phase) * level;
        
        for (int channel = 0; channel < outputBuffer.getNumChannels(); ++channel)
        {
            outputBuffer.addSample(channel, startSample + sample, currentSample);
        }
        
        phase += frequency / getSampleRate();
        if (phase >= 1.0)
            phase -= 1.0;
    }
}

void KairosAudioProcessor::SynthVoice::controllerMoved(int, int)
{
}

void KairosAudioProcessor::SynthVoice::pitchWheelMoved(int)
{
}

juce::AudioProcessorValueTreeState::ParameterLayout KairosAudioProcessor::createParameters()
{
    std::vector<std::unique_ptr<juce::RangedAudioParameter>> params;

    // Oscillator parameters
    params.push_back(std::make_unique<juce::AudioParameterFloat>("osc1_level", "Oscillator 1 Level", 0.0f, 1.0f, 0.5f));
    params.push_back(std::make_unique<juce::AudioParameterChoice>("osc1_waveform", "Oscillator 1 Waveform", juce::StringArray("Sine", "Saw", "Square", "Triangle"), 0));
    params.push_back(std::make_unique<juce::AudioParameterFloat>("osc2_level", "Oscillator 2 Level", 0.0f, 1.0f, 0.5f));
    params.push_back(std::make_unique<juce::AudioParameterChoice>("osc2_waveform", "Oscillator 2 Waveform", juce::StringArray("Sine", "Saw", "Square", "Triangle"), 0));

    // Filter parameters
    params.push_back(std::make_unique<juce::AudioParameterFloat>("filter_cutoff", "Filter Cutoff", 20.0f, 20000.0f, 1000.0f));
    params.push_back(std::make_unique<juce::AudioParameterFloat>("filter_resonance", "Filter Resonance", 0.1f, 10.0f, 1.0f));

    // ADSR parameters
    params.push_back(std::make_unique<juce::AudioParameterFloat>("attack", "Attack", 0.0f, 5.0f, 0.1f));
    params.push_back(std::make_unique<juce::AudioParameterFloat>("decay", "Decay", 0.0f, 5.0f, 0.1f));
    params.push_back(std::make_unique<juce::AudioParameterFloat>("sustain", "Sustain", 0.0f, 1.0f, 0.8f));
    params.push_back(std::make_unique<juce::AudioParameterFloat>("release", "Release", 0.0f, 5.0f, 0.1f));

    // Effects parameters
    params.push_back(std::make_unique<juce::AudioParameterFloat>("reverb_level", "Reverb Level", 0.0f, 1.0f, 0.0f));
    params.push_back(std::make_unique<juce::AudioParameterFloat>("delay_level", "Delay Level", 0.0f, 1.0f, 0.0f));

    return { params.begin(), params.end() };
}

juce::AudioProcessor* JUCE_CALLTYPE createPluginFilter()
{
    return new KairosAudioProcessor();
} 