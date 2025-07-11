package com.nba.map.controller;

import com.nba.map.model.Trade;
import com.nba.map.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "*")
public class TradeController {
    
    @Autowired
    private TradeService tradeService;
    
    @GetMapping
    public List<Trade> getAllTrades() {
        return tradeService.getAllTrades();
    }
    
    @GetMapping("/year/{year}")
    public List<Trade> getTradesByYear(@PathVariable int year) {
        return tradeService.getTradesByYear(year);
    }
    
    @GetMapping("/team/{teamName}")
    public List<Trade> getTradesByTeam(@PathVariable String teamName) {
        return tradeService.getTradesByTeam(teamName);
    }
    
    @GetMapping("/between/{team1}/{team2}")
    public List<Trade> getTradesBetweenTeams(@PathVariable String team1, @PathVariable String team2) {
        return tradeService.getTradesBetweenTeams(team1, team2);
    }
    
    @GetMapping("/recent")
    public List<Trade> getRecentTrades() {
        return tradeService.getRecentTrades();
    }
    
    @GetMapping("/years")
    public List<Integer> getAvailableYears() {
        return tradeService.getAvailableYears();
    }
    
    @GetMapping("/stats/{year}")
    public Map<String, Object> getTradeStatistics(@PathVariable int year) {
        return tradeService.getTradeStatisticsForYear(year);
    }
    
    @PostMapping("/scrape/{year}")
    public Map<String, Object> scrapeTradesForYear(@PathVariable int year) {
        try {
            List<Trade> scrapedTrades = tradeService.scrapeTradesForYear(year);
            return Map.of(
                "success", true,
                "message", "Successfully scraped " + scrapedTrades.size() + " trades for year " + year,
                "tradesCount", scrapedTrades.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Error scraping trades for year " + year + ": " + e.getMessage(),
                "tradesCount", 0
            );
        }
    }
    
    @PostMapping("/scrape/bulk")
    public Map<String, Object> scrapeBulkYears(@RequestBody List<Integer> years) {
        try {
            tradeService.scrapeTradesForYears(years);
            return Map.of(
                "success", true,
                "message", "Successfully initiated bulk scraping for years: " + years,
                "years", years
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Error in bulk scraping: " + e.getMessage(),
                "years", years
            );
        }
    }
}