package com.mariamole.demo.controller;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariamole.demo.model.Usuario;
import com.mariamole.demo.repository.UsuarioRepository;
import com.mariamole.demo.service.MusicQueueService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioRepository usuarioRepository;
    private final MusicQueueService musicQueueService;
    
    @Autowired
    private JavaMailSender mailSender; 

    private final Map<String, String> tokensTemporarios = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> dadosPendentes = new ConcurrentHashMap<>();
    
    private final Map<String, LocalDateTime> temposTokens = new ConcurrentHashMap<>();

    @Autowired
    public AuthController(UsuarioRepository usuarioRepository, MusicQueueService musicQueueService) {
        this.usuarioRepository = usuarioRepository;
        this.musicQueueService = musicQueueService;
    }

    private String obterIpDoCliente(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String limparTexto(String texto) {
        if (texto == null) return "";
        String semEspaco = texto.replaceAll("\\s+", "").toLowerCase();
        String semAcento = Normalizer.normalize(semEspaco, Normalizer.Form.NFD);
        return semAcento.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private boolean nomesSaoParecidos(String nome1, String nome2) {
        String n1 = limparTexto(nome1);
        String n2 = limparTexto(nome2);

        if (n1.isEmpty() && n2.isEmpty()) return true;
        if (n1.isEmpty() || n2.isEmpty()) return false;

        int[][] dp = new int[n1.length() + 1][n2.length() + 1];
        for (int i = 0; i <= n1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= n2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= n1.length(); i++) {
            for (int j = 1; j <= n2.length(); j++) {
                int custo = (n1.charAt(i - 1) == n2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + custo);
            }
        }

        int maxLen = Math.max(n1.length(), n2.length());
        double similaridade = 1.0 - ((double) dp[n1.length()][n2.length()] / maxLen);

        return similaridade >= 0.80;
    }

    @PostMapping("/login/solicitar-token")
    public ResponseEntity<?> solicitarToken(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String nome = (String) payload.get("nome");
        String email = (String) payload.get("email");
        String telefone = (String) payload.get("telefone");
        boolean forcarSubstituicao = payload.containsKey("forcar") && (Boolean) payload.get("forcar");
        String ipCliente = obterIpDoCliente(request);

        logger.info("[LOGIN - SOLICITACAO] Iniciando tentativa. Email: [{}]", email);

        if (nome == null || email == null || telefone == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "É necessário preencher Nome, E-mail e Telefone."));
        }

        LocalDateTime tempoUltimoToken = temposTokens.get(email);
        if (tempoUltimoToken != null && tempoUltimoToken.plusMinutes(5).isAfter(LocalDateTime.now())) {
            logger.info("[LOGIN - ANTI-SPAM] Token ainda valido para [{}]. Nao enviaremos novo email.", email);
            
            Map<String, Object> infoUsuario = Map.of("nome", nome, "telefone", telefone, "ip", ipCliente);
            dadosPendentes.put(email, infoUsuario);

            return ResponseEntity.ok().body(Map.of(
                "reutilizado", true,
                "mensagem", "Um código ainda válido já foi enviado aos seus e-mails há pouco tempo. Por favor, verifique a caixa de entrada e spam."
            ));
        }

        List<Usuario> usuariosNesseIp = usuarioRepository.findByUltimoIp(ipCliente);
        for (Usuario u : usuariosNesseIp) {
            if (nomesSaoParecidos(nome, u.getNome())) {
                telefone = u.getTelefone(); 
                break; 
            }
        }

        int posicaoNaFila = musicQueueService.getPosicaoPorTelefone(telefone); 

        if (posicaoNaFila > 0 && !forcarSubstituicao) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "requerConfirmacao", true, 
                        "erro", "Este utilizador já tem uma música na fila em outro aparelho. Deseja cancelar o pedido anterior e aceder por aqui?"
                    ));
        }

        if (posicaoNaFila > 0 && forcarSubstituicao) {
            musicQueueService.removeSongByUserId(telefone); 
        }

        String token = String.format("%04d", new Random().nextInt(10000));
        
        tokensTemporarios.put(email, token);
        temposTokens.put(email, LocalDateTime.now()); 
        
        Map<String, Object> infoUsuario = Map.of(
            "nome", nome, "telefone", telefone, "ip", ipCliente
        );
        dadosPendentes.put(email, infoUsuario);

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setTo(email);
            mensagem.setSubject("Código de Acesso - Karaokê");
            mensagem.setText("Olá " + nome + "!\nSeu código de acesso para pedir músicas é: " + token + "\n\n(Este código é válido por 5 minutos).");
            mailSender.send(mensagem);
            
            logger.info("[LOGIN - EMAIL ENVIADO] Token [{}] enviado com sucesso para Email: [{}]", token, email);
        } catch (Exception e) {
            logger.error("[LOGIN - ERRO EMAIL] Falha ao disparar email para [{}].", email);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Não foi possível enviar o código para este e-mail. Verifique se está correto."));
        }

        return ResponseEntity.ok().body(Map.of("mensagem", "Código enviado com sucesso para o seu e-mail!"));
    }

    @PostMapping("/login/validar-token")
    public ResponseEntity<?> validarToken(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String tokenDigitado = payload.get("token");

        if (email == null || tokenDigitado == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "O E-mail e o Código são obrigatórios."));
        }

        LocalDateTime tempoGerado = temposTokens.get(email);
        if (tempoGerado == null || tempoGerado.plusMinutes(5).isBefore(LocalDateTime.now())) {
            tokensTemporarios.remove(email);
            temposTokens.remove(email);
            dadosPendentes.remove(email);
            
            logger.warn("[VALIDACAO - EXPIRADO] Token expirado para Email: [{}]", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "O código expirou após 5 minutos. Por favor, volte e solicite um novo."));
        }

        String tokenCorreto = tokensTemporarios.get(email);
        
        if (tokenCorreto == null || !tokenCorreto.equals(tokenDigitado)) {
            logger.warn("[VALIDACAO - FALHA] Token incorreto para Email: [{}]", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "O código digitado está incorreto. Tente novamente."));
        }

        Map<String, Object> dados = dadosPendentes.get(email);
        String telefone = (String) dados.get("telefone"); 
        String nome = (String) dados.get("nome");
        String ip = (String) dados.get("ip");

        Optional<Usuario> optUsuario = usuarioRepository.findByTelefone(telefone);

        if (optUsuario.isPresent()) {
            Usuario usuarioExistente = optUsuario.get();
            usuarioExistente.setNome(nome);
            usuarioExistente.setEmail(email); 
            usuarioExistente.setUltimoIp(ip); 
            usuarioExistente.setUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(usuarioExistente);
        } else {
            Usuario novoUsuario = new Usuario(); 
            novoUsuario.setNome(nome);
            novoUsuario.setEmail(email);
            novoUsuario.setTelefone(telefone);
            novoUsuario.setUltimoIp(ip); 
            novoUsuario.setUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(novoUsuario);
        }

        tokensTemporarios.remove(email);
        temposTokens.remove(email);
        dadosPendentes.remove(email);

        logger.info("[VALIDACAO - SUCESSO] Login concluido para o Email: [{}]", email);
        return ResponseEntity.ok().body(Map.of("mensagem", "Login efetuado com sucesso!"));
    }
}