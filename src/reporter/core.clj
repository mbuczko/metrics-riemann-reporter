(ns reporter.core
  (:require [clojure.java.jmx :as jmx]
            [reporter.beans
             :refer
             [init-cpu-mbean init-heap-mbean unregister-cpu-heap-mbeans]]
            [riemann.client :as riemann])
  (:import com.codahale.metrics.JmxReporter
           io.riemann.riemann.client.RiemannClient
           pl.defunkt.CpuHeapUsage))

(defn- riemann-client*
  [{:keys [host port]}]
  (riemann/tcp-client :host host :port port))

(def ^{:private true :tag RiemannClient} riemann-client (memoize riemann-client*))

(def ^{:private true} scheduler ^java.util.concurrent.ScheduledExecutorService
  (java.util.concurrent.Executors/newScheduledThreadPool 1))

(defn- generate-fn [service-prefix reduced {:keys [mbean metrics tags event service]}]
  (let [srv (or service service-prefix)]
    (concat reduced (map #(hash-map
                           :service (str srv \. event \. (name %))
                           :state "ok"
                           :tags tags
                           :metric (% (jmx/mbean mbean))) metrics))))

(defn generate-events
  "Generates list of riemann events based on provided beans definition.
  Each bean definition is a following map:

   {:mbean   \"JMX-mbean-name\"
    :event   \"event-name\"
    :service \"optional-service-name\"
    :metrics [:mbean-attribute-1 :mbean-attribute-2 ...]
    :tags    [\"optional-tag\"]}

  Uses service when no bean-defined service was found."

  [service beans]
  (reduce (partial generate-fn service) {} beans))

(defn send-events [riemann-config service beans]
  (let [events (generate-events service beans)]
    (riemann/send-events (riemann-client riemann-config) events)))

(defn init-poller
  "Schedules a function f to be run periodically at given interval."

  [f interval]
  (.scheduleAtFixedRate scheduler f 3000 interval java.util.concurrent.TimeUnit/MILLISECONDS))

(defn stop-poller
  "Stops periodically run function."

  [poller]
  (when poller
    (.cancel poller false)))

(defn init-reporter
  "Initializes metrics, CPU and Heap mbeans.
  Starts polling function which transforms mbeans values into riemann events.
  Having riemann-config provided sends events to riemann aggregator."

  [riemann-config metrics-registry service beans & interval]
  (let [ch-usage (CpuHeapUsage. jmx/*connection*)
        builder  (JmxReporter/forRegistry metrics-registry)
        heap-ref (init-heap-mbean ch-usage)
        cpu-ref  (init-cpu-mbean ch-usage)
        poller   (init-poller
                  (fn []
                    (dosync
                     (alter cpu-ref  assoc :CpuUsed (.getCpuUsed ch-usage))
                     (alter heap-ref assoc :HeapUsed (.getHeapUsed ch-usage)))
                    (when riemann-config
                      (send-events riemann-config service beans)))
                  (or interval 2000))
        reporter  (.build builder)]

    (.start reporter)
    {:jmx-reporter reporter
     :poller poller}))

(defn shutdown-reporter
  "Unregisters metrics, CPU and Heap mbeans.
  Stops polling function."

  [{:keys [jmx-reporter poller]}]
  (unregister-cpu-heap-mbeans)
  (stop-poller poller)
  (.close jmx-reporter))
