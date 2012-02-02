package Arbor::Schema::Result::User

use strict;
use warnings;

use base qw/ DBIx::Class::Core /;

__PACKAGE__->table('users');
__PACKAGE__->add_columns(
    user_id => {
        data_type => 'int',
        is_auto_increment => 1,
    },
    user_name => {
        data_type => 'text',
        size => 20,
    },
    user_password => {
        data_type => 'text',
        size => 32,
    },
);

__PACKAGE__->set_primary_key( 'user_id' );

# TODO Define Relationships between data
1;
