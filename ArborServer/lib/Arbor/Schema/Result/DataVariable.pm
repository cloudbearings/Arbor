use utf8;
package Arbor::Schema::Result::DataVariable;

# Created by DBIx::Class::Schema::Loader
# DO NOT MODIFY THE FIRST PART OF THIS FILE

=head1 NAME

Arbor::Schema::Result::DataVariable

=cut

use strict;
use warnings;

use base 'DBIx::Class::Core';

=head1 TABLE: C<data_variables>

=cut

__PACKAGE__->table("data_variables");

=head1 ACCESSORS

=head2 id

  data_type: 'integer'
  is_auto_increment: 1
  is_nullable: 0

=head2 data_id

  data_type: 'integer'
  is_foreign_key: 1
  is_nullable: 1

=head2 key_value

  data_type: 'varchar'
  is_nullable: 0
  size: 128

=head2 value_data

  data_type: 'clob'
  is_nullable: 1

=cut

__PACKAGE__->add_columns(
  "id",
  { data_type => "integer", is_auto_increment => 1, is_nullable => 0 },
  "data_id",
  { data_type => "integer", is_foreign_key => 1, is_nullable => 1 },
  "key_value",
  { data_type => "varchar", is_nullable => 0, size => 128 },
  "value_data",
  { data_type => "clob", is_nullable => 1 },
);

=head1 PRIMARY KEY

=over 4

=item * L</id>

=back

=cut

__PACKAGE__->set_primary_key("id");

=head1 RELATIONS

=head2 data

Type: belongs_to

Related object: L<Arbor::Schema::Result::BlockData>

=cut

__PACKAGE__->belongs_to(
  "data",
  "Arbor::Schema::Result::BlockData",
  { id => "data_id" },
  {
    is_deferrable => 1,
    join_type     => "LEFT",
    on_delete     => "CASCADE",
    on_update     => "CASCADE",
  },
);


# Created by DBIx::Class::Schema::Loader v0.07017 @ 2012-03-25 15:34:31
# DO NOT MODIFY THIS OR ANYTHING ABOVE! md5sum:UhDmPXkOg32Vvwaq6kh4TA


# You can replace this text with custom code or comments, and it will be preserved on regeneration
1;
