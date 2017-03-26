(ns reporter-test
  (:require [clojure.test :refer :all]
            [clojure.java.jmx :as jmx]
            [reporter :refer :all]))

(def mocked-metrics {:Count 1 :OneMinuteRate  2 :FiveMinuteRate 3})

(deftest events-generator-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]
    (let [beans [{:metric "metrics:name=foo" :attrs [:Count] :tags ["pool"] :event "foo-ev"}
                 {:metric "metrics:name=bar" :attrs [:OneMinuteRate :FiveMinuteRate] :event "bar-ev" :tags ["baz"]}]
          events (generate-events "foo" beans)]

      (testing "number of generated events"
        (is (= 3 (count events))))

      (testing "type of event"
        (let [event (first events)]
          (is (map? event))
          (is (contains? event :tags))
          (is (contains? event :service))
          (is (contains? event :state))
          (is (contains? event :metric)))))))

(deftest events-service-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]

    (testing "provided service name"
      (let [beans [{:metric "metrics:sample" :attrs [:Count] :event "ev"}]
            event (first (generate-events "foo" beans))]
        (is (= "foo.ev.Count" (:service event)))))

    (testing "event-defined service name"
      (let [beans [{:metric "metrics:sample" :attrs [:Count] :event "ev" :service "bar"}]
            event (first (generate-events "foo" beans))]
        (is (= "bar.ev.Count" (:service event)))))))

(deftest events-tags-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]

    (testing "no tags"
      (let [beans [{:metric "metrics:sample" :attrs [:Count] :event "ev"}]
            event (first (generate-events "foo" beans))]
        (is (nil? (:tags event)))))

    (testing "event-defined tags"
      (let [beans [{:metric "metrics:sample" :attrs [:Count] :event "ev" :service "bar" :tags ["baz"]}]
            event (first (generate-events "foo" beans))]
        (is (= "baz" (first (:tags event))))))))

(deftest events-metrics-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]
    (let [beans [{:metric "metrics:sample" :attrs [:Count] :event "ev"}]
          event (first (generate-events "foo" beans))]
      (is (= (:Count mocked-metrics) (:metric event))))))
