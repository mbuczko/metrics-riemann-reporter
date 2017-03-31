(ns reporter.beans
  (:require [clojure.java.jmx :as jmx]))

(defn init-heap-mbean [ch-usage]
  (let [heap-ref (ref {:HeapInit (.getHeapInit ch-usage)
                       :HeapUsed (.getHeapUsed ch-usage)
                       :HeapMax  (.getHeapMax  ch-usage)
                       :HeapCommitted (.getHeapCommitted ch-usage)})]
    (jmx/register-mbean
     (jmx/create-bean heap-ref)
     "java.lang:name=Heap")
    heap-ref))

(defn init-cpu-mbean [ch-usage]
  (let [cpu-ref (ref {:CpuUsed (.getCpuUsed ch-usage)})]
    (jmx/register-mbean
     (jmx/create-bean cpu-ref)
     "java.lang:name=Cpu")
    cpu-ref))

(defn unregister-heap-mbean []
  (jmx/unregister-mbean "java.lang:name=Heap"))

(defn unregister-cpu-mbean []
  (jmx/unregister-mbean "java.lang:name=Cpu"))

(defn unregister-cpu-heap-mbeans []
  (unregister-cpu-mbean)
  (unregister-heap-mbean))
