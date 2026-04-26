# Projeto RAG — Java + Spring Boot

API REST de RAG (Retrieval-Augmented Generation) que indexa PDFs e responde perguntas com base no conteúdo dos documentos.

---

## Stack

| Tecnologia | Versão / Detalhe |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring AI | 1.0.0 |
| Ollama (embedding) | nomic-embed-text — 768 dims — porta 11434 |
| Ollama (chat) | llama3.2 |
| pgvector | pgvector/pgvector:pg17 — porta 5434 — banco: ragdb |
| PDFBox | 3.0.3 |
| Lombok | — |
| Maven | — |

---

## Como funciona

1. Recebe PDF via upload (`POST /api/ingest`)
2. Extrai texto com PDFBox
3. Faz chunking com `TokenTextSplitter`
4. Gera embeddings com `nomic-embed-text` via Ollama
5. Salva vetores no pgvector
6. Responde perguntas (`POST /api/perguntar`) buscando os top-4 chunks mais similares e enviando como contexto pro `llama3.2`

---

## Estrutura de Packages

```
src/main/java/com/rag/project/
├── controller/
│   └── RagController.java
├── service/
│   ├── PdfIngestService.java
│   └── RagService.java
├── dto/
│   ├── PerguntaRequest.java
│   └── PerguntaResponse.java
├── config/
│   └── ChatClientConfig.java
└── ProjectApplication.java
```

| Package | Responsabilidade |
|---|---|
| `controller` | Recebe requisições HTTP e delega para os services |
| `service` | Toda a lógica de negócio (ingestão, chunking, embedding, RAG) |
| `dto` | Records de entrada e saída dos endpoints |
| `config` | Beans do Spring (ChatClient) |

> `entity` e `repository` não são necessários pois o Spring AI gerencia a tabela `vector_store` internamente via pgvector. Adicione essa camada apenas se precisar persistir metadados extras dos PDFs.

---

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/ingest` | Upload de PDF para indexação (param: `file`) |
| POST | `/api/perguntar` | Faz pergunta ao RAG (param: `ask`) |

---

## Testes com cURL

### 1. Ingerir PDF (endpoint /api/ingest)

```bash
# Fazer upload de um PDF para indexação
curl -X POST -F "file=@/caminho/para/seu.pdf" http://localhost:8080/api/ingest
```

**Exemplo:**
```bash
curl -X POST -F "file=@/home/gusta/Downloads/rag/project/docs/acoes_extramuros_prep_pep.pdf" http://localhost:8080/api/ingest
```

**Resposta esperada (sucesso):**
```
PDF processado com sucesso!
```

---

### 2. Fazer pergunta (endpoint /api/perguntar)

```bash
# Enviar uma pergunta ao RAG
curl -X POST "http://localhost:8080/api/perguntar?ask=Sua%20pergunta%20aqui"
```

**Exemplos:**
```bash
# Pergunta simples
curl -X POST "http://localhost:8080/api/perguntar?ask=O%20que%20contem%20o%20documento"

# Pergunta sobre conteúdo específico
curl -X POST "http://localhost:8080/api/perguntar?ask=Quais%20sao%20os%20principais%20topicos"
```

**Resposta:** Retorna a resposta gerada pelo modelo llama3.2 com base no contexto dos documentos indexados.

---

### 3. Verificar se a aplicação está rodando

```bash
curl http://localhost:8080/api/perguntar?ask=test
```

---

## Configuração

### application.properties

```properties
spring.application.name=project

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text

# PGVector
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.schema-mode=UPDATE

# PostgreSQL (VPS)
spring.datasource.url=jdbc:postgresql://<IP_VPS>:5434/ragdb
spring.datasource.username=postgres
spring.datasource.password=<SENHA>
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## Docker

O Ollama roda localmente como serviço do sistema (`systemctl`). O pgvector roda na VPS como container separado.

### docker-compose.yml (Ollama local — opcional)

```yaml
services:
  ollama:
    image: ollama/ollama
    container_name: ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped

volumes:
  ollama_data:
```

```bash
docker compose up -d
```

### pgvector na VPS

```bash
docker run -d \
  --name postgres-vector \
  -e POSTGRES_DB=ragdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=<SENHA> \
  -p 5434:5432 \
  -v pgvector_data:/var/lib/postgresql/data \
  pgvector/pgvector:pg17
```

Habilitar a extensão:

```bash
docker exec -it postgres-vector psql -U postgres -d ragdb -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

---

## Modelos Ollama

| Modelo | Dimensões | Uso |
|---|---|---|
| `nomic-embed-text` | 768 | Embedding — recomendado |
| `mxbai-embed-large` | 1024 | Embedding — mais preciso |
| `all-minilm` | 384 | Embedding — ultraleve |
| `llama3.2` | — | Chat / geração de resposta |

```bash
ollama pull nomic-embed-text
ollama pull llama3.2
```

---

## Ordem de Implementação

1. Configurar `application.properties`
2. Criar `ChatClientConfig`
3. Criar DTOs (`PerguntaRequest`, `PerguntaResponse`)
4. Criar `PdfIngestService`
5. Criar `RagService`
6. Criar `RagController`
7. Testar `POST /api/ingest` com um PDF
8. Testar `POST /api/perguntar`

---

## Possíveis Problemas

| Problema | Solução |
|---|---|
| Modelos Ollama não instalados | `ollama pull nomic-embed-text && ollama pull llama3.2` |
| Porta 11434 em uso | O Ollama já está rodando via `systemctl` — não precisa do container |
| Version faltando nas deps Spring AI | Adicionar `spring-ai-bom` no `dependencyManagement` |
| groupId com link markdown no pom.xml | Verificar se groupId é `org.springframework.ai` sem colchetes |

---

## Referências

- [Spring AI PGVector](https://docs.spring.io/spring-ai/reference/api/vectorstores/pgvector.html)
- [Spring AI Ollama](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [PDFBox](https://pdfbox.apache.org/)
- [pgvector](https://github.com/pgvector/pgvector)