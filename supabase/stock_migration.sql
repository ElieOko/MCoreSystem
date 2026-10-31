-- ============================================================
-- MCoreSystem — Migration du module stock / livreurs / commandes
--
-- À exécuter dans l'éditeur SQL Supabase sur une base qui possède
-- déjà public.organisms (voir schema.sql).
-- Le script est idempotent : on peut le relancer.
--
-- Comptes du module (table stock_accounts), distincts de public.users :
--   OWNER   propriétaire de l'organisation, suit tout le stock et l'historique
--   LIVREUR ne voit que son mini-stock et enregistre les sorties de livraison
--
-- Commandes :
--   BROUILLON | ASSIGNEE | EN_LIVRAISON | LIVREE | ANNULEE
-- Les lignes prévoient quantity_ordered et quantity_delivered pour
-- rattacher chaque sortie de livraison à une commande.
-- ============================================================

create table if not exists public.stock_products (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    unit text not null default 'pièce',
    central_quantity double precision not null default 0,
    updated_at timestamptz not null default now()
);

create table if not exists public.stock_livreurs (
    uuid uuid primary key default gen_random_uuid(),
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    name text not null default '',
    phone text not null default '',
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
