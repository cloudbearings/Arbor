package Arbor::Server::Data;
use Dancer ':syntax';

set serializer => 'JSON';

prefix '/data';

get '/block/*' => sub {
    my ( $block_id ) = splat;
    return "fetching block id $block_id";
};

post '/ping' => sub {
    return { dataMode => params->{mProvider} };
}
true;
