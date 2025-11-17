package com.hospital.tickets;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void init() throws Exception {
        String envUrl = System.getenv("PG_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            dbUrl = envUrl;
        } else {
            String host = System.getenv("PG_HOST") != null ? System.getenv("PG_HOST") : "localhost";
            String port = System.getenv("PG_PORT") != null ? System.getenv("PG_PORT") : "5432";
            String db = System.getenv("PG_DB") != null ? System.getenv("PG_DB") : "hospital";
            dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        }
        dbUser = System.getenv("PG_USER") != null ? System.getenv("PG_USER") : "postgres";
        dbPassword = System.getenv("PG_PASSWORD") != null ? System.getenv("PG_PASSWORD") : "";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PostgreSQL JDBC driver não encontrado no classpath", e);
        }
        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                        "id TEXT PRIMARY KEY, " +
                        "email TEXT UNIQUE NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "role TEXT NOT NULL, " +
                        "sector TEXT, " +
                        "password_hash TEXT NOT NULL, " +
                        "created_at TEXT NOT NULL" +
                        ")");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS tickets (" +
                        "id TEXT PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "description TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "priority TEXT NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "location TEXT, " +
                        "requester_name TEXT, " +
                        "requester_sector TEXT, " +
                        "assigned_to TEXT, " +
                        "user_id TEXT NOT NULL, " +
                        "created_at TEXT NOT NULL, " +
                        "updated_at TEXT NOT NULL" +
                        ")");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao conectar ao PostgreSQL. Defina PG_URL ou PG_HOST, PG_PORT, PG_DB, PG_USER, PG_PASSWORD.", e);
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static long countUsers() throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    public static long countTickets() throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM tickets");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    public static Map<String, Object> findUserByEmail(String email) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, email, name, role, sector, password_hash, created_at FROM users WHERE LOWER(email)=LOWER(?)")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("email", rs.getString("email"));
                map.put("name", rs.getString("name"));
                map.put("role", rs.getString("role"));
                map.put("sector", rs.getString("sector"));
                map.put("password_hash", rs.getString("password_hash"));
                map.put("created_at", rs.getString("created_at"));
                return map;
            }
        }
    }

    public static Map<String, Object> findTicketById(String id) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title, description, category, priority, status, location, requester_name, requester_sector, assigned_to, user_id, created_at, updated_at FROM tickets WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> t = new HashMap<>();
                t.put("id", rs.getString("id"));
                t.put("title", rs.getString("title"));
                t.put("description", rs.getString("description"));
                t.put("category", rs.getString("category"));
                t.put("priority", rs.getString("priority"));
                t.put("status", rs.getString("status"));
                t.put("location", rs.getString("location"));
                t.put("requester_name", rs.getString("requester_name"));
                t.put("requester_sector", rs.getString("requester_sector"));
                t.put("assigned_to", rs.getString("assigned_to"));
                t.put("user_id", rs.getString("user_id"));
                t.put("created_at", rs.getString("created_at"));
                t.put("updated_at", rs.getString("updated_at"));
                return t;
            }
        }
    }

    public static void insertUser(Map<String, Object> user) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (id, email, name, role, sector, password_hash, created_at) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, String.valueOf(user.get("id")));
            ps.setString(2, String.valueOf(user.get("email")));
            ps.setString(3, String.valueOf(user.get("name")));
            ps.setString(4, String.valueOf(user.get("role")));
            ps.setString(5, String.valueOf(user.get("sector")));
            ps.setString(6, String.valueOf(user.get("password_hash")));
            ps.setString(7, String.valueOf(user.get("created_at")));
            ps.executeUpdate();
        }
    }

    public static void insertTicket(Map<String, Object> t) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tickets (id, title, description, category, priority, status, location, requester_name, requester_sector, assigned_to, user_id, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, String.valueOf(t.get("id")));
            ps.setString(2, String.valueOf(t.get("title")));
            ps.setString(3, String.valueOf(t.get("description")));
            ps.setString(4, String.valueOf(t.get("category")));
            ps.setString(5, String.valueOf(t.get("priority")));
            ps.setString(6, String.valueOf(t.get("status")));
            ps.setString(7, (t.get("location") == null) ? null : String.valueOf(t.get("location")));
            ps.setString(8, (t.get("requester_name") == null) ? null : String.valueOf(t.get("requester_name")));
            ps.setString(9, (t.get("requester_sector") == null) ? null : String.valueOf(t.get("requester_sector")));
            ps.setString(10, (t.get("assigned_to") == null || "null".equals(String.valueOf(t.get("assigned_to")))) ? null : String.valueOf(t.get("assigned_to")));
            ps.setString(11, String.valueOf(t.get("user_id")));
            ps.setString(12, String.valueOf(t.get("created_at")));
            ps.setString(13, String.valueOf(t.get("updated_at")));
            ps.executeUpdate();
        }
    }

    public static void updateTicket(Map<String, Object> t) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE tickets SET title=?, description=?, category=?, priority=?, status=?, location=?, requester_name=?, requester_sector=?, assigned_to=?, user_id=?, created_at=?, updated_at=? WHERE id=?")) {
            ps.setString(1, String.valueOf(t.get("title")));
            ps.setString(2, String.valueOf(t.get("description")));
            ps.setString(3, String.valueOf(t.get("category")));
            ps.setString(4, String.valueOf(t.get("priority")));
            ps.setString(5, String.valueOf(t.get("status")));
            ps.setString(6, (t.get("location") == null) ? null : String.valueOf(t.get("location")));
            ps.setString(7, (t.get("requester_name") == null) ? null : String.valueOf(t.get("requester_name")));
            ps.setString(8, (t.get("requester_sector") == null) ? null : String.valueOf(t.get("requester_sector")));
            ps.setString(9, (t.get("assigned_to") == null || "null".equals(String.valueOf(t.get("assigned_to")))) ? null : String.valueOf(t.get("assigned_to")));
            ps.setString(10, String.valueOf(t.get("user_id")));
            ps.setString(11, String.valueOf(t.get("created_at")));
            ps.setString(12, String.valueOf(t.get("updated_at")));
            ps.setString(13, String.valueOf(t.get("id")));
            ps.executeUpdate();
        }
    }

    public static void updateTicketStatus(String id, String status, String updatedAt) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE tickets SET status=?, updated_at=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setString(2, updatedAt);
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public static void updateUserProfile(String id, String name, String sector) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET name=COALESCE(?, name), sector=COALESCE(?, sector) WHERE id=?")) {
            if (name == null || name.isBlank()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, name);
            }
            if (sector == null || sector.isBlank()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, sector);
            }
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public static void deleteTicketById(String id) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM tickets WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public static List<Map<String, Object>> listTickets() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title, description, category, priority, status, location, requester_name, requester_sector, assigned_to, user_id, created_at, updated_at FROM tickets ORDER BY created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> t = new HashMap<>();
                t.put("id", rs.getString("id"));
                t.put("title", rs.getString("title"));
                t.put("description", rs.getString("description"));
                t.put("category", rs.getString("category"));
                t.put("priority", rs.getString("priority"));
                t.put("status", rs.getString("status"));
                t.put("location", rs.getString("location"));
                t.put("requester_name", rs.getString("requester_name"));
                t.put("requester_sector", rs.getString("requester_sector"));
                t.put("assigned_to", rs.getString("assigned_to"));
                t.put("user_id", rs.getString("user_id"));
                t.put("created_at", rs.getString("created_at"));
                t.put("updated_at", rs.getString("updated_at"));
                list.add(t);
            }
        }
        return list;
    }

    public static List<Map<String, Object>> listUsers() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, email, name, role, sector, created_at FROM users ORDER BY created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> u = new HashMap<>();
                u.put("id", rs.getString("id"));
                u.put("email", rs.getString("email"));
                u.put("name", rs.getString("name"));
                u.put("role", rs.getString("role"));
                u.put("sector", rs.getString("sector"));
                u.put("created_at", rs.getString("created_at"));
                list.add(u);
            }
        }
        return list;
    }
}