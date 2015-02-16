(ns hface.stats
  (:require [hface.tools :refer [byte-size]]))

;; memory

(defn os-mem-used [{:keys [memory-heap-memory-used
                           memory-non-heap-memory-used
                           os-memory-total-physical-memory]}]
  (-> (+ memory-heap-memory-used memory-non-heap-memory-used)
      (* 100) ;; i.e. 100%
      (/ os-memory-total-physical-memory)))

(defn map-mem-cost [{:keys [owned-entry-memory-cost
                            backup-entry-memory-cost
                            heap-cost]}]
  ;; (.log js/console "owned: " owned-entry-memory-cost ", backups: " backup-entry-memory-cost ", heap: " heap-cost)
  (+ owned-entry-memory-cost backup-entry-memory-cost))

(defn map-mem-used [maps total-memory]
  (-> (reduce + (map map-mem-cost maps))
      (* 100) ;; i.e. 100%
      (/ total-memory)))

(defn node-total-memory [stats]
  (-> @stats :aggregated :top :os-memory-total-physical-memory))


;; members

(defn members [s]
  (map name (-> s :per-node keys)))


;; maps

(defn map-ops [m stats mtype]
  (let [m-stats (-> @stats :aggregated mtype m)
        puts (:put-rate m-stats)
        gets (:get-rate m-stats)]
    (+ puts gets)))

(defn map-highlevel [{:keys [m-name m-type]} stats]
  (if (and (seq m-name) m-type (seq @stats))
    (let [m-name (keyword m-name)
          {:keys [owned-entry-count heap-cost]} 
          (-> @stats :aggregated m-type m-name)]
      {:map-name m-name :entries owned-entry-count :mem (byte-size heap-cost)})
      {:map-name "didn't specify" :entries "no entries" :mem "takes no memory"}))
