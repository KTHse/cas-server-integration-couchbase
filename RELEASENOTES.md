Release notes
=============

1.1.0

* CAS 3.5.2.

1.0.0

* First major release.
* Added some unit tests.
* Ran some stability tests for a couple of days creating some
  10 000 ticket granting tickets and creating and validating
  some 50 000 service tickets finding no issues.  

0.1.2

* Remove extra round trips to database in CouchbaseTicketRegistry.getTickets().

0.1.1

* Fix NPE in statistics view of CAS due to CouchbaseTicketRegistry adding
  null, i.e., tickets that have expired while the list is generated, to
  the response of getTickets().
