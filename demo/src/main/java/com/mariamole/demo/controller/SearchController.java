package com.mariamole.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.api.services.youtube.model.SearchListResponse;
import com.mariamole.demo.service.YouTubeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api") 
@CrossOrigin(origins = "*") 
public class SearchController {

    private final YouTubeService youTubeService;

    @Autowired
    public SearchController(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(@RequestParam("q") String query) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("O parâmetro 'q' da query não pode estar vazio.");
        }

        try {
            SearchListResponse response = youTubeService.search(query);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            e.printStackTrace(); 
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erro ao comunicar com a API do YouTube: " + e.getMessage());
        }
    }
}
