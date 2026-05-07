CREATE TABLE products (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          NUMERIC(10, 2) NOT NULL CONSTRAINT products_price_positive CHECK (price > 0),
    stock_quantity INT            NOT NULL DEFAULT 0
                                  CONSTRAINT products_stock_non_negative CHECK (stock_quantity >= 0),
    active         BOOLEAN        NOT NULL DEFAULT true,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now()
);
