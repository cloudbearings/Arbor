use utf8;
package Arbor::Schema::Result::Block;

# Created by DBIx::Class::Schema::Loader
# DO NOT MODIFY THE FIRST PART OF THIS FILE

=head1 NAME

Arbor::Schema::Result::Block

=cut

use strict;
use warnings;

use base 'DBIx::Class::Core';

=head1 TABLE: C<block>

=cut

__PACKAGE__->table("block");

=head1 ACCESSORS

=head2 id

  data_type: 'integer'
  is_auto_increment: 1
  is_nullable: 0

=head2 longitudea

  data_type: 'float'
  is_nullable: 0

=head2 latitudea

  data_type: 'float'
  is_nullable: 0

=head2 longitudeb

  data_type: 'float'
  is_nullable: 0

=head2 latitudeb

  data_type: 'float'
  is_nullable: 0

=head2 last_updated

  data_type: 'datetime'
  default_value: current_timestamp
  is_nullable: 1

=cut

__PACKAGE__->add_columns(
  "id",
  { data_type => "integer", is_auto_increment => 1, is_nullable => 0 },
  "longitudea",
  { data_type => "float", is_nullable => 0 },
  "latitudea",
  { data_type => "float", is_nullable => 0 },
  "longitudeb",
  { data_type => "float", is_nullable => 0 },
  "latitudeb",
  { data_type => "float", is_nullable => 0 },
  "x",
  { data_type => "integer", is_nullable => 0 },
  "y",
  { data_type => "integer", is_nullable => 0 },
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

=head2 block_datas

Type: has_many

Related object: L<Arbor::Schema::Result::BlockData>

=cut

__PACKAGE__->has_many(
  "block_datas",
  "Arbor::Schema::Result::BlockData",
  { "foreign.block_id" => "self.id" },
  { cascade_copy => 0, cascade_delete => 0 },
);


# Created by DBIx::Class::Schema::Loader v0.07017 @ 2012-03-25 15:34:31
# DO NOT MODIFY THIS OR ANYTHING ABOVE! md5sum:0Y/d1kyeR488d0lFApeZpw


# You can replace this text with custom code or comments, and it will be preserved on regeneration

sub get_neighbours {
    my ( $self ) = @_;
    my @neighbours;

    # TODO Create query to fetch surrounding neighbours
    return @neighbours;
}
1;
