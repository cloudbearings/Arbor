package Arbor::Server;
use Dancer ':syntax';
use Arbor::Data;
use Arbor::Profile;

our $VERSION = '0.1';
set 'template' => 'template_toolkit';

get '/' => sub {
    template 'index';
};

true;
