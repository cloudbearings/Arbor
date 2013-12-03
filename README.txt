This project requires two distinct steps for building and deployment.

The CreatedSQL.sql file is included for reference, but both the client and server 
are capable of self-generating the sql.

For testing purposes, an instance of the server is running at the address
http://sectorzerostudios.com and is the default web address configured in the app.

CLIENT - Requires an Android mobile device with GPS
1. Install Android SDK into Eclipse
2. Import ArborClient project into Eclipse
3. Connect device in debug mode to computer
4. Configure remote host to point to desired server (default sectorzerostudios.com)
5. Build and deploy project, app should start on device

SERVER - Requires a Linux distribution
1. Install perl (I recommend following the steps for Perlbrew at http://perlbrew.pl/)
2. Build sqlite3 for your local installation
3. Optional - Install nginx and configure starman as a FastCGI App
4. cd into ArborServer project and do a "perl Build.PL" followed by "./build install"
5. run "perl bin/deploy.pl"
6. Start the app either through starman/nginx or via "perl bin/app.pl"
7. Navigate to http://<address>:<port>/data/generate to generate core test data.