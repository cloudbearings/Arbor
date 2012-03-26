use utf8;
package Arbor::Schema::Result::BlockData;

# Created by DBIx::Class::Schema::Loader
# DO NOT MODIFY THE FIRST PART OF THIS FILE

=head1 NAME

Arbor::Schema::Result::BlockData

=cut

use strict;
use warnings;

use base 'DBIx::Class::Core';

=head1 TABLE: C<block_data>

=cut

__PACKAGE__->table("block_data");

=head1 ACCESSORS

=head2 id

  data_type: 'integer'
  is_auto_increment: 1
  is_nullable: 0

=head2 block_id

  data_type: 'integer'
  is_foreign_key: 1
  is_nullable: 1

=head2 name

  data_type: 'varchar'
  is_nullable: 0
  size: 128

=head2 longitude

  data_type: 'float'
  is_nullable: 0

=head2 latitude

  data_type: 'float'
  is_nullable: 0

=head2 altitude

  data_type: 'float'
  is_nullable: 1

=head2 last_updated

  data_type: 'datetime'
  default_value: current_timestamp
  is_nullable: 1

=cut

__PACKAGE__->add_columns(
  "id",
  { data_type => "integer", is_auto_increment => 1, is_nullable => 0 },
  "block_id",
  { data_type => "integer", is_foreign_key => 1, is_nullable => 1 },
  "name",
  { data_type => "varchar", is_nullable => 0, size => 128 },
  "longitude",
  { data_type => "float", is_nullable => 0 },
  "latitude",
  { data_type => "float", is_nullable => 0 },
  "altitude",
  { data_type => "float", is_nullable => 1 },
  "last_updated",
  {
    data_type     => "datetime",
    default_value => \"current_timestamp",
    is_nullable   => 1,
  },
);

=head1 PRIMARY KEY

=over 4

=item * L</id>

=back

=cut

__PACKAGE__->set_primary_key("id");

=head1 RELATIONS

=head2 block

Type: belongs_to

Related object: L<Arbor::Schema::Result::Block>

=cut

__PACKAGE__->belongs_to(
  "block",
  "Arbor::Schema::Result::Block",
  { id => "block_id" },
  {
    is_deferrable => 1,
    join_type     => "LEFT",
    on_delete     => "CASCADE",
    on_update     => "CASCADE",
  },
);

=head2 data_variables

Type: has_many

Related object: L<Arbor::Schema::Result::DataVariable>

=cut

__PACKAGE__->has_many(
  "data_variables",
  "Arbor::Schema::Result::DataVariable",
  { "foreign.data_id" => "self.id" },
  { cascade_copy => 0, cascade_delete => 0 },
);


# Created by DBIx::Class::Schema::Loader v0.07017 @ 2012-03-25 15:34:31
# DO NOT MODIFY THIS OR ANYTHING ABOVE! md5sum:a9MTPIw2PtNNTmHXnD75bA


# You can replace this text with custom code or comments, and it will be preserved on regeneration
1;
