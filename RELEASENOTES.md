Release notes
=============

0.1.1

* Fix NPE in statistics view of CAS due to CouchbaseTicketRegistry adding
  null, i.e., tickets that have expired while the list is generated, to
  the response of getTickets().
