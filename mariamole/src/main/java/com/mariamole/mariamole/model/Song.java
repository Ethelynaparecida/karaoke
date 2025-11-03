package com.mariamole.mariamole.model;

import lombok.Data;

@Data
public class Song {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private String channelTitle;
    private String requestedBy; 
}