package Arbor::Server::Data;
use Dancer ':syntax';
use Dancer::Plugin::DBIC;
use JSON;

set serializer => 'JSON';

prefix '/data';

any ['get', 'post'] => '/block/list' => sub {
    my ( $latitude, $longitude, $altitude ) = map { params->{$_} }
     (qw/ latitude longitude altitude /);
    
    my $schema = schema 'arbor';

    my $block_rs = $schema->resultset('Block')->search({
        longitudea => { '<=' => $longitude },
        latitudea => { '>=' => $latitude },
        longitudeb => { '>=' => $longitude },
        latitudeb => { '<=' => $latitude },
    });

    # should match just one block in the database
    my ( $block ) = $block_rs->all;

    # error out here if no block was found
    return { blockList => [] } unless $block;

    my @neighbours = $block->get_neighbours;

    my @block_list = map {
        {
            id => $_->id,
            last_updated => $_->last_updated,
            longitudeA => $_->longitudea,
            latitudeA => $_->latitudea,
            longitudeB => $_->longitudeb,
            latitudeB => $_->latitudeb,
            size => $_->block_datas->count,
        }
    } ($block, @neighbours); 

    return { blockList => \@block_list };
};

any ['get', 'post'] =>  '/blocks' => sub {
    my $schema = schema 'arbor';
    
    my $json_data = decode_json params->{json};
    
    my @block_list = @{$json_data->{blockList}};

    my $blocks_rs = $schema->resultset('BlockData')->search({
        block_id => { 'IN' => \@block_list }, # TODO Consider performance issues
    });
   
    my @data;
    
    while( my $block = $blocks_rs->next ) {
    
        my $metaData = {};
        if( $block->data_variables ) {
            my @variables = $block->data_variables->all;
            $metaData = { map { $_->key_value => $_->value_data } @variables }

        }
        push @data, {
            blockId => $block->block_id,
            name => $block->name,
            latitude => $block->latitude,
            longitude => $block->longitude,
            altitude => $block->altitude,
            metaData => $metaData,
        };
    }
     
    return { blockList => \@data };
};

any ['get', 'post'] =>  '/ping' => sub {
    return { pong  => params->{testData} };
};

true;
