package com.mariamole.model;


public class MusicaFila {
    
    private String videoId;
    private String titulo; 
    private String nomeUsuario; 
    private String cpfUsuario; 
    private boolean jaTocou;

    public MusicaFila(String videoId, String titulo, String nomeUsuario, String cpfUsuario) {
        this.videoId = videoId;
        this.titulo = titulo;
        this.nomeUsuario = nomeUsuario;
        this.cpfUsuario = cpfUsuario;
        this.jaTocou = false;
    }

    public String getCpfUsuario() {
        return cpfUsuario;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }
    
    public String getVideoId() {
        return videoId;
    }

    public String getTitulo() {
        return titulo;
    }
    
    public boolean isJaTocou() {
        return jaTocou;
    }

    public void setJaTocou(boolean jaTocou) {
        this.jaTocou = jaTocou;
    }
}