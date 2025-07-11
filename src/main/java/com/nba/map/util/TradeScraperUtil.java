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
        Map.entry("SAS", "San Antonio Spurs")
    );
    
    /**
     * Scrape trades from Spotrac for a specific year
     */
    public List<Trade> scrapeTradesForYear(int year) {
        List<Trade> trades = new ArrayList<>();
        
        try {
            String url = year == 0 ? SPOTRAC_BASE_URL : SPOTRAC_BASE_URL + "/" + year + "/start/" + year + "-01-01/end/" + year + "-12-31";
            System.out.println("🌐 Scraping trades from: " + url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .get();
            
            System.out.println("📄 Page loaded successfully, parsing content...");
            
            // Look for trade entries in various possible structures
            Elements tradeElements = doc.select("tr, .trade-row, .transaction-row");
            System.out.println("🔍 Found " + tradeElements.size() + " potential trade elements");
            
            LocalDate currentDate = null; // Track the current date
            
            for (int i = 0; i < tradeElements.size(); i++) {
                Element element = tradeElements.get(i);
                String elementText = element.text().trim();
                
                // Check if this element contains just a date
                if (isDateElement(elementText)) {
                    currentDate = extractDateFromDateElement(elementText, year);
                    System.out.println("📅 Found date element: " + currentDate);
                    continue; // Skip to next element
                }
                
                // Check if this element contains trade information
                if (containsTradeIndicators(elementText)) {
                    System.out.println("🔍 Processing trade element with date: " + currentDate);
                    
                    try {
                        Trade trade = parseTradeElementWithDate(element, currentDate, year);
                        if (trade != null && isValidTrade(trade)) {
                            trades.add(trade);
                            System.out.println("✅ Parsed trade: " + trade.getFromTeam() + " → " + trade.getToTeam());
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error parsing trade element: " + e.getMessage());
                    }
                }
            }
            
            // If no trades found with the above selectors, try alternative parsing
            if (trades.isEmpty()) {
                System.out.println("⚠️ No trades found with primary selectors, trying alternative parsing...");
                trades.addAll(parseAlternativeFormat(doc, year));
            }
            
            System.out.println("🎯 Successfully scraped " + trades.size() + " trades for year " + year);
            
        } catch (Exception e) {
            System.err.println("❌ Error scraping trades for year " + year + ": " + e.getMessage());
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
    private LocalDate extractDateFromDateElement(String text, int year) {
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
        
        return LocalDate.of(year > 0 ? year : 2025, 7, 1);
    }
    
    /**
     * Parse a trade element using a previously found date
     */
    private Trade parseTradeElementWithDate(Element element, LocalDate tradeDate, int year) {
        try {
            String elementText = element.text();
            
            System.out.println("🔍 Parsing trade with preset date: " + tradeDate);
            System.out.println("📝 Trade text: " + elementText.substring(0, Math.min(100, elementText.length())));
            
            // Use the provided date, or fall back to extracting from text
            LocalDate finalDate = tradeDate != null ? tradeDate : extractDate(elementText, year);
            
            // Extract teams
            List<String> teams = extractTeams(element);
            if (teams.size() < 2) {
                teams = extractTeamsFromText(elementText);
            }
            
            if (teams.size() < 2) {
                System.out.println("⚠️ Could not find 2 teams in trade");
                return null;
            }
            
            String fromTeam = normalizeTeamName(teams.get(0));
            String toTeam = normalizeTeamName(teams.get(1));
            
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
            
            System.out.println("✅ Created trade: " + fromTeam + " → " + toTeam + " on " + finalDate);
            return trade;
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing trade element with date: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse a single trade element
     */
    @SuppressWarnings("unused")
    private Trade parseTradelement(Element element, int year) {
        try {
            String elementText = element.text();
            
            // Skip if doesn't contain trade indicators
            if (!containsTradeIndicators(elementText)) {
                return null;
            }
            
            System.out.println("🔍 Parsing trade text: " + elementText.substring(0, Math.min(150, elementText.length())));
            
            // Extract date - use the actual text, not just the element
            LocalDate tradeDate = extractDate(elementText, year);
            System.out.println("📅 Extracted trade date: " + tradeDate);
            
            // Extract teams
            List<String> teams = extractTeams(element);
            if (teams.size() < 2) {
                teams = extractTeamsFromText(elementText);
            }
            
            if (teams.size() < 2) {
                System.out.println("⚠️ Could not find 2 teams in trade");
                return null;
            }
            
            String fromTeam = normalizeTeamName(teams.get(0));
            String toTeam = normalizeTeamName(teams.get(1));
            
            // Extract players and assets
            List<String> players = extractPlayers(elementText);
            List<String> assets = extractAssets(elementText);
            
            // Generate trade ID
            String tradeId = generateTradeId(tradeDate, fromTeam, toTeam);
            
            Trade trade = new Trade(
                tradeId,
                tradeDate,
                fromTeam,
                toTeam,
                players,
                assets,
                elementText.trim(),
                SPOTRAC_BASE_URL
            );
            
            System.out.println("✅ Created trade: " + fromTeam + " → " + toTeam + " on " + tradeDate);
            return trade;
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing trade element: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Alternative parsing method for different HTML structures
     */
    private List<Trade> parseAlternativeFormat(Document doc, int year) {
        List<Trade> trades = new ArrayList<>();
        
        try {
            // Look for text patterns that indicate trades
            Elements textElements = doc.select("*:contains(acquires), *:contains(trades), *:contains(traded)");
            
            for (Element element : textElements) {
                String text = element.text();
                if (containsTradeIndicators(text) && text.length() > 20) {
                    Trade trade = parseTradeFromText(text, year);
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
    private Trade parseTradeFromText(String text, int year) {
        try {
            // Extract date
            LocalDate tradeDate = extractDateFromText(text, year);
            
            // Extract teams using pattern matching
            List<String> teams = extractTeamsFromText(text);
            if (teams.size() < 2) {
                return null;
            }
            
            String fromTeam = normalizeTeamName(teams.get(0));
            String toTeam = normalizeTeamName(teams.get(1));
            
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
     * Extract date from element text
     */
    private LocalDate extractDate(String text, int year) {
        try {
            System.out.println("🔍 Extracting date from text: '" + text.substring(0, Math.min(20, text.length())) + "'");
            
            // Simple pattern to match "Jun 15, 2025" format at the beginning
            Pattern datePattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),?\\s+(\\d{4})");
            Matcher matcher = datePattern.matcher(text);
            
            if (matcher.find()) {
                String month = matcher.group(1);
                String day = matcher.group(2);
                String yearStr = matcher.group(3);
                
                String dateStr = month + " " + day + ", " + yearStr;
                LocalDate parsedDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MMM d, yyyy"));
                
                System.out.println("✅ Successfully parsed date: " + parsedDate + " from '" + matcher.group(0) + "'");
                return parsedDate;
            }
            
            System.out.println("❌ No date pattern found in: '" + text.substring(0, Math.min(50, text.length())) + "'");
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing date: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback
        LocalDate fallbackDate = LocalDate.of(year > 0 ? year : 2025, 7, 1);
        System.out.println("🔄 Using fallback date: " + fallbackDate);
        return fallbackDate;
    }
    
    /**
     * Extract date from text using various patterns
     */
    private LocalDate extractDateFromText(String text, int year) {
        // More flexible date extraction
        Pattern datePattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),?\\s+(\\d{4})");
        Matcher matcher = datePattern.matcher(text);
        
        if (matcher.find()) {
            try {
                String dateStr = matcher.group(1) + " " + matcher.group(2) + ", " + matcher.group(3);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                // Fall through to default
            }
        }
        
        return LocalDate.of(year > 0 ? year : LocalDate.now().getYear(), 1, 1);
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
        
        // Check against known team names
        for (String teamName : TEAM_ALIASES.keySet()) {
            if (text.contains(teamName)) {
                String normalizedName = TEAM_ALIASES.get(teamName);
                if (!teams.contains(normalizedName)) {
                    teams.add(normalizedName);
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
               lowerPhrase.equals("trade exception") ||
               lowerPhrase.equals("round pick") ||
               lowerPhrase.equals("cash considerations");
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
        
        // Add years from 2010 to current year
        for (int year = currentYear; year >= 2010; year--) {
            years.add(year);
        }
        
        return years;
    }
}