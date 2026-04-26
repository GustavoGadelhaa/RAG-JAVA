# RAG API - Java + Spring Boot

API REST de RAG (Retrieval-Augmented Generation) para indexação de PDFs e respostas a perguntas usando Ollama e PGVector.

---

## Stack

| Tecnologia | Versão / Detalhe |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring AI | 2.0.0-M4 |
| Ollama (embedding) | nomic-embed-text — 768 dims |
| Ollama (chat) | llama3.2 |
| PostgreSQL + pgvector | Porta 5434 — banco: ragdb |
| PDFBox | 3.0.3 |
| Lombok | — |
| Maven | — |

---

## Como funciona

1. Recebe PDF via upload (`POST /api/ingest`)
2. Extrai texto com PDFBox
3. Faz chunking do texto (~500 chars por chunk)
4. Gera embeddings com `nomic-embed-text` via Ollama
5. Salva vetores no pgvector
6. Responde perguntas (`POST /api/perguntar`) buscando os top-4 chunks mais similares e enviando como contexto pro `llama3.2`

---

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/ingest` | Upload de PDF para indexação (param: `file`) |
| POST | `/api/perguntar` | Faz pergunta ao RAG (param: `ask`) |

---

## Como rodar

### 1. Configurar aplicação

Crie um arquivo `src/main/resources/application.properties` com o conteúdo:

```properties
spring.application.name=project

# Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text

# PGVector
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.schema-mode=UPDATE

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5434/ragdb
spring.datasource.username=postgres
spring.datasource.password=SUA_SENHA_AQUI
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### 2. Subir dependências (Docker)

```yaml
# docker-compose.yaml
services:
  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama

  postgres:
    image: pgvector/pgvector:pg16
    ports:
      - "5434:5432"
    environment:
      POSTGRES_DB: ragdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: SUA_SENHA_AQUI

volumes:
  ollama_data:
```

### 3. Baixar modelos Ollama

```bash
docker exec ollama ollama pull nomic-embed-text
docker exec ollama ollama pull llama3.2
```

### 4. Compilar e rodar

```bash
mvn clean package
java -jar target/project-0.0.1-SNAPSHOT.jar
```

---

## Testes com cURL

### Ingerir PDF

```bash
curl -X POST -F "file=@/caminho/para/seu.pdf" http://localhost:8080/api/ingest
```

**Resposta:** `PDF processado com sucesso!`

### Fazer pergunta

```bash
curl -X POST "http://localhost:8080/api/perguntar?ask=Sua%20pergunta%20aqui"
```

**Resposta:** Retorna a resposta gerada pelo modelo com base no contexto dos documentos indexados.

---

## Estrutura do projeto

```
src/main/java/com/rag/project/
├── controller/
│   └── RagController.java      # Endpoints REST
├── service/
│   ├── PdfIngestService.java  # Extração, chunking e embedding
│   └── RagService.java        # Similarity search e chat
├── config/
│   └── ChatClientConfig.java  # Configuração do ChatClient
└── ProjectApplication.java    # Main class
```

---

## Variáveis de ambiente sensíveis

O arquivo `application.properties` contém credenciais sensíveis (senha do banco de dados). **Não versione este arquivo** — ele já está no `.gitignore`.

---

## Licença

MIT