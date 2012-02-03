use Test::More tests => 4;
use warnings;

# the order is important
use Arbor::Server;
use Dancer::Test;

route_exists [GET => '/login'], 'a route handler is defined for /login';
response_status_is ['GET' => '/login'], 200, 'response status is 200 for /login';

route_exists [GET => '/logout'], 'a route handler is defined for /logout';
response_status_is ['GET' => '/logout'], 302, 'response status is 302 redirect for /logout';


