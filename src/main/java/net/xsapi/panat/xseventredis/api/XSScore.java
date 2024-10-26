package net.xsapi.panat.xseventredis.api;

import org.bukkit.entity.Player;

public class XSScore {
    private double score = 0;
    private String playerUUID;

    public XSScore(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }
}