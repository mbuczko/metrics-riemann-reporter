(ns reporter.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jmx :as jmx]
            [reporter.core :refer :all]))

(def mocked-metrics {:Count 1 :OneMinuteRate  2 :FiveMinuteRate 3})

(deftest events-generator-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]
    (let [beans [{:mbean "metrics:name=foo" :metrics [:Count] :tags ["pool"] :event "foo-ev"}
                 {:mbean "metrics:name=bar" :metrics [:OneMinuteRate :FiveMinuteRate] :event "bar-ev" :tags ["baz"]}]
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
      (let [beans [{:mbean "metrics:sample" :metrics [:Count] :event "ev"}]
            event (first (generate-events "foo" beans))]
        (is (= "foo.ev.Count" (:service event)))))

    (testing "event-defined service name"
      (let [beans [{:mbean "metrics:sample" :metrics [:Count] :event "ev" :service "bar"}]
            event (first (generate-events "foo" beans))]
        (is (= "bar.ev.Count" (:service event)))))))

(deftest events-tags-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]

    (testing "no tags"
      (let [beans [{:mbean "metrics:sample" :metrics [:Count] :event "ev"}]
            event (first (generate-events "foo" beans))]
        (is (nil? (:tags event)))))

    (testing "event-defined tags"
      (let [beans [{:mbean "metrics:sample" :metrics [:Count] :event "ev" :service "bar" :tags ["baz"]}]
            event (first (generate-events "foo" beans))]
        (is (= "baz" (first (:tags event))))))))

(deftest events-metrics-test
  (with-redefs [jmx/mbean (fn [metric] mocked-metrics)]
    (let [beans [{:mbean "metrics:sample" :metrics [:Count] :event "ev"}]
          event (first (generate-events "foo" beans))]
      (is (= (:Count mocked-metrics) (:metric event))))))
