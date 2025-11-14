package com.mariamole.demo.model;

import jakarta.persistence.*; 
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) 
    private String nome;

    @Column(nullable = false) 
    private String email;

    @Column(nullable = false, unique = true)
    private String telefone;

    private LocalDateTime dataCadastro;
    private LocalDateTime ultimoLogin;

    public Usuario() {}

    public Usuario(String nome, String email, String telefone) { 
        this.nome = nome;
        this.email = email;
        this.telefone = telefone; 
        this.dataCadastro = LocalDateTime.now();
        this.ultimoLogin = LocalDateTime.now();
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; } 
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
    public LocalDateTime getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(LocalDateTime ultimoLogin) { this.ultimoLogin = ultimoLogin; }
}
