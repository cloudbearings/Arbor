package Arbor::Profile;
use Dancer ':syntax';
use Dancer::Plugin::DBIC;
use Digest::MD5;

# we want to only access /profile/... if the user is logged in
hook 'before' => sub {
    if( !session('username') && request->path_info =~ m{^/profile} ) {
        var requested_path => request->path_info;
        request->path_info('/login');
    }
};

prefix '/profile';

# including the users information page
get '/user/*' => sub {
    my ( $user ) = splat;
    var user => $user;
    template 'profile.tt', { username => $user };
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

true;
