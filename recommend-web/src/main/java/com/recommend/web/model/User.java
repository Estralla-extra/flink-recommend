 package com.recommend.web.model;
 
 public class User {
     private int id;
     private String username;
     private String password;
     private String displayName;
     private String avatar;
     private String email;
     private String createdAt;
     private String updatedAt;
 
     public User() {}
 
     public int getId() { return id; }
     public void setId(int id) { this.id = id; }
     public String getUsername() { return username; }
     public void setUsername(String username) { this.username = username; }
     public String getPassword() { return password; }
     public void setPassword(String password) { this.password = password; }
     public String getDisplayName() { return displayName; }
     public void setDisplayName(String displayName) { this.displayName = displayName; }
     public String getAvatar() { return avatar; }
     public void setAvatar(String avatar) { this.avatar = avatar; }
     public String getEmail() { return email; }
     public void setEmail(String email) { this.email = email; }
     public String getCreatedAt() { return createdAt; }
     public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
     public String getUpdatedAt() { return updatedAt; }
     public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
 }
