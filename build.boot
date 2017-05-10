(set-env!
 :source-paths  #{"src" "test" "java"}
 :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [metosin/boot-alt-test "0.3.2" :scope "test"]
                 [org.clojure/java.jmx "0.3.4"]
                 [metrics-clojure "2.9.0"]
                 [riemann-clojure-client "0.4.5"]])

;; to check the newest versions:
;; boot -d boot-deps ancient

(require '[adzerk.bootlaces :refer [bootlaces! build-jar push-release]]
         '[metosin.boot-alt-test :refer :all])

(def +version+ "0.0.8")
(bootlaces! +version+)

(deftask deploy-clojars []
  (comp
   (javac)
   (build-jar)
   (push-release)))

(task-options! pom {:project 'defunkt/metrics-riemann-reporter
                    :version +version+
                    :description "Reporting JMX metrics to riemann aggregator."
                    :url "https://github.com/mbuczko/metrics-riemann-reporter"
                    :scm {:url "https://github.com/mbuczko/metrics-riemann-reporter"}
                    :license {"name" "Eclipse Public License"
                              "url" "http://www.eclipse.org/legal/epl-v10.html"}})
