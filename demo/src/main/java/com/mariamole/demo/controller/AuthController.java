package com.mariamole.demo.controller;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
    
    @Value("${resend.api.key}")
    private String resendApiKey;

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
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    private String limparTexto(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto.replaceAll("\\s+", "").toLowerCase(), Normalizer.Form.NFD);
        return semAcento.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private boolean nomesSaoParecidos(String nome1, String nome2) {
        String n1 = limparTexto(nome1);
        String n2 = limparTexto(nome2);
        if (n1.isEmpty() || n2.isEmpty()) return n1.equals(n2);
        
        int[][] dp = new int[n1.length() + 1][n2.length() + 1];
        for (int i = 0; i <= n1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= n2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= n1.length(); i++) {
            for (int j = 1; j <= n2.length(); j++) {
                int custo = (n1.charAt(i - 1) == n2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + custo);
            }
        }
        return (1.0 - ((double) dp[n1.length()][n2.length()] / Math.max(n1.length(), n2.length()))) >= 0.80;
    }


    @PostMapping("/login/solicitar-token")
    public ResponseEntity<?> solicitarToken(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String nome = (String) payload.get("nome");
        String email = (String) payload.get("email");
        String telefone = (String) payload.get("telefone");
        boolean forcar = payload.containsKey("forcar") && (Boolean) payload.get("forcar");
        String ipCliente = obterIpDoCliente(request);

        logger.info("[LOGIN - SOLICITACAO] Email: [{}]", email);

        if (nome == null || email == null || telefone == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Preencha todos os campos."));
        }

        LocalDateTime ultimo = temposTokens.get(email);
        if (ultimo != null && ultimo.plusMinutes(5).isAfter(LocalDateTime.now())) {
            return ResponseEntity.ok().body(Map.of("reutilizado", true, "mensagem", "Código ainda válido. Verifique o seu e-mail."));
        }

        List<Usuario> usuariosIp = usuarioRepository.findByUltimoIp(ipCliente);
        for (Usuario u : usuariosIp) {
            if (nomesSaoParecidos(nome, u.getNome())) {
                telefone = u.getTelefone();
                break;
            }
        }

        if (musicQueueService.getPosicaoPorTelefone(telefone) > 0 && !forcar) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("requerConfirmacao", true, "erro", "Já tens uma música na fila!"));
        }
        if (forcar) musicQueueService.removeSongByUserId(telefone);

        String token = String.format("%04d", new Random().nextInt(10000));
        tokensTemporarios.put(email, token);
        temposTokens.put(email, LocalDateTime.now());
        dadosPendentes.put(email, Map.of("nome", nome, "telefone", telefone, "ip", ipCliente));

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Karaoke <onboarding@resend.dev>");
            body.put("to", List.of(email));
            body.put("subject", "Seu Código de Acesso");
            body.put("html", "<strong>Olá " + nome + "!</strong><br>Seu código é: <h1 style='color:blue'>" + token + "</h1>");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);

            logger.info("[LOGIN - RESEND] Email enviado com sucesso para [{}]", email);
            return ResponseEntity.ok().body(Map.of("mensagem", "Código enviado com sucesso!"));

        } catch (Exception e) {
            logger.error("[LOGIN - ERRO RESEND] Motivo: {}", e.getMessage());
            tokensTemporarios.remove(email);
            temposTokens.remove(email);
            return ResponseEntity.status(500).body(Map.of("erro", "Falha ao enviar e-mail."));
        }
    }

    @PostMapping("/login/validar-token")
    public ResponseEntity<?> validarToken(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String tokenDigitado = payload.get("token");

        LocalDateTime gerado = temposTokens.get(email);
        if (gerado == null || gerado.plusMinutes(5).isBefore(LocalDateTime.now())) {
            limparSessao(email);
            return ResponseEntity.status(401).body(Map.of("erro", "Código expirado."));
        }

        if (!tokenDigitado.equals(tokensTemporarios.get(email))) {
            return ResponseEntity.status(401).body(Map.of("erro", "Código incorreto."));
        }

        Map<String, Object> dados = dadosPendentes.get(email);
        salvarUsuarioNoBanco(email, (String)dados.get("nome"), (String)dados.get("telefone"), (String)dados.get("ip"));
        limparSessao(email);

        return ResponseEntity.ok().body(Map.of("mensagem", "Login efetuado!"));
    }

    private void limparSessao(String email) {
        tokensTemporarios.remove(email);
        temposTokens.remove(email);
        dadosPendentes.remove(email);
    }

    private void salvarUsuarioNoBanco(String email, String nome, String telefone, String ip) {
        Usuario user = usuarioRepository.findByTelefone(telefone).orElse(new Usuario());
        user.setNome(nome);
        user.setEmail(email);
        user.setTelefone(telefone);
        user.setUltimoIp(ip);
        user.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(user);
    }
}