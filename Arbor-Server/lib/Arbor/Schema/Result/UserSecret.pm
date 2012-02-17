package Arbor::Schema::Result::UserSecret;

use strict;
use warnings;

use base qw/ DBIx::Class::Core /;

__PACKAGE__->table('user_secret');
__PACKAGE__->add_columns(
    user_id => {
        data_type => 'int',
        is_auto_increment => 1,
    },
    api_secret => {
        data_type => 'text',
        size => 32,
        is_nullable => 1,
    },
);

__PACKAGE__->set_primary_key( 'user_id' );

__PACKAGE__->belongs_to( user => 'Arbor::Schema::Result::User', 'user_id' );
1;
