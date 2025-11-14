package com.mariamole.demo.model;


import jakarta.persistence.*; 
import java.time.LocalDateTime;


@Entity 
@Table(name = "historico_musicas")
public class HistoricoMusica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeUsuario;
    private String telefoneUsuario;    private String videoId;
    private String titulo;

    private LocalDateTime horarioCadastro; 
    private LocalDateTime horarioExibicao; 

    public HistoricoMusica() {}

    public HistoricoMusica(String nomeUsuario, String telefoneUsuario, String videoId, String titulo, LocalDateTime horarioCadastro) {
        this.nomeUsuario = nomeUsuario;
        this.telefoneUsuario = telefoneUsuario;
        this.videoId = videoId;
        this.titulo = titulo;
        this.horarioCadastro = horarioCadastro;
    }

   public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public String getTelefoneUsuario() { return telefoneUsuario; } 
    public void setTelefoneUsuario(String telefoneUsuario) { this.telefoneUsuario = telefoneUsuario; }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDateTime getHorarioCadastro() {
        return horarioCadastro;
    }

    public void setHorarioCadastro(LocalDateTime horarioCadastro) {
        this.horarioCadastro = horarioCadastro;
    }

    public LocalDateTime getHorarioExibicao() {
        return horarioExibicao;
    }

    public void setHorarioExibicao(LocalDateTime horarioExibicao) {
        this.horarioExibicao = horarioExibicao;
    }
}

