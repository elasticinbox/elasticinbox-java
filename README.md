ElasticInbox is reliable, distributed, scalable email store.

## Overview

ElasticInbox provides highly available email store without a single point of 
failure which can run on commodity hardware and scale linearly. ElasticInbox 
can easily scale to millions of mailboxes, with hundreds of thousands messages 
in each mailbox.

## Requirements

 * Java >= 1.6
 * Apache Cassandra >= 0.8.0 (see http://cassandra.apache.org/)

## Getting started

Please visit http://www.elasticinbox.com/ for more information.

## Building from Source

To build and run from source you will need Maven 3 and Git:

```bash
% git clone git://github.com/elasticinbox/elasticinbox.git elasticinbox
% cd elasticinbox
% mvn clean install pax:provision -DskipITs
```

## License

Licensed under the Modified BSD License.