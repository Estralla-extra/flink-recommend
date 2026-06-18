package com.recommend.web.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Map<String, UserInfo> users = new HashMap<>();

    public AuthController() {
        users.put("admin", new UserInfo("admin", "admin123", "\u7ba1\u7406\u5458"));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> result = new LinkedHashMap<>();

        UserInfo user = users.get(username);
        if (user != null && user.password.equals(password)) {
            result.put("token", UUID.randomUUID().toString());
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", username);
            userMap.put("name", user.displayName);
            userMap.put("username", username);
            result.put("user", userMap);
            return result;
        }

        throw new RuntimeException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> result = new LinkedHashMap<>();

        if (users.containsKey(username)) {
            throw new RuntimeException("\u7528\u6237\u5df2\u5b58\u5728");
        }

        users.put(username, new UserInfo(username, password, username));
        result.put("success", true);
        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader("Authorization") String token) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", "admin");
        result.put("name", "\u7ba1\u7406\u5458");
        result.put("username", "admin");
        return result;
    }

    private static class UserInfo {
        String username;
        String password;
        String displayName;

        UserInfo(String username, String password, String displayName) {
            this.username = username;
            this.password = password;
            this.displayName = displayName;
        }
    }
}
