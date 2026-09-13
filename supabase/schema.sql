-- MyHealth Appointment — run this once in Supabase: SQL Editor -> New query -> Run
-- Login uses email + password stored in the users table.
-- There is NO email confirmation / verification step.

create table if not exists public.departments (
    id bigint generated always as identity primary key,
    name text not null unique
);

create table if not exists public.doctors (
    id bigint generated always as identity primary key,
    name text not null,
    department_id bigint not null references public.departments (id) on delete cascade,
    room text,
    rating numeric(2, 1) not null default 4.5
);

create table if not exists public.users (
    id bigint generated always as identity primary key,
    full_name text not null,
    phone text not null,
    email text not null unique,
    date_of_birth date,
    password text not null,
    role text not null default 'patient'
);

create table if not exists public.time_slots (
    id bigint generated always as identity primary key,
    doctor_id bigint not null references public.doctors (id) on delete cascade,
    slot_date date not null,
    slot_time time not null,
    is_booked boolean not null default false
);

create table if not exists public.appointments (
    id bigint generated always as identity primary key,
    user_id bigint not null references public.users (id) on delete cascade,
    doctor_id bigint not null references public.doctors (id) on delete cascade,
    slot_id bigint not null references public.time_slots (id) on delete cascade,
    reason text,
    status text not null default 'confirmed'
);

-- Student demo: allow the Android app to read/write with the anon public key
alter table public.departments disable row level security;
alter table public.doctors disable row level security;
alter table public.users disable row level security;
alter table public.time_slots disable row level security;
alter table public.appointments disable row level security;

grant select, insert, update, delete on public.departments to anon, authenticated;
grant select, insert, update, delete on public.doctors to anon, authenticated;
grant select, insert, update, delete on public.users to anon, authenticated;
grant select, insert, update, delete on public.time_slots to anon, authenticated;
grant select, insert, update, delete on public.appointments to anon, authenticated;

-- Seed departments
insert into public.departments (name) values
    ('General Medicine'),
    ('Cardiology'),
    ('Dermatology'),
    ('Pediatrics')
on conflict (name) do nothing;

-- Seed doctors (including the Figma sample: Dr. Khalid Al-Rashid)
insert into public.doctors (name, department_id, room, rating)
select v.name, d.id, v.room, v.rating
from (
    values
        ('Dr. Omar Nasser', 'General Medicine', 'Room 101', 4.6),
        ('Dr. Layla Ibrahim', 'General Medicine', 'Room 105', 4.7),
        ('Dr. Khalid Al-Rashid', 'Cardiology', 'Room 204', 4.9),
        ('Dr. Amina Hassan', 'Cardiology', 'Room 210', 4.8),
        ('Dr. Sara Mahmoud', 'Dermatology', 'Room 302', 4.5),
        ('Dr. Yusuf Ali', 'Pediatrics', 'Room 401', 4.8)
) as v(name, dept, room, rating)
join public.departments d on d.name = v.dept
where not exists (select 1 from public.doctors existing where existing.name = v.name);

-- Seed available weekday slots for the next 14 days
insert into public.time_slots (doctor_id, slot_date, slot_time, is_booked)
select
    d.id,
    gs::date,
    t.slot_time,
    false
from public.doctors d
cross join generate_series(current_date, current_date + 14, interval '1 day') as gs
cross join (
    values
        ('09:00:00'::time),
        ('10:30:00'::time),
        ('14:00:00'::time),
        ('15:30:00'::time)
) as t(slot_time)
where extract(dow from gs) between 1 and 5
  and not exists (
      select 1
      from public.time_slots s
      where s.doctor_id = d.id
        and s.slot_date = gs::date
        and s.slot_time = t.slot_time
  );
