create database shorten_url;

use shorten_url;

create table url_info (
	id bigint,
	short_url varchar(11),
	original_url varchar(2048),
	is_alias bit,
	status varchar(1),
	created_by varchar(50),
	created_datetime timestamp,
	last_access_datetime timestamp,
	primary key (id),
	index idx_url_info_original_url(short_url)
);

create table url_visit (
	id bigint auto_increment,
	shorten_url_id bigint,
	visited_datetime timestamp,
	user_agent varchar(50),
	ip_address varchar(30),
	country varchar(50),
	primary key (id),
	constraint fk_url_visit_url_info foreign key (shorten_url_id) references url_info(id)
);