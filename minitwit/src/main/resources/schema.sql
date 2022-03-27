create table if not exists "user" (
  user_id serial primary key,
  username varchar not null,
  email varchar not null,
  pw_hash varchar not null
);

create table if not exists follower (
  who_id integer,
  whom_id integer
);

create table if not exists message (
  message_id serial primary key,
  author_id integer not null,
  text varchar not null,
  pub_date integer,
  flagged integer
);