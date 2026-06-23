 package com.recommend.web.dao;
 
 import com.recommend.web.model.User;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.jdbc.core.JdbcTemplate;
 import org.springframework.stereotype.Repository;
 
 @Repository
 public class UserDao {
 
     @Autowired
     private JdbcTemplate jdbcTemplate;
 
     public User findByUsername(String username) {
         try {
             return jdbcTemplate.queryForObject(
                 "SELECT id, username, password, display_name, avatar, email, created_at, updated_at FROM users WHERE username = ?",
                 (rs, rowNum) -> {
                     User u = new User();
                     u.setId(rs.getInt("id"));
                     u.setUsername(rs.getString("username"));
                     u.setPassword(rs.getString("password"));
                     u.setDisplayName(rs.getString("display_name"));
                     u.setAvatar(rs.getString("avatar"));
                     u.setEmail(rs.getString("email"));
                     u.setCreatedAt(rs.getString("created_at"));
                     u.setUpdatedAt(rs.getString("updated_at"));
                     return u;
                 }, username);
         } catch (Exception e) {
             return null;
         }
     }
 
     public User findById(int id) {
         try {
             return jdbcTemplate.queryForObject(
                 "SELECT id, username, password, display_name, avatar, email, created_at, updated_at FROM users WHERE id = ?",
                 (rs, rowNum) -> {
                     User u = new User();
                     u.setId(rs.getInt("id"));
                     u.setUsername(rs.getString("username"));
                     u.setPassword(rs.getString("password"));
                     u.setDisplayName(rs.getString("display_name"));
                     u.setAvatar(rs.getString("avatar"));
                     u.setEmail(rs.getString("email"));
                     u.setCreatedAt(rs.getString("created_at"));
                     u.setUpdatedAt(rs.getString("updated_at"));
                     return u;
                 }, id);
         } catch (Exception e) {
             return null;
         }
     }
 
     public void insert(String username, String password, String displayName) {
         jdbcTemplate.update(
             "INSERT INTO users (username, password, display_name) VALUES (?, ?, ?)",
             username, password, displayName);
     }
 }
