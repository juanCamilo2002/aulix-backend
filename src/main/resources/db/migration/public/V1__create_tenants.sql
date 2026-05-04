CREATE TABLE IF NOT EXISTS tenants (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug              VARCHAR(63)  UNIQUE NOT NULL,
    name              VARCHAR(255) NOT NULL,
    custom_domain     VARCHAR(255),
    brand_color       VARCHAR(7)   DEFAULT '#6366F1',
    logo_url          TEXT,
    settings          JSONB        DEFAULT '{}',
    stripe_account_id VARCHAR(255),
    active            BOOLEAN      DEFAULT TRUE,
    created_at        TIMESTAMP    DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_tenants_slug   ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenants_domain ON tenants(custom_domain);