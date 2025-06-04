import java.util.ArrayList;
import java.util.List;

public class debug_sql {
    public static void main(String[] args) {
        String testSQL = "INSERT OR REPLACE INTO CD (productID, artists, recordLabel, tracklist, cd_genre, releaseDate) VALUES ('cd_001', 'The Beatles', 'Apple Records', 'Come Together;Something;Maxwell''''s Silver Hammer;Oh! Darling;Octopus''''s Garden;I Want You (She''''s So Heavy)', 'Rock', '1969-09-26');";
        
        System.out.println("Original SQL:");
        System.out.println(testSQL);
        System.out.println("\nSplitting with simple split:");
        String[] simple = testSQL.split(";");
        for (int i = 0; i < simple.length; i++) {
            System.out.println(i + ": '" + simple[i] + "'");
        }
        
        System.out.println("\nSplitting with smart parser:");
        List<String> smart = splitSQLStatements(testSQL);
        for (int i = 0; i < smart.size(); i++) {
            System.out.println(i + ": '" + smart.get(i) + "'");
        }
    }
    
    private static List<String> splitSQLStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inSingleQuote = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            if (c == '\'') {
                // Check if this is an escaped single quote (doubled)
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    // This is an escaped quote - add both characters and skip ahead
                    currentStatement.append("''");
                    i++; // Skip the next quote
                } else {
                    // This is a normal quote - toggle the state
                    inSingleQuote = !inSingleQuote;
                    currentStatement.append(c);
                }
            } else if (c == ';' && !inSingleQuote) {
                // End of statement - only split if not inside quotes
                statements.add(currentStatement.toString().trim());
                currentStatement = new StringBuilder();
            } else {
                currentStatement.append(c);
            }
        }
        
        // Add the last statement if there's any remaining content
        if (currentStatement.length() > 0) {
            statements.add(currentStatement.toString().trim());
        }
        
        return statements;
    }
}
