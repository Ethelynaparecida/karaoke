package com.mariamole.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mariamole.demo.model.Usuario;
import com.mariamole.demo.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api") 
@CrossOrigin(origins = "*") 
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    
    @Autowired
    public AuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> payload) {
        
        String nome = payload.get("nome");
        String email = payload.get("email");
        String cpf = payload.get("cpf");

        if (nome == null || email == null || cpf == null) {
            return ResponseEntity.badRequest().body("Dados de login incompletos.");
        }
        Optional<Usuario> optUsuario = usuarioRepository.findByCpf(cpf);

        if (optUsuario.isPresent()) {
            Usuario usuarioExistente = optUsuario.get();
            usuarioExistente.setNome(nome); 
            usuarioExistente.setEmail(email); 
            usuarioExistente.setUltimoLogin(LocalDateTime.now());
            
            usuarioRepository.save(usuarioExistente); 
            System.out.println("Login (Atualização) para o CPF: " + cpf);

        } else {
            Usuario novoUsuario = new Usuario(nome, email, cpf);
            usuarioRepository.save(novoUsuario); 
            System.out.println("Login (Novo Utilizador) para o CPF: " + cpf);
        }

        System.out.println("Login recebido para o CPF: " + cpf + " (Nome: " + nome + ")");
        
        return ResponseEntity.ok().body("Login bem-sucedido.");
    }
}