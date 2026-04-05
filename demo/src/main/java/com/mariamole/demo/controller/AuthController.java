package com.mariamole.demo.controller;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
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
    
    @Value("${mailjet.api.key}")
    private String mailjetApiKey;

    @Value("${mailjet.secret.key}")
    private String mailjetSecretKey;

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
        String semAcento = Normalizer.normalize(texto.replaceAll("\\s+", "").toLowerCase(), Normalizer.Form.NFD);
        return semAcento.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private boolean nomesSaoParecidos(String n1, String n2) {
        String s1 = limparTexto(n1);
        String s2 = limparTexto(n2);
        if (s1.isEmpty() || s2.isEmpty()) return s1.equals(s2);
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int custo = (s1.charAt(i-1) == s2.charAt(j-1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i-1][j]+1, dp[i][j-1]+1), dp[i-1][j-1]+custo);
            }
        }
        return (1.0 - ((double) dp[s1.length()][s2.length()] / Math.max(s1.length(), s2.length()))) >= 0.80;
    }

    @PostMapping("/login/solicitar-token")
    public ResponseEntity<?> solicitarToken(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String nome = (String) payload.get("nome");
        String email = (String) payload.get("email");
        String telefone = (String) payload.get("telefone");
        boolean forcar = payload.containsKey("forcar") && (Boolean) payload.get("forcar");
        String ipCliente = obterIpDoCliente(request);

        logger.info("[LOGIN - SOLICITACAO] Iniciando processo para Email: [{}], IP: [{}]", email, ipCliente);

        if (nome == null || email == null || telefone == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Preencha todos os campos obrigatórios."));
        }

        LocalDateTime ultimoEnvio = temposTokens.get(email);
        if (ultimoEnvio != null && ultimoEnvio.plusMinutes(5).isAfter(LocalDateTime.now())) {
            logger.info("[LOGIN - ANTI-SPAM] Token ainda valido em memoria para [{}]. Reutilizando.", email);
            return ResponseEntity.ok().body(Map.of(
                "reutilizado", true, 
                "mensagem", "Um código ainda válido já foi enviado. Verifique sua caixa de entrada e spam."
            ));
        }

        List<Usuario> usuariosNoMesmoIp = usuarioRepository.findByUltimoIp(ipCliente);
        for (Usuario u : usuariosNoMesmoIp) {
            if (nomesSaoParecidos(nome, u.getNome())) {
                logger.info("[LOGIN - RECUPERACAO] Usuario reconhecido pelo IP. Telefone ajustado de [{}] para [{}]", telefone, u.getTelefone());
                telefone = u.getTelefone();
                break;
            }
        }

        if (musicQueueService.getPosicaoPorTelefone(telefone) > 0 && !forcar) {
            logger.warn("[LOGIN - CONFLITO] Usuario [{}] ja possui musica na fila. Solicitando confirmacao.", telefone);
            return ResponseEntity.status(409).body(Map.of("requerConfirmacao", true, "erro", "Você já tem uma música na fila!"));
        }
        if (forcar) {
            logger.info("[LOGIN - SOBREPOSICAO] Removendo musica anterior para o telefone: [{}]", telefone);
            musicQueueService.removeSongByUserId(telefone);
        }

        String token = String.format("%04d", new Random().nextInt(10000));
        tokensTemporarios.put(email, token);
        temposTokens.put(email, LocalDateTime.now());
        dadosPendentes.put(email, Map.of("nome", nome, "telefone", telefone, "ip", ipCliente));

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String auth = mailjetApiKey + ":" + mailjetSecretKey;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> messages = new ArrayList<>();
            
            Map<String, Object> message = new HashMap<>();
            message.put("From", Map.of("Email", "mariamolekaraoke@gmail.com", "Name", "Karaoke Maria Mole"));
            message.put("To", List.of(Map.of("Email", email)));
            message.put("Subject", "Seu Código de Acesso");
            message.put("HTMLPart", "<h3>Olá " + nome + "!</h3><p>Seu código para pedir músicas é: <b style='font-size:24px; color:#007bff;'>" + token + "</b></p><p>Este código expira em 5 minutos.</p>");
            
            messages.add(message);
            body.put("Messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.mailjet.com/v3.1/send", entity, String.class);

            logger.info("[LOGIN - MAILJET] Sucesso! Email com token [{}] enviado para [{}]", token, email);
            return ResponseEntity.ok().body(Map.of("mensagem", "Código enviado com sucesso!"));

        } catch (Exception e) {
            logger.error("[LOGIN - ERRO MAILJET] Falha critica ao enviar para [{}]. Erro: {}", email, e.getMessage());
            tokensTemporarios.remove(email);
            temposTokens.remove(email);
            return ResponseEntity.status(500).body(Map.of("erro", "Não foi possível enviar o e-mail agora. Tente novamente."));
        }
    }

    @PostMapping("/login/validar-token")
    public ResponseEntity<?> validarToken(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String tokenDigitado = payload.get("token");

        logger.info("[VALIDACAO - INICIO] Validando token para: [{}]", email);

        LocalDateTime horaGerado = temposTokens.get(email);
        if (horaGerado == null || horaGerado.plusMinutes(5).isBefore(LocalDateTime.now())) {
            logger.warn("[VALIDACAO - EXPIRADO] O token para [{}] nao existe ou expirou.", email);
            limparSessao(email);
            return ResponseEntity.status(401).body(Map.of("erro", "O código expirou. Por favor, peça um novo."));
        }

        if (!tokenDigitado.equals(tokensTemporarios.get(email))) {
            logger.warn("[VALIDACAO - INCORRETO] Token digitado [{}] nao confere para [{}]", tokenDigitado, email);
            return ResponseEntity.status(401).body(Map.of("erro", "Código incorreto. Verifique o e-mail."));
        }

        Map<String, Object> dados = dadosPendentes.get(email);
        salvarUsuarioNoBanco(email, (String)dados.get("nome"), (String)dados.get("telefone"), (String)dados.get("ip"));
        
        logger.info("[VALIDACAO - SUCESSO] Login concluído com êxito para [{}]", email);
        limparSessao(email);

        return ResponseEntity.ok().body(Map.of("mensagem", "Login efetuado com sucesso!"));
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