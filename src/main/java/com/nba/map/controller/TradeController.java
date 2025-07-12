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
    
    @GetMapping("/season/{season}")
    public List<Trade> getTradesBySeason(@PathVariable String season) {
        return tradeService.getTradesBySeason(season);
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
    
    @GetMapping("/seasons")
    public List<String> getAvailableSeasons() {
        return tradeService.getAvailableSeasons();
    }
    
    @GetMapping("/stats/year/{year}")
    public Map<String, Object> getTradeStatisticsForYear(@PathVariable int year) {
        return tradeService.getTradeStatisticsForYear(year);
    }
    
    @GetMapping("/stats/season/{season}")
    public Map<String, Object> getTradeStatisticsForSeason(@PathVariable String season) {
        return tradeService.getTradeStatisticsForSeason(season);
    }
    
    @GetMapping("/cache/stats")
    public Map<String, Object> getCacheStatistics() {
        return tradeService.getCacheStatistics();
    }
    
    @PostMapping("/cache/refresh")
    public Map<String, Object> refreshCache() {
        return tradeService.refreshCache();
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
    
    @PostMapping("/scrape/historical")
    public Map<String, Object> scrapeAllHistoricalTrades() {
        return tradeService.scrapeAllHistoricalTrades();
    }
    
    @PostMapping("/scrape/current-season")
    public Map<String, Object> scrapeCurrentSeason() {
        try {
            List<Trade> scrapedTrades = tradeService.scrapeTradesForCurrentSeason();
            return Map.of(
                "success", true,
                "message", "Successfully scraped " + scrapedTrades.size() + " trades for current season",
                "tradesCount", scrapedTrades.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Error scraping current season trades: " + e.getMessage(),
                "tradesCount", 0
            );
        }
    }
    
    @PostMapping("/scrape/date-range")
    public Map<String, Object> scrapeTradesForDateRange(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        try {
            List<Trade> scrapedTrades = tradeService.scrapeTradesForDateRange(
                java.time.LocalDate.parse(startDate), 
                java.time.LocalDate.parse(endDate)
            );
            return Map.of(
                "success", true,
                "message", "Successfully scraped " + scrapedTrades.size() + " trades for date range " + startDate + " to " + endDate,
                "tradesCount", scrapedTrades.size(),
                "startDate", startDate,
                "endDate", endDate
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Error scraping trades for date range: " + e.getMessage(),
                "tradesCount", 0,
                "startDate", startDate,
                "endDate", endDate
            );
        }
    }
    
    @GetMapping("/scrape/progress/{progressId}")
    public Map<String, Object> getBulkScrapeProgress(@PathVariable String progressId) {
        return tradeService.getBulkScrapeProgress(progressId);
    }
    
    @PostMapping("/scrape/cleanup")
    public Map<String, Object> cleanupProgress() {
        try {
            tradeService.cleanupProgress();
            return Map.of(
                "success", true,
                "message", "Progress cleanup completed"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Error during cleanup: " + e.getMessage()
            );
        }
    }
}