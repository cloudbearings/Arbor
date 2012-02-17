package Arbor::Schema::Result::User;

use strict;
use warnings;

use base qw/ DBIx::Class::Core /;

__PACKAGE__->table('users');
__PACKAGE__->add_columns(
    user_id => {
        data_type => 'int',
        is_auto_increment => 1,
    },
    username => {
        data_type => 'text',
        size => 20,
    },
    password => {
        data_type => 'text',
        size => 32,
    },
    fullname => {
        data_type => 'text',
        size => 64,
    },
);

__PACKAGE__->set_primary_key( 'user_id' );

__PACKAGE__->belongs_to( secret => 'Arbor::Schema::Result::UserSecret', 'user_id' );
1;
