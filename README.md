# contextual-deepseek-java-api

## Description

`contextual-deepseek-java-api` is a java Spring Boot-based API that provides contextual responses based on a context file. The API answers queries in Spanish about Venezuelan Reina Pepiada arepas. The responses are derived from the contents of the context file, which is read upon each request.

## Features

- **Contextual Responses**: Provides answers based on the contents of a context document.
- **Spanish Responses**: Answers are returned in Spanish.
- **Easy to Use**: Send queries via HTTP POST and get concise answers in JSON format.

## Prerequisites

- **Ollama** installed and configured
- **DeepSeek AI** (using Ollama)
- **Java 21** or higher
- **Maven** (or Gradle) as a build tool

## Installation

1. To install DeepSeek with Ollama, follow these steps:

````bash
# Download and install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull the DeepSeek model
sudo ollama pull deepseek-r1:8b


2. To install and run the contextual-deepseek-java-api, follow these steps:

Clone the repository:
```bash
git clone <repository-url>
cd contextual-deepseek-java-api
mvn clean package
````

## Usage

Start ollama server and Contextual DeepSeek API and test:

```bash
nohup ollama serve &
mvn spring-boot:run
```

Test Contextual DeepSeek API:

```bash
curl -X GET http://localhost:8080/ -H "X-Forwarded-For: 127.0.0.1"

curl -X POST http://localhost:8080/ask \
-H "X-Forwarded-For: 127.0.0.1" \
-H "Content-Type: application/json" \
-d '{"query": "las arepas de reina pepiada llevan pescado?"}'
```