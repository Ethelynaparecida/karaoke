package com.mariamole.demo.model;

import jakarta.persistence.*; 
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity 
@Table(name = "historico_musicas")
@Getter 
@Setter 
@NoArgsConstructor 
public class HistoricoMusica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeUsuario;
    private String telefoneUsuario;
    private String videoId;
    private String titulo;
    private String motivoCancelamento;
    private String ipUsuario;

    private LocalDateTime horarioCadastro; 
    private LocalDateTime horarioExibicao; 

    public HistoricoMusica(String nomeUsuario, String telefoneUsuario, String videoId, String titulo, LocalDateTime horarioCadastro) {
        this.nomeUsuario = nomeUsuario;
        this.telefoneUsuario = telefoneUsuario;
        this.videoId = videoId;
        this.titulo = titulo;
        this.horarioCadastro = horarioCadastro;
    }
}