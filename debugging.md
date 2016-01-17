# Common Setup mistakes and their solutions...

## fedora.index.service

edu.indiana.dlib.fedoraindexer.server.IndexOperationException: Photocat Item index: Unable to generate index document!
	at edu.indiana.dlib.fedoraindexer.server.index.AtomicObjectLuceneIndex.indexObject(AtomicObjectLuceneIndex.java:46)
	at edu.indiana.dlib.fedoraindexer.server.IndexManager.indexObject(IndexManager.java:169)
	at edu.indiana.dlib.fedoraindexer.server.IndexRestService.onMessage(IndexRestService.java:190)
	at fedora.client.messaging.JmsMessagingClient.onMessage(JmsMessagingClient.java:284)
	at org.apache.activemq.ActiveMQMessageConsumer.dispatch(ActiveMQMessageConsumer.java:967)
	at org.apache.activemq.ActiveMQSessionExecutor.dispatch(ActiveMQSessionExecutor.java:122)
	at org.apache.activemq.ActiveMQSessionExecutor.iterate(ActiveMQSessionExecutor.java:192)
	at org.apache.activemq.thread.PooledTaskRunner.runTask(PooledTaskRunner.java:122)
	at org.apache.activemq.thread.PooledTaskRunner$1.run(PooledTaskRunner.java:43)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)
Caused by: java.lang.NullPointerException
	at edu.indiana.dlib.fedoraindexer.server.index.PhotocatItemIndex.createIndexDocument(PhotocatItemIndex.java:81)
	at edu.indiana.dlib.fedoraindexer.server.index.AtomicObjectLuceneIndex.indexObject(AtomicObjectLuceneIndex.java:43)
	... 11 more

This is the result of not having the fedora 3 resource index set up to sync updates.  What happens is that Fedora 3
emits a JMS message about an update, and the indexing routine performs a resource index query which does not yet
include the expected data.
