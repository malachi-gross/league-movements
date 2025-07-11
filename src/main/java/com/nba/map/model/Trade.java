package com.nba.map.model;

import java.time.LocalDate;
import java.util.List;

public class Trade {
    private String id;
    private LocalDate tradeDate;
    private String fromTeam;
    private String toTeam;
    private List<String> playersTraded;
    private List<String> assetsTraded; // picks, cash, etc.
    private String description;
    private String sourceUrl;
    private int year;
    
    public Trade() {}
    
    public Trade(String id, LocalDate tradeDate, String fromTeam, String toTeam, 
                 List<String> playersTraded, List<String> assetsTraded, 
                 String description, String sourceUrl) {
        this.id = id;
        this.tradeDate = tradeDate;
        this.fromTeam = fromTeam;
        this.toTeam = toTeam;
        this.playersTraded = playersTraded;
        this.assetsTraded = assetsTraded;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.year = tradeDate != null ? tradeDate.getYear() : 0;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { 
        this.tradeDate = tradeDate; 
        this.year = tradeDate != null ? tradeDate.getYear() : 0;
    }
    
    public String getFromTeam() { return fromTeam; }
    public void setFromTeam(String fromTeam) { this.fromTeam = fromTeam; }
    
    public String getToTeam() { return toTeam; }
    public void setToTeam(String toTeam) { this.toTeam = toTeam; }
    
    public List<String> getPlayersTraded() { return playersTraded; }
    public void setPlayersTraded(List<String> playersTraded) { this.playersTraded = playersTraded; }
    
    public List<String> getAssetsTraded() { return assetsTraded; }
    public void setAssetsTraded(List<String> assetsTraded) { this.assetsTraded = assetsTraded; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}