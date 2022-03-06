drop table if exists "user";
create table "user" (
  user_id serial primary key,
  username varchar not null,
  email varchar not null,
  pw_hash varchar not null
);

drop table if exists follower;
create table follower (
  who_id integer,
  whom_id integer
);

drop table if exists message;
create table message (
  message_id serial primary key,
  author_id integer not null,
  text varchar not null,
  pub_date integer,
  flagged integer
);