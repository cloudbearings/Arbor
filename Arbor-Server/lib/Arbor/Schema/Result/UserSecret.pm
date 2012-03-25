use utf8;
package Arbor::Schema::Result::UserSecret;

# Created by DBIx::Class::Schema::Loader
# DO NOT MODIFY THE FIRST PART OF THIS FILE

=head1 NAME

Arbor::Schema::Result::UserSecret

=cut

use strict;
use warnings;

use base 'DBIx::Class::Core';

=head1 TABLE: C<user_secret>

=cut

__PACKAGE__->table("user_secret");

=head1 ACCESSORS

=head2 user_id

  data_type: 'integer'
  is_auto_increment: 1
  is_foreign_key: 1
  is_nullable: 0

=head2 api_secret

  data_type: 'text'
  is_nullable: 1

=cut

__PACKAGE__->add_columns(
  "user_id",
  {
    data_type         => "integer",
    is_auto_increment => 1,
    is_foreign_key    => 1,
    is_nullable       => 0,
  },
  "api_secret",
  { data_type => "text", is_nullable => 1 },
);

=head1 PRIMARY KEY

=over 4

=item * L</user_id>

=back

=cut

__PACKAGE__->set_primary_key("user_id");

=head1 RELATIONS

=head2 active_user

Type: might_have

Related object: L<Arbor::Schema::Result::User>

=cut

__PACKAGE__->might_have(
  "active_user",
  "Arbor::Schema::Result::User",
  { "foreign.user_id" => "self.user_id" },
  { cascade_copy => 0, cascade_delete => 0 },
);

=head2 user

Type: belongs_to

Related object: L<Arbor::Schema::Result::User>

=cut

__PACKAGE__->belongs_to(
  "user",
  "Arbor::Schema::Result::User",
  { user_id => "user_id" },
  { is_deferrable => 1, on_delete => "CASCADE", on_update => "CASCADE" },
);


# Created by DBIx::Class::Schema::Loader v0.07017 @ 2012-03-25 15:34:31
# DO NOT MODIFY THIS OR ANYTHING ABOVE! md5sum:tminenkRbWVtc9cbWB8cqg


# You can replace this text with custom code or comments, and it will be preserved on regeneration
1;
