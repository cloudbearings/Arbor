package Arbor::Server::Profile;
use Dancer ':syntax';
use Dancer::Plugin::DBIC;
use Digest::MD5;

set serializer => 'JSON';

# we want to only access /profile/... if the user is logged in
hook 'before' => sub {
    if( !session('username') && request->path_info =~ m{^/profile} ) {
        var requested_path => request->path_info;
        request->path_info('/login');
    }
};

prefix '/profile';

# including the users information page
get '/user/:id' => sub {
    var user => params->{id};

    my $schema = schema 'arbor';

    my $user = $schema->resultset('User')->find({
        username => params->{id},
    });

    return 'user not found' unless $user;

    my $secret = $user->secret ? $user->secret->api_secret : 'no api key found';

    template 'profile.tt', { 
        username => params->{id},
        fullname => $user->fullname,
        api_secret => $secret,
    };
};

# create a key for a user
post '/user/:id/secret' => sub {

    my $schema = schema 'arbor';
   
    my $seed = join'', map +(0..9,'a'..'z','A'..'Z')[rand(10+26*2)], 1..16;

    my $ctx = Digest::MD5->new;
    $ctx->add( $seed ); 

    my $unique_key = 0;

    do {
        $ctx->add( $seed );

        my $rs = $schema->resultset('UserSecret')->search({
            api_secret => $ctx->clone->hexdigest,
        });

        $unique_key = 1 unless $rs->count;
    
    } while ( !$unique_key );
    
    my $user = $schema->resultset('User')->find({
        username => params->{id}
    });

    if( !$user ) {
        status 'not_found';
        return 'user not found';
    }

    my $key = $schema->resultset('UserSecret')->find_or_create({
        user_id => $user->user_id,
    });

    $key->api_secret( $ctx->hexdigest );

    $key->update_or_insert;

    return { api_secret => $key->api_secret };
};

#Handle logins at the root of the path
prefix undef;

# display a login page for the user
get '/login' => sub {
    template 'login.tt', { path => vars->{requested_path} };
};

# process the users login
post '/login' => sub {
    my $username = params->{username};
    my $password = params->{password};

    my $ctx = Digest::MD5->new;
    $ctx->add($password);
    my $schema = schema 'arbor';

    my $user = $schema->resultset('User')->find({
        username => $username,
        password => $ctx->hexdigest,
    });

    if( $user ) {
        session username => $username;
        redirect params->{path} || "/profile/user/$username";
    }
    else {
        redirect "/login?failed=1";
    }
};

get '/logout' => sub {
    session->destroy;
    redirect '/';
};

true;
