package com.nba.map.service;

import com.nba.map.model.NbaTeam;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class NbaTeamService {
    
    private final List<NbaTeam> teams;
    
    public NbaTeamService() {
        this.teams = initializeTeams();
    }
    
    public List<NbaTeam> getAllTeams() {
        return new ArrayList<>(teams);
    }
    
    public NbaTeam getTeamByName(String name) {
        return teams.stream()
                .filter(team -> team.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    private List<NbaTeam> initializeTeams() {
        List<NbaTeam> teamList = new ArrayList<>();
        
        // Eastern Conference - Atlantic Division
        teamList.add(new NbaTeam("Boston Celtics", "Boston", "Eastern", "Atlantic", 
                42.3601, -71.0589, "/images/teams/celtics.png", "#007A33", "#BA9653"));
        teamList.add(new NbaTeam("Brooklyn Nets", "Brooklyn", "Eastern", "Atlantic", 
                40.6782, -73.9442, "/images/teams/nets.png", "#000000", "#FFFFFF"));
        teamList.add(new NbaTeam("New York Knicks", "New York", "Eastern", "Atlantic", 
                40.7128, -74.0060, "/images/teams/knicks.png", "#006BB6", "#F58426"));
        teamList.add(new NbaTeam("Philadelphia 76ers", "Philadelphia", "Eastern", "Atlantic", 
                39.9526, -75.1652, "/images/teams/76ers.png", "#006BB6", "#ED174C"));
        teamList.add(new NbaTeam("Toronto Raptors", "Toronto", "Eastern", "Atlantic", 
                43.6532, -79.3832, "/images/teams/raptors.png", "#CE1141", "#000000"));
        
        // Eastern Conference - Central Division
        teamList.add(new NbaTeam("Chicago Bulls", "Chicago", "Eastern", "Central", 
                41.8781, -87.6298, "/images/teams/bulls.png", "#CE1141", "#000000"));
        teamList.add(new NbaTeam("Cleveland Cavaliers", "Cleveland", "Eastern", "Central", 
                41.4993, -81.6944, "/images/teams/cavaliers.png", "#860038", "#FDBB30"));
        teamList.add(new NbaTeam("Detroit Pistons", "Detroit", "Eastern", "Central", 
                42.3314, -83.0458, "/images/teams/pistons.png", "#C8102E", "#1D42BA"));
        teamList.add(new NbaTeam("Indiana Pacers", "Indianapolis", "Eastern", "Central", 
                39.7684, -86.1581, "/images/teams/pacers.png", "#002D62", "#FDBB30"));
        teamList.add(new NbaTeam("Milwaukee Bucks", "Milwaukee", "Eastern", "Central", 
                43.0389, -87.9065, "/images/teams/bucks.png", "#00471B", "#EEE1C6"));
        
        // Eastern Conference - Southeast Division
        teamList.add(new NbaTeam("Atlanta Hawks", "Atlanta", "Eastern", "Southeast", 
                33.7490, -84.3880, "/images/teams/hawks.png", "#E03A3E", "#C1D32F"));
        teamList.add(new NbaTeam("Charlotte Hornets", "Charlotte", "Eastern", "Southeast", 
                35.2271, -80.8431, "/images/teams/hornets.png", "#1D1160", "#00788C"));
        teamList.add(new NbaTeam("Miami Heat", "Miami", "Eastern", "Southeast", 
                25.7617, -80.1918, "/images/teams/heat.png", "#98002E", "#F9A01B"));
        teamList.add(new NbaTeam("Orlando Magic", "Orlando", "Eastern", "Southeast", 
                28.5383, -81.3792, "/images/teams/magic.png", "#0077C0", "#C4CED4"));
        teamList.add(new NbaTeam("Washington Wizards", "Washington", "Eastern", "Southeast", 
                38.9072, -77.0369, "/images/teams/wizards.png", "#002B5C", "#E31837"));
        
        // Western Conference - Northwest Division
        teamList.add(new NbaTeam("Denver Nuggets", "Denver", "Western", "Northwest", 
                39.7392, -104.9903, "/images/teams/nuggets.png", "#0E2240", "#FEC524"));
        teamList.add(new NbaTeam("Minnesota Timberwolves", "Minneapolis", "Western", "Northwest", 
                44.9778, -93.2650, "/images/teams/timberwolves.png", "#0C2340", "#236192"));
        teamList.add(new NbaTeam("Oklahoma City Thunder", "Oklahoma City", "Western", "Northwest", 
                35.4676, -97.5164, "/images/teams/thunder.png", "#007AC1", "#EF3B24"));
        teamList.add(new NbaTeam("Portland Trail Blazers", "Portland", "Western", "Northwest", 
                45.5152, -122.6784, "/images/teams/blazers.png", "#E03A3E", "#000000"));
        teamList.add(new NbaTeam("Utah Jazz", "Salt Lake City", "Western", "Northwest", 
                40.7608, -111.8910, "/images/teams/jazz.png", "#002B5C", "#00471B"));
        
        // Western Conference - Pacific Division
        teamList.add(new NbaTeam("Golden State Warriors", "San Francisco", "Western", "Pacific", 
                37.7749, -122.4194, "/images/teams/warriors.png", "#1D428A", "#FFC72C"));
        teamList.add(new NbaTeam("LA Clippers", "Los Angeles", "Western", "Pacific", 
                34.0522, -118.2437, "/images/teams/clippers.png", "#C8102E", "#1D428A"));
        teamList.add(new NbaTeam("Los Angeles Lakers", "Los Angeles", "Western", "Pacific", 
                34.0522, -118.2437, "/images/teams/lakers.png", "#552583", "#FDB927"));
        teamList.add(new NbaTeam("Phoenix Suns", "Phoenix", "Western", "Pacific", 
                33.4484, -112.0740, "/images/teams/suns.png", "#1D1160", "#E56020"));
        teamList.add(new NbaTeam("Sacramento Kings", "Sacramento", "Western", "Pacific", 
                38.5816, -121.4944, "/images/teams/kings.png", "#5A2D81", "#63727A"));
        
        // Western Conference - Southwest Division
        teamList.add(new NbaTeam("Dallas Mavericks", "Dallas", "Western", "Southwest", 
                32.7767, -96.7970, "/images/teams/mavericks.png", "#00538C", "#002F65"));
        teamList.add(new NbaTeam("Houston Rockets", "Houston", "Western", "Southwest", 
                29.7604, -95.3698, "/images/teams/rockets.png", "#CE1141", "#000000"));
        teamList.add(new NbaTeam("Memphis Grizzlies", "Memphis", "Western", "Southwest", 
                35.1495, -90.0490, "/images/teams/grizzlies.png", "#5D76A9", "#12173F"));
        teamList.add(new NbaTeam("New Orleans Pelicans", "New Orleans", "Western", "Southwest", 
                29.9511, -90.0715, "/images/teams/pelicans.png", "#0C2340", "#C8102E"));
        teamList.add(new NbaTeam("San Antonio Spurs", "San Antonio", "Western", "Southwest", 
                29.4241, -98.4936, "/images/teams/spurs.png", "#C4CED4", "#000000"));
        
        return teamList;
    }
}