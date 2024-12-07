package com.example.springboot.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.LibVosk;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class AudioController {

    // Load Vosk model once when the application starts
    private static final String MODEL_PATH = "/home/ssharma4/IdeaProjects/SpringBoot/src/main/resources/vosk-model-small-en-us-0.15";
    private static Model model;

    static {
        // Initialize Vosk
        LibVosk.setLogLevel(LogLevel.DEBUG); // Set log level for detailed output
        try {
            // Load the Vosk model
            model = new Model(MODEL_PATH);
        } catch (IOException e) {
            System.err.println("Failed to load Vosk model: " + e.getMessage());
        }
    }

    @PostMapping("/audio-to-text")
    public String convertAudioToText(@RequestParam("audioFile") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return "{\"text\": \"No audio file uploaded.\"}";
        }

        // Log audio file details
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());
        System.out.println("Received audio file: " + audioFile.getOriginalFilename());
        System.out.println("File size: " + audioFile.getSize() + " bytes");

        // Save the file temporarily to check if it is received correctly
        File tempFile;
        try {
            tempFile = new File("/home/ssharma4/IdeaProjects/SpringBoot/temp_" + audioFile.getOriginalFilename());
            audioFile.transferTo(tempFile);
            System.out.println("Audio file saved to: " + tempFile.getAbsolutePath());
        } catch (IOException e) {
            return "{\"text\": \"Error saving audio file: " + e.getMessage() + "\"}";
        }

        // Convert audio to text using Vosk
        String transcription = transcribeAudio(tempFile);

        // Optionally, delete the temporary file if not needed
        // tempFile.delete();

        return "{\"text\": \"" + transcription + "\"}";
    }

    private String transcribeAudio(File audioFile) {
        StringBuilder result = new StringBuilder();

        try (InputStream audioStream = new BufferedInputStream(new FileInputStream(audioFile))) {
            // Get audio input stream
            AudioInputStream ais = AudioSystem.getAudioInputStream(audioStream);

            // Log audio format information
            System.out.println("Audio format: " + ais.getFormat());
            System.out.println("Audio encoding: " + ais.getFormat().getEncoding());
            System.out.println("Sample rate: " + ais.getFormat().getSampleRate());
            System.out.println("Channels: " + ais.getFormat().getChannels());

            // Check if the audio format is supported
            if (!ais.getFormat().getEncoding().toString().equalsIgnoreCase("PCM_SIGNED") ||
                    ais.getFormat().getSampleRate() != 16000 ||
                    ais.getFormat().getChannels() != 1) {
                return "Unsupported audio format. Please upload a WAV file with PCM encoding, 16 kHz sample rate, and mono channel.";
            }

            // Create recognizer and process audio
            try (Recognizer recognizer = new Recognizer(model, 16000)) {
                int nbytes;
                byte[] buffer = new byte[4096];
                while ((nbytes = ais.read(buffer)) >= 0) {
                    if (recognizer.acceptWaveForm(buffer, nbytes)) {
                        System.out.println("Result: " + recognizer.getResult());
                        result.append(recognizer.getResult()).append(" ");
                    } else {
                        System.out.println("Partial result: " + recognizer.getPartialResult());
                    }
                }

                // Get the final transcription
                System.out.println("Final result: " + recognizer.getFinalResult());
                result.append(recognizer.getFinalResult());
            }

        } catch (IOException | UnsupportedAudioFileException e) {
            System.err.println("Error processing audio file: " + e.getMessage());
            return "Error processing audio file.";
        }

        return result.toString().trim(); // Return the transcription result
    }


}
