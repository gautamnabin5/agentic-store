CREATE TABLE order_items (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID           NOT NULL REFERENCES orders (id),
    product_id UUID           NOT NULL REFERENCES products (id),
    quantity   INT            NOT NULL CONSTRAINT order_items_quantity_positive CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL CONSTRAINT order_items_unit_price_positive CHECK (unit_price > 0)
);
