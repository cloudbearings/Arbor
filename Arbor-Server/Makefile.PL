use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'Arbor::Server',
    AUTHOR              => q{Dean Pearce <deanpearce@gmail.com>},
    VERSION_FROM        => 'lib/Arbor/Server.pm',
    ABSTRACT            => 'Web Service Component of Augmented Reality Mobile Framework',
    ($ExtUtils::MakeMaker::VERSION >= 6.3002
      ? ('LICENSE'=> 'perl')
      : ()),
    PL_FILES            => {},
    PREREQ_PM => {
        'Test::More' => 0,
        'YAML'       => 0,
        'Dancer'     => 1.3091,
	'DBI'	     => 0,
	'DBD::SQLite'=> 0,
	'Dancer::Plugin::DBIC' => 0,
    },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'Arbor-Server-*' },
);
