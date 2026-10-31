-- ============================================================
-- MCoreSystem — Schéma Supabase (PostgreSQL) — SANS RLS
--
-- Modèle simplifié :
--   * PAS de politiques Row Level Security
--   * PAS d'utilisation de Supabase Auth (auth.users)
--   * Les comptes vivent dans la table applicative `users`
--     (seul le hash SHA-256 du mot de passe est stocké)
--
-- À exécuter dans l'éditeur SQL du projet Supabase.
--
-- Configuration côté application (jamais dans le code source) :
--   local.properties :
--     SUPABASE_URL=https://<votre-projet>.supabase.co
--     SUPABASE_PUBLISHABLE_KEY=<clé publiable sb_publishable_...>
-- ============================================================

-- ---------- Nettoyage de l'ancienne version (idempotent) ----------

-- Supprime toutes les politiques RLS existantes du schéma public.
do $$
declare
    pol record;
begin
    for pol in
        select schemaname, tablename, policyname
        from pg_policies
        where schemaname = 'public'
    loop
        execute format('drop policy if exists %I on %I.%I', pol.policyname, pol.schemaname, pol.tablename);
    end loop;
end $$;

drop function if exists public.current_organism_uuid();
drop table if exists public.profiles;

-- ---------- Tables ----------

create table if not exists public.organisms (
    uuid uuid primary key default gen_random_uuid(),
    name text,
    updated_at timestamptz not null default now()
);

-- Table utilisateurs applicative : indépendante de Supabase Auth.
create table if not exists public.users (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    username text not null default '',
    email text,
    phone text,
    role text not null default 'MEMBER', -- ADMIN | MEMBER | LIVREUR
    password_hash text,                  -- SHA-256, jamais le mot de passe en clair
    updated_at timestamptz not null default now()
);

alter table public.users add column if not exists password_hash text;

create table if not exists public.currencies (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    code text not null default '',
    symbol text not null default '',
    updated_at timestamptz not null default now()
);

create table if not exists public.payment_methods (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    updated_at timestamptz not null default now()
);

create table if not exists public.type_categories (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    description text not null default '',
    is_active boolean not null default true,
    updated_at timestamptz not null default now()
);

create table if not exists public.categories (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    type_category_uuid uuid references public.type_categories (uuid) on delete set null,
    name text not null default '',
    description text not null default '',
    updated_at timestamptz not null default now()
);

create table if not exists public.operations (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    category_uuid uuid references public.categories (uuid) on delete set null,
    user_uuid uuid references public.users (uuid) on delete set null,
    payment_method_uuid uuid references public.payment_methods (uuid) on delete set null,
    currency_uuid uuid references public.currencies (uuid) on delete set null,
    amount double precision not null default 0,
    task_name text not null default '',
    description text not null default '',
    created_on text not null default '',
    is_active boolean not null default true,
    status text not null default 'OUVERT', -- OUVERT | EN_ATTENTE | CLOTURE
    updated_at timestamptz not null default now()
);

-- ---------- Index ----------

create index if not exists idx_users_org on public.users (organism_uuid);
create index if not exists idx_currencies_org on public.currencies (organism_uuid);
create index if not exists idx_payment_methods_org on public.payment_methods (organism_uuid);
create index if not exists idx_type_categories_org on public.type_categories (organism_uuid);
create index if not exists idx_categories_org on public.categories (organism_uuid);
create index if not exists idx_operations_org on public.operations (organism_uuid);
create index if not exists idx_operations_status on public.operations (organism_uuid, status);

-- ---------- Désactivation de RLS sur toutes les tables ----------

alter table public.organisms disable row level security;
alter table public.users disable row level security;
alter table public.currencies disable row level security;
alter table public.payment_methods disable row level security;
alter table public.type_categories disable row level security;
alter table public.categories disable row level security;
alter table public.operations disable row level security;

-- ---------- Realtime (facultatif) ----------
-- alter publication supabase_realtime add table public.operations;
-- alter publication supabase_realtime add table public.categories;

-- ---------- Module stock, livreurs et commandes ----------
-- Même script que supabase/stock_migration.sql, pour une installation neuve.

create table if not exists public.stock_products (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    unit text not null default 'pièce',
    central_quantity double precision not null default 0,
    unit_price double precision not null default 0,
    updated_at timestamptz not null default now()
);

create table if not exists public.stock_livreurs (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    phone text not null default '',
    user_uuid text,
    updated_at timestamptz not null default now()
);

create table if not exists public.stock_accounts (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    livreur_uuid uuid references public.stock_livreurs (uuid) on delete cascade,
    username text not null,
    password_hash text,
    role text not null default 'LIVREUR', -- OWNER | LIVREUR
    updated_at timestamptz not null default now(),
    unique (organism_uuid, username)
);

create table if not exists public.mini_stocks (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    livreur_uuid uuid not null references public.stock_livreurs (uuid) on delete cascade,
    product_uuid uuid not null references public.stock_products (uuid) on delete cascade,
    quantity double precision not null default 0,
    updated_at timestamptz not null default now(),
    unique (livreur_uuid, product_uuid)
);

create table if not exists public.stock_orders (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    livreur_uuid uuid references public.stock_livreurs (uuid) on delete set null,
    customer_name text not null default '',
    status text not null default 'BROUILLON', -- BROUILLON | ASSIGNEE | EN_LIVRAISON | LIVREE | ANNULEE
    note text not null default '',
    created_on text not null default '',
    updated_at timestamptz not null default now()
);

create table if not exists public.stock_order_lines (
    uuid uuid primary key default gen_random_uuid(),
    order_uuid uuid not null references public.stock_orders (uuid) on delete cascade,
    product_uuid uuid not null references public.stock_products (uuid) on delete restrict,
    quantity_ordered double precision not null default 0,
    quantity_delivered double precision not null default 0,
    updated_at timestamptz not null default now()
);

create table if not exists public.stock_movements (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    type text not null, -- ENTREE | RETRAIT | DOTATION | RETOUR | SORTIE
    product_uuid uuid references public.stock_products (uuid) on delete set null,
    product_name text not null default '',
    livreur_uuid uuid references public.stock_livreurs (uuid) on delete set null,
    livreur_name text,
    order_uuid uuid references public.stock_orders (uuid) on delete set null,
    quantity double precision not null default 0,
    note text not null default '',
    created_on text not null default '',
    updated_at timestamptz not null default now()
);

create index if not exists idx_stock_products_org on public.stock_products (organism_uuid);
create index if not exists idx_stock_livreurs_org on public.stock_livreurs (organism_uuid);
create index if not exists idx_stock_accounts_org on public.stock_accounts (organism_uuid);
create index if not exists idx_mini_stocks_livreur on public.mini_stocks (livreur_uuid);
create index if not exists idx_stock_orders_org on public.stock_orders (organism_uuid, status);
create index if not exists idx_stock_order_lines_order on public.stock_order_lines (order_uuid);
create index if not exists idx_stock_movements_org on public.stock_movements (organism_uuid, created_on);
create index if not exists idx_stock_movements_order on public.stock_movements (order_uuid);

alter table public.stock_products disable row level security;
alter table public.stock_livreurs disable row level security;
alter table public.stock_accounts disable row level security;
alter table public.mini_stocks disable row level security;
alter table public.stock_orders disable row level security;
alter table public.stock_order_lines disable row level security;
alter table public.stock_movements disable row level security;

alter table public.stock_products add column if not exists unit_price double precision not null default 0;
alter table public.stock_livreurs add column if not exists user_uuid text;
