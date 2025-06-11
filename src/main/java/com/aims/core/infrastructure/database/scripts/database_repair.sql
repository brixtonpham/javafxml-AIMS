-- AIMS Database Schema Repair Script
-- Purpose: Fix missing LP table that's causing search functionality to fail
-- Error: [SQLITE_ERROR] SQL error or missing database (no such table: LP)

-- Enable foreign key constraints
PRAGMA foreign_keys = ON;

-- Check if LP table exists, create if missing
CREATE TABLE IF NOT EXISTS LP (
    productID TEXT PRIMARY KEY, -- Foreign key to PRODUCT
    artists TEXT,
    recordLabel TEXT,
    tracklist TEXT,
    genre TEXT,
    releaseDate TEXT, -- Store as TEXT 'YYYY-MM-DD'
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_lp_artists ON LP(artists);
CREATE INDEX IF NOT EXISTS idx_lp_genre ON LP(genre);

-- Verify table structure
-- .schema LP

-- Check if table was created successfully
-- SELECT name FROM sqlite_master WHERE type='table' AND name='LP';

-- Show all tables to verify complete schema
-- SELECT name FROM sqlite_master WHERE type='table';