package com.mariamole.demo.model;


public class MusicaFila {
    
    private String videoId;
    private String titulo; 
    private String nomeUsuario; 
    private String telefoneusuario; 
    private boolean jaTocou;

    public MusicaFila(String videoId, String titulo, String nomeUsuario, String telefoneusuario) {
        this.videoId = videoId;
        this.titulo = titulo;
        this.nomeUsuario = nomeUsuario;
        this.telefoneusuario = telefoneusuario;
        this.jaTocou = false;
    }

    public String getTelefoneUsuario() {
        return telefoneusuario;
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
