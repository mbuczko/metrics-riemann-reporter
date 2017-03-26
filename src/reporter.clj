(ns reporter
  (:require [riemann.client :as riemann]
            [clojure.java.jmx :as jmx])
  (:import com.codahale.metrics.JmxReporter
           io.riemann.riemann.client.RiemannClient
           java.net.InetAddress))

(defn- riemann-client*
  [{:keys [host port]}]
  (riemann/tcp-client :host host :port port))

(def ^{:private true :tag RiemannClient} riemann-client (memoize riemann-client*))

(def ^{:private true} scheduler ^java.util.concurrent.ScheduledExecutorService
  (java.util.concurrent.Executors/newScheduledThreadPool 1))

(defn- generate-fn [service-prefix reduced {:keys [metric attrs tags event service]}]
  (let [srv (or service service-prefix)]
    (concat reduced (map #(hash-map
                           :service (str srv \. event \. (name %))
                           :state "ok"
                           :metric (% (jmx/mbean metric))
                           :tags tags) attrs))))

(defn generate-events
  "Generates list of riemann events based on provided beans definition.
  Each bean definition is a following map:

   {:metric  \"JMX-mbean\"
    :event   \"event-name\"
    :service \"optional-service-name\"
    :attrs   [:mbean-attribute-1 :mbean-attribute-2 ...]
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
  (.scheduleAtFixedRate scheduler f 0 interval java.util.concurrent.TimeUnit/MILLISECONDS))

(defn stop-periodic
  "Stops periodically run function."

  [periodic]
  (when periodic
    (.cancel periodic false)))

(defn init-reporter [riemann-config metrics-registry service beans & interval]
  (let [reporter (JmxReporter/forRegistry metrics-registry)
        periodic (init-periodic
                  #(send-events riemann-config service beans)
                  (or interval 2000))]
    (-> reporter
        (.build)
        (.start))

    {:reporter reporter
     :periodic periodic}))

(defn shutdown-reporter [reporter]
  (stop-periodic (:periodic reporter)))
