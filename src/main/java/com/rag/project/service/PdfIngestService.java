package com.rag.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfIngestService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final VectorStore vectorStore;

    public record IngestResult(String message, int chunksCount) {}

    public IngestResult ingestPdf(MultipartFile file) {
        try {
            log.debug("Iniciando extração de texto do PDF");
            String texto = extrairTexto(file);
            
            log.debug("Texto extraído, iniciando chunking");
            List<String> chunks = chunkText(texto);
            
            log.debug("Criando {} documentos para embedding", chunks.size());
            List<Document> documents = chunks.stream()
                    .map(chunk -> new Document(chunk))
                    .toList();
            
            log.debug("Salvando vetores no banco");
            vectorStore.add(documents);
            
            log.info("PDF processado com sucesso: {} chunks criados", chunks.size());
            return new IngestResult("PDF processado com sucesso!", chunks.size());
            
        } catch (IOException e) {
            log.error("Erro ao processar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar PDF", e);
        }
    }

    private String extrairTexto(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder chunk = new StringBuilder();

        for (String word : words) {
            if (chunk.length() + word.length() > CHUNK_SIZE) {
                chunks.add(chunk.toString().trim());
                String overlap = chunk.toString();
                int spaceIndex = overlap.lastIndexOf(' ');
                chunk = new StringBuilder(overlap.substring(Math.max(0, spaceIndex - CHUNK_OVERLAP)));
            }
            chunk.append(word).append(" ");
        }

        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString().trim());
        }

        return chunks;
    }
}