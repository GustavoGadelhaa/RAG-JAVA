# Testes cURL - RAG API

Documentação de testes para usar a API REST de RAG.

---

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/ingest` | Upload de PDF para indexação |
| POST | `/api/perguntar` | Faz pergunta ao RAG |

---

## 1. Ingerir PDF

Envia um PDF para ser processado, chunkado e indexed no banco vetorial.

```bash
curl -X POST -F "file=@/caminho/para/arquivo.pdf" http://localhost:8080/api/ingest
```

### Exemplo completo

```bash
curl -X POST -F "file=@./docs/acoes_extramuros_prep_pep.pdf" http://localhost:8080/api/ingest
```

### Parâmetros

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `file` | file | Arquivo PDF a ser enviado |

### Resposta de sucesso

```
PDF processado com sucesso!
```

### Resposta de erro

```json
{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/api/ingest"}
```

---

## 2. Fazer pergunta

Envia uma pergunta para o RAG, que busca os 4 documentos mais relevantes e usa como contexto para o modelo generar uma resposta.

```bash
curl -X POST "http://localhost:8080/api/perguntar?ask=Sua%20pergunta%20aqui"
```

### Exemplo

```bash
curl -X POST "http://localhost:8080/api/perguntar?ask=Qual%20o%20titulo%20do%20documento"
```

### Parâmetros

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `ask` | string | Pergunta a ser feita ao RAG |

### Resposta

Retorna o texto da resposta gerada pelo modelo llama3.2 com base nos documentos indexados.

---

## 3. Fluxo completo de uso

### Passo 1: Verificar se a API está rodando

```bash
curl -X POST "http://localhost:8080/api/perguntar?ask=test" --max-time 10
```

Se a API estiver rodando, receberá uma resposta (pode ser erro se não houver documentos indexados).

### Passo 2: Ingerir um PDF

```bash
curl -X POST -F "file=@./docs/acoes_extramuros_prep_pep.pdf" http://localhost:8080/api/ingest
```

**Tempo estimado:** ~2-5 minutos (depende do tamanho do PDF)

### Passo 3: Fazer perguntas sobre o documento

```bash
# Pergunta 1
curl -X POST "http://localhost:8080/api/perguntar?ask=Qual%20o%20titulo%20do%20documento"

# Pergunta 2
curl -X POST "http://localhost:8080/api/perguntar?ask=Quais%20sao%20os%20principais%20topicos"

# Pergunta 3
curl -X POST "http://localhost:8080/api/perguntar?ask=Resuma%20o%20conteudo%20do%20arquivo"
```

---

## Opções úteis do curl

### Timeout

```bash
curl -X POST "http://localhost:8080/api/perguntar?ask=pergunta" --max-time 60
```

### Verbose (mostrar detalhes)

```bash
curl -v -X POST "http://localhost:8080/api/perguntar?ask=test"
```

### Salvar resposta em arquivo

```bash
curl -X POST "http://localhost:8080/api/perguntar?ask=pergunta" -o resposta.txt
```

---

## Possíveis erros

### "model not found"

Significa que o modelo Ollama não está instalado:

```bash
docker exec ollama ollama pull nomic-embed-text
docker exec ollama ollama pull llama3.2
```

### "Connection refused"

A API não está rodando. Inicie com:

```bash
java -jar target/project-0.0.1-SNAPSHOT.jar
```

### 500 Internal Server Error

Verifique os logs da aplicação ou se o Ollama está respondendo:

```bash
curl http://localhost:11434/api/tags
```