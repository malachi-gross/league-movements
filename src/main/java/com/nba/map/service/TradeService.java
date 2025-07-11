package com.nba.map.service;

import com.nba.map.model.Trade;
import com.nba.map.util.TradeScraperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TradeService {
    
    private final List<Trade> trades;
    
    @Autowired
    private TradeScraperUtil tradeScraperUtil;
    
    public TradeService() {
        this.trades = Collections.synchronizedList(new ArrayList<>());
        
        // Automatically scrape recent trades on startup (in background)
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds for app to fully start
                System.out.println("🏀 Auto-loading recent NBA trades...");
                scrapeTradesForYear(2025); // Current year
                System.out.println("✅ Finished loading recent trades");
            } catch (Exception e) {
                System.err.println("❌ Error auto-loading trades: " + e.getMessage());
            }
        }).start();
    }
    
    public List<Trade> getAllTrades() {
        return new ArrayList<>(trades);
    }
    
    public List<Trade> getTradesByYear(int year) {
        return trades.stream()
                .filter(trade -> trade.getYear() == year)
                .collect(Collectors.toList());
    }
    
    public List<Trade> getTradesByTeam(String teamName) {
        return trades.stream()
                .filter(trade -> trade.getFromTeam().equals(teamName) || trade.getToTeam().equals(teamName))
                .collect(Collectors.toList());
    }
    
    public List<Integer> getAvailableYears() {
        return trades.stream()
                .map(Trade::getYear)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
    
    /**
     * Scrape trade data from Spotrac for a specific year
     */
    public List<Trade> scrapeTradesForYear(int year) {
        try {
            List<Trade> scrapedTrades = tradeScraperUtil.scrapeTradesForYear(year);
            
            // Add scraped trades to our collection, avoiding duplicates
            for (Trade trade : scrapedTrades) {
                if (!containsTrade(trade)) {
                    trades.add(trade);
                }
            }
            
            return scrapedTrades;
        } catch (Exception e) {
            System.err.println("Error scraping trades for year " + year + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if trade already exists in collection
     */
    private boolean containsTrade(Trade newTrade) {
        return trades.stream().anyMatch(existingTrade -> 
            existingTrade.getFromTeam().equals(newTrade.getFromTeam()) &&
            existingTrade.getToTeam().equals(newTrade.getToTeam()) &&
            existingTrade.getTradeDate().equals(newTrade.getTradeDate()) &&
            existingTrade.getDescription().equals(newTrade.getDescription())
        );
    }
    
    /**
     * Bulk scrape trades for multiple years
     */
    public void scrapeTradesForYears(List<Integer> years) {
        for (Integer year : years) {
            try {
                scrapeTradesForYear(year);
                // Add delay to be respectful to the server
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error scraping year " + year + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Get trades between two specific teams
     */
    public List<Trade> getTradesBetweenTeams(String team1, String team2) {
        return trades.stream()
                .filter(trade -> 
                    (trade.getFromTeam().equals(team1) && trade.getToTeam().equals(team2)) ||
                    (trade.getFromTeam().equals(team2) && trade.getToTeam().equals(team1))
                )
                .sorted(Comparator.comparing(Trade::getTradeDate).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get recent trades (last 30 days)
     */
    public List<Trade> getRecentTrades() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        return trades.stream()
                .filter(trade -> trade.getTradeDate().isAfter(thirtyDaysAgo))
                .sorted(Comparator.comparing(Trade::getTradeDate).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get trade statistics for a year
     */
    public Map<String, Object> getTradeStatisticsForYear(int year) {
        List<Trade> yearTrades = getTradesByYear(year);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTrades", yearTrades.size());
        stats.put("teamsInvolved", yearTrades.stream()
                .flatMap(trade -> Arrays.stream(new String[]{trade.getFromTeam(), trade.getToTeam()}))
                .distinct()
                .count());
        stats.put("playersTraded", yearTrades.stream()
                .mapToInt(trade -> trade.getPlayersTraded().size())
                .sum());
        stats.put("assetsTraded", yearTrades.stream()
                .mapToInt(trade -> trade.getAssetsTraded().size())
                .sum());
        
        return stats;
    }
}