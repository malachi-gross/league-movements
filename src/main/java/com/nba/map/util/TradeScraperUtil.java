package com.nba.map.util;

import com.nba.map.model.Trade;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TradeScraperUtil {
    
    private static final String SPOTRAC_BASE_URL = "https://www.spotrac.com/nba/transactions/trade";
    private static final Pattern PLAYER_PATTERN = Pattern.compile("\\b[A-Z][a-z]+ [A-Z][a-z]+\\b");
    private static final Pattern PICK_PATTERN = Pattern.compile("(\\d{4})\\s+(1st|2nd)\\s+round\\s+pick", Pattern.CASE_INSENSITIVE);
    private static final Pattern CASH_PATTERN = Pattern.compile("\\bcash\\b", Pattern.CASE_INSENSITIVE);
    
    // Enhanced team name mapping
    private static final Map<String, String> TEAM_ALIASES = Map.ofEntries(
        // Full names
        Map.entry("Boston Celtics", "Boston Celtics"),
        Map.entry("Brooklyn Nets", "Brooklyn Nets"),
        Map.entry("New York Knicks", "New York Knicks"),
        Map.entry("Philadelphia 76ers", "Philadelphia 76ers"),
        Map.entry("Toronto Raptors", "Toronto Raptors"),
        
        // Short names
        Map.entry("Boston", "Boston Celtics"),
        Map.entry("Brooklyn", "Brooklyn Nets"),
        Map.entry("New York", "New York Knicks"),
        Map.entry("Philadelphia", "Philadelphia 76ers"),
        Map.entry("Toronto", "Toronto Raptors"),
        Map.entry("Chicago", "Chicago Bulls"),
        Map.entry("Cleveland", "Cleveland Cavaliers"),
        Map.entry("Detroit", "Detroit Pistons"),
        Map.entry("Indiana", "Indiana Pacers"),
        Map.entry("Milwaukee", "Milwaukee Bucks"),
        Map.entry("Atlanta", "Atlanta Hawks"),
        Map.entry("Charlotte", "Charlotte Hornets"),
        Map.entry("Miami", "Miami Heat"),
        Map.entry("Orlando", "Orlando Magic"),
        Map.entry("Washington", "Washington Wizards"),
        Map.entry("Denver", "Denver Nuggets"),
        Map.entry("Minnesota", "Minnesota Timberwolves"),
        Map.entry("Oklahoma City", "Oklahoma City Thunder"),
        Map.entry("Portland", "Portland Trail Blazers"),
        Map.entry("Utah", "Utah Jazz"),
        Map.entry("Golden State", "Golden State Warriors"),
        Map.entry("LA Clippers", "LA Clippers"),
        Map.entry("Los Angeles", "Los Angeles Lakers"),
        Map.entry("Phoenix", "Phoenix Suns"),
        Map.entry("Sacramento", "Sacramento Kings"),
        Map.entry("Dallas", "Dallas Mavericks"),
        Map.entry("Houston", "Houston Rockets"),
        Map.entry("Memphis", "Memphis Grizzlies"),
        Map.entry("New Orleans", "New Orleans Pelicans"),
        Map.entry("San Antonio", "San Antonio Spurs"),
        
        // Additional aliases
        Map.entry("LAL", "Los Angeles Lakers"),
        Map.entry("LAC", "LA Clippers"),
        Map.entry("GSW", "Golden State Warriors"),
        Map.entry("NYK", "New York Knicks"),
        Map.entry("BKN", "Brooklyn Nets"),
        Map.entry("PHI", "Philadelphia 76ers"),
        Map.entry("TOR", "Toronto Raptors"),
        Map.entry("CHI", "Chicago Bulls"),
        Map.entry("CLE", "Cleveland Cavaliers"),
        Map.entry("DET", "Detroit Pistons"),
        Map.entry("IND", "Indiana Pacers"),
        Map.entry("MIL", "Milwaukee Bucks"),
        Map.entry("ATL", "Atlanta Hawks"),
        Map.entry("CHA", "Charlotte Hornets"),
        Map.entry("MIA", "Miami Heat"),
        Map.entry("ORL", "Orlando Magic"),
        Map.entry("WAS", "Washington Wizards"),
        Map.entry("DEN", "Denver Nuggets"),
        Map.entry("MIN", "Minnesota Timberwolves"),
        Map.entry("OKC", "Oklahoma City Thunder"),
        Map.entry("POR", "Portland Trail Blazers"),
        Map.entry("UTA", "Utah Jazz"),
        Map.entry("PHX", "Phoenix Suns"),
        Map.entry("SAC", "Sacramento Kings"),
        Map.entry("DAL", "Dallas Mavericks"),
        Map.entry("HOU", "Houston Rockets"),
        Map.entry("MEM", "Memphis Grizzlies"),
        Map.entry("NOP", "New Orleans Pelicans"),
        Map.entry("SAS", "San Antonio Spurs"),
        
        // Historical team names
        Map.entry("Seattle SuperSonics", "Oklahoma City Thunder"),
        Map.entry("Seattle", "Oklahoma City Thunder"),
        Map.entry("New Jersey Nets", "Brooklyn Nets"),
        Map.entry("New Jersey", "Brooklyn Nets"),
        Map.entry("Charlotte Bobcats", "Charlotte Hornets"),
        Map.entry("Vancouver Grizzlies", "Memphis Grizzlies"),
        Map.entry("Vancouver", "Memphis Grizzlies"),
        Map.entry("Washington Bullets", "Washington Wizards"),
        Map.entry("New Orleans Hornets", "New Orleans Pelicans"),
        Map.entry("New Orleans/Oklahoma City Hornets", "New Orleans Pelicans")
    );
    
    /**
     * Scrape trades from Spotrac for a specific year
     */
    public List<Trade> scrapeTradesForYear(int year) {
        String startDate = year + "-01-01";
        String endDate = year + "-12-31";
        return scrapeTradesForDateRange(startDate, endDate);
    }
    
    /**
     * Scrape trades from Spotrac for a specific date range
     */
    public List<Trade> scrapeTradesForDateRange(String startDate, String endDate) {
        List<Trade> trades = new ArrayList<>();
        
        try {
            String url = SPOTRAC_BASE_URL + "/start/" + startDate + "/end/" + endDate;
            System.out.println("🌐 Scraping trades from: " + url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(30000) // Increased timeout for large date ranges
                    .get();
            
            System.out.println("📄 Page loaded successfully, parsing content...");
            
            // Look for trade entries in various possible structures
            Elements tradeElements = doc.select("tr, .trade-row, .transaction-row");
            System.out.println("🔍 Found " + tradeElements.size() + " potential trade elements");
            
            LocalDate currentDate = null; // Track the current date
            int tradesFound = 0;
            
            for (int i = 0; i < tradeElements.size(); i++) {
                Element element = tradeElements.get(i);
                String elementText = element.text().trim();
                
                // Check if this element contains just a date
                if (isDateElement(elementText)) {
                    currentDate = extractDateFromDateElement(elementText);
                    continue; // Skip to next element
                }
                
                // Check if this element contains trade information
                if (containsTradeIndicators(elementText)) {
                    try {
                        Trade trade = parseTradeElementWithDate(element, currentDate);
                        if (trade != null && isValidTrade(trade)) {
                            trades.add(trade);
                            tradesFound++;
                            
                            // Log progress for large scraping operations
                            if (tradesFound % 50 == 0) {
                                System.out.println("📊 Progress: Found " + tradesFound + " trades so far...");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error parsing trade element: " + e.getMessage());
                    }
                }
            }
            
            // If no trades found with the above selectors, try alternative parsing
            if (trades.isEmpty()) {
                System.out.println("⚠️ No trades found with primary selectors, trying alternative parsing...");
                trades.addAll(parseAlternativeFormat(doc));
            }
            
            System.out.println("🎯 Successfully scraped " + trades.size() + " trades for date range " + startDate + " to " + endDate);
            
        } catch (Exception e) {
            System.err.println("❌ Error scraping trades for date range " + startDate + " to " + endDate + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return trades;
    }
    
    /**
     * Check if an element contains only a date (no trade information)
     */
    private boolean isDateElement(String text) {
        // Check if the text is just a date pattern and nothing else (or very little else)
        Pattern dateOnlyPattern = Pattern.compile("^\\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),?\\s+(\\d{4})\\s*$");
        return dateOnlyPattern.matcher(text).matches();
    }
    
    /**
     * Extract date from a date-only element
     */
    private LocalDate extractDateFromDateElement(String text) {
        try {
            Pattern datePattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),?\\s+(\\d{4})");
            Matcher matcher = datePattern.matcher(text);
            
            if (matcher.find()) {
                String dateStr = matcher.group(1) + " " + matcher.group(2) + ", " + matcher.group(3);
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MMM d, yyyy"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing date from date element: " + e.getMessage());
        }
        
        return LocalDate.now(); // Fallback to current date
    }
    
    /**
     * Parse a trade element using a previously found date
     */
    private Trade parseTradeElementWithDate(Element element, LocalDate tradeDate) {
        try {
            String elementText = element.text();
            
            // Use the provided date, or fall back to extracting from text
            LocalDate finalDate = tradeDate != null ? tradeDate : extractDateFromText(elementText);
            
            // Extract teams
            List<String> teams = extractTeams(element);
            if (teams.size() < 2) {
                teams = extractTeamsFromText(elementText);
            }
            
            if (teams.size() < 2) {
                return null;
            }
            
            String fromTeam = normalizeTeamName(teams.get(0));
            String toTeam = normalizeTeamName(teams.get(1));
            
            // Skip if teams couldn't be normalized (unknown teams)
            if (fromTeam.isEmpty() || toTeam.isEmpty()) {
                return null;
            }
            
            // Extract players and assets
            List<String> players = extractPlayers(elementText);
            List<String> assets = extractAssets(elementText);
            
            // Generate trade ID
            String tradeId = generateTradeId(finalDate, fromTeam, toTeam);
            
            Trade trade = new Trade(
                tradeId,
                finalDate,
                fromTeam,
                toTeam,
                players,
                assets,
                elementText.trim(),
                SPOTRAC_BASE_URL
            );
            
            return trade;
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing trade element with date: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Alternative parsing method for different HTML structures
     */
    private List<Trade> parseAlternativeFormat(Document doc) {
        List<Trade> trades = new ArrayList<>();
        
        try {
            // Look for text patterns that indicate trades
            Elements textElements = doc.select("*:contains(acquires), *:contains(trades), *:contains(traded)");
            
            for (Element element : textElements) {
                String text = element.text();
                if (containsTradeIndicators(text) && text.length() > 20) {
                    Trade trade = parseTradeFromText(text);
                    if (trade != null && isValidTrade(trade)) {
                        trades.add(trade);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in alternative parsing: " + e.getMessage());
        }
        
        return trades;
    }
    
    /**
     * Parse trade information from raw text
     */
    private Trade parseTradeFromText(String text) {
        try {
            // Extract date
            LocalDate tradeDate = extractDateFromText(text);
            
            // Extract teams using pattern matching
            List<String> teams = extractTeamsFromText(text);
            if (teams.size() < 2) {
                return null;
            }
            
            String fromTeam = normalizeTeamName(teams.get(0));
            String toTeam = normalizeTeamName(teams.get(1));
            
            if (fromTeam.isEmpty() || toTeam.isEmpty()) {
                return null;
            }
            
            // Extract players and assets
            List<String> players = extractPlayers(text);
            List<String> assets = extractAssets(text);
            
            String tradeId = generateTradeId(tradeDate, fromTeam, toTeam);
            
            return new Trade(
                tradeId,
                tradeDate,
                fromTeam,
                toTeam,
                players,
                assets,
                text.trim(),
                SPOTRAC_BASE_URL
            );
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if text contains trade indicators
     */
    private boolean containsTradeIndicators(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("acquires") || 
               lowerText.contains("trades") || 
               lowerText.contains("traded") ||
               lowerText.contains("receives") ||
               lowerText.contains("sends");
    }
    
    /**
     * Extract date from text using various patterns
     */
    private LocalDate extractDateFromText(String text) {
        try {
            // More flexible date extraction
            Pattern datePattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),?\\s+(\\d{4})");
            Matcher matcher = datePattern.matcher(text);
            
            if (matcher.find()) {
                String dateStr = matcher.group(1) + " " + matcher.group(2) + ", " + matcher.group(3);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
                return LocalDate.parse(dateStr, formatter);
            }
        } catch (Exception e) {
            // Fall through to default
        }
        
        return LocalDate.now();
    }
    
    /**
     * Extract teams from HTML element
     */
    private List<String> extractTeams(Element element) {
        List<String> teams = new ArrayList<>();
        
        // Look for team names in img alt attributes
        Elements images = element.select("img[alt]");
        for (Element img : images) {
            String alt = img.attr("alt");
            if (isTeamName(alt)) {
                teams.add(alt);
            }
        }
        
        // If no teams found in images, try text extraction
        if (teams.isEmpty()) {
            teams = extractTeamsFromText(element.text());
        }
        
        return teams;
    }
    
    /**
     * Extract teams from text using pattern matching
     */
    private List<String> extractTeamsFromText(String text) {
        List<String> teams = new ArrayList<>();
        Set<String> foundTeams = new HashSet<>();
        
        // Check against known team names (prioritize longer names first)
        List<String> sortedTeamNames = new ArrayList<>(TEAM_ALIASES.keySet());
        sortedTeamNames.sort((a, b) -> Integer.compare(b.length(), a.length()));
        
        for (String teamName : sortedTeamNames) {
            if (text.contains(teamName)) {
                String normalizedName = TEAM_ALIASES.get(teamName);
                if (!foundTeams.contains(normalizedName)) {
                    teams.add(normalizedName);
                    foundTeams.add(normalizedName);
                }
                
                // Stop after finding 2 teams
                if (teams.size() >= 2) {
                    break;
                }
            }
        }
        
        return teams;
    }
    
    /**
     * Check if string is a team name
     */
    private boolean isTeamName(String name) {
        return TEAM_ALIASES.containsKey(name) || 
               TEAM_ALIASES.containsValue(name);
    }
    
    /**
     * Extract player names from text
     */
    private List<String> extractPlayers(String text) {
        List<String> players = new ArrayList<>();
        
        // Use regex to find potential player names (two capitalized words)
        Matcher matcher = PLAYER_PATTERN.matcher(text);
        while (matcher.find()) {
            String playerName = matcher.group();
            // Filter out common non-player words
            if (!isCommonNonPlayerPhrase(playerName)) {
                players.add(playerName);
            }
        }
        
        return players;
    }
    
    /**
     * Extract assets (picks, cash, etc.) from text
     */
    private List<String> extractAssets(String text) {
        List<String> assets = new ArrayList<>();
        
        // Extract draft picks
        Matcher pickMatcher = PICK_PATTERN.matcher(text);
        while (pickMatcher.find()) {
            assets.add(pickMatcher.group());
        }
        
        // Extract cash considerations
        Matcher cashMatcher = CASH_PATTERN.matcher(text);
        if (cashMatcher.find()) {
            assets.add("Cash Considerations");
        }
        
        // Look for other common assets
        String lowerText = text.toLowerCase();
        if (lowerText.contains("trade exception")) {
            assets.add("Trade Exception");
        }
        if (lowerText.contains("rights to")) {
            assets.add("Player Rights");
        }
        if (lowerText.contains("future considerations")) {
            assets.add("Future Considerations");
        }
        
        return assets;
    }
    
    /**
     * Check if phrase is commonly confused with player names
     */
    private boolean isCommonNonPlayerPhrase(String phrase) {
        String lowerPhrase = phrase.toLowerCase();
        return lowerPhrase.equals("new york") ||
               lowerPhrase.equals("los angeles") ||
               lowerPhrase.equals("san antonio") ||
               lowerPhrase.equals("golden state") ||
               lowerPhrase.equals("oklahoma city") ||
               lowerPhrase.equals("new orleans") ||
               lowerPhrase.equals("trade exception") ||
               lowerPhrase.equals("round pick") ||
               lowerPhrase.equals("cash considerations") ||
               lowerPhrase.equals("future considerations") ||
               lowerPhrase.equals("player rights");
    }
    
    /**
     * Normalize team name using aliases
     */
    private String normalizeTeamName(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return "";
        }
        
        String normalized = TEAM_ALIASES.get(teamName.trim());
        return normalized != null ? normalized : teamName.trim();
    }
    
    /**
     * Generate unique trade ID
     */
    private String generateTradeId(LocalDate date, String fromTeam, String toTeam) {
        return String.format("trade_%s_%s_%s_%d",
            date.toString(),
            fromTeam.replaceAll("\\s+", "").toLowerCase(),
            toTeam.replaceAll("\\s+", "").toLowerCase(),
            System.currentTimeMillis() % 10000
        );
    }
    
    /**
     * Validate that trade has minimum required information
     */
    private boolean isValidTrade(Trade trade) {
        return trade != null &&
               trade.getFromTeam() != null && !trade.getFromTeam().isEmpty() &&
               trade.getToTeam() != null && !trade.getToTeam().isEmpty() &&
               !trade.getFromTeam().equals(trade.getToTeam()) &&
               trade.getTradeDate() != null;
    }
    
    /**
     * Get available years for trade data
     */
    public List<Integer> getAvailableYears() {
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        
        // Add years from 1989 to current year
        for (int year = currentYear; year >= 1989; year--) {
            years.add(year);
        }
        
        return years;
    }
}