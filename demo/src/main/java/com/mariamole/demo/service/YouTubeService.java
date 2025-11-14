package com.mariamole.demo.service;
import com.google.api.client.googleapis.json.GoogleJsonResponseException; 
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.VideoListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List; 
import java.util.concurrent.atomic.AtomicInteger; 
import java.util.concurrent.atomic.AtomicLong;

@Service
public class YouTubeService {

    @Value("${youtube.api.keys}")
    private List<String> apiKeys;

    private YouTube youtube;
    private final String APPLICATION_NAME = "MariamoleKaraokeApp";
    
    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);
    private final AtomicLong quotaUsedToday = new AtomicLong(0);

    @PostConstruct
    public void init() {
        this.youtube = new YouTube.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            request -> {} 
        ).setApplicationName(APPLICATION_NAME).build();
    }


    public SearchListResponse search(String query) throws IOException {
        
        if (currentKeyIndex.get() >= apiKeys.size()) {
            System.err.println("ROTAÇÃO DE CHAVES: Todas as chaves do YouTube atingiram a cota.");
            throw new IOException("Todas as chaves da API do YouTube atingiram a cota.");
        }

        String activeKey = apiKeys.get(currentKeyIndex.get());
        
        try {
            YouTube.Search.List searchRequest = youtube.search().list(Collections.singletonList("snippet"));
            searchRequest.setKey(activeKey); 
            searchRequest.setQ(query);
            searchRequest.setType(Collections.singletonList("video"));
            searchRequest.setMaxResults(15L);
            searchRequest.setVideoEmbeddable("true");

            SearchListResponse response = searchRequest.execute();
            
            quotaUsedToday.addAndGet(100);
            System.out.println("LOG COTA: Busca usou 100 unidades (Chave " + currentKeyIndex.get() + "). Total: " + quotaUsedToday.get());
            
            return response;

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 403 && e.getDetails().getErrors().stream()
                    .anyMatch(err -> "quotaExceeded".equals(err.getReason()))) {
                
                System.err.println("AVISO: Cota da Chave " + currentKeyIndex.get() + " excedida. A rodar para a próxima chave.");
                currentKeyIndex.incrementAndGet(); 
                
                return search(query);
            } else {
                throw e;
            }
        }
    }


    public VideoListResponse getVideoDetails(String videoId) throws IOException {
        
        if (currentKeyIndex.get() >= apiKeys.size()) {
            System.err.println("ROTAÇÃO DE CHAVES: Todas as chaves do YouTube atingiram a cota.");
            throw new IOException("Todas as chaves da API do YouTube atingiram a cota.");
        }

        String activeKey = apiKeys.get(currentKeyIndex.get());

        try {
            YouTube.Videos.List videoRequest = youtube.videos().list(Collections.singletonList("snippet"));
            videoRequest.setKey(activeKey);
            videoRequest.setId(Collections.singletonList(videoId));
            VideoListResponse response = videoRequest.execute();
            quotaUsedToday.addAndGet(1);
            System.out.println("LOG COTA: Details usou 1 unidade (Chave " + currentKeyIndex.get() + "). Total: " + quotaUsedToday.get());
            
            return response;
            
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 403 && e.getDetails().getErrors().stream()
                    .anyMatch(err -> "quotaExceeded".equals(err.getReason()))) {
                
                System.err.println("AVISO: Cota da Chave " + currentKeyIndex.get() + " excedida. A rodar para a próxima chave.");
                currentKeyIndex.incrementAndGet();
                
                return getVideoDetails(videoId);
            } else {
                throw e;
            }
        }
    }
    
    public long getCurrentQuotaUsage() {
        return quotaUsedToday.get();
    }
    
    @Scheduled(cron = "0 0 8 * * ?", zone = "UTC") // 8:00 UTC = ~Meia-noite na Califórnia
    public void resetQuotaCounter() {
        System.out.println("SCHEDULER: A resetar contadores. Cota antiga: " + quotaUsedToday.get());
        
        quotaUsedToday.set(0);
        currentKeyIndex.set(0); 
    }

    public int getNumberOfKeys() {
        if (this.apiKeys == null) {
            return 0;
        }
        return this.apiKeys.size();
    }
}