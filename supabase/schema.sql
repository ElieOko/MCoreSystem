-- ============================================================
-- MCoreSystem — Schéma Supabase (PostgreSQL) + politiques RLS
-- À exécuter dans l'éditeur SQL du projet Supabase.
--
-- Configuration côté application (jamais dans le code source) :
--   local.properties (ou variables d'environnement CI) :
--     SUPABASE_URL=https://<votre-projet>.supabase.co
--     SUPABASE_ANON_KEY=<clé anonyme publique>
-- ============================================================

-- ---------- Tables ----------

create table if not exists public.organisms (
    uuid uuid primary key default gen_random_uuid(),
    name text,
    updated_at timestamptz not null default now()
);

-- Profil applicatif : uuid = auth.users.id
create table if not exists public.profiles (
    uuid uuid primary key references auth.users (id) on delete cascade,
    organism_uuid uuid not null references public.organisms (uuid) on delete cascade,
    username text not null,
    email text,
    phone text,
    role text not null default 'MEMBER', -- ADMIN | MEMBER
    updated_at timestamptz not null default now()
);

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
    user_uuid uuid references public.profiles (uuid) on delete set null,
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

create index if not exists idx_currencies_org on public.currencies (organism_uuid);
create index if not exists idx_payment_methods_org on public.payment_methods (organism_uuid);
create index if not exists idx_type_categories_org on public.type_categories (organism_uuid);
create index if not exists idx_categories_org on public.categories (organism_uuid);
create index if not exists idx_operations_org on public.operations (organism_uuid);
create index if not exists idx_operations_status on public.operations (organism_uuid, status);

-- ---------- Fonction utilitaire RLS ----------
-- Retourne l'organisation de l'utilisateur connecté.
create or replace function public.current_organism_uuid()
returns uuid
language sql
security definer
stable
as $$
    select organism_uuid from public.profiles where uuid = auth.uid()
$$;

-- ---------- Row Level Security ----------
-- Isolation stricte : chaque utilisateur n'accède qu'aux données de SON organisation.

alter table public.organisms enable row level security;
alter table public.profiles enable row level security;
alter table public.currencies enable row level security;
alter table public.payment_methods enable row level security;
alter table public.type_categories enable row level security;
alter table public.categories enable row level security;
alter table public.operations enable row level security;

-- Organisations : lecture de la sienne ; création libre (inscription) ;
-- modification réservée aux administrateurs.
drop policy if exists organisms_select on public.organisms;
create policy organisms_select on public.organisms
    for select using (uuid = public.current_organism_uuid());

drop policy if exists organisms_insert on public.organisms;
create policy organisms_insert on public.organisms
    for insert with check (auth.uid() is not null);

drop policy if exists organisms_update on public.organisms;
create policy organisms_update on public.organisms
    for update using (
        uuid = public.current_organism_uuid()
        and exists (select 1 from public.profiles p where p.uuid = auth.uid() and p.role = 'ADMIN')
    );

-- Profils : chacun voit les profils de son organisation, ne modifie que le sien.
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
    for select using (organism_uuid = public.current_organism_uuid() or uuid = auth.uid());

drop policy if exists profiles_insert on public.profiles;
create policy profiles_insert on public.profiles
    for insert with check (uuid = auth.uid());

drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles
    for update using (uuid = auth.uid());

-- Données métier : CRUD limité à l'organisation de l'utilisateur.
do $$
declare
    t text;
begin
    foreach t in array array['currencies','payment_methods','type_categories','categories','operations']
    loop
        execute format('drop policy if exists %I_select on public.%I', t, t);
        execute format(
            'create policy %I_select on public.%I for select using (organism_uuid = public.current_organism_uuid())', t, t);

        execute format('drop policy if exists %I_insert on public.%I', t, t);
        execute format(
            'create policy %I_insert on public.%I for insert with check (organism_uuid = public.current_organism_uuid())', t, t);

        execute format('drop policy if exists %I_update on public.%I', t, t);
        execute format(
            'create policy %I_update on public.%I for update using (organism_uuid = public.current_organism_uuid())', t, t);

        execute format('drop policy if exists %I_delete on public.%I', t, t);
        execute format(
            'create policy %I_delete on public.%I for delete using (organism_uuid = public.current_organism_uuid())', t, t);
    end loop;
end $$;

-- ---------- Realtime (facultatif) ----------
-- alter publication supabase_realtime add table public.operations;
-- alter publication supabase_realtime add table public.categories;
-- alter publication supabase_realtime add table public.type_categories;
-- alter publication supabase_realtime add table public.currencies;
-- alter publication supabase_realtime add table public.payment_methods;
