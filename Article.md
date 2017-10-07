## Event-Driven Reactive Services on the JVM:

### Reactive paradigm on the JVM

JVM has been establishing itself as a platform which can meet high-throughput low-latency demands of the modern data as 
well as compute intensive workloads. With ambitious OpenJDK projects like Valhalla, Panama and Loom, to name a few, we
can see an even better platform-level support for various primitives like Fibers, Continuations, Cache-friendly compact 
data structures and what not coming up. Even today, the Java platform is no short of high performance concurrency support. But 
unfortunately, the low-level threading model exposed by Java has turned out to be an unpleasant match with it's classic
imperative paradigm support. To address the problem of low-level thread management and yet provide a high-concurrency programming model,
we have seen various efforts lately.

### What is Vertx
 
Vertx has been an effort in that direction. Inspired by NodeJS-style single-I/O event loop,
Vertx exposes a programming model based on decided task context while taking the burden of underlying thread management off
of programmer's shoulder. It's based on even-driven message passing style which means I/O events trigger the task to perform
rather than the task waiting or blocking for I/O to complete. The entire toolkit is modular and the core is under 1 MB in size.
Such an architecture allows for easy extension without reaching a point where the entire design collapses under it's own weight.     
Better yet, an app based on Vertx can be deployed as an standalone jar as well as HA cluster. Within that cluster every Verticle
can send messages to other verticles. How cool is that!. 

### How Vertx works

Under the hood, Vertx APIs' are event-driven. That's how it comes as a reactive platform. Programmer-defined
handlers are called using an "Event Loop" thread. That's how an event loop takes care of various events and is able to 
deliver great performance. This might come as something counter-intuitive that a single thread can be more efficient than multiple threads. 
You may also read about the famous LMAX disruptor platform which leverages the same underlying technique of running a non-blocking 
single thread and deliver remarkable results.

### Fully async flow

A typical app has generally many blocking round trips. Example are read/write to persistent data stores, remote resources etc.
To cater such needs, Vertx support various async modules for optimal performance. But the programmer can easily step out into
a worker thread and do the majority tasks old-school blocking way without halting the event loop.

### Demonstration

To under everything explained so far, the best way is to analyze an example with a real-world scenario.
We will dissect an app built on Vertx toolkit using the async modules to talk to a persistence store and 
a remote resource. 

#### Setup

Entire source is open under Apache 2.0 license and hosted on Github:

https://github.com/nimtiazm/asynserver

The project set up guide can be followed at the URL mentioned above. At the minimum, we need Docker 17.x, JDK 1.8 and
Maven 3 installed on the system.

We will simply consume two REST resources.
	1) First we search a name for available Wikipedia Articles in our server's data store. Here we will see how Vertx's async
	mysql module helps us do non-blocking reads with our data store.
	2) We choose one of the search results and request the latest version of that Wikipedia article. Here we will see how Vertx's
	async http client helps us consume a remote resource which is a typical blocking I/O.
	
You can start observing from the `HttpVerticle.java` @ http://bit.ly/2xnsEot. It's a Verticle on it own which works as an
HTTP server and listens at port 8080 for all the incoming requests. We don't need any container to host it. It's rather a 
self-hosting web server.

URL routes are defined in `initRoutes` method. To play around, you can add further route definitions there.

#### Async Mysql

Once a request is received to search for a given name in all Wikipedia titles, our server has to execute a blocking query
into Mysql data store to find out if there are any matching titles. But this entire operation can be run as an
asynchronous block using Vertx `JDBCClient` library and an async result handler `Handler<AsyncResult<T>>` which will operate
on the data set received after the query is executed. Consider the `handleQuery` method in `WikiQueriesHandler` class.
The entire responsibility of which thread handles what task and waits when is taken care of by Vertx. With the new 
async Mysql client library, we don't even have to handle database connection either. All we need to tell Vertx is 
what's the query and what to do with the result. 

#### Async HTTP client

Consuming remote REST resources is one of the most common tasks we do as developers which is a classic example of blocking I/O
operation. Vertx gives us an async http client to consume REST services in a style consistent to other modules (like we just 
saw in async sql) without worrying about the underlying thread management. Let's take a look at the `handleFetch` method of our
`WikiQueriesHandler` class. We have to be careful when specifying the REST endpoint and resource. They go as separate identifiers
along with the port number. It is unlike some other common http clients. We also have to specify the port number and also if we're 
talking through a secure http connection. In order to consume the response body received back from this remote resource, we provide 
an async handler as well. In this particular example we've allowed our client to follow redirection because if our query is correct,
the actual result we receive from this Wikipedia API is an http 302 with the URL to the article. 

#### Performing I/O in a Worker Thread

So far we've seen how Vertx introduces us to a programming paradigm that's efficient and quite different from a typical imperative-style
code. This reactive, message-passing style also relieves us of low-level thread management and scheduling yet utilize our modern
multi-core hardware to it's potential. 
But there are occasions when you may need to write some routines which depend on the result of another routine. Such a scenarios might
drive us to the famous callback hell which destroys the readability and maintainability of our code. For those occasions, Vertx provides
us blocking executor API. 
Consider a scenario in which we expose a REST API which first queries a data store and then consumes a remote REST resource. Parting
from the async style we've enjoyed so far, we can also implement this scenario like so (pseudocode):

```java
...
router.get("/multiStoreResource").blockingHandler(this::queryMultiStore);
...
public void queryMultiStore(RoutingContext rctx)
{
	//can use conventional jdbc drivers and blocking http clients here
}
```
 

### More Guides

To learn more about the core of Vertx, visit the official documentation at http://vertx.io/docs/vertx-core/java/.
From within there, more detail documentation can be found for other Vertx modules. 