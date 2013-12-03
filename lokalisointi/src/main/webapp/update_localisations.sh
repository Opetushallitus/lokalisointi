#!/bin/sh

#
# Make backup
#
cp localisations.json localisations.json_bakup

#
# Get translations
#
wget --no-check-certificate https://itest-virkailija.oph.ware.fi/lokalisointi/cxf/rest/v1/localisation -O tmp.json

#
# Remove ID's
#
cat tmp.json | grep -v "\"id\"" > localisations.json

#
# Cleanup
#
rm -f tmp.json
