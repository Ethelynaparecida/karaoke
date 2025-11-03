package com.mariamole.demo.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct; 
import java.io.IOException;
import java.util.Collections; 

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private YouTube youtube;
    private final String APPLICATION_NAME = "MariamoleKaraoke";
    
    @PostConstruct
    public void init() {
        this.youtube = new YouTube.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            request -> {} 
        ).setApplicationName(APPLICATION_NAME).build();
    }

    /*** Pesquisa de vídeos no YouTube.
     * @return A resposta da API do YouTube. */
    public SearchListResponse search(String query) throws IOException {
        
        YouTube.Search.List searchRequest = youtube.search()
                .list(Collections.singletonList("snippet")); 
                searchRequest.setKey(apiKey);     
        searchRequest.setQ(query);          
        searchRequest.setType(Collections.singletonList("video")); 
        searchRequest.setMaxResults(15L);  
        return searchRequest.execute();
    }
}