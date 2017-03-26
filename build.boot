(set-env!
 :source-paths #{"src" "test"}
 :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [ch.qos.logback/logback-classic "1.2.1"]
                 [org.clojure/java.jmx "0.3.3"]
                 [metrics-clojure "2.9.0"]
                 [riemann-clojure-client "0.4.4"]
                 [metosin/boot-alt-test "0.3.0"]])

;; to check the newest versions:
;; boot -d boot-deps ancient

(def +version+ "0.0.1")

(require '[adzerk.bootlaces :refer [bootlaces! build-jar push-release]]
         '[metosin.boot-alt-test :refer :all])

(bootlaces! +version+ :dont-modify-paths? true)

(task-options! pom {:project 'defunkt/metrics-riemann-reporter
                    :version +version+
                    :description "Metrics reporter pushing events via riemann."
                    :url "https://github.com/mbuczko/metrics-riemann-reporter"
                    :scm {:url "https://github.com/mbuczko/metrics-riemann-reporter"}
                    :license {"name" "Eclipse Public License"
                              "url" "http://www.eclipse.org/legal/epl-v10.html"}})
