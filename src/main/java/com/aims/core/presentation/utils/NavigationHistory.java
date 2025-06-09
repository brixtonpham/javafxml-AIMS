package com.aims.core.presentation.utils;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

/**
 * NavigationHistory manages a stack-based navigation history to enable
 * smart back navigation in the AIMS application. It maintains a limited
 * history of navigation contexts to prevent memory issues.
 */
public class NavigationHistory {
    private final Stack<NavigationContext> historyStack;
    private static final int MAX_HISTORY_SIZE = 10;
    
    /**
     * Creates a new NavigationHistory instance with an empty history stack.
     */
    public NavigationHistory() {
        this.historyStack = new Stack<>();
    }
    
    /**
     * Pushes a new navigation context onto the history stack.
     * Prevents duplicate consecutive entries and maintains size limit.
     * 
     * @param context The navigation context to add to history
     */
    public void pushNavigation(NavigationContext context) {
        if (context == null) {
            System.out.println("NavigationHistory.pushNavigation: Null context provided, ignoring");
            return;
        }
        
        // Prevent duplicate consecutive entries
        if (!historyStack.isEmpty()) {
            NavigationContext top = historyStack.peek();
            if (top.getScreenPath().equals(context.getScreenPath())) {
                System.out.println("NavigationHistory.pushNavigation: Duplicate consecutive entry detected, updating existing: " + context.getScreenPath());
                // Update the existing entry instead of adding a duplicate
                historyStack.pop();
                historyStack.push(context);
                return;
            }
        }
        
        System.out.println("NavigationHistory.pushNavigation: Adding to history: " + context.getScreenPath() + " - " + context.getScreenTitle());
        historyStack.push(context);
        
        // Maintain size limit by removing oldest entries
        while (historyStack.size() > MAX_HISTORY_SIZE) {
            NavigationContext removed = historyStack.remove(0);
            System.out.println("NavigationHistory.pushNavigation: Removed oldest entry due to size limit: " + removed.getScreenPath());
        }
        
        System.out.println("NavigationHistory.pushNavigation: History size is now: " + historyStack.size());
    }
    
    /**
     * Pops and returns the most recent navigation context from the history stack.
     * 
     * @return The most recent NavigationContext, or null if history is empty
     */
    public NavigationContext popNavigation() {
        if (historyStack.isEmpty()) {
            System.out.println("NavigationHistory.popNavigation: History stack is empty");
            return null;
        }
        
        NavigationContext context = historyStack.pop();
        System.out.println("NavigationHistory.popNavigation: Popped from history: " + context.getScreenPath() + " - " + context.getScreenTitle());
        System.out.println("NavigationHistory.popNavigation: History size is now: " + historyStack.size());
        return context;
    }
    
    /**
     * Peeks at the most recent navigation context without removing it from the stack.
     * 
     * @return The most recent NavigationContext, or null if history is empty
     */
    public NavigationContext peekPrevious() {
        if (historyStack.isEmpty()) {
            return null;
        }
        
        NavigationContext context = historyStack.peek();
        System.out.println("NavigationHistory.peekPrevious: Top of history: " + context.getScreenPath() + " - " + context.getScreenTitle());
        return context;
    }
    
    /**
     * Checks if there are any previous navigation contexts in the history.
     * 
     * @return true if navigation history is available
     */
    public boolean hasPrevious() {
        boolean hasHistory = !historyStack.isEmpty();
        System.out.println("NavigationHistory.hasPrevious: " + hasHistory + " (size: " + historyStack.size() + ")");
        return hasHistory;
    }
    
    /**
     * Clears all navigation history.
     */
    public void clear() {
        int sizeBefore = historyStack.size();
        historyStack.clear();
        System.out.println("NavigationHistory.clear: Cleared navigation history (was size: " + sizeBefore + ")");
    }
    
    /**
     * Gets the current size of the navigation history.
     * 
     * @return The number of entries in the history stack
     */
    public int size() {
        return historyStack.size();
    }
    
    /**
     * Gets a copy of the current navigation history as a list.
     * The list is ordered from oldest to newest entry.
     * 
     * @return A list containing copies of all navigation contexts in history
     */
    public List<NavigationContext> getHistoryList() {
        return new ArrayList<>(historyStack);
    }
    
    /**
     * Finds the most recent navigation context that matches the specified screen path.
     * This is useful for finding the last time a user was on a specific screen.
     * 
     * @param screenPath The FXML path to search for
     * @return The most recent NavigationContext for the specified path, or null if not found
     */
    public NavigationContext findMostRecentByPath(String screenPath) {
        if (screenPath == null) {
            return null;
        }
        
        // Search from top of stack (most recent) downward
        for (int i = historyStack.size() - 1; i >= 0; i--) {
            NavigationContext context = historyStack.get(i);
            if (screenPath.equals(context.getScreenPath())) {
                System.out.println("NavigationHistory.findMostRecentByPath: Found match for " + screenPath + " at position " + i);
                return context;
            }
        }
        
        System.out.println("NavigationHistory.findMostRecentByPath: No match found for " + screenPath);
        return null;
    }
    
    /**
     * Removes all entries for a specific screen path from the history.
     * This can be useful when a screen is no longer accessible or valid.
     * 
     * @param screenPath The FXML path to remove from history
     * @return The number of entries removed
     */
    public int removeEntriesForPath(String screenPath) {
        if (screenPath == null) {
            return 0;
        }
        
        int removedCount = 0;
        historyStack.removeIf(context -> {
            if (screenPath.equals(context.getScreenPath())) {
                System.out.println("NavigationHistory.removeEntriesForPath: Removing entry for " + screenPath);
                return true;
            }
            return false;
        });
        
        System.out.println("NavigationHistory.removeEntriesForPath: Removed " + removedCount + " entries for " + screenPath);
        return removedCount;
    }
    
    /**
     * Gets debug information about the current navigation history.
     * 
     * @return A string representation of the navigation history for debugging
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("NavigationHistory Debug Info:\n");
        sb.append("Size: ").append(historyStack.size()).append("/").append(MAX_HISTORY_SIZE).append("\n");
        
        if (historyStack.isEmpty()) {
            sb.append("History is empty\n");
        } else {
            sb.append("History (oldest to newest):\n");
            for (int i = 0; i < historyStack.size(); i++) {
                NavigationContext context = historyStack.get(i);
                sb.append("  ").append(i + 1).append(". ")
                  .append(context.getScreenTitle()).append(" (").append(context.getScreenPath()).append(")")
                  .append(" - ").append(context.getTimestamp()).append("\n");
                
                if (context.hasSearchContext()) {
                    sb.append("     Search: '").append(context.getSearchTerm()).append("'")
                      .append(", Category: '").append(context.getCategoryFilter()).append("'")
                      .append(", Sort: '").append(context.getSortBy()).append("'")
                      .append(", Page: ").append(context.getCurrentPage()).append("\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "NavigationHistory{size=" + historyStack.size() + ", maxSize=" + MAX_HISTORY_SIZE + "}";
    }
}