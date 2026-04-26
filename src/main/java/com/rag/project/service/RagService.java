package com.rag.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public record PerguntaResult(String resposta, int documentosUsados) {}

    public PerguntaResult perguntar(String pergunta) {
        log.debug("Iniciando busca por documentos similares");
        List<Document> documentosRelevantes = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(pergunta)
                        .topK(4)
                        .build()
        );
        
        log.debug("Encontrados {} documentos relevantes", documentosRelevantes.size());
        
        String contexto = documentosRelevantes.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String prompt = String.format("""
                Você é um assistente helpful. Use o contexto abaixo para responder à pergunta.
                Se a resposta não estiver no contexto, diga que não sabe.
                
                Contexto:
                %s
                
                Pergunta: %s
                """, contexto, pergunta);

        log.debug("Enviando prompt para o modelo");
        String resposta = chatClient.prompt(prompt).call().content();
        
        log.info("Resposta gerada usando {} documentos", documentosRelevantes.size());
        return new PerguntaResult(resposta, documentosRelevantes.size());
    }
}