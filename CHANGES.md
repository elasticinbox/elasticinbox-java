0.3.0, 2012-07-22
-----------------

  * Fixed bug where stale message indexes were not deleted. #23
  * Added new "scrub" operation for recalculating counters. #20
  * Prevent negative label counters. #19
  * Added throttling support for batch operations. #18
  * Avoid storing reserved labels. #14

0.3.0-RC1, 2012-05-17
---------------------

  * Make blob names AWS friendly by prefacing UUID
  * New structure for Cassandra Counters using composite keys. #15
  * Use "unread" messages instead of "new". #11
  * New REST API v2 with JSON error messages. #9, #13
  * Added support for case insensitive and nested labels #7, #8
  * New LMTP implementation using Apache James Protocols and Netty #3, #5 (thanks to @normanmaurer)
  * Added Blob compression support #4

0.2.0, 2011-12-04
-----------------

  * Initial Release