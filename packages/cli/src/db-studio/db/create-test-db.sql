-- Test Database for DB Studio Playwright Tests
-- This creates a simple database with known schema and data for testing

-- Users table
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  price REAL NOT NULL,
  stock INTEGER DEFAULT 0
);

-- Orders table (with foreign keys)
CREATE TABLE orders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Seed data for users
INSERT INTO users (name, email) VALUES
  ('Alice Johnson', 'alice@example.com'),
  ('Bob Smith', 'bob@example.com'),
  ('Charlie Brown', 'charlie@example.com'),
  ('Diana Prince', 'diana@example.com'),
  ('Eve Wilson', 'eve@example.com');

-- Seed data for products
INSERT INTO products (name, price, stock) VALUES
  ('Laptop', 999.99, 10),
  ('Mouse', 29.99, 50),
  ('Keyboard', 79.99, 30),
  ('Monitor', 299.99, 15),
  ('Headphones', 149.99, 25);

-- Seed data for orders
INSERT INTO orders (user_id, product_id, quantity) VALUES
  (1, 1, 1),  -- Alice ordered 1 Laptop
  (2, 2, 2),  -- Bob ordered 2 Mice
  (3, 3, 1),  -- Charlie ordered 1 Keyboard
  (1, 4, 1),  -- Alice ordered 1 Monitor
  (4, 5, 1);  -- Diana ordered 1 Headphones
