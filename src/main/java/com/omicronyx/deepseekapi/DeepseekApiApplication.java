package com.omicronyx.deepseekapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import io.github.ollama4j.*;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@SpringBootApplication
@RestController
public class DeepseekApiApplication {
    private static final String MODEL = "deepseek-r1:8b";
    private static final String HOST = "http://localhost:11434/";
    private static final String FILE_PATH = System.getenv().getOrDefault("DATA_FILE_PATH", "arepas_reina_pepiada.spanish.txt");
    private static final Config CONFIG = extractTopicAndLanguage(FILE_PATH);
    private static final OllamaAPI OLLAMA_API = new OllamaAPI(HOST);

    public static void main(String[] args) {
        SpringApplication.run(DeepseekApiApplication.class, args);
        log("SERVER", "API server running");
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> body, @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        if (clientIp == null) return badRequestResponse("The 'X-Forwarded-For' header is required.");

        String query = body.getOrDefault("query", "Hello");
        log(clientIp, "Processing /ask request");

        String data = readFileData();
        if (data == null) return Map.of("error", "Data file is missing or cannot be read.");

        try {
            if (!isRelatedToTopic(query)) return badRequestResponse("This API only responds to questions about " + CONFIG.topic);
            return Map.of("response", processQuery(data, query));
        } catch (OllamaBaseException | IOException | InterruptedException e) {
            return internalServerError(e);
        }
    }

    @GetMapping("/")
    public Map<String, Object> home(@RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        if (clientIp == null) return badRequestResponse("The 'X-Forwarded-For' header is required.");

        log(clientIp, "Request: /");
        return Map.of(
            "message", "Welcome to the DeepSeek AI API! This API provides answers in " + CONFIG.language + " about " + CONFIG.topic + ".",
            "usage", Map.of(
                "endpoint", "/ask",
                "method", "POST",
                "body", Map.of("query", "Your question here"),
                "example", Map.of("query", "Cómo hacer unas " + CONFIG.topic + "?"),
                "response", Map.of("response", "Step-by-step instructions in Spanish.")
            )
        );
    }

    private static Config extractTopicAndLanguage(String fileName) {
        String[] parts = fileName.replace(".txt", "").split("\\.");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid file name format. Expected {topic}.{language}.txt");
        return new Config(parts[0].replace("_", " "), capitalize(parts[1]));
    }

    private static String readFileData() {
        try {
            return Files.readString(Path.of(FILE_PATH));
        } catch (IOException e) {
            log("SERVER", "Error reading the file: " + e.getMessage());
            return null;
        }
    }

    private static boolean isRelatedToTopic(String userInput) throws OllamaBaseException, IOException, InterruptedException {
        return askOllama(constructValidationQuery(userInput)).contains("yes");
    }

    private static String processQuery(String data, String query) throws OllamaBaseException, IOException, InterruptedException {
        return askOllama(constructQuery(data, query));
    }

    private static String askOllama(String prompt) throws OllamaBaseException, IOException, InterruptedException {
        OLLAMA_API.setRequestTimeoutSeconds(120);
        OllamaResult result = OLLAMA_API.generate(MODEL, prompt, true, new OptionsBuilder().build());
        return (result != null) ? result.getResponse().trim().toLowerCase() : "No response received.";
    }

    private static String constructValidationQuery(String userInput) {
        return String.format("Be concise, respond only in %s. Is the following query related to \"%s\"? Respond with \"yes\" or \"no\". Query: %s", CONFIG.language, CONFIG.topic, userInput);
    }

    private static String constructQuery(String data, String userInput) {
        return String.format("Directives: Be as brief and concise as possible, language only %s. Data: %s. Query: %s.", CONFIG.language, data, userInput);
    }

    private static void log(String ip, String message) {
        System.out.println(Instant.now() + " - " + ip + " - " + message);
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static Map<String, Object> badRequestResponse(String message) {
        return Map.of(
            "timestamp", Instant.now().toString(),
            "status", 400,
            "error", "Bad Request",
            "message", message,
            "path", "/"
        );
    }

    private static Map<String, Object> internalServerError(Exception e) {
        return Map.of(
            "timestamp", Instant.now().toString(),
            "status", 500,
            "error", "Internal Server Error",
            "message", e.getMessage(),
            "path", "/"
        );
    }

    private record Config(String topic, String language) {}
}
