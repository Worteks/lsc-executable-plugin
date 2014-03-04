#!/usr/bin/perl -w

#===================================================
#                  ==LICENSE NOTICE==
#
# Copyright (c) 2008, LSC Project
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
#     * Neither the name of the LSC Project nor the names of its
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
# IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
# TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
# PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
# OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#                  ==LICENSE NOTICE==
#
# Author: Ronan Lanore
# Author: Clement Oudot
#===================================================

my $VERSION = 1.0;

use strict;
use Net::LDAP::LDIF;

#===================================================
# Read LDIF entry
#===================================================
my $ldif = Net::LDAP::LDIF->new( \*STDIN, "r", onerror => 'undef' );
my $entry = $ldif->read_entry();

if ( $ldif->error() ) {
    print STDERR "ERROR: ", $ldif->error(),       "\n";
    print STDERR "ERROR: ", $ldif->error_lines(), "\n";
    exit 1;
}

#===================================================
# Launch commands
#===================================================
else {

    # modify check
    if ( $entry->changetype() eq "add" ) {
    }
    if ( $entry->changetype() eq "modify" ) {
    }
    if ( $entry->changetype() eq "delete" ) {
    }
    if ( $entry->changetype() eq "modrdn" or $entry->changetype() eq "moddn" ) {
    }

}
$ldif->done();

exit 0;
