(ns reporter.core
  (:require [riemann.client :as riemann]
            [clojure.java.jmx :as jmx]
            [reporter.beans :refer [init-cpu-bean init-heap-bean]])
  (:import pl.defunkt.CpuHeapUsage
           com.codahale.metrics.JmxReporter
           io.riemann.riemann.client.RiemannClient
           java.net.InetAddress))

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

   {:mbean   \"JMX-mbean\"
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

(defn init-periodic
  "Schedules a function f to be run periodically at given interval."

  [f interval]
  (.scheduleAtFixedRate scheduler f 3000 interval java.util.concurrent.TimeUnit/MILLISECONDS))

(defn stop-periodic
  "Stops periodically run function."

  [periodic]
  (when periodic
    (.cancel periodic false)))

(defn init-reporter [riemann-config metrics-registry service beans & interval]
  (let [ch-usage (CpuHeapUsage. jmx/*connection*)
        reporter (JmxReporter/forRegistry metrics-registry)
        heap-ref (init-heap-bean ch-usage)
        cpu-ref  (init-cpu-bean ch-usage)
        periodic (init-periodic
                  (fn []
                    (dosync
                     (alter cpu-ref  assoc :CpuUsed (.getCpuUsed ch-usage))
                     (alter heap-ref assoc :HeapUsed (.getHeapUsed ch-usage)))
                    (when riemann-config
                      (send-events riemann-config service beans)))
                  (or interval 2000))]

    (-> reporter
        (.build)
        (.start))

    {:reporter reporter
     :periodic periodic}))

(defn shutdown-reporter [reporter]
  (stop-periodic (:periodic reporter)))
