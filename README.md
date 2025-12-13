# Agentics AI Library

A powerful Java library for building and executing AI agents with function calling capabilities, memory management, and multi-provider support.

## Features

- 🤖 **AI Agent Framework**: Build intelligent agents with configurable behavior
- 🔧 **Function Calling**: Execute tools and functions within agentExecutionService conversations
- 🧠 **Memory Management**: Conversation memory with multiple strategies
- 🎯 **Smart Triggers**: Conditional agentExecutionService activation based on various criteria
- 🌐 **Multi-Provider Support**: OpenAI, Azure OpenAI, Amazon Bedrock (planned)
- 📊 **Observability**: Built-in metrics, tracing, and logging
- ⚡ **Reactive**: Non-blocking, reactive programming model
- 🔌 **Framework Agnostic**: Works with Spring Boot, Quarkus, Micronaut, or pure Java

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.agentics</groupId>
    <artifactId>agentics-ai</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Setup Provider Clients

```java
// Lazy initialization - clients created when first requested
ProviderClientFactory factory = new ProviderClientFactory();

// Or register manually (overrides lazy initialization)
ProviderClientOpenAI client = ProviderClientOpenAI.create("your-api-key");
factory.registerClient(client);
```

### 3. Create an Agent

```java
public class MyAgentService {
    
    public Agent createMathAgent() {
        // Create a calculator tool
        Tool calculator = Tool.builder()
            .name("calculator")
            .description("Performs mathematical calculations")
            .function(parameters -> {
                String expression = (String) parameters.get("expression");
                // Your calculation logic here
                return Tool.ToolResult.success("42");
            })
            .build();
        
        // Create provider client factory (lazy initialization)
        ProviderClientFactory factory = new ProviderClientFactory();
        
        // Optionally register custom client
        // factory.registerClient(ProviderClientOpenAI.create("your-api-key"));
        
        // Create the agentExecutionService
        return Agent.builder()
            .name("MathAgent")
            .instructions("You are a helpful math assistant")
            .modelConfig(ModelConfig.openaiGpt4())
            .tools(List.of(calculator))
            .trigger(Trigger.keywordTrigger(List.of("calculate", "math")))
            .memory(ConversationMemory.builder().build())
            .providerClientFactory(factory)
            .build();
    }
}
```

### 4. Use the Agent

```java
public class AgentController {
    
    private final MyAgentService agentService = new MyAgentService();
    
    public AgentResponse chat(String message) {
        Agent agentExecutionService = agentService.createMathAgent();
        
        if (agentExecutionService.shouldTrigger(message)) {
            return agentExecutionService.execute(message);
        }
        
        return AgentResponse.error("Agent not triggered");
    }
    
    public AgentResponse chatWithContext(String message, Map<String, Object> context) {
        Agent agentExecutionService = agentService.createMathAgent();
        
        if (agentExecutionService.shouldTrigger(message)) {
            return agentExecutionService.execute(message, context);
        }
        
        return AgentResponse.error("Agent not triggered");
    }
}
```

## Configuration Options

### Provider Configuration

```properties
# OpenAI
agentics.ai.providers.openai.api-key=your-key
agentics.ai.providers.openai.api-url=https://api.openai.com/v1
agentics.ai.providers.openai.model=gpt-4

# Azure OpenAI
agentics.ai.providers.azure.api-key=your-key
agentics.ai.providers.azure.api-url=https://your-resource.openai.azure.com
agentics.ai.providers.azure.model=gpt-4
```

### Agent Settings

```properties
agentics.ai.agentExecutionService.enable-function-calling=true
agentics.ai.agentExecutionService.enable-memory=true
agentics.ai.agentExecutionService.default-max-iterations=10
agentics.ai.agentExecutionService.default-timeout-ms=30000
```

### Logging & Debugging

```properties
agentics.ai.logging.enable-debug-logging=true
agentics.ai.logging.log-requests=true
agentics.ai.logging.log-responses=true
agentics.ai.logging.log-tool-calls=true
```

## Architecture

### Core Components

- **Agent**: Main agentExecutionService class with builder pattern
- **Tool**: Function calling interface
- **Trigger**: Conditional activation system
- **ConversationMemory**: Memory management
- **ModelConfig**: AI model configuration
- **ProviderClient**: Multi-provider abstraction

### Memory Strategies

- **SLIDING_WINDOW**: Keep last N messages
- **TOKEN_LIMIT**: Keep messages within token limit
- **SUMMARY**: Summarize old messages
- **PERSISTENT**: Keep all messages

### Trigger Types

- **ALWAYS**: Always trigger
- **KEYWORD**: Trigger on keywords
- **REGEX**: Trigger on regex patterns
- **INTENT**: Trigger on specific intent
- **CONDITIONAL**: Custom conditions

## Usage Examples

### Basic Agent Creation
```java
// Create agentExecutionService with lazy initialization
ProviderClientFactory factory = new ProviderClientFactory();
Agent agentExecutionService = Agent.builder()
    .name("MyAgent")
    .instructions("You are a helpful assistant")
    .modelConfig(ModelConfig.openaiGpt4())
    .providerClientFactory(factory)
    .build();

// Execute agentExecutionService
AgentResponse response = agentExecutionService.execute("Hello!");
```

## Roadmap

- [ ] Amazon Bedrock integration
- [ ] Advanced memory strategies
- [ ] Agent orchestration
- [ ] Streaming responses
- [ ] Custom model fine-tuning
- [ ] Agent persistence
- [ ] Multi-agentExecutionService conversations

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
