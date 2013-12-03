package Arbor::Server;
use Dancer ':syntax';
use Arbor::Server::Data;
use Arbor::Server::Profile;

our $VERSION = '0.1';
set 'template' => 'template_toolkit';

get '/' => sub {
    template 'index';
};

true;
