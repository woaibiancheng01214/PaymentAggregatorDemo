-- Insert sample merchants
INSERT INTO merchants (id, business_name, country, created_at, last_modified_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'Demo Merchant US', 'US', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440002', 'Demo Merchant UK', 'GB', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440003', 'Demo Merchant DE', 'DE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample customers
INSERT INTO customers (id, request_id, email, name, country, address_line1, address_city, address_state, address_country, address_postal_code, created_at, last_modified_at) VALUES
('650e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440011', 'john.doe@example.com', 'John Doe', 'US', '123 Main St', 'New York', 'NY', 'US', '10001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('650e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440012', 'jane.smith@example.com', 'Jane Smith', 'GB', '456 High St', 'London', '', 'GB', 'SW1A 1AA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('650e8400-e29b-41d4-a716-446655440003', '650e8400-e29b-41d4-a716-446655440013', 'hans.mueller@example.com', 'Hans Mueller', 'DE', 'Hauptstraße 789', 'Berlin', '', 'DE', '10115', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert initial routing configuration
INSERT INTO routing_config (id, config_key, config_value, created_at, last_modified_at) VALUES
('750e8400-e29b-41d4-a716-446655440001', 'providers', 
'[
  {
    "name": "StripeMock",
    "currencies": ["USD", "EUR"],
    "countries": ["US", "GB", "DE"],
    "networks": ["VISA", "MASTERCARD"],
    "baseFeeBps": 300,
    "enabled": true
  },
  {
    "name": "AdyenMock", 
    "currencies": ["USD", "EUR", "GBP"],
    "countries": ["US", "NL", "GB", "DE"],
    "networks": ["VISA", "MASTERCARD", "AMEX"],
    "baseFeeBps": 290,
    "enabled": true
  },
  {
    "name": "LocalBankMock",
    "currencies": ["USD"],
    "countries": ["US"],
    "networks": ["VISA", "MASTERCARD"],
    "baseFeeBps": 220,
    "enabled": true
  }
]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('750e8400-e29b-41d4-a716-446655440002', 'routing_rules',
'[
  {
    "condition": {"country": "US", "network": "AMEX"},
    "action": {"prefer": ["AdyenMock"], "mode": "PREFERRED"}
  },
  {
    "condition": {"binRange": "411111-411119"},
    "action": {"prefer": ["LocalBankMock"], "mode": "STRICT"}
  }
]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('750e8400-e29b-41d4-a716-446655440003', 'routing_strategies',
'{"cost": true, "weight": true, "health": true}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('750e8400-e29b-41d4-a716-446655440004', 'routing_weights',
'{"StripeMock": 60, "AdyenMock": 30, "LocalBankMock": 10}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('750e8400-e29b-41d4-a716-446655440005', 'fx_rates',
'[
  {"from": "USD", "to": "EUR", "rate": 0.90},
  {"from": "EUR", "to": "USD", "rate": 1.11},
  {"from": "USD", "to": "GBP", "rate": 0.78},
  {"from": "GBP", "to": "USD", "rate": 1.28}
]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
