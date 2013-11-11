======================================================================
Lokalisointi
======================================================================

Running:

	mvn -Dlog4j.configuration=file:`pwd`/src/test/resources/log4j.xml jetty:run

Spy on requests:

        sudo tcpdump -A -i lo0 'tcp port 8319'
