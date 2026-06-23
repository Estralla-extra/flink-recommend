 package com.recommend.web.controller;
 
 import com.recommend.web.dao.UserDao;
 import com.recommend.web.model.User;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;
 import redis.clients.jedis.Jedis;
 import redis.clients.jedis.JedisPool;
 
 import java.security.MessageDigest;
 import java.util.*;
 
 @RestController
 @RequestMapping("/api/auth")
 public class AuthController {
 
     @Autowired
     private JedisPool jedisPool;
 
     @Autowired
     private UserDao userDao;
 
     private static final long TOKEN_TTL = 7 * 24 * 60 * 60; // 7 days in seconds
 
     @PostMapping("/login")
     public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
         String username = body.get("username");
         String password = body.get("password");
         Map<String, Object> result = new LinkedHashMap<>();
 
         if (username == null || password == null) {
             result.put("error", "用户名和密码不能为空");
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
         }
 
         User user = userDao.findByUsername(username);
         if (user == null || !user.getPassword().equals(sha256(password))) {
             result.put("error", "用户名或密码错误");
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
         }
 
         String token = UUID.randomUUID().toString();
         try (Jedis jedis = jedisPool.getResource()) {
             jedis.setex("token:" + token, TOKEN_TTL, String.valueOf(user.getId()));
         }
 
         result.put("token", token);
         Map<String, Object> userMap = new LinkedHashMap<>();
         userMap.put("id", user.getId());
         userMap.put("username", user.getUsername());
         userMap.put("name", user.getDisplayName());
         result.put("user", userMap);
         return ResponseEntity.ok(result);
     }
 
     @PostMapping("/register")
     public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
         String username = body.get("username");
         String password = body.get("password");
         Map<String, Object> result = new LinkedHashMap<>();
 
         if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
             result.put("error", "用户名和密码不能为空");
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
         }
 
         if (userDao.findByUsername(username) != null) {
             result.put("error", "用户已存在");
             return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
         }
 
         userDao.insert(username, sha256(password), username);
         result.put("success", true);
         return ResponseEntity.ok(result);
     }
 
     @GetMapping("/me")
     public ResponseEntity<Map<String, Object>> me(@RequestHeader("Authorization") String token) {
         Map<String, Object> result = new LinkedHashMap<>();
 
         if (token == null || token.isEmpty()) {
             result.put("error", "未登录");
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
         }
 
         // Strip "Bearer " prefix if present (standard Authorization header format)
         String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
 
         try (Jedis jedis = jedisPool.getResource()) {
             String userId = jedis.get("token:" + actualToken);
             if (userId == null) {
                 result.put("error", "登录已过期，请重新登录");
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
             }
 
             User user = userDao.findById(Integer.parseInt(userId));
             if (user == null) {
                 result.put("error", "用户不存在");
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
             }
 
             result.put("id", user.getId());
             result.put("username", user.getUsername());
             result.put("name", user.getDisplayName());
             result.put("avatar", user.getAvatar());
             result.put("email", user.getEmail());
             result.put("createdAt", user.getCreatedAt());
             return ResponseEntity.ok(result);
         }
     }
 
     @PostMapping("/logout")
     public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String token) {
         Map<String, Object> result = new LinkedHashMap<>();
 
         if (token != null && !token.isEmpty()) {
             String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
             try (Jedis jedis = jedisPool.getResource()) {
                 jedis.del("token:" + actualToken);
             }
         }
 
         result.put("success", true);
         return ResponseEntity.ok(result);
     }
 
     private static String sha256(String str) {
         try {
             MessageDigest md = MessageDigest.getInstance("SHA-256");
             byte[] hash = md.digest(str.getBytes("UTF-8"));
             StringBuilder hexString = new StringBuilder();
             for (byte b : hash) {
                 String hex = Integer.toHexString(0xff & b);
                 if (hex.length() == 1) hexString.append('0');
                 hexString.append(hex);
             }
             return hexString.toString();
         } catch (Exception e) {
             throw new RuntimeException("SHA-256 not available", e);
         }
     }
 }
