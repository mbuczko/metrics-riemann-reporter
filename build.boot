(set-env!
 :source-paths   #{"src/clj" "src/java" "test"}
 :resource-paths #{"src/clj" "src/java"}
 :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [metosin/boot-alt-test "0.3.0" :scope "test"]
                 [org.clojure/java.jmx "0.3.3"]
                 [metrics-clojure "2.9.0"]
                 [riemann-clojure-client "0.4.4"]])

;; to check the newest versions:
;; boot -d boot-deps ancient

(def +version+ "0.0.2")

(require '[adzerk.bootlaces :refer [bootlaces! build-jar push-release]]
         '[metosin.boot-alt-test :refer :all])

(bootlaces! +version+ :dont-modify-paths? true)

(task-options! pom {:project 'defunkt/metrics-riemann-reporter
                    :version +version+
                    :description "Reporting JMX metrics to riemann aggregator."
                    :url "https://github.com/mbuczko/metrics-riemann-reporter"
                    :scm {:url "https://github.com/mbuczko/metrics-riemann-reporter"}
                    :license {"name" "Eclipse Public License"
                              "url" "http://www.eclipse.org/legal/epl-v10.html"}})
