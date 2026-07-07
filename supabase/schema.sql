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
    role text not null default 'MEMBER', -- ADMIN | MEMBER
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
