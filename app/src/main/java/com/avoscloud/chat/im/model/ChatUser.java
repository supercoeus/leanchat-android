package com.avoscloud.chat.im.model;

/**
 * Created by lzw on 15/4/26.
 */
public class ChatUser {
  private String username;
  private String userId;
  private String avatarUrl;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }
}
