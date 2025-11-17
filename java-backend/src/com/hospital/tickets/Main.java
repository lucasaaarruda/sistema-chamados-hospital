package com.hospital.tickets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class Main {
    
    private static final String JWT_SECRET = Optional.ofNullable(System.getenv("JAVA_BACKEND_JWT_SECRET")).orElse("LOCAL_DEV_SECRET");
    private static final String CORS_ORIGIN = Optional.ofNullable(System.getenv("CORS_ORIGIN")).orElse("http://localhost:5173");

    public static void main(String[] args) throws Exception {
        Database.init();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/auth/signup", Main::handleSignup);
        server.createContext("/auth/login", Main::handleLogin);
        server.createContext("/auth/me", Main::handleMe);

        server.createContext("/tickets", Main::handleTickets);
        server.createContext("/ticket", Main::handleTicketById);
        server.createContext("/users", Main::handleUsers);

        server.setExecutor(null);
        System.out.println("Java backend iniciado em http://localhost:8080");
        server.start();
    }


    
    

    private static void setCors(HttpExchange exchange) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String allowOrigin = CORS_ORIGIN;
        if (requestOrigin != null && (
                requestOrigin.startsWith("http://localhost:5173") ||
                requestOrigin.startsWith("http://localhost:5174")
        )) {
            allowOrigin = requestOrigin;
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowOrigin);
        exchange.getResponseHeaders().set("Vary", "Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        setCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void respondText(HttpExchange exchange, int status, String body) throws IOException {
        setCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    
    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String s = json.trim();
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
        }
        
        int i = 0;
        StringBuilder token = new StringBuilder();
        boolean inString = false;
        List<String> pairs = new ArrayList<>();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') inString = !inString;
            if (c == ',' && !inString) {
                pairs.add(token.toString());
                token.setLength(0);
            } else {
                token.append(c);
            }
            i++;
        }
        if (token.length() > 0) pairs.add(token.toString());
        for (String p : pairs) {
            String[] kv = p.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String val = kv[1].trim();
                key = stripQuotes(key);
                val = stripQuotes(val);
                map.put(key, val);
            }
        }
        return map;
    }

    private static String stripQuotes(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String toJson(Object o) {
        if (o instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Object k : m.keySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(k).append("\":").append(valueToJson(m.get(k)));
            }
            sb.append("}");
            return sb.toString();
        } else if (o instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Object v : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(valueToJson(v));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return valueToJson(o);
        }
    }

    private static String valueToJson(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        } else if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        } else if (v instanceof Map || v instanceof List) {
            return toJson(v);
        } else {
            return "\"" + String.valueOf(v) + "\"";
        }
    }

    



    
    private static void handleSignup(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 204, ""); return; }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}"); return; }
        Map<String, String> body = parseJsonObject(readBody(exchange));
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");
        String name = body.getOrDefault("name", "").trim();
        String role = body.getOrDefault("role", "").trim().toLowerCase();
        String sector = body.getOrDefault("sector", "").trim();
        if (email.isEmpty() || password.isEmpty()) { respond(exchange, 400, "{\"error\":\"Email e senha são obrigatórios\"}"); return; }
        if (name.isEmpty()) { respond(exchange, 400, "{\"error\":\"Nome é obrigatório\"}"); return; }
        if (!("usuario".equals(role) || "tecnico".equals(role))) { respond(exchange, 400, "{\"error\":\"Papel inválido: use 'usuario' ou 'tecnico'\"}"); return; }

        try {
            Map<String, Object> existing = Database.findUserByEmail(email);
            if (existing != null) { respond(exchange, 409, "{\"error\":\"Usuário já existe\"}"); return; }
            String id = UUID.randomUUID().toString();
            String createdAt = Instant.now().toString();
            String hash = sha256(password);
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("id", id);
            newUser.put("email", email);
            newUser.put("name", name);
            newUser.put("role", role);
            newUser.put("sector", sector);
            newUser.put("password_hash", hash);
            newUser.put("created_at", createdAt);
            Database.insertUser(newUser);
            respond(exchange, 201, toJson(Map.of("id", id, "email", email, "name", name, "role", role, "sector", sector)));
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Falha ao criar usuário\"}");
        }
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 204, ""); return; }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}"); return; }
        Map<String, String> body = parseJsonObject(readBody(exchange));
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");
        String requestedRole = body.getOrDefault("role", "").trim().toLowerCase();
        if (email.isEmpty() || password.isEmpty()) { respond(exchange, 400, "{\"error\":\"Email e senha são obrigatórios\"}"); return; }
        try {
            Map<String, Object> u = Database.findUserByEmail(email);
            if (u == null) {
                respondText(exchange, 401, "Usuário não encontrado. Cadastre-se para acessar o sistema");
                return;
            }
            if (!sha256(password).equals(u.get("password_hash"))) {
                respondText(exchange, 401, "Login inválido");
                return;
            }

            String name = String.valueOf(u.getOrDefault("name", ""));
            String role = String.valueOf(u.getOrDefault("role", "usuario"));
            String sector = String.valueOf(u.getOrDefault("sector", ""));
            if (!requestedRole.isBlank() && !role.equals(requestedRole)) {
                String msg = "usuario".equals(role)
                        ? "Login cadastrado como usuário"
                        : "Login cadastrado como técnico";
                respondText(exchange, 401, msg);
                return;
            }
            String token = createToken(String.valueOf(u.get("id")), email, name, role, sector);
            respond(exchange, 200, toJson(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", u.get("id"),
                            "email", email,
                            "name", name,
                            "role", role,
                            "sector", sector
                    )
            )));
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Falha ao autenticar\"}");
        }
    }

    private static void handleMe(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 204, ""); return; }
        Optional<Map<String, String>> auth = authenticate(exchange);
        if (auth.isEmpty()) { respond(exchange, 401, "{\"error\":\"Não autenticado\"}"); return; }
        Map<String, String> payload = auth.get();

        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            respond(exchange, 200, toJson(Map.of(
                    "id", payload.get("sub"),
                    "email", payload.get("email"),
                    "name", payload.getOrDefault("name", ""),
                    "role", payload.getOrDefault("role", "usuario"),
                    "sector", payload.getOrDefault("sector", "")
            )));
            return;
        } else if ("PUT".equalsIgnoreCase(method)) {
            try {
                Map<String, String> body = parseJsonObject(readBody(exchange));
                String newName = body.getOrDefault("name", null);
                String newSector = body.getOrDefault("sector", null);
                String uid = payload.get("sub");
                String role = payload.getOrDefault("role", "usuario");
                if ("tecnico".equals(role)) {
                    newSector = null;
                }
                Database.updateUserProfile(uid, newName, newSector);

                Map<String, Object> u = Database.findUserByEmail(payload.get("email"));
                if (u == null) { respond(exchange, 404, "{\"error\":\"Usuário não encontrado\"}"); return; }
                String email = String.valueOf(u.get("email"));
                String sector = String.valueOf(u.getOrDefault("sector", ""));
                String name = String.valueOf(u.getOrDefault("name", ""));
                String token = createToken(uid, email, name, role, sector);
                respond(exchange, 200, toJson(Map.of(
                        "token", token,
                        "user", Map.of(
                                "id", uid,
                                "email", email,
                                "name", name,
                                "role", role,
                                "sector", sector
                        )
                )));
                return;
            } catch (Exception e) {
                respond(exchange, 500, "{\"error\":\"Falha ao atualizar perfil\"}");
                return;
            }
        } else {
            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
    }

    
    private static void handleTickets(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 204, ""); return; }
        Optional<Map<String, String>> auth = authenticate(exchange);
        if (auth.isEmpty()) { respond(exchange, 401, "{\"error\":\"Não autenticado\"}"); return; }
        Map<String, String> user = auth.get();

        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            try {
                List<Map<String, Object>> tickets = Database.listTickets();
                String role = user.getOrDefault("role", "usuario");
                List<Map<String, Object>> visible;
                if ("tecnico".equals(role)) {
                    visible = tickets;
                } else {
                    String uid = user.get("sub");
                    visible = new ArrayList<>();
                    for (Map<String, Object> t : tickets) {
                        if (Objects.equals(String.valueOf(t.get("user_id")), uid)) {
                            visible.add(t);
                        }
                    }
                }
                respond(exchange, 200, toJson(visible));
            } catch (Exception e) {
                respond(exchange, 500, "{\"error\":\"Falha ao listar tickets\"}");
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            Map<String, String> body = parseJsonObject(readBody(exchange));
            String id = UUID.randomUUID().toString();
            String now = Instant.now().toString();
            Map<String, Object> ticket = new HashMap<>();
            ticket.put("id", id);
            ticket.put("title", body.getOrDefault("title", ""));
            ticket.put("description", body.getOrDefault("description", ""));
            ticket.put("category", body.getOrDefault("category", "Geral"));
            ticket.put("priority", body.getOrDefault("priority", "Média"));
            String status = body.getOrDefault("status", "Aberto");
            ticket.put("status", status);
            ticket.put("location", body.getOrDefault("location", ""));
            ticket.put("requester_name", body.getOrDefault("requester_name", ""));
            ticket.put("requester_sector", body.getOrDefault("requester_sector", ""));
            String assigned = body.getOrDefault("responsible_name", null);
            ticket.put("assigned_to", assigned == null || assigned.isBlank() ? null : assigned);
            ticket.put("user_id", user.get("sub"));
            ticket.put("created_at", now);
            ticket.put("updated_at", now);
            try {
                Database.insertTicket(ticket);
                respond(exchange, 201, toJson(ticket));
            } catch (Exception e) {
                respond(exchange, 500, "{\"error\":\"Falha ao criar ticket\"}");
            }
        } else {
            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
        }
    }

    private static void handleTicketById(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 204, ""); return; }
        Optional<Map<String, String>> auth = authenticate(exchange);
        if (auth.isEmpty()) { respond(exchange, 401, "{\"error\":\"Não autenticado\"}"); return; }
        Map<String, String> user = auth.get();

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) { respond(exchange, 404, "{\"error\":\"Not Found\"}"); return; }
        String id = parts[2];
        String method = exchange.getRequestMethod();
        try {
            Map<String, Object> found = Database.findTicketById(id);
            if (found == null) { respond(exchange, 404, "{\"error\":\"Ticket não encontrado\"}"); return; }

            if ("DELETE".equalsIgnoreCase(method)) {
                
                if (!"tecnico".equals(user.getOrDefault("role", "usuario"))) {
                    respond(exchange, 403, "{\"error\":\"Apenas técnicos podem deletar tickets\"}");
                    return;
                }
                Database.deleteTicketById(id);
                respond(exchange, 204, "");
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                Map<String, String> body = parseJsonObject(readBody(exchange));
                for (Map.Entry<String, String> e : body.entrySet()) {
                    String k = e.getKey();
                    String v = e.getValue();
                    if (k.equals("responsible_name")) {
                        found.put("assigned_to", v);
                    } else {
                        found.put(k, v);
                    }
                }
                found.put("updated_at", Instant.now().toString());
                Database.updateTicket(found);
                respond(exchange, 200, toJson(found));
                return;
            }

            if (parts.length >= 4 && "status".equalsIgnoreCase(parts[3]) && "PATCH".equalsIgnoreCase(method)) {
                Map<String, String> body = parseJsonObject(readBody(exchange));
                String status = body.getOrDefault("status", null);
                if (status == null || status.isBlank()) { respond(exchange, 400, "{\"error\":\"Status inválido\"}"); return; }
                
                if (!"tecnico".equals(user.getOrDefault("role", "usuario"))) {
                    respond(exchange, 403, "{\"error\":\"Apenas técnicos podem alterar status\"}");
                    return;
                }
                found.put("status", status);
                found.put("updated_at", Instant.now().toString());
                Database.updateTicketStatus(id, status, String.valueOf(found.get("updated_at")));
                respond(exchange, 200, toJson(found));
                return;
            }

            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Falha ao manipular ticket\"}");
        }
    }

    private static void handleUsers(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 204, ""); return; }
        Optional<Map<String, String>> auth = authenticate(exchange);
        if (auth.isEmpty()) { respond(exchange, 401, "{\"error\":\"Não autenticado\"}"); return; }
        Map<String, String> user = auth.get();
        if (!"tecnico".equals(user.getOrDefault("role", "usuario"))) {
            respond(exchange, 403, "{\"error\":\"Apenas técnicos podem listar usuários\"}");
            return;
        }
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method)) { respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}"); return; }
        try {
            List<Map<String, Object>> users = Database.listUsers();
            respond(exchange, 200, toJson(users));
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Falha ao listar usuários\"}");
        }
    }


    
    private static Optional<Map<String, String>> authenticate(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return Optional.empty();
        String token = auth.substring("Bearer ".length());
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();
            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];
            String expected = hmacSha256(header + "." + payload, JWT_SECRET);
            if (!Objects.equals(signature, expected)) return Optional.empty();
            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            Map<String, String> map = parseJsonObject(json);
            return Optional.of(map);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String createToken(String userId, String email, String name, String role, String sector) throws Exception {
        String headerJson = toJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payloadJson = toJson(Map.of(
                "sub", userId,
                "email", email,
                "name", name,
                "role", role,
                "sector", sector,
                "iat", String.valueOf(Instant.now().getEpochSecond())
        ));
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha256(header + "." + payload, JWT_SECRET);
        return header + "." + payload + "." + signature;
    }

    private static String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }
}