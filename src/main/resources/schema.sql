CREATE TABLE shop_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_name TEXT NOT NULL,
    gst_number TEXT,
    address TEXT,
    phone_number TEXT,
    email TEXT,
    logo_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE printer_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    printer_name TEXT NOT NULL,
    paper_width INTEGER, -- in mm
    paper_height INTEGER, -- in mm
    is_default BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sticker_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_name TEXT NOT NULL,
    supplier_name TEXT,
    price DECIMAL(10,2),
    quantity INTEGER,
    printed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);