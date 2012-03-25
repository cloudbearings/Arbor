use DBIx::Class::Schema::Loader qw/ make_schema_at /;
make_schema_at(
   'Arbor::Schema',
    { debug => 1,
      dump_directory => './lib_test',
    },
    [ 'dbi:SQLite:arbor.db',
    ],
);
