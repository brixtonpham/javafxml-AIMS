#!/bin/bash

# Create directories
mkdir -p src/main/resources/images/books
mkdir -p src/main/resources/images/cds
mkdir -p src/main/resources/images/dvds

echo "📁 Created image directories"

# Download Book Images
echo "📚 Downloading book images..."

# Harry Potter
curl -L "https://images-na.ssl-images-amazon.com/images/I/51HSkTKlauL._SX346_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/harry_potter_1.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Clean Code
curl -L "https://images-na.ssl-images-amazon.com/images/I/41xShlnTZTL._SX376_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/clean_code.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Machine Learning Book
curl -L "https://images-na.ssl-images-amazon.com/images/I/51aqYc1QyrL._SX379_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/ml_book.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Art Techniques
curl -L "https://images-na.ssl-images-amazon.com/images/I/51rF8BBZC6L._SX383_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/art.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Environment Science
curl -L "https://images-na.ssl-images-amazon.com/images/I/51CbZNRKJ+L._SX384_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/environment.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Additional Books
curl -L "https://images-na.ssl-images-amazon.com/images/I/51FzcKVLRuL._SX329_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/great_gatsby.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images-na.ssl-images-amazon.com/images/I/51IXWZzlgSL._SX330_BO1,204,203,200_.jpg" \
  -o "src/main/resources/images/books/mockingbird.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/books/travel.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/books/math.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1606092195730-5d7b9af1efc5?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/books/history.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/books/poetry.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/books/psychology.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

echo "🎵 Downloading CD images..."

# Abbey Road
curl -L "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg" \
  -o "src/main/resources/images/cds/abbey_road.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Back in Black
curl -L "https://upload.wikimedia.org/wikipedia/en/7/76/Acdc_backinblack_500.jpg" \
  -o "src/main/resources/images/cds/back_in_black.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Classic Rock
curl -L "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/classic_rock.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Blues
curl -L "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/blues.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Piano Classical
curl -L "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/piano.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Country
curl -L "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/country.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# EDM
curl -L "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/edm.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Folk
curl -L "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/folk.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Ambient
curl -L "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/ambient.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Additional CDs
curl -L "https://upload.wikimedia.org/wikipedia/en/2/27/DarkSideOfTheMoon.png" \
  -o "src/main/resources/images/cds/dark_side_moon.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/5/55/Michael_Jackson_-_Thriller.png" \
  -o "src/main/resources/images/cds/thriller.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/4/4b/Hotelcalifornia.jpg" \
  -o "src/main/resources/images/cds/hotel_california.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/indie.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/cds/beethoven.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

echo "🎬 Downloading DVD images..."

# Action Movie
curl -L "https://images.unsplash.com/photo-1489599133044-64d49d28429a?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/dvds/action.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Documentary
curl -L "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/dvds/documentary.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Additional DVDs
curl -L "https://upload.wikimedia.org/wikipedia/en/8/8b/Star_Wars_Episode_IV_A_New_Hope_1977_original_poster.jpg" \
  -o "src/main/resources/images/dvds/star_wars_nh.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/1/1b/Godfather_ver1.jpg" \
  -o "src/main/resources/images/dvds/godfather.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/8/81/ShawshankRedemptionMoviePoster.jpg" \
  -o "src/main/resources/images/dvds/shawshank.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/8/82/Pulp_Fiction_cover.jpg" \
  -o "src/main/resources/images/dvds/pulp_fiction.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/8/87/Inception_ver3.jpg" \
  -o "src/main/resources/images/dvds/inception.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://upload.wikimedia.org/wikipedia/en/f/fb/Lord_Rings_Fellowship_Ring.jpg" \
  -o "src/main/resources/images/dvds/lotr_fellowship.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1489599133044-64d49d28429a?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/dvds/thriller.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1518676590629-3dcbd9c5a5c9?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/dvds/romance.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1446776653964-20c1d3a81b06?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/dvds/scifi.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

curl -L "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=400&h=400&fit=crop" \
  -o "src/main/resources/images/dvds/drama.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# Fix the test product with invalid path
curl -L "https://via.placeholder.com/400x400/95A5A6/FFFFFF?text=Test+Product" \
  -o "src/main/resources/images/test_product.jpg" \
  --user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

echo "✅ All images downloaded!"

# Verify downloads
echo "📊 Verifying downloads..."
find src/main/resources/images -name "*.jpg" -exec ls -lh {} \;

echo "🎉 Download complete! Run your application again."