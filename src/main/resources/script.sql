create table user_sec (
id serial not null,
name varchar(100) not null,
username varchar(255) not null,
password varchar(255) not null,
authorities varchar(255) not null
)

select * from user_sec

insert into user_sec (id, name, username, password, authorities) values (1, 'Fabricio', 'fabricio', '{bcrypt}$2a$10$xOPqQwnMYSQTuAZhbwjGyeB3wonirlvMnSe33Fc2qF74qJcI1siqC', 'ROLE_ADMIN');
insert into user_sec (id, name, username, password, authorities) values (2, 'Lucas', 'lucas', '{bcrypt}$2a$10$xOPqQwnMYSQTuAZhbwjGyeB3wonirlvMnSe33Fc2qF74qJcI1siqC', 'ROLE_USER');
commit;