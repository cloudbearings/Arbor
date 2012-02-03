#!/usr/bin/env perl 

use strict;
use warnings;

use Arbor::Schema;

exit if -e 'arbor.db';

my $schema = Arbor::Schema->connect( 'dbi:SQLite:arbor.db' );

$schema->deploy;

my $admin = $schema->resultset('User')->new({
    username => 'administrator',
    password => '200ceb26807d6bf99fd6f4f0d1ca54d4',
    fullname => 'System Administrator',
});

$admin->insert;
