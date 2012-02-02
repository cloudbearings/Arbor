package Arbor::Profile;
use Dancer ':syntax';

prefix '/profile';

# we want to only access /profile/... if the user is logged in
hook 'before' => sub {
    if( !session('user_id') && request->path_info =~ m{^/profile} ) {
        var requested_path => request->path_info;
        request->path_info('/login');
    }
};

# including the users information page
get '/user/*' => sub {
    my ( $user ) = splat;
    var user => $user;
    return "profile page for $user";
};

#Handle logins at the root of the path
prefix undef;

# display a login page for the user
get '/login' => sub {
    template 'login', { path => vars->{requested_path} };
};

# process the users login
post '/login' => sub {
    my $user = param->{username};
    my $password = param->{password};
};

true;

