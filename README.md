# metrics-riemann-reporter

Simple way to push JMX-based metrics to [Riemann](http://riemann.io/) aggregator. 

## Rationale

_Things we don't measure surprisinly often occur to be the most interesting ones._

_Things we don't measure carefully enough are analyzed later in "post-mortems"._

Sounds familiar? Fear not, rescue is coming.

## Riemann? JMX?

Riemann is unique and elegant way of aggregating incoming events and pushing them futher into (almost) whatever store has been invented so far.
It's a push-based architecture where client application needs to take care of sending own events to aggregator by itself. What are those events?
Well, these may be an application logs (at debug/info/error level for example) or metrics created by famous [dropwizard metrics library](http://metrics.dropwizard.io)
or even memory/cpu usage at given time. Whatever our events are it would be nice to have a common and dead-simple way to create them.

This is where JMX comes onto scene.

Think of JMX as a collection of beans which you can ask for any information that your application (and JVM!) was able to expose.
By default you may find there quite impressing amount of information:

![VisualVM 1](images/VisualVM_1.png?raw=true "JMX mbeans")

Imagine now that we will expose this way our own metrics and transform them into events periodically:

![VisualVM 2](images/VisualVM_2.png?raw=true "Metrics exposed")

Indeed, this is what `metrics-riemann-reporter` does.

## Ehmm... what it actually does?

Under the hood `metrics-riemann-reporter` makes use of metric library and its ability to register mbeans serving every single metric that we already defined.
Having mbeans registered, separate thread polls it, once per 2 seconds by default, transforming each metric into event. As you may guess, finally events are sent to riemann aggregator.

## How may I use that?

Import `metrics-riemann-reporter` library:

``` clojure
    [defunkt/metrics-riemann-reporter "0.0.2"]
```

    
It depends on `metrics-clojure` which you may use to create metrics registry:

``` clojure
    (require '[metrics.core :refer [new-registry]
             '[metrics.counters :refer [counter inc!]])
    
    (defonce registry (new-registry))
```

Time to define first metric. Let it be a [counter](http://metrics.dropwizard.io/3.2.2/manual/core.html):

``` clojure
    (def sessions (counter registry "sessions-created"))

    (inc! sessions)
```
Now, let's expose our metrics in JMX and decide which mbeans should be polled for data that we want to send out as events:

``` clojure
    (require '[reporter.core :as r])

    (def beans [{:mbean "metrics:name=default.default.sessions-created" :metrics [:Count] :event "sessions-created"}])
    
    (def reporter (r/init-reporter {:host "localhost" :port 5555} registry "my-service" beans))
```
    
Mysterious `beans` is a vector of:

``` edn
    {:mbean   object-name,
     :metrics object-attributes
     :event   event-name,
     :service optional-service-name,
     :tags    optional-tags}
```

and resulting event will look like this:
    
``` edn
    {:service "my-service.sessions-created.Count"
     :state "ok"
     :metric 1
     :tags nil}
```
     
Where the service name came from? It's combined of 3 elements: _service name_, _event_ and _metric_ where the service name is provided as argument of `init-reporter` or may be set up in each bean definition separately (and has a priority over a former one).

Additionally each bean may define its own vector of `tags` which might be used during events aggregation by riemann.

Unused reporter should be shut down:

``` clojure
    (r/shutdown-reporter reporter)
```

## There is one more thing...

To make things even easier reporter exposes 2 its own mbeans by default:

 - `java.lang:name=Cpu` with attribute `:CpuUsed` reporting current CPU usage
 - `java.lang:name=Heap` with attributes `:HeapInit`, `:HeapUsed`, `:HeapMax` and `:HeapComitted` reporting heap memory usage
 
 which can be turned into events based on following beans definition:
 
     [{:mbean "java.lang:name=Cpu" :metrics [:CpuUsed] :event "cpu"}
      {:mbean "java.lang:name=Heap" :metrics [:HeapInit :HeapUsed] :event "memory"}] 
 



