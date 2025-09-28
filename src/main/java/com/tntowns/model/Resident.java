package com.tntowns.model;

import java.util.UUID;

public class Resident {

    private UUID uuid;
    private String name;
    private ResidentRole role;
    private String townId; // nullable
    private ChatChannel chatChannel = ChatChannel.GLOBAL;
    private boolean jailed;

    public Resident() {
    }

    public Resident(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.role = ResidentRole.MEMBER;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResidentRole getRole() {
        return role;
    }

    public void setRole(ResidentRole role) {
        this.role = role;
    }

    public String getTownId() {
        return townId;
    }

    public void setTownId(String townId) {
        this.townId = townId;
    }

    public ChatChannel getChatChannel() {
        return chatChannel;
    }

    public void setChatChannel(ChatChannel chatChannel) {
        this.chatChannel = chatChannel;
    }

    public boolean isJailed() {
        return jailed;
    }

    public void setJailed(boolean jailed) {
        this.jailed = jailed;
    }
}


