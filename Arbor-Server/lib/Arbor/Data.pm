package Arbor::Data;
use Dancer ':syntax';

prefix '/data';

get '/block/*' => sub {
   my ( $block_id ) = splat;
   return "fetching block id $block_id";
};

true;
