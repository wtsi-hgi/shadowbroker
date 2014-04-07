Shadowbroker is a tool to connect the HGI RT queues to the HGI chatroom.

## To build ##

Shadowbroker is build with [sbt](http://www.scala-sbt.org/). To build, just run:

    sbt compile

To produce a distributed assembly jar, run:

    sbt assembly

## To run ##

This section is largely useless to people outside of HGI!

Shadowbroker is currently running on `hgi-im` in `/home/mercury`. To run, just do a `nohup shadowbroker.sh`, which should kill previous instances and run a new instance. There are also entries in crontab to restart shadowbroker every week (it seems to not like being run for too long) and on restarts:

```
0 0 * * 0 nohup /home/mercury/bin/shadowbroker.sh
@reboot nohup /home/mercury/bin/shadowbroker.sh
```
