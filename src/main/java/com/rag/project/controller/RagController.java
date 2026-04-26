package com.rag.project.controller;

import com.rag.project.service.PdfIngestService;
import com.rag.project.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RagController {

    private final PdfIngestService pdfIngestService;
    private final RagService ragService;

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestPdf(@RequestParam("file") MultipartFile file) {
        log.info("Recebendo PDF: {}", file.getOriginalFilename());
        var resultado = pdfIngestService.ingestPdf(file);
        log.info("PDF processado com {} chunks", resultado.chunksCount());
        return ResponseEntity.ok(resultado.message());
    }

    @PostMapping("/perguntar")
    public ResponseEntity<String> perguntar(@RequestParam("ask") String pergunta) {
        log.info("Pergunta recebida: {}", pergunta);
        var resultado = ragService.perguntar(pergunta);
        log.info("Resposta gerada com {} documentos", resultado.documentosUsados());
        return ResponseEntity.ok(resultado.resposta());
    }
}