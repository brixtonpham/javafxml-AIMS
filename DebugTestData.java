import java.io.InputStream;

public class DebugTestData {
    public static void main(String[] args) {
        String scriptPath = "/test_data/V2__seed_test_data.sql";
        
        // Try to get the resource
        try (InputStream inputStream = DebugTestData.class.getResourceAsStream(scriptPath)) {
            if (inputStream == null) {
                System.out.println("ERROR: Resource not found: " + scriptPath);
                
                // Try alternative paths
                String[] altPaths = {
                    "/V2__seed_test_data.sql",
                    "test_data/V2__seed_test_data.sql",
                    "V2__seed_test_data.sql"
                };
                
                for (String altPath : altPaths) {
                    try (InputStream altStream = DebugTestData.class.getResourceAsStream(altPath)) {
                        if (altStream != null) {
                            System.out.println("FOUND at alternative path: " + altPath);
                            break;
                        } else {
                            System.out.println("NOT FOUND at: " + altPath);
                        }
                    }
                }
            } else {
                String sql = new String(inputStream.readAllBytes());
                System.out.println("SUCCESS: Found resource at: " + scriptPath);
                System.out.println("SQL length: " + sql.length());
                System.out.println("First 200 chars: " + sql.substring(0, Math.min(200, sql.length())));
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
