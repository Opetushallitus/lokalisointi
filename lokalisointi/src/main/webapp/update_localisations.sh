#!/bin/sh

#
# Make backup
#
cp localisations.json localisations.json_bakup

#
# Get translations
#
wget --no-check-certificate https://itest-virkailija.oph.ware.fi/lokalisointi/cxf/rest/v1/localisation -O localisations.json

# Done.
