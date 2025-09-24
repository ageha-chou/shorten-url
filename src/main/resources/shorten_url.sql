create database if not exists shorten_url;
create database if not exists shorten_url_test;

use shorten_url;

SET FOREIGN_KEY_CHECKS = 0;
drop table users;
drop table auth_provider;
drop table url_visit;
drop table url_info;
SET FOREIGN_KEY_CHECKS = 1;

create table users (
   id bigint auto_increment,
   email varchar(255),
   username varchar(50),
   password varchar(255),
   first_name varchar(50),
   last_name varchar(50),
   status varchar(2),
   created_datetime timestamp,
   updated_datetime timestamp,
   primary key (id),
   unique index uidx_users_username(username),
   unique index uidx_users_email(email)
);

create table auth_provider (
   id bigint auto_increment,
   user_id bigint,
   provider_type varchar(50) not null,
   provider_user_id varchar(255),
   created_datetime timestamp,
   last_access_datetime timestamp,
   primary key (id),
   constraint fk_auth_provider_users foreign key (user_id) references users(id),
   unique index uidx_auth_provider_provider_user_id_provider_type(provider_user_id, provider_type)
);

create table url_info (
	id bigint,
	short_code varchar(50) not null,
	original_url varchar(2048),
	is_alias bit,
	status varchar(1),
    created_by bigint,
    created_by_ip varchar(30),
    created_by_user_agent varchar(512),
	created_datetime timestamp,
	last_access_datetime timestamp,
	primary key (id),
    constraint fk_url_info_users foreign key (created_by) references users(id),
    unique index uidx_url_info_original_url(short_url)
);

create table url_visit (
	id bigint auto_increment,
	shorten_url_id bigint,
	visited_datetime timestamp,
	user_agent varchar(512),
	ip_address varchar(30),
	country varchar(50),
	primary key (id),
	constraint fk_url_visit_url_info foreign key (shorten_url_id) references url_info(id)
);