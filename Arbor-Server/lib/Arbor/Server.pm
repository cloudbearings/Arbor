package Arbor::Server;
use Dancer ':syntax';

our $VERSION = '0.1';

get '/' => sub {
    template 'index';
};

prefix '/data';

get '/block/*' => sub {
   my ( $block_id ) = splat;
   return "fetching block id $block_id";
};

prefix '/user';

get '/profile/*' => sub {
    my ( $user ) = splat;
    var user => $user;
    return "profile page for $user";
};

get '/login' => sub {
    return "login page";
};

post '/login' => sub {
    my $user = param->{username};
    my $password = param->{password};
};

true;
