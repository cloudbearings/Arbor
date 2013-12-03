/* defines region captured by a block
   note that this is used for listing
   blocks and for relating data points
*/
CREATE TABLE block (
	id integer primary key,
	longitudeA float not null,
	latitudeA float not null,
	longitudeB float not null,
	latitudeB float not null,
	x integer not null,
	y integer not null,
	last_updated datetime default CURRENT_TIMESTAMP,
	unique(x, y)
);

-- defines specific data points for a block
CREATE TABLE block_data (
	id integer primary key,
	block_id integer,
	name varchar(128) not null,
	longitude float not null,
	latitude float not null,
	altitude float,
	last_updated datetime default CURRENT_TIMESTAMP,
	FOREIGN KEY(block_id) REFERENCES block(id)
);

-- defines potential additional metadata for a piece of data
CREATE TABLE data_variables (
	id integer primary key,
	data_id integer,
	key_value varchar(128) not null,
	value_data clob,
	FOREIGN KEY(data_id) REFERENCES block_data(id)
);

-- defines the path
CREATE TABLE user_path (
	_id integer primary key autoincrement,
	path_length integer,
	last_used datetime default CURRENT_TIMESTAMP
);

-- basically a doubly linked list
CREATE TABLE path_nodes (
	path_id integer,
	previous_x integer,
	previous_y integer,
	current_x integer not null,
	current_y integer not null,
	next_x integer,
	next_y integer,
	weight float not null,
	FOREIGN KEY(path_id) REFERENCES user_path(_id)
);

-- some test data
insert into block (latitudeA, longitudeA, latitudeB, longitudeB, x, y )
values (45.395, -75.685, 45.390, -75.678, 0, 0);

insert into block_data(block_id, name, latitude, longitude, altitude)
values (1, 'Tim Hortons', 45.390726, -75.679675, 0.0);

insert into block_data(block_id, name, latitude, longitude, altitude)
values(1, 'Cedars & Co.', 45.390982, -75.679890, 0.0);

insert into data_variables(data_id, key_value, value_data)
values (1, 'description', 'Tim Hortons is a Canandian fast casual restaurant known for its coffee and doughnuts');