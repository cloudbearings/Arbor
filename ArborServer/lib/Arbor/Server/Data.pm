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

    my @block_list = map {
        {
            id => $_->id,
            last_updated => $_->last_updated,
            longitudeA => $_->longitudea,
            latitudeA => $_->latitudea,
            longitudeB => $_->longitudeb,
            latitudeB => $_->latitudeb,
            size => $_->block_datas->count,
            x => $_->x,
            y => $_->y,
        }
    } ( $block ); 

    return { blockList => \@block_list };
};

any ['get', 'post'] => '/virtual/list' => sub {
    
    my $json_data = decode_json params->{json};
    
    my $schema = schema 'arbor';
    
    my @points = @{$json_data->{points}};
    
    my @block_list;

    foreach my $point ( @points ) {
        my $block_rs = $schema->resultset('Block')->search({
            x => $point->{x},
            y => $point->{y},
        });
    

        # should match just one block in the database
        my ( $block ) = $block_rs->all;

        # error out here if no block was found
        next unless $block;

        push @block_list, map {
            {
                id => $_->id,
                last_updated => $_->last_updated,
                longitudeA => $_->longitudea,
                latitudeA => $_->latitudea,
                longitudeB => $_->longitudeb,
                latitudeB => $_->latitudeb,
                size => $_->block_datas->count,
                x => $_->x,
                y => $_->y,
            }
        } ( $block ); 
    }

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
            id => $block->id,
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

# generate blocks of size 0.001
any ['get', 'post'] => '/generate' => sub {
    #45.392 -75.700 to 45.380 -75.672 => 28 wide 12 deep
    my $schema = schema 'arbor';
    
    my $upper_y = 45.380; #latitude
    my $upper_x = -75.672; # longitude
    my $block_size = 0.001;
    
    foreach my $latitude (0..12) {
        foreach my $longitude (0..28) {
            $schema->resultset('Block')->create({
                longitudeb  => $upper_x - $block_size * $longitude,
                latitudeb   => $upper_y + $block_size * $latitude,
                longitudea  => $upper_x - $block_size * ($longitude + 1),
                latitudea   => $upper_y + $block_size * ($latitude + 1),
                x => $longitude,
                y => $latitude,
            });
        }
    }
    
    return 'success';
};

true;

