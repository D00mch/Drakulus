(ns drakulus.core
  "Drakulus is a library offering algorithms on weighted digraphs.

  Graph is represented as maps (almost like Adjacency list):

  {:1 {:2 5 :3 6}
   :2 {:3 4}
   :3 {:1 5}
   :4 {}}

  In the example above vertex :1 has two adjacent vertices — :2 and :3
  with weights on edges equal to 5 and 6 respectively."
  {:author "Artur Dumchev"}
  (:require
    [dorothy.core :as d]
    [dorothy.jvm :refer [show!]]
    [clojure.core.memoize :as memo]
    [clojure.data.priority-map :refer [priority-map-by]]
    #_[criterium.core :refer [quick-bench benchmark]]))

;; # Random digraph, spanning tree generation

(def ^:private n->key (comp keyword str))

(defn- empty-graph [v-count]
  (into {} (for [i (range v-count)] [(n->key i) {}])))

(defn- make-spanning-tree [^long v-count ^long max-w]
  (loop [curr-v (n->key (rand-int v-count))
         visited (java.util.HashSet.)
         g (empty-graph v-count)]
    (if (= (.size visited) v-count)
      g
      (let [adj-v (n->key (rand-int v-count))]
        (if (.contains visited adj-v)
          (recur adj-v visited g)
          (recur adj-v
                 (doto visited (.add adj-v))
                 (assoc-in g [curr-v adj-v] (rand-int max-w))))))))

(defn- lazy-shuffle
  "Get vector, return lazy shuffled sequence; O(n)"
  [v]
  (lazy-seq
    (when (seq v)
      (let [idx (rand-int (count v))]
        (cons (nth v idx)
              (lazy-shuffle (pop (assoc v idx (peek v)))))))))

(defn- all-edges-comb-seq [v-count]
  (for [^long i (range v-count)
        ^long j (range v-count)
        :when (not (== i j))]
    [(n->key i) (n->key j)]))

(defn- random-edges-seq [v-count]
  (lazy-shuffle
    (vec (all-edges-comb-seq v-count))))

(defn- add-rand-weighted-edges [g max-value edges]
  (reduce (fn [g vs] (assoc-in g vs (rand-int max-value))) g edges))

(defn make-graph
  "Args:
  `v` — number of verticies; > 0
  `e` — number of directed edges (in range [v-1..v*(v-1)])
  `max-w` — optinal; max edge weight; <= Integer/MAX_VALUE

  Returns a random graph"
  ([v e] (make-graph v e 100))
  ([v e max-w]
   (assert (<= (dec v) e (* v (dec v)))
           "edges count not in range [v-1,v*(v-1)]")
   (if (= (* (dec v) v) e)
     (add-rand-weighted-edges {} max-w (all-edges-comb-seq v))
     (let [g (make-spanning-tree v max-w)
           e (- e (dec v))] ;; removing spanning tree edges count
       (->> (random-edges-seq v)
            (remove (fn [v1->v2] (get-in g v1->v2)))
            (take e)
            (add-rand-weighted-edges g max-w))))))

;; # Distance/Path

;; nth (instead of first) makes `dijkstra` about 17% faster
(def ^:private compare-by-first #(compare (nth %1 0) (nth %2 0)))

(defn dijkstra
  "Args:
  `g` — graph,
  `v-start` — start vertex;
  `v-dest` — optional; returns result immediatelly when finds `v-dest`.

  Returns map of: vertex —> [distance path], where
  `distance` — sum of edges in between;
  `path` — verticies between v-start and other vertex, including both.

  Usage:
  (dijkstra {:1 {:2 10} :2 {}} :1 :2) ;=> {:1 [0 [:1]], :2 [10 [:1 :2]]}"
  [g v-start & [v-dest]]
  ;; both queue and result have map entries: vertex -> [distance path]
  ;; queue remains sorted by distance (0 for v-start)
  (loop [queue (priority-map-by compare-by-first v-start [0 [v-start]])
         result {}]
    (if (contains? result v-dest)
        result
        (if-let [[curr-v [^long dist path]] (peek queue)]
          (recur
            (into (pop queue)                          ; removing curr-v
                  (for [[adj-v ^long w] (get g curr-v) ; check neighbours
                        :when (not (get result adj-v)) ; ignore visited
                        :let [[^long dist-old :as old] (get queue adj-v)]]
                    (if (and dist-old (< dist-old (+ w dist)))
                      [adj-v old]
                      [adj-v [(+ w dist) (conj path adj-v)]])))
            (assoc result curr-v (get queue curr-v)))
          result))))

(defn shortest-path [g v-start v-dest]
  (if-let [result (get (dijkstra g v-start v-dest) v-dest)]
    (second result)
    []))

(defn ecc-count-edges-dist-fn ^long [d] (dec (count (nth d 1))))
(defn ecc-distance-fn ^long [d] (nth d 0))

(defn eccentricity
  "Args:
  `g` — graph,
  `v` — vertex and dist-fn
  `dist-fn` takes `[total-distance, [:as path]]`, returns number or ##Inf

  Returns max distance from `v` to any vertex, calculated by the `dist-fn`.
  ##Inf means there is no path from `v` to some vertex.

  Usage:
  (eccentricity {:1 {:2 7}, :2 {}} :1) ;=> 1
  (eccentricity {:1 {:2 7}, :2 {}} :1 ecc-count-edges-dist-fn) ;=> 1
  (eccentricity {:1 {:2 7}, :2 {}} :1 ecc-distance-fn) ;=> 7"
  ([g v] (eccentricity g v ecc-count-edges-dist-fn))
  ([g v dist-fn]
   (if-let [distances (seq (dissoc (dijkstra g v) v))]
     (reduce (fn [^long _max d] (max _max (dist-fn (nth d 1)))) 0 distances)
     ##Inf)))

(def ^:private eccentricities-memo
  (memo/fifo
    (fn [g dist-fn]
      (pmap (fn [[v _]] (eccentricity g v dist-fn)) g))
    ;; Using 2, because want to work with one graph, having 2 dist-fn
    :fifo/threshold 2))

(defn eccentricities-by [g dist-fn select-fn]
  (apply select-fn (eccentricities-memo g dist-fn)))

(defn radius
  "Args: graph, dist-fn (check `eccentricity` function docs)
  Retruns min of all eccentricities"
  ([g] (radius g ecc-count-edges-dist-fn))
  ([g dist-fn] (eccentricities-by g dist-fn min)))

(defn diameter
  "Args: graph, dist-fn (check `eccentricity` function docs)
  Retruns max of all eccentricities"
  ([g] (diameter g ecc-count-edges-dist-fn))
  ([g dist-fn] (eccentricities-by g dist-fn max)))

;; # DFS/BFS

(defn- seq-graph [d g s]
  ((fn rec-seq [explored frontier]
     (lazy-seq
       (if (empty? frontier)
         nil
         (let [v (peek frontier)
               neighbors (keys (get g v))]
           (cons v (rec-seq
                     (into explored neighbors)
                     (into (pop frontier) (remove explored neighbors))))))))
   #{s} (conj d s)))

(def ^{:doc "Args: graph, start vertex"} seq-graph-dfs
  (partial seq-graph []))

(def ^{:doc "Args: graph, start vertex"} seq-graph-bfs
  (partial seq-graph (clojure.lang.PersistentQueue/EMPTY)))

;; # Visualization

(defn dorothy-digraph [g]
  (d/digraph (for [[v es] g
                   [v2 w] es]
               [v v2 {:weight w}])))

(defn show-graph!
  "graphviz needs to be installed on the system path"
  [g]
  (-> (dorothy-digraph g)
      d/dot
      (show! {:format :svg})))

(comment

  (set! *warn-on-reflection* true)
  (set! *unchecked-math* :warn-on-boxed)

  (make-spanning-tree 5 5)

  (def G (make-graph 8 17))

  (seq-graph-dfs G :1)
  (seq-graph-bfs G :1)

  (dijkstra G :1)

  (eccentricities-by G ecc-count-edges-dist-fn vector)

  (radius G)
  (diameter G)
  (eccentricity G :3 ecc-distance-dist-fn)

  (show-graph! G)
  (show-graph! (make-graph 9 11))

  (time
    (shortest-path G :1 :999))
  ,)
