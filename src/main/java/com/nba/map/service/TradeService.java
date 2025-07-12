package com.nba.map.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nba.map.model.Trade;
import com.nba.map.util.TradeScraperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class TradeService {
    
    private final List<Trade> trades;
    private final ObjectMapper objectMapper;
    private final String CACHE_FILE_PATH = "data/trades_cache.json";
    private final Map<String, Object> bulkScrapeProgress = new ConcurrentHashMap<>();
    
    @Autowired
    private TradeScraperUtil tradeScraperUtil;
    
    public TradeService() {
        this.trades = Collections.synchronizedList(new ArrayList<>());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Create data directory if it doesn't exist
        createDataDirectory();
        
        // Load cached trades on startup
        loadCachedTrades();
        
        // If no cached trades, start background loading of recent trades
        if (trades.isEmpty()) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait for app to fully start
                    System.out.println("🏀 Auto-loading recent NBA trades...");
                    scrapeTradesForCurrentSeason();
                    System.out.println("✅ Finished loading recent trades");
                } catch (Exception e) {
                    System.err.println("❌ Error auto-loading trades: " + e.getMessage());
                }
            }).start();
        } else {
            System.out.println("📋 Loaded " + trades.size() + " cached trades");
        }
    }
    
    private void createDataDirectory() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    private void loadCachedTrades() {
        try {
            File cacheFile = new File(CACHE_FILE_PATH);
            if (cacheFile.exists()) {
                List<Trade> cachedTrades = objectMapper.readValue(cacheFile, new TypeReference<List<Trade>>() {});
                trades.addAll(cachedTrades);
                System.out.println("📥 Loaded " + cachedTrades.size() + " trades from cache");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading cached trades: " + e.getMessage());
        }
    }
    
    private void saveTradesToCache() {
        try {
            objectMapper.writeValue(new File(CACHE_FILE_PATH), trades);
            System.out.println("💾 Saved " + trades.size() + " trades to cache");
        } catch (IOException e) {
            System.err.println("❌ Error saving trades to cache: " + e.getMessage());
        }
    }
    
    public List<Trade> getAllTrades() {
        return new ArrayList<>(trades);
    }
    
    public List<Trade> getTradesByYear(int year) {
        return trades.stream()
                .filter(trade -> trade.getYear() == year)
                .collect(Collectors.toList());
    }
    
    public List<Trade> getTradesBySeason(String season) {
        String[] parts = season.split("-");
        if (parts.length != 2) {
            return new ArrayList<>();
        }
        
        try {
            int startYear = Integer.parseInt(parts[0]);
            int endYear = Integer.parseInt(parts[1]);
            
            LocalDate seasonStart = LocalDate.of(startYear, 7, 1);
            LocalDate seasonEnd = LocalDate.of(endYear, 6, 30);
            
            return trades.stream()
                    .filter(trade -> {
                        LocalDate tradeDate = trade.getTradeDate();
                        return !tradeDate.isBefore(seasonStart) && !tradeDate.isAfter(seasonEnd);
                    })
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
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
    
    public List<String> getAvailableSeasons() {
        Set<String> seasons = new HashSet<>();
        
        for (Trade trade : trades) {
            LocalDate tradeDate = trade.getTradeDate();
            
            // Determine which season this trade belongs to
            int seasonStartYear;
            if (tradeDate.getMonthValue() >= 7) {
                // Trade is in second half of year (July-December), so it's the start of a season
                seasonStartYear = tradeDate.getYear();
            } else {
                // Trade is in first half of year (January-June), so it's the end of a season
                seasonStartYear = tradeDate.getYear() - 1;
            }
            
            int seasonEndYear = seasonStartYear + 1;
            String season = String.format("%d-%02d", seasonStartYear, seasonEndYear % 100);
            seasons.add(season);
        }
        
        return seasons.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
    
    /**
     * Scrape trades for the current NBA season
     */
    public List<Trade> scrapeTradesForCurrentSeason() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        
        // Determine current season start year
        int seasonStartYear = now.getMonthValue() >= 7 ? currentYear : currentYear - 1;
        
        LocalDate seasonStart = LocalDate.of(seasonStartYear, 7, 1);
        LocalDate seasonEnd = LocalDate.of(seasonStartYear + 1, 6, 30);
        
        return scrapeTradesForDateRange(seasonStart, seasonEnd);
    }
    
    /**
     * Scrape trade data from Spotrac for a specific year
     */
    public List<Trade> scrapeTradesForYear(int year) {
        try {
            List<Trade> scrapedTrades = tradeScraperUtil.scrapeTradesForYear(year);
            addTradesToCollection(scrapedTrades);
            return scrapedTrades;
        } catch (Exception e) {
            System.err.println("Error scraping trades for year " + year + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Scrape all historical trades from 1989 to current date
     */
    public Map<String, Object> scrapeAllHistoricalTrades() {
        String progressId = "historical_" + System.currentTimeMillis();
        
        // Initialize progress tracking
        bulkScrapeProgress.put(progressId, Map.of(
            "status", "starting",
            "message", "Preparing to scrape all historical trades...",
            "progress", 0,
            "total", 1,
            "tradesFound", 0,
            "startTime", System.currentTimeMillis()
        ));
        
        // Start scraping in background thread
        new Thread(() -> {
            try {
                LocalDate startDate = LocalDate.of(1989, 9, 1);
                LocalDate endDate = LocalDate.now();
                
                updateProgress(progressId, "in_progress", "Scraping trades from " + startDate + " to " + endDate, 0, 1, 0);
                
                List<Trade> scrapedTrades = scrapeTradesForDateRange(startDate, endDate);
                
                updateProgress(progressId, "completed", 
                    "Successfully scraped " + scrapedTrades.size() + " historical trades", 
                    1, 1, scrapedTrades.size());
                
                // Save to cache after successful scraping
                saveTradesToCache();
                
            } catch (Exception e) {
                updateProgress(progressId, "error", 
                    "Error during historical scraping: " + e.getMessage(), 
                    0, 1, 0);
            }
        }).start();
        
        return Map.of(
            "success", true,
            "message", "Historical trade scraping initiated",
            "progressId", progressId
        );
    }
    
    /**
     * Scrape trades for a specific date range
     */
    public List<Trade> scrapeTradesForDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            List<Trade> scrapedTrades = tradeScraperUtil.scrapeTradesForDateRange(formattedStartDate, formattedEndDate);
            addTradesToCollection(scrapedTrades);
            
            return scrapedTrades;
        } catch (Exception e) {
            System.err.println("Error scraping trades for date range " + startDate + " to " + endDate + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private void addTradesToCollection(List<Trade> newTrades) {
        int addedCount = 0;
        for (Trade trade : newTrades) {
            if (!containsTrade(trade)) {
                trades.add(trade);
                addedCount++;
            }
        }
        
        if (addedCount > 0) {
            System.out.println("➕ Added " + addedCount + " new trades to collection");
            // Save to cache after adding new trades
            saveTradesToCache();
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
        return calculateTradeStatistics(yearTrades, "year " + year);
    }
    
    /**
     * Get trade statistics for a season
     */
    public Map<String, Object> getTradeStatisticsForSeason(String season) {
        List<Trade> seasonTrades = getTradesBySeason(season);
        return calculateTradeStatistics(seasonTrades, season + " season");
    }
    
    private Map<String, Object> calculateTradeStatistics(List<Trade> tradeList, String period) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTrades", tradeList.size());
        stats.put("period", period);
        stats.put("teamsInvolved", tradeList.stream()
                .flatMap(trade -> Arrays.stream(new String[]{trade.getFromTeam(), trade.getToTeam()}))
                .distinct()
                .count());
        stats.put("playersTraded", tradeList.stream()
                .mapToInt(trade -> trade.getPlayersTraded().size())
                .sum());
        stats.put("assetsTraded", tradeList.stream()
                .mapToInt(trade -> trade.getAssetsTraded().size())
                .sum());
        
        // Most active teams
        Map<String, Long> teamActivity = tradeList.stream()
                .flatMap(trade -> Arrays.stream(new String[]{trade.getFromTeam(), trade.getToTeam()}))
                .collect(Collectors.groupingBy(team -> team, Collectors.counting()));
        
        stats.put("mostActiveTeams", teamActivity.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                )));
        
        return stats;
    }
    
    /**
     * Get progress of bulk scraping operation
     */
    public Map<String, Object> getBulkScrapeProgress(String progressId) {
        return (Map<String, Object>) bulkScrapeProgress.getOrDefault(progressId, 
            Map.of("status", "not_found", "message", "Progress ID not found"));
    }
    
    /**
     * Clear completed progress entries (cleanup)
     */
    public void cleanupProgress() {
        long now = System.currentTimeMillis();
        bulkScrapeProgress.entrySet().removeIf(entry -> {
            Map<String, Object> progress = (Map<String, Object>) entry.getValue();
            String status = (String) progress.get("status");
            long startTime = (Long) progress.getOrDefault("startTime", now);
            
            // Remove completed or errored entries older than 1 hour
            return ("completed".equals(status) || "error".equals(status)) && 
                   (now - startTime) > 3600000;
        });
    }
    
    private void updateProgress(String progressId, String status, String message, int progress, int total, int tradesFound) {
        bulkScrapeProgress.put(progressId, Map.of(
            "status", status,
            "message", message,
            "progress", progress,
            "total", total,
            "tradesFound", tradesFound,
            "startTime", bulkScrapeProgress.containsKey(progressId) ? 
                ((Map<String, Object>) bulkScrapeProgress.get(progressId)).get("startTime") : 
                System.currentTimeMillis()
        ));
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        File cacheFile = new File(CACHE_FILE_PATH);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTradesInMemory", trades.size());
        stats.put("cacheFileExists", cacheFile.exists());
        stats.put("cacheFileSizeBytes", cacheFile.exists() ? cacheFile.length() : 0);
        stats.put("oldestTrade", trades.stream()
            .map(Trade::getTradeDate)
            .min(LocalDate::compareTo)
            .orElse(null));
        stats.put("newestTrade", trades.stream()
            .map(Trade::getTradeDate)
            .max(LocalDate::compareTo)
            .orElse(null));
        stats.put("availableSeasons", getAvailableSeasons().size());
        stats.put("availableYears", getAvailableYears().size());
        
        return stats;
    }
    
    /**
     * Force refresh cache by clearing and reloading
     */
    public Map<String, Object> refreshCache() {
        try {
            trades.clear();
            loadCachedTrades();
            return Map.of(
                "success", true,
                "message", "Cache refreshed successfully",
                "tradesLoaded", trades.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Error refreshing cache: " + e.getMessage(),
                "tradesLoaded", 0
            );
        }
    }
}