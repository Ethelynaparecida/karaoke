package com.mariamole.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.services.youtube.model.SearchListResponse;
import com.mariamole.demo.service.MusicQueueService;
import com.mariamole.demo.service.YouTubeService;

@RestController
@RequestMapping("/api") 
@CrossOrigin(origins = "*") 
public class SearchController {

    private final YouTubeService youTubeService;
    private final MusicQueueService musicQueueService;

    @Autowired
    public SearchController(YouTubeService youTubeService, MusicQueueService musicQueueService) {
        this.youTubeService = youTubeService;
        this.musicQueueService = musicQueueService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("O parâmetro 'q' da query não pode estar vazio.");
        }

        List<Map<String, Object>> sugestoesLocais = musicQueueService.sugerirMusicasPorTexto(query);
        if (!sugestoesLocais.isEmpty()) {
            return ResponseEntity.ok(sugestoesLocais);
        }

        try {
            SearchListResponse response = youTubeService.search(query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erro ao buscar músicas: " + e.getMessage());
        }
    }

    @GetMapping("/search/local")
    public ResponseEntity<List<Map<String, Object>>> searchLocalSuggestions(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(musicQueueService.sugerirMusicasPorTexto(query));
    }
}
