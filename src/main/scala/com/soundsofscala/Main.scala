package com.soundsofscala

import cats.effect.{ExitCode, IO, IOApp}
import com.soundsofscala.Instruments.{ScalaSynth, SimpleScala808DrumMachine}
import com.soundsofscala.models.*
import com.soundsofscala.models.AtomicMusicalEvent.*
import com.soundsofscala.songs.{TestSong1, TestSong2}
import com.soundsofscala.synthesis.Oscillator.*
import com.soundsofscala.synthesis.WaveType.*
import com.soundsofscala.synthesis.{Oscillator, WaveType}
import com.soundsofscala.transport.Sequencer
import org.scalajs.dom
import org.scalajs.dom.*
import org.scalajs.dom.html.Select
import com.soundsofscala.syntax.all.*

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = buildUI().as(ExitCode.Success)

  import cats.effect.unsafe.implicits.global

  given audioContext: AudioContext = new AudioContext()
  val startingFrequency = 440
  val startingVolume = 0.3
  val waveTypes = List(
    WaveType.Sine,
    WaveType.Sawtooth,
    WaveType.Square,
    WaveType.Triangle
  )

  val oscillatorSine: Oscillator = SineOscillator(startingFrequency)
  val oscillatorSaw: Oscillator = SawtoothOscillator(startingFrequency / 4)
  val oscillatorSquare: Oscillator = SquareOscillator(startingFrequency / 2)
  val oscillatorTriangle: Oscillator = TriangleOscillator(startingFrequency - 3)

  private def updateSynthVolume(waveType: WaveType): Unit =
    val volume = retrieveValueFromInputNode(s"${waveType.toString.toLowerCase}-volume")
    waveType match
      case WaveType.Sine =>
        oscillatorSine.updateVolume(volume)
      case WaveType.Sawtooth =>
        oscillatorSaw.updateVolume(volume)
      case WaveType.Square =>
        oscillatorSquare.updateVolume(volume)
      case WaveType.Triangle =>
        oscillatorTriangle.updateVolume(volume)

  private def updateSynthFilterFrequency(): Unit =
    val frequency = retrieveValueFromInputNode("filter-frequency")
    oscillatorSaw.updateFilterFrequency(frequency)
    oscillatorSine.updateFilterFrequency(frequency)
    oscillatorSquare.updateFilterFrequency(frequency)
    oscillatorTriangle.updateFilterFrequency(frequency)

  private def updateSynthFrequency(): Unit =
    val frequency = retrieveValueFromInputNode("frequency")
    oscillatorSaw.updateFrequency(frequency / 4)
    oscillatorSine.updateFrequency(frequency)
    oscillatorSquare.updateFrequency(frequency / 2)
    oscillatorTriangle.updateFrequency(frequency - 3)

  private def compoundSynthPlayButton(): AudioContext ?=> Element =
    val div = document.createElement("div")
    div.classList.add("button-pad")

    val compoundSynth = document.createElement("button")
    compoundSynth.classList.add("transport-button")
    compoundSynth.textContent = "▶️"

    compoundSynth.addEventListener(
      "click",
      (e: dom.MouseEvent) =>

        def getCurrentVolume(waveType: WaveType) =
          retrieveValueFromInputNode(s"${waveType.toString.toLowerCase}-volume")
        oscillatorSaw.play(getCurrentVolume(Sawtooth))
        oscillatorSine.play(getCurrentVolume(Sine))
        oscillatorSquare.play(getCurrentVolume(Square))
        oscillatorTriangle.play(getCurrentVolume(Triangle))
    )
    div.appendChild(compoundSynth)
    div

  private def buildUI(): IO[Unit] = IO {
    document.addEventListener(
      "DOMContentLoaded",
      (e: dom.Event) =>

        val scalaSynth = ScalaSynth()
        given ScalaSynth = scalaSynth
        val homeDiv = document.createElement("div")
        homeDiv.classList.add("home-div")

        val heading = document.createElement("h1")
        heading.textContent = "Welcome to Sounds of Scala"

        val playground = document.createElement("p")
        playground.textContent = "Feel free to use this playground page to try things out"

        val scalaSynthTitle = document.createElement("h2")
        scalaSynthTitle.textContent = "ScalaSynth"

        val drumMachineLabel = document.createElement("label")
        drumMachineLabel.textContent = "SimpleScala808DrumMachine"

        val sequencerLabel = document.createElement("label")
        sequencerLabel.textContent = "Sequencer/Scheduler Example Song"

        val drumSynth = document.createElement("label")
        drumSynth.textContent = "Drum Synth"

        val simpleSineSynthLabel = document.createElement("h1")
        simpleSineSynthLabel.textContent = "Oscillators"

        // Append elements to Document

        homeDiv.append(
          heading,
          playground,
          scalaSynthTitle,
          compoundButtonPad(compoundSynthButton)(scalaSynth),
          drumMachineLabel,
          simple808DrumMachineDiv,
          sequencerLabel,
          playSong,
          drumSynth,
          synthDrums,
          simpleSineSynthLabel,
          buildDropDownOscillatorSelecter(),
          buttonPad(makeAButton),
          buildCompoundSynthPanel
        )
        document.body.appendChild(homeDiv)
    )
  }

  private def buildCompoundSynthPanel =
    val compoundSynthContainer = document.createElement("div")
    compoundSynthContainer.classList.add("compound-container")
    val compoundSynthLabel = document.createElement("h1")
    val levelsLabel = document.createElement("h2")
    levelsLabel.textContent = "Oscillator Levels"
    val filterLabel = document.createElement("h2")
    filterLabel.textContent = "Lowpass Filter"
    compoundSynthLabel.textContent = "Compound Synth"
    val levelsContainer = document.createElement("div")
    levelsContainer.classList.add("levels-container")
    val levelsDiv = document.createElement("div")
    levelsDiv.classList.add("levels")
    val filterContainer = document.createElement("div")
    filterContainer.classList.add("levels-container")
    val filterDiv = document.createElement("div")
    filterDiv.classList.add("levels")
    val transport = document.createElement("div")
    transport.classList.add("transport")
    transport.append(
      compoundSynthPlayButton(),
      stopSynthButton()
    )
    filterDiv.append(
      filterLabel,
      buildFilterFreqSlider(10000)
    )
    levelsDiv.append(
      levelsLabel,
      buildVolumeSlider(startingVolume, Sine),
      buildVolumeSlider(startingVolume, Sawtooth),
      buildVolumeSlider(startingVolume * 2, Triangle),
      buildVolumeSlider(startingVolume, Square)
    )

    filterContainer.appendChild(filterDiv)
    levelsContainer.appendChild(levelsDiv)

    compoundSynthContainer.append(
      compoundSynthLabel,
      transport,
      buildPitchSlider(startingFrequency),
      filterContainer,
      levelsContainer
    )
    compoundSynthContainer

  def appendH2(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("h2")
    parNode.textContent = text
    targetNode.appendChild(parNode)
  }

  private def simple808DrumMachineDiv: Element =
    val div = document.createElement("div")
    div.classList.add("button-pad")
    val drumMachine = SimpleScala808DrumMachine(Tempo(100), LookAhead(25), ScheduleWindow(0.1))
    val sequencerButtonDiv = document.createElement("button")
    sequencerButtonDiv.textContent = "️🥁"
    sequencerButtonDiv.addEventListener(
      "click",
      (_: dom.MouseEvent) =>
        drumMachine
          .playGroove(TestSong1.kick, TestSong1.snare, TestSong1.hats, TestSong1.clap)
          .unsafeRunAndForget()
    )
    div.appendChild(sequencerButtonDiv)
    div

  private def playSong: Element =
    val div = document.createElement("div")
    div.classList.add("button-pad")
    val sequencerButtonDiv = document.createElement("button")
    sequencerButtonDiv.textContent = "️🎼"
    sequencerButtonDiv.addEventListener(
      "click",
      (_: dom.MouseEvent) =>
        val sequencer = Sequencer()
        sequencer.playSong(TestSong1.demoSong()).unsafeRunAndForget()
    )
    div.appendChild(sequencerButtonDiv)
    div

  private def synthDrums: Element =
    val div = document.createElement("div")
    div.classList.add("button-pad")
    val sequencerButtonDiv = document.createElement("button")
    sequencerButtonDiv.textContent = "️🛢️"
    sequencerButtonDiv.addEventListener(
      "click",
      (_: dom.MouseEvent) =>
        val sequencer = Sequencer()
        sequencer.playSong(TestSong2.drumSynthSong()).unsafeRunAndForget()
    )
    div.appendChild(sequencerButtonDiv)
    div

  private def buildDropDownOscillatorSelecter(): Element =
    val div = document.createElement("div")
    div.classList.add("button-pad")

    val select: html.Select = document.createElement("select") match {
      case selectElement: html.Select => selectElement
    }
    select.id = "oscillator"
    select.classList.add("oscillator")
    val options = waveTypes.map(waveSelectionOptions)
    select.append(options: _*)
    div.appendChild(select)
    div

  private def waveSelectionOptions(waveType: WaveType): Element =
    val option = document.createElement("option")
    option.textContent = waveType.toString
    option

  def buttonPad(buttonBuilder: Pitch => Element): Element =
    val buttonPad = document.createElement("div")
    buttonPad.classList.add("button-pad")

    val buttonList = List(
      Pitch.C,
      Pitch.D,
      Pitch.E,
      Pitch.F,
      Pitch.G,
      Pitch.A,
      Pitch.B
    ).map(buttonBuilder)

    buttonPad.append(buttonList*)
    buttonPad

  private def retrieveValueFromInputNode(elementId: String): Double =
    dom.document.getElementById(elementId) match
      case input: html.Input => input.value.toDouble
      case _ => 0.0

  private def getSlider(id: String) = dom.document.createElement(id) match
    case sliderElement: html.Input => sliderElement

  def compoundButtonPad(buttonBuilder: Note => Element)(scalaSynth: ScalaSynth): Element =
    given ScalaSynth = scalaSynth
    val buttonPad = document.createElement("div")
    buttonPad.classList.add("button-pad")

    val buttonList = List(
      C3,
      D3,
      E3,
      F3,
      G3,
      A3,
      B3
    ).map(buttonBuilder)

    buttonPad.append(buttonList: _*)
    buttonPad
// val testingDSL = C(5).flat.quarter + D(5).flat.quarter + E(5).flat.quarter

  def makeAButton(pitch: Pitch): AudioContext ?=> Element =
    val button = document.createElement("button")
    button.textContent = pitch.toString
    button.addEventListener(
      "click",
      (_: dom.MouseEvent) =>
        val waveType = Oscillator.stringToWaveType(
          document.getElementById("oscillator") match
            case select: Select => select.value
        )
        val oscillator = Oscillator(waveType = waveType, frequency = 440, volume = 0.5)
        oscillator.updateFrequencyFromPitch(pitch)
        oscillator.play(audioContext.currentTime)
        oscillator.stop(audioContext.currentTime + 0.5)
    )
    button

  private def buildVolumeSlider(startingV: Double, waveType: WaveType) =
    val div = document.createElement("div")
    val label = document.createElement("label")
    label.textContent = s"${waveType.toString}"
    label.classList.add("vol-label")
    div.appendChild(label)
    div.classList.add("slider-pad")
    val slider: html.Input = getSlider("input")
    slider.className = "range"
    slider.classList.add("volume")
    slider.id = s"${waveType.toString.toLowerCase}-volume"
    slider.`type` = "range"
    slider.min = "0"
    slider.max = "1"
    slider.step = "0.01"
    slider.value = startingV.toString

    slider.addEventListener(
      "input",
      (_: dom.Event) => updateSynthVolume(waveType)
    )
    div.appendChild(slider)
    div

  private def buildFilterFreqSlider(startingF: Int) =
    val sliderContainer = document.createElement("div")
    sliderContainer.classList.add("slider-container")
    val label = document.createElement("label")
    label.textContent = "Frequency"
    val slider = getSlider("input")
    slider.className = "range"
    slider.id = "filter-frequency"
    slider.`type` = "range"
    slider.min = "20"
    slider.max = "12000"
    slider.step = "0.1"
    slider.value = startingF.toString

    slider.addEventListener(
      "input",
      (_: dom.Event) => updateSynthFilterFrequency()
    )
    sliderContainer.append(label, slider)
    sliderContainer

  private def buildPitchSlider(startingF: Int) =
    val sliderContainer = document.createElement("div")
    sliderContainer.classList.add("slider-container")
    val label = document.createElement("label")
    label.textContent = "Frequency"
    val slider = getSlider("input")
    slider.className = "range"
    slider.id = "frequency"
    slider.`type` = "range"
    slider.min = "20"
    slider.max = "1000"
    slider.step = "0.01"
    slider.value = startingF.toString

    slider.addEventListener(
      "input",
      (_: dom.Event) => updateSynthFrequency()
    )
    sliderContainer.append(label, slider)
    sliderContainer

  private def stopSynthButton(): Element =
    val div = document.createElement("div")
    div.classList.add("button-pad")

    val stopSynth = document.createElement("button")
    stopSynth.classList.add("transport-button")
    stopSynth.textContent = "🛑"
    stopSynth.addEventListener(
      "click",
      (e: dom.MouseEvent) =>
        oscillatorSine.stop(1)
        oscillatorSaw.stop(1)
        oscillatorSquare.stop(1)
        oscillatorTriangle.stop(1)
    )
    div.appendChild(stopSynth)
    div

  def compoundSynthButton(note: Note)(using synth: ScalaSynth)(
      using ac: AudioContext): Element =
    val button = document.createElement("button")
    button.textContent = note.pitch.toString
    button.addEventListener(
      "click",
      (_: dom.MouseEvent) =>
        synth.attackRelease(ac.currentTime, note, Tempo(120), Release(1)).unsafeRunAndForget()
    )
    button

}
