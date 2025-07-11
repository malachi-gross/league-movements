package com.nba.map.controller;

import com.nba.map.model.NbaTeam;
import com.nba.map.service.NbaTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class NbaTeamController {
    
    @Autowired
    private NbaTeamService teamService;
    
    @GetMapping
    public List<NbaTeam> getAllTeams() {
        return teamService.getAllTeams();
    }
    
    @GetMapping("/{name}")
    public NbaTeam getTeam(@PathVariable String name) {
        return teamService.getTeamByName(name);
    }
}