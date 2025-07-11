package com.nba.map.model;

public class NbaTeam {
    private String name;
    private String city;
    private String conference;
    private String division;
    private double latitude;
    private double longitude;
    private String logoPath;
    private String primaryColor;
    private String secondaryColor;
    
    public NbaTeam() {}
    
    public NbaTeam(String name, String city, String conference, String division, 
                   double latitude, double longitude, String logoPath, 
                   String primaryColor, String secondaryColor) {
        this.name = name;
        this.city = city;
        this.conference = conference;
        this.division = division;
        this.latitude = latitude;
        this.longitude = longitude;
        this.logoPath = logoPath;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getConference() { return conference; }
    public void setConference(String conference) { this.conference = conference; }
    
    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
}