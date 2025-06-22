-- V2__seed_initial_data.sql
-- Comprehensive sample data for AIMS application
-- Covers all 18 database tables with realistic test data

PRAGMA foreign_keys = ON;

-- ======================================
-- ROLES DATA (Foundation)
-- ======================================

INSERT INTO ROLE (roleID, roleName) VALUES
('ADMIN', 'Administrator'),
('PRODUCT_MANAGER', 'Product Manager'),
('CUSTOMER', 'Customer');

-- ======================================
-- USER ACCOUNTS (Users for all roles)
-- ======================================

-- Admin users
INSERT INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES
('USR_ADMIN_001', 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'admin@aims.com', 'ACTIVE'),
('USR_ADMIN_002', 'sysadmin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'sysadmin@aims.com', 'ACTIVE');

-- Product Manager users
INSERT INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES
('USR_PM_001', 'pmjohn', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'john.pm@aims.com', 'ACTIVE'),
('USR_PM_002', 'pmjane', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'jane.pm@aims.com', 'ACTIVE');

-- Customer users
INSERT INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES
('USR_CUST_001', 'customer1', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'customer1@example.com', 'ACTIVE'),
('USR_CUST_002', 'customer2', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'customer2@example.com', 'ACTIVE'),
('USR_CUST_003', 'customer3', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'customer3@example.com', 'ACTIVE'),
('USR_CUST_004', 'johndoe', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'john.doe@example.com', 'ACTIVE'),
('USR_CUST_005', 'janesmith', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'jane.smith@example.com', 'INACTIVE'),
('USR_CUST_006', 'mikeross', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHga31lW', 'mike.ross@example.com', 'ACTIVE');

-- ======================================
-- USER ROLE ASSIGNMENTS
-- ======================================

-- Admin role assignments
INSERT INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES
('USR_ADMIN_001', 'ADMIN'),
('USR_ADMIN_002', 'ADMIN');

-- Product Manager role assignments
INSERT INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES
('USR_PM_001', 'PRODUCT_MANAGER'),
('USR_PM_002', 'PRODUCT_MANAGER');

-- Customer role assignments
INSERT INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES
('USR_CUST_001', 'CUSTOMER'),
('USR_CUST_002', 'CUSTOMER'),
('USR_CUST_003', 'CUSTOMER'),
('USR_CUST_004', 'CUSTOMER'),
('USR_CUST_005', 'CUSTOMER'),
('USR_CUST_006', 'CUSTOMER');

-- ======================================
-- PRODUCTS (Base product table)
-- ======================================

-- Books
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
('BOOK_001', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Programming', 15.0, 45.99, 25, 'A practical guide to writing clean, maintainable code', '/images/books/clean_code.jpg', '9780132350884', '23.4x18.7x2.3', 0.8, '2024-01-15', 'BOOK'),
('BOOK_002', 'The Great Gatsby', 'Classic Literature', 8.0, 12.99, 50, 'A classic American novel by F. Scott Fitzgerald', '/images/books/great_gatsby.jpg', '9780743273565', '19.4x13.2x1.5', 0.3, '2024-01-20', 'BOOK'),
('BOOK_003', 'Harry Potter and the Philosopher''s Stone', 'Fantasy', 10.0, 15.99, 40, 'The first book in the beloved Harry Potter series', '/images/books/harry_potter_1.jpg', '9780747532699', '19.8x12.9x2.1', 0.4, '2024-01-25', 'BOOK'),
('BOOK_004', 'To Kill a Mockingbird', 'Classic Literature', 9.0, 14.99, 30, 'Harper Lee''s timeless tale of justice and morality', '/images/books/mockingbird.jpg', '9780061120084', '19.4x13.2x1.8', 0.35, '2024-02-01', 'BOOK'),
('BOOK_005', 'Machine Learning Yearning', 'Technology', 20.0, 39.99, 15, 'Technical strategy for AI engineers by Andrew Ng', '/images/books/ml_book.jpg', '9780999951002', '23.0x15.5x2.0', 0.6, '2024-02-05', 'BOOK'),
('BOOK_006', 'Mathematics for Computer Science', 'Academic', 25.0, 79.99, 20, 'Comprehensive mathematics textbook for CS students', '/images/books/math.jpg', '9780262043298', '25.4x20.3x3.2', 1.2, '2024-02-10', 'BOOK'),
('BOOK_007', 'The Psychology of Computer Programming', 'Psychology', 18.0, 34.99, 12, 'Classic text on software development psychology', '/images/books/psychology.jpg', '9780932633429', '23.1x15.2x1.9', 0.5, '2024-02-15', 'BOOK'),
('BOOK_008', 'Environmental Science: A Global Concern', 'Science', 30.0, 129.99, 8, 'Comprehensive environmental science textbook', '/images/books/environment.jpg', '9781259822490', '27.9x21.6x4.1', 1.8, '2024-02-20', 'BOOK'),
('BOOK_009', 'A History of the World in 100 Objects', 'History', 22.0, 29.99, 18, 'Fascinating journey through human history', '/images/books/history.jpg', '9780143124153', '21.6x14.0x2.5', 0.7, '2024-02-25', 'BOOK'),
('BOOK_010', 'The Art of Travel', 'Travel', 12.0, 19.99, 35, 'Philosophical exploration of travel and its meaning', '/images/books/travel.jpg', '9780375420290', '20.3x13.3x1.7', 0.4, '2024-03-01', 'BOOK'),
('BOOK_011', 'Poetry for Beginners', 'Poetry', 8.0, 16.99, 22, 'An introduction to poetry appreciation and writing', '/images/books/poetry.jpg', '9781234567890', '19.1x12.7x1.3', 0.3, '2024-03-05', 'BOOK'),
('BOOK_012', 'Digital Art Mastery', 'Art', 15.0, 42.99, 14, 'Complete guide to digital art techniques', '/images/books/art.jpg', '9780987654321', '25.4x20.3x2.8', 0.9, '2024-03-10', 'BOOK');

-- CDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
('CD_001', 'Abbey Road', 'Rock', 12.0, 18.99, 45, 'The Beatles'' iconic album featuring "Come Together"', '/images/cds/abbey_road.jpg', '0094636245621', '14.2x12.5x1.0', 0.1, '2024-01-10', 'CD'),
('CD_002', 'The Dark Side of the Moon', 'Progressive Rock', 15.0, 22.99, 38, 'Pink Floyd''s legendary concept album', '/images/cds/dark_side_moon.jpg', '0094635795721', '14.2x12.5x1.0', 0.1, '2024-01-15', 'CD'),
('CD_003', 'Thriller', 'Pop', 18.0, 19.99, 42, 'Michael Jackson''s best-selling album of all time', '/images/cds/thriller.jpg', '0074643851329', '14.2x12.5x1.0', 0.1, '2024-01-20', 'CD'),
('CD_004', 'Back in Black', 'Hard Rock', 14.0, 17.99, 33, 'AC/DC''s tribute to Bon Scott and breakthrough album', '/images/cds/back_in_black.jpg', '0075678164729', '14.2x12.5x1.0', 0.1, '2024-01-25', 'CD'),
('CD_005', 'Hotel California', 'Classic Rock', 16.0, 21.99, 29, 'Eagles'' masterpiece with the iconic title track', '/images/cds/hotel_california.jpg', '0081227954123', '14.2x12.5x1.0', 0.1, '2024-02-01', 'CD'),
('CD_006', 'Beethoven: Symphony No. 9', 'Classical', 20.0, 24.99, 25, 'Karajan conducts Beethoven''s Ninth Symphony', '/images/cds/beethoven.jpg', '0028947753629', '14.2x12.5x1.0', 0.1, '2024-02-05', 'CD'),
('CD_007', 'Kind of Blue', 'Jazz', 22.0, 23.99, 31, 'Miles Davis'' influential modal jazz masterpiece', '/images/cds/blues.jpg', '0074646362723', '14.2x12.5x1.0', 0.1, '2024-02-10', 'CD'),
('CD_008', 'Classic Rock Hits', 'Rock', 10.0, 15.99, 50, 'Greatest rock hits from the 70s and 80s', '/images/cds/classic_rock.jpg', '0123456789012', '14.2x12.5x1.0', 0.1, '2024-02-15', 'CD'),
('CD_009', 'Country Roads Collection', 'Country', 8.0, 14.99, 27, 'Best of country music compilation', '/images/cds/country.jpg', '0234567890123', '14.2x12.5x1.0', 0.1, '2024-02-20', 'CD'),
('CD_010', 'Electronic Dance Hits', 'Electronic', 12.0, 16.99, 35, 'High-energy EDM tracks for the dance floor', '/images/cds/edm.jpg', '0345678901234', '14.2x12.5x1.0', 0.1, '2024-02-25', 'CD'),
('CD_011', 'Folk Songs of America', 'Folk', 14.0, 18.99, 23, 'Traditional and modern American folk music', '/images/cds/folk.jpg', '0456789012345', '14.2x12.5x1.0', 0.1, '2024-03-01', 'CD'),
('CD_012', 'Indie Alternative Mix', 'Indie', 11.0, 17.99, 28, 'Fresh sounds from independent artists', '/images/cds/indie.jpg', '0567890123456', '14.2x12.5x1.0', 0.1, '2024-03-05', 'CD'),
('CD_013', 'Piano Masterpieces', 'Classical', 19.0, 26.99, 20, 'Beautiful piano compositions through the ages', '/images/cds/piano.jpg', '0678901234567', '14.2x12.5x1.0', 0.1, '2024-03-10', 'CD'),
('CD_014', 'Ambient Soundscapes', 'Ambient', 13.0, 19.99, 32, 'Relaxing ambient music for meditation', '/images/cds/ambient.jpg', '0789012345678', '14.2x12.5x1.0', 0.1, '2024-03-15', 'CD');

-- DVDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
('DVD_001', 'The Godfather', 'Crime Drama', 25.0, 19.99, 30, 'Francis Ford Coppola''s masterpiece about the Corleone family', '/images/dvds/godfather.jpg', '0097360814842', '19.0x13.5x1.5', 0.08, '2024-01-12', 'DVD'),
('DVD_002', 'Inception', 'Sci-Fi Thriller', 22.0, 24.99, 35, 'Christopher Nolan''s mind-bending thriller about dreams', '/images/dvds/inception.jpg', '0883929034444', '19.0x13.5x1.5', 0.08, '2024-01-18', 'DVD'),
('DVD_003', 'The Lord of the Rings: Fellowship', 'Fantasy Adventure', 30.0, 29.99, 28, 'Peter Jackson''s epic adaptation of Tolkien''s masterpiece', '/images/dvds/lotr_fellowship.jpg', '0794043558429', '19.0x13.5x1.5', 0.08, '2024-01-24', 'DVD'),
('DVD_004', 'Pulp Fiction', 'Crime Drama', 20.0, 18.99, 32, 'Quentin Tarantino''s groundbreaking nonlinear narrative', '/images/dvds/pulp_fiction.jpg', '0786936716726', '19.0x13.5x1.5', 0.08, '2024-01-30', 'DVD'),
('DVD_005', 'The Shawshank Redemption', 'Drama', 18.0, 16.99, 40, 'Stephen King''s story of hope and friendship in prison', '/images/dvds/shawshank.jpg', '0883929303445', '19.0x13.5x1.5', 0.08, '2024-02-05', 'DVD'),
('DVD_006', 'Star Wars: A New Hope', 'Sci-Fi Adventure', 28.0, 27.99, 25, 'The original Star Wars film that started it all', '/images/dvds/star_wars_nh.jpg', '0024543013129', '19.0x13.5x1.5', 0.08, '2024-02-12', 'DVD'),
('DVD_007', 'Action Movie Collection', 'Action', 15.0, 22.99, 45, 'High-octane action films compilation', '/images/dvds/action.jpg', '1234567890123', '19.0x13.5x1.5', 0.08, '2024-02-18', 'DVD'),
('DVD_008', 'Documentary Classics', 'Documentary', 12.0, 19.99, 38, 'Award-winning documentaries collection', '/images/dvds/documentary.jpg', '2345678901234', '19.0x13.5x1.5', 0.08, '2024-02-24', 'DVD'),
('DVD_009', 'Drama Masterpieces', 'Drama', 20.0, 24.99, 33, 'Critically acclaimed drama films', '/images/dvds/drama.jpg', '3456789012345', '19.0x13.5x1.5', 0.08, '2024-03-02', 'DVD'),
('DVD_010', 'Romantic Comedies', 'Romance', 14.0, 17.99, 42, 'Feel-good romantic comedy collection', '/images/dvds/romance.jpg', '4567890123456', '19.0x13.5x1.5', 0.08, '2024-03-08', 'DVD'),
('DVD_011', 'Sci-Fi Classics', 'Science Fiction', 25.0, 26.99, 29, 'Classic science fiction films', '/images/dvds/scifi.jpg', '5678901234567', '19.0x13.5x1.5', 0.08, '2024-03-14', 'DVD'),
('DVD_012', 'Thriller Collection', 'Thriller', 18.0, 21.99, 36, 'Edge-of-your-seat thriller movies', '/images/dvds/thriller.jpg', '6789012345678', '19.0x13.5x1.5', 0.08, '2024-03-20', 'DVD');

-- LPs (Vinyl Records)
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
('LP_001', 'Abbey Road (Vinyl)', 'Rock', 35.0, 49.99, 20, 'The Beatles'' Abbey Road on 180g vinyl', '/images/cds/abbey_road.jpg', '0602577424472', '31.4x31.4x0.5', 0.18, '2024-01-14', 'LP'),
('LP_002', 'Dark Side of the Moon (Vinyl)', 'Progressive Rock', 40.0, 54.99, 18, 'Pink Floyd''s masterpiece on audiophile vinyl', '/images/cds/dark_side_moon.jpg', '0602547202345', '31.4x31.4x0.5', 0.18, '2024-01-21', 'LP'),
('LP_003', 'Thriller (Vinyl)', 'Pop', 38.0, 52.99, 22, 'Michael Jackson''s Thriller on premium vinyl', '/images/cds/thriller.jpg', '0074643851346', '31.4x31.4x0.5', 0.18, '2024-01-28', 'LP'),
('LP_004', 'Kind of Blue (Vinyl)', 'Jazz', 45.0, 59.99, 15, 'Miles Davis'' jazz masterpiece on vinyl', '/images/cds/blues.jpg', '0074646362740', '31.4x31.4x0.5', 0.18, '2024-02-04', 'LP'),
('LP_005', 'Hotel California (Vinyl)', 'Classic Rock', 42.0, 56.99, 16, 'Eagles'' Hotel California remastered on vinyl', '/images/cds/hotel_california.jpg', '0081227954140', '31.4x31.4x0.5', 0.18, '2024-02-11', 'LP'),
('LP_006', 'Classic Jazz Collection (Vinyl)', 'Jazz', 50.0, 69.99, 12, 'Premium jazz collection on double vinyl', '/images/cds/blues.jpg', '7890123456789', '31.4x31.4x0.5', 0.18, '2024-02-18', 'LP'),
('LP_007', 'Rock Legends (Vinyl)', 'Rock', 48.0, 64.99, 14, 'Greatest rock hits on premium vinyl', '/images/cds/classic_rock.jpg', '8901234567890', '31.4x31.4x0.5', 0.18, '2024-02-25', 'LP'),
('LP_008', 'Electronic Anthology (Vinyl)', 'Electronic', 44.0, 58.99, 10, 'Electronic music history on vinyl', '/images/cds/edm.jpg', '9012345678901', '31.4x31.4x0.5', 0.18, '2024-03-04', 'LP');

-- ======================================
-- PRODUCT TYPE SPECIFIC DATA
-- ======================================

-- Book specific data
INSERT INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES
('BOOK_001', 'Robert C. Martin', 'Paperback', 'Prentice Hall', '2008-08-01', 464, 'English', 'Programming'),
('BOOK_002', 'F. Scott Fitzgerald', 'Paperback', 'Scribner', '1925-04-10', 180, 'English', 'Classic Literature'),
('BOOK_003', 'J.K. Rowling', 'Hardcover', 'Bloomsbury', '1997-06-26', 223, 'English', 'Fantasy'),
('BOOK_004', 'Harper Lee', 'Paperback', 'J. B. Lippincott & Co.', '1960-07-11', 376, 'English', 'Classic Literature'),
('BOOK_005', 'Andrew Ng', 'Paperback', 'Self-Published', '2018-01-01', 118, 'English', 'Technology'),
('BOOK_006', 'Eric Lehman, F. Thomson Leighton', 'Hardcover', 'MIT Press', '2017-06-12', 988, 'English', 'Academic'),
('BOOK_007', 'Gerald M. Weinberg', 'Paperback', 'Dorset House Publishing', '1971-01-01', 288, 'English', 'Psychology'),
('BOOK_008', 'William P. Cunningham, Mary Ann Cunningham', 'Hardcover', 'McGraw-Hill Education', '2020-01-07', 640, 'English', 'Science'),
('BOOK_009', 'Neil MacGregor', 'Paperback', 'Penguin Books', '2010-09-30', 736, 'English', 'History'),
('BOOK_010', 'Alain de Botton', 'Paperback', 'Vintage', '2002-05-07', 272, 'English', 'Travel'),
('BOOK_011', 'Various Authors', 'Paperback', 'Learning Press', '2023-01-15', 200, 'English', 'Poetry'),
('BOOK_012', 'Sarah Johnson', 'Paperback', 'Art Masters', '2023-05-20', 350, 'English', 'Art');

-- CD specific data
INSERT INTO CD (productID, artists, recordLabel, tracklist, cd_genre, releaseDate) VALUES
('CD_001', 'The Beatles', 'Apple Records', 'Come Together;Something;Maxwell''s Silver Hammer;Oh! Darling;Octopus''s Garden;I Want You;Here Comes the Sun;Because;You Never Give Me Your Money;Sun King;Mean Mr. Mustard;Polythene Pam;She Came in Through the Bathroom Window;Golden Slumbers;Carry That Weight;The End;Her Majesty', 'Rock', '1969-09-26'),
('CD_002', 'Pink Floyd', 'Harvest Records', 'Speak to Me;Breathe;On the Run;Time;The Great Gig in the Sky;Money;Us and Them;Any Colour You Like;Brain Damage;Eclipse', 'Progressive Rock', '1973-03-01'),
('CD_003', 'Michael Jackson', 'Epic Records', 'Wanna Be Startin'' Somethin'';Baby Be Mine;The Girl Is Mine;Thriller;Beat It;Billie Jean;Human Nature;P.Y.T.;The Lady in My Life', 'Pop', '1982-11-30'),
('CD_004', 'AC/DC', 'Atlantic Records', 'Hells Bells;Shoot to Thrill;What Do You Do for Money Honey;Given the Dog a Bone;Let Me Put My Love into You;Back in Black;You Shook Me All Night Long;Have a Drink on Me;Shake a Leg;Rock and Roll Ain''t Noise Pollution', 'Hard Rock', '1980-07-25'),
('CD_005', 'Eagles', 'Asylum Records', 'Hotel California;New Kid in Town;Life in the Fast Lane;Wasted Time;Wasted Time (Reprise);Victim of Love;Pretty Maids All in a Row;Try and Love Again;The Last Resort', 'Classic Rock', '1976-12-08'),
('CD_006', 'Herbert von Karajan, Berlin Philharmonic', 'Deutsche Grammophon', 'Symphony No. 9 in D minor, Op. 125 - I. Allegro ma non troppo;II. Molto vivace;III. Adagio molto e cantabile;IV. Presto - Allegro assai', 'Classical', '1977-01-01'),
('CD_007', 'Miles Davis', 'Columbia Records', 'So What;Freddie Freeloader;Blue in Green;All Blues;Flamenco Sketches', 'Jazz', '1959-08-17'),
('CD_008', 'Various Artists', 'Rock Classics Records', 'Born to Be Wild;Free Bird;Smoke on the Water;Sweet Child O'' Mine;Don''t Stop Believin''', 'Rock', '2020-01-01'),
('CD_009', 'Various Artists', 'Country Gold Records', 'Country Roads;Sweet Caroline;Ring of Fire;I Walk the Line;Jolene', 'Country', '2020-06-01'),
('CD_010', 'Various Artists', 'EDM Records', 'Levels;Animals;Titanium;Wake Me Up;Clarity', 'Electronic', '2021-01-01'),
('CD_011', 'Various Artists', 'Folk Heritage Records', 'This Land Is Your Land;Blowin'' in the Wind;The Times They Are a-Changin''', 'Folk', '2021-03-01'),
('CD_012', 'Various Artists', 'Indie Collective', 'Young Folks;Time to Dance;Mr. Brightside;Somebody Told Me', 'Indie', '2021-06-01'),
('CD_013', 'Various Artists', 'Classical Piano Masters', 'Moonlight Sonata;Für Elise;Clair de Lune;Gymnopédie No. 1', 'Classical', '2021-09-01'),
('CD_014', 'Various Artists', 'Ambient Works', 'Music for Airports;Weightless;An Ending (Ascent);Stars of the Lid', 'Ambient', '2021-12-01');

-- DVD specific data
INSERT INTO DVD (productID, discType, director, runtime_minutes, studio, dvd_language, subtitles, dvd_releaseDate, dvd_genre) VALUES
('DVD_001', 'DVD-Video', 'Francis Ford Coppola', 175, 'Paramount Pictures', 'English', 'English,Spanish,French', '1972-03-24', 'Crime Drama'),
('DVD_002', 'DVD-Video', 'Christopher Nolan', 148, 'Warner Bros.', 'English', 'English,Spanish,French,German', '2010-07-16', 'Sci-Fi Thriller'),
('DVD_003', 'DVD-Video', 'Peter Jackson', 178, 'New Line Cinema', 'English', 'English,Spanish,French,German,Italian', '2001-12-19', 'Fantasy Adventure'),
('DVD_004', 'DVD-Video', 'Quentin Tarantino', 154, 'Miramax Films', 'English', 'English,Spanish,French', '1994-10-14', 'Crime Drama'),
('DVD_005', 'DVD-Video', 'Frank Darabont', 142, 'Columbia Pictures', 'English', 'English,Spanish,French,German', '1994-09-23', 'Drama'),
('DVD_006', 'DVD-Video', 'George Lucas', 121, 'Lucasfilm', 'English', 'English,Spanish,French,German,Japanese', '1977-05-25', 'Sci-Fi Adventure'),
('DVD_007', 'DVD-Video', 'Various Directors', 360, 'Action Studios', 'English', 'English,Spanish', '2020-01-01', 'Action'),
('DVD_008', 'DVD-Video', 'Various Directors', 480, 'Documentary Films', 'English', 'English,Spanish,French', '2020-03-01', 'Documentary'),
('DVD_009', 'DVD-Video', 'Various Directors', 420, 'Drama Productions', 'English', 'English,Spanish,French,German', '2020-06-01', 'Drama'),
('DVD_010', 'DVD-Video', 'Various Directors', 300, 'Romance Pictures', 'English', 'English,Spanish,French', '2020-09-01', 'Romance'),
('DVD_011', 'DVD-Video', 'Various Directors', 450, 'Sci-Fi Studios', 'English', 'English,Spanish,French,German,Japanese', '2020-12-01', 'Science Fiction'),
('DVD_012', 'DVD-Video', 'Various Directors', 390, 'Thriller Productions', 'English', 'English,Spanish,French,German', '2021-03-01', 'Thriller');

-- LP specific data
INSERT INTO LP (productID, artists, recordLabel, tracklist, genre, releaseDate) VALUES
('LP_001', 'The Beatles', 'Apple Records', 'Come Together;Something;Maxwell''s Silver Hammer;Oh! Darling;Octopus''s Garden;I Want You;Here Comes the Sun;Because;You Never Give Me Your Money;Sun King;Mean Mr. Mustard;Polythene Pam;She Came in Through the Bathroom Window;Golden Slumbers;Carry That Weight;The End;Her Majesty', 'Rock', '1969-09-26'),
('LP_002', 'Pink Floyd', 'Harvest Records', 'Speak to Me;Breathe;On the Run;Time;The Great Gig in the Sky;Money;Us and Them;Any Colour You Like;Brain Damage;Eclipse', 'Progressive Rock', '1973-03-01'),
('LP_003', 'Michael Jackson', 'Epic Records', 'Wanna Be Startin'' Somethin'';Baby Be Mine;The Girl Is Mine;Thriller;Beat It;Billie Jean;Human Nature;P.Y.T.;The Lady in My Life', 'Pop', '1982-11-30'),
('LP_004', 'Miles Davis', 'Columbia Records', 'So What;Freddie Freeloader;Blue in Green;All Blues;Flamenco Sketches', 'Jazz', '1959-08-17'),
('LP_005', 'Eagles', 'Asylum Records', 'Hotel California;New Kid in Town;Life in the Fast Lane;Wasted Time;Wasted Time (Reprise);Victim of Love;Pretty Maids All in a Row;Try and Love Again;The Last Resort', 'Classic Rock', '1976-12-08'),
('LP_006', 'Various Artists', 'Jazz Masters', 'Take Five;Round Midnight;Autumn Leaves;All of Me;Summertime', 'Jazz', '2022-01-01'),
('LP_007', 'Various Artists', 'Rock Legends Vinyl', 'Stairway to Heaven;Bohemian Rhapsody;Hotel California;Sweet Child O'' Mine', 'Rock', '2022-03-01'),
('LP_008', 'Various Artists', 'Electronic Masters', 'Oxygène;Popcorn;Blue Monday;Born Slippy', 'Electronic', '2022-06-01');

-- ======================================
-- PAYMENT METHODS
-- ======================================

-- Default payment methods for customers
INSERT INTO PAYMENT_METHOD (paymentMethodID, methodType, userID, isDefault) VALUES
('PM_CUST_001_01', 'CREDIT_CARD', 'USR_CUST_001', 1),
('PM_CUST_001_02', 'DOMESTIC_DEBIT_CARD', 'USR_CUST_001', 0),
('PM_CUST_002_01', 'CREDIT_CARD', 'USR_CUST_002', 1),
('PM_CUST_003_01', 'DOMESTIC_DEBIT_CARD', 'USR_CUST_003', 1),
('PM_CUST_004_01', 'CREDIT_CARD', 'USR_CUST_004', 1),
('PM_CUST_004_02', 'DOMESTIC_DEBIT_CARD', 'USR_CUST_004', 0),
('PM_CUST_006_01', 'CREDIT_CARD', 'USR_CUST_006', 1);

-- Card details for payment methods
INSERT INTO CARD_DETAILS (paymentMethodID, cardholderName, cardNumber_masked, expiryDate_MMYY, validFromDate_MMYY, issuingBank) VALUES
('PM_CUST_001_01', 'John Customer', '****-****-****-1234', '12/26', '01/22', 'Vietcombank'),
('PM_CUST_001_02', 'John Customer', '****-****-****-5678', '08/25', '02/21', 'BIDV'),
('PM_CUST_002_01', 'Jane Customer', '****-****-****-9012', '10/27', '03/23', 'Techcombank'),
('PM_CUST_003_01', 'Customer Three', '****-****-****-3456', '06/26', '05/22', 'VPBank'),
('PM_CUST_004_01', 'John Doe', '****-****-****-7890', '09/28', '01/24', 'ACB'),
('PM_CUST_004_02', 'John Doe', '****-****-****-2468', '11/25', '04/21', 'Sacombank'),
('PM_CUST_006_01', 'Mike Ross', '****-****-****-1357', '03/27', '08/23', 'MB Bank');

-- ======================================
-- CART DATA (Shopping Carts)
-- ======================================

-- Active carts for customers
INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES
('CART_SESS_001', 'USR_CUST_001', '2024-03-25 14:30:00'),
('CART_SESS_002', 'USR_CUST_002', '2024-03-25 16:45:00'),
('CART_SESS_003', 'USR_CUST_004', '2024-03-25 09:15:00'),
('CART_SESS_004', NULL, '2024-03-25 11:20:00'); -- Guest cart

-- Cart items
INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES
-- Customer 1's cart
('CART_SESS_001', 'BOOK_001', 2),
('CART_SESS_001', 'CD_001', 1),
('CART_SESS_001', 'DVD_002', 1),
-- Customer 2's cart
('CART_SESS_002', 'BOOK_003', 1),
('CART_SESS_002', 'LP_001', 1),
-- Customer 4's cart
('CART_SESS_003', 'BOOK_005', 1),
('CART_SESS_003', 'CD_007', 2),
('CART_SESS_003', 'DVD_001', 1),
-- Guest cart
('CART_SESS_004', 'BOOK_002', 3),
('CART_SESS_004', 'CD_003', 1);

-- ======================================
-- ORDER DATA (Complete order workflows)
-- ======================================

-- Orders with various statuses
INSERT INTO ORDER_ENTITY (orderID, userID, orderDate, order_status, totalProductPriceExclVAT, totalProductPriceInclVAT, calculatedDeliveryFee, totalAmountPaid) VALUES
('ORD_001', 'USR_CUST_001', '2024-03-20 10:30:00', 'DELIVERED', 85.97, 94.57, 5.00, 99.57),
('ORD_002', 'USR_CUST_002', '2024-03-21 14:15:00', 'SHIPPING', 65.98, 72.58, 5.00, 77.58),
('ORD_003', 'USR_CUST_003', '2024-03-22 09:45:00', 'APPROVED', 142.96, 157.26, 8.00, 165.26),
('ORD_004', 'USR_CUST_001', '2024-03-23 16:20:00', 'PENDING_PROCESSING', 78.97, 86.87, 5.00, 91.87),
('ORD_005', 'USR_CUST_004', '2024-03-24 11:30:00', 'PAYMENT_FAILED', 124.95, 137.45, 7.00, 0.00),
('ORD_006', 'USR_CUST_002', '2024-03-15 13:45:00', 'DELIVERED', 89.96, 98.96, 5.00, 103.96),
('ORD_007', 'USR_CUST_006', '2024-03-18 08:30:00', 'SHIPPED', 156.93, 172.62, 10.00, 182.62),
('ORD_008', 'USR_CUST_001', '2024-03-25 12:00:00', 'PENDING_PAYMENT', 67.98, 74.78, 5.00, 0.00),
('ORD_009', 'USR_CUST_003', '2024-03-14 15:30:00', 'CANCELLED', 45.99, 50.59, 5.00, 0.00),
('ORD_010', 'USR_CUST_004', '2024-03-19 10:15:00', 'REFUNDED', 112.97, 124.27, 8.00, 132.27),
('ORD_011', NULL, '2024-03-25 17:45:00', 'PENDING_DELIVERY_INFO', 51.97, 57.17, 0.00, 0.00), -- Guest order
('ORD_012', 'USR_CUST_006', '2024-03-22 14:30:00', 'APPROVED', 199.94, 219.93, 12.00, 231.93),
('ORD_013', 'USR_CUST_002', '2024-03-16 09:20:00', 'DELIVERED', 73.98, 81.38, 5.00, 86.38),
('ORD_014', 'USR_CUST_001', '2024-03-17 11:45:00', 'REJECTED', 98.97, 108.87, 6.00, 0.00),
('ORD_015', 'USR_CUST_003', '2024-03-23 13:15:00', 'SHIPPING', 167.94, 184.73, 15.00, 199.73);

-- Order items
INSERT INTO ORDER_ITEM (orderID, productID, quantity, priceAtTimeOfOrder, isEligibleForRushDelivery) VALUES
-- ORD_001 items
('ORD_001', 'BOOK_001', 1, 45.99, 1),
('ORD_001', 'CD_001', 1, 18.99, 1),
('ORD_001', 'DVD_002', 1, 20.99, 0),
-- ORD_002 items
('ORD_002', 'BOOK_003', 2, 15.99, 1),
('ORD_002', 'LP_001', 1, 34.00, 0),
-- ORD_003 items
('ORD_003', 'BOOK_005', 1, 39.99, 1),
('ORD_003', 'CD_007', 2, 23.99, 1),
('ORD_003', 'DVD_001', 1, 19.99, 0),
('ORD_003', 'LP_002', 1, 54.99, 0),
-- ORD_004 items
('ORD_004', 'BOOK_002', 2, 12.99, 1),
('ORD_004', 'CD_003', 1, 19.99, 1),
('ORD_004', 'DVD_005', 1, 32.99, 0),
-- ORD_005 items
('ORD_005', 'BOOK_006', 1, 79.99, 1),
('ORD_005', 'CD_006', 1, 24.99, 1),
('ORD_005', 'DVD_003', 1, 19.97, 0),
-- ORD_006 items
('ORD_006', 'BOOK_004', 2, 14.99, 1),
('ORD_006', 'CD_008', 2, 15.99, 1),
('ORD_006', 'DVD_007', 1, 27.99, 0),
-- ORD_007 items
('ORD_007', 'BOOK_007', 1, 34.99, 1),
('ORD_007', 'CD_009', 2, 14.99, 1),
('ORD_007', 'DVD_008', 2, 19.99, 0),
('ORD_007', 'LP_003', 1, 52.99, 0),
-- ORD_008 items
('ORD_008', 'BOOK_008', 1, 29.99, 1),
('ORD_008', 'CD_010', 1, 16.99, 1),
('ORD_008', 'DVD_009', 1, 20.99, 0),
-- ORD_009 items
('ORD_009', 'BOOK_001', 1, 45.99, 1),
-- ORD_010 items
('ORD_010', 'BOOK_009', 1, 29.99, 1),
('ORD_010', 'CD_011', 2, 18.99, 1),
('ORD_010', 'DVD_010', 2, 17.99, 0),
('ORD_010', 'LP_004', 1, 27.99, 0),
-- ORD_011 items (guest order)
('ORD_011', 'BOOK_002', 3, 12.99, 1),
('ORD_011', 'CD_003', 1, 12.99, 1),
-- ORD_012 items
('ORD_012', 'BOOK_010', 2, 19.99, 1),
('ORD_012', 'CD_012', 3, 17.99, 1),
('ORD_012', 'DVD_011', 2, 26.99, 0),
('ORD_012', 'LP_005', 2, 47.99, 0),
-- ORD_013 items
('ORD_013', 'BOOK_011', 2, 16.99, 1),
('ORD_013', 'CD_013', 1, 19.99, 1),
('ORD_013', 'DVD_012', 1, 20.99, 0),
-- ORD_014 items
('ORD_014', 'BOOK_012', 1, 42.99, 1),
('ORD_014', 'CD_014', 2, 19.99, 1),
('ORD_014', 'DVD_004', 1, 15.99, 0),
-- ORD_015 items
('ORD_015', 'BOOK_006', 1, 79.99, 1),
('ORD_015', 'CD_001', 2, 18.99, 1),
('ORD_015', 'DVD_001', 1, 19.99, 0),
('ORD_015', 'LP_006', 1, 29.99, 0);

-- ======================================
-- DELIVERY INFO (Shipping details)
-- ======================================

INSERT INTO DELIVERY_INFO (deliveryInfoID, orderID, recipientName, email, phoneNumber, deliveryProvinceCity, deliveryAddress, deliveryInstructions, deliveryMethodChosen, requestedRushDeliveryTime) VALUES
('DELIV_001', 'ORD_001', 'John Customer', 'customer1@example.com', '+84901234567', 'Ho Chi Minh City', '123 Nguyen Hue Street, District 1, Ho Chi Minh City', 'Please deliver during business hours', 'STANDARD', NULL),
('DELIV_002', 'ORD_002', 'Jane Customer', 'customer2@example.com', '+84912345678', 'Hanoi', '456 Hoan Kiem Street, Hoan Kiem District, Hanoi', 'Leave with security if not home', 'RUSH', '2024-03-22 10:00:00'),
('DELIV_003', 'ORD_003', 'Customer Three', 'customer3@example.com', '+84923456789', 'Da Nang', '789 Bach Dang Street, Hai Chau District, Da Nang', 'Call before delivery', 'STANDARD', NULL),
('DELIV_004', 'ORD_004', 'John Customer', 'customer1@example.com', '+84901234567', 'Ho Chi Minh City', '123 Nguyen Hue Street, District 1, Ho Chi Minh City', 'Ring doorbell twice', 'RUSH', '2024-03-24 14:00:00'),
('DELIV_006', 'ORD_006', 'Jane Customer', 'customer2@example.com', '+84912345678', 'Hanoi', '456 Hoan Kiem Street, Hoan Kiem District, Hanoi', 'Deliver to office reception', 'STANDARD', NULL),
('DELIV_007', 'ORD_007', 'Mike Ross', 'mike.ross@example.com', '+84934567890', 'Can Tho', '321 Ninh Kieu Street, Ninh Kieu District, Can Tho', 'Fragile items - handle with care', 'RUSH', '2024-03-19 09:00:00'),
('DELIV_010', 'ORD_010', 'John Doe', 'john.doe@example.com', '+84945678901', 'Nha Trang', '654 Tran Phu Street, Nha Trang City', 'Refund delivery - original address', 'STANDARD', NULL),
('DELIV_012', 'ORD_012', 'Mike Ross', 'mike.ross@example.com', '+84934567890', 'Can Tho', '321 Ninh Kieu Street, Ninh Kieu District, Can Tho', 'Large order - please coordinate delivery time', 'STANDARD', NULL),
('DELIV_013', 'ORD_013', 'Jane Customer', 'customer2@example.com', '+84912345678', 'Hanoi', '456 Hoan Kiem Street, Hoan Kiem District, Hanoi', 'Weekend delivery preferred', 'STANDARD', NULL),
('DELIV_015', 'ORD_015', 'Customer Three', 'customer3@example.com', '+84923456789', 'Da Nang', '789 Bach Dang Street, Hai Chau District, Da Nang', 'Express delivery needed', 'RUSH', '2024-03-24 08:00:00');

-- ======================================
-- INVOICES (Order invoices)
-- ======================================

INSERT INTO INVOICE (invoiceID, orderID, invoiceDate, invoicedTotalAmount) VALUES
('INV_001', 'ORD_001', '2024-03-20 10:35:00', 99.57),
('INV_002', 'ORD_002', '2024-03-21 14:20:00', 77.58),
('INV_003', 'ORD_003', '2024-03-22 09:50:00', 165.26),
('INV_004', 'ORD_004', '2024-03-23 16:25:00', 91.87),
('INV_006', 'ORD_006', '2024-03-15 13:50:00', 103.96),
('INV_007', 'ORD_007', '2024-03-18 08:35:00', 182.62),
('INV_010', 'ORD_010', '2024-03-19 10:20:00', 132.27), -- For refunded order
('INV_012', 'ORD_012', '2024-03-22 14:35:00', 231.93),
('INV_013', 'ORD_013', '2024-03-16 09:25:00', 86.38),
('INV_015', 'ORD_015', '2024-03-23 13:20:00', 199.73);

-- ======================================
-- PAYMENT TRANSACTIONS (Payment history)
-- ======================================

INSERT INTO PAYMENT_TRANSACTION (transactionID, orderID, paymentMethodID, transactionType, externalTransactionID, transaction_status, transactionDateTime, amount, transactionContent) VALUES
-- Successful payments
('TXN_001', 'ORD_001', 'PM_CUST_001_01', 'PAYMENT', 'VNPAY_001_20240320', 'SUCCESS', '2024-03-20 10:32:00', 99.57, 'Payment for order ORD_001 - Books, CDs, DVDs'),
('TXN_002', 'ORD_002', 'PM_CUST_002_01', 'PAYMENT', 'VNPAY_002_20240321', 'SUCCESS', '2024-03-21 14:18:00', 77.58, 'Payment for order ORD_002 - Books, LPs'),
('TXN_003', 'ORD_003', 'PM_CUST_003_01', 'PAYMENT', 'VNPAY_003_20240322', 'SUCCESS', '2024-03-22 09:48:00', 165.26, 'Payment for order ORD_003 - Mixed media products'),
('TXN_006', 'ORD_006', 'PM_CUST_002_01', 'PAYMENT', 'VNPAY_006_20240315', 'SUCCESS', '2024-03-15 13:48:00', 103.96, 'Payment for order ORD_006 - Books and CDs'),
('TXN_007', 'ORD_007', 'PM_CUST_006_01', 'PAYMENT', 'VNPAY_007_20240318', 'SUCCESS', '2024-03-18 08:33:00', 182.62, 'Payment for order ORD_007 - Large mixed order'),
('TXN_010_PAY', 'ORD_010', 'PM_CUST_004_01', 'PAYMENT', 'VNPAY_010_20240319', 'SUCCESS', '2024-03-19 10:18:00', 132.27, 'Payment for order ORD_010 - Books, CDs, DVDs, LPs'),
('TXN_012', 'ORD_012', 'PM_CUST_006_01', 'PAYMENT', 'VNPAY_012_20240322', 'SUCCESS', '2024-03-22 14:33:00', 231.93, 'Payment for order ORD_012 - Premium mixed collection'),
('TXN_013', 'ORD_013', 'PM_CUST_002_01', 'PAYMENT', 'VNPAY_013_20240316', 'SUCCESS', '2024-03-16 09:23:00', 86.38, 'Payment for order ORD_013 - Books and CDs'),
('TXN_015', 'ORD_015', 'PM_CUST_003_01', 'PAYMENT', 'VNPAY_015_20240323', 'SUCCESS', '2024-03-23 13:18:00', 199.73, 'Payment for order ORD_015 - Express delivery order'),

-- Failed payments
('TXN_005_FAIL', 'ORD_005', 'PM_CUST_004_01', 'PAYMENT', 'VNPAY_005_20240324', 'FAILED', '2024-03-24 11:33:00', 144.45, 'Failed payment for order ORD_005 - Insufficient funds'),
('TXN_004_PEND', 'ORD_004', 'PM_CUST_001_01', 'PAYMENT', 'VNPAY_004_20240323', 'PENDING', '2024-03-23 16:23:00', 91.87, 'Pending payment for order ORD_004 - Awaiting confirmation'),
('TXN_014_FAIL', 'ORD_014', 'PM_CUST_001_01', 'PAYMENT', 'VNPAY_014_20240317', 'FAILED', '2024-03-17 11:48:00', 114.87, 'Failed payment for order ORD_014 - Payment declined'),

-- Refund transaction
('TXN_010_REF', 'ORD_010', 'PM_CUST_004_01', 'REFUND', 'VNPAY_REF_010_20240320', 'SUCCESS', '2024-03-20 15:30:00', 132.27, 'Refund for order ORD_010 - Customer requested cancellation');

-- ======================================
-- PRODUCT MANAGER AUDIT LOG
-- ======================================

INSERT INTO PRODUCT_MANAGER_AUDIT_LOG (auditLogID, managerId, operationType, productId, operationDateTime, details) VALUES
('AUDIT_001', 'USR_PM_001', 'ADD', 'BOOK_012', '2024-03-10 09:30:00', 'Added new book: Digital Art Mastery'),
('AUDIT_002', 'USR_PM_002', 'PRICE_UPDATE', 'CD_001', '2024-03-12 14:15:00', 'Updated price from $17.99 to $18.99 for Abbey Road CD'),
('AUDIT_003', 'USR_PM_001', 'UPDATE', 'DVD_002', '2024-03-14 11:20:00', 'Updated description and subtitle information for Inception DVD'),
('AUDIT_004', 'USR_PM_002', 'ADD', 'LP_008', '2024-03-16 16:45:00', 'Added new LP: Electronic Anthology (Vinyl)'),
('AUDIT_005', 'USR_PM_001', 'PRICE_UPDATE', 'BOOK_006', '2024-03-18 10:30:00', 'Updated price from $74.99 to $79.99 for Mathematics textbook'),
('AUDIT_006', 'USR_PM_002', 'UPDATE', 'CD_014', '2024-03-20 13:45:00', 'Updated tracklist for Ambient Soundscapes CD'),
('AUDIT_007', 'USR_PM_001', 'ADD', 'BOOK_011', '2024-03-22 09:15:00', 'Added new book: Poetry for Beginners'),
('AUDIT_008', 'USR_PM_002', 'PRICE_UPDATE', 'DVD_006', '2024-03-24 15:30:00', 'Updated price from $25.99 to $27.99 for Star Wars DVD'),
('AUDIT_009', 'USR_PM_001', 'UPDATE', 'LP_006', '2024-03-25 11:00:00', 'Updated description for Classic Jazz Collection vinyl'),
('AUDIT_010', 'USR_PM_002', 'ADD', 'CD_013', '2024-03-25 14:20:00', 'Added new CD: Piano Masterpieces');

-- ======================================
-- SUMMARY STATISTICS
-- ======================================

-- Total products: 36 (12 Books, 14 CDs, 12 DVDs, 8 LPs)
-- Total users: 10 (2 Admins, 2 Product Managers, 6 Customers)
-- Total orders: 15 (various statuses covering complete workflows)
-- Total transactions: 13 (10 successful payments, 2 failed, 1 refund)
-- Complete customer journey data from browsing to delivery
-- Comprehensive test data for all application features

PRAGMA foreign_keys = OFF;
