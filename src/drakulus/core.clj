(ns drakulus.core
  (:require
    [clojure.data.priority-map :refer [priority-map-by]]))

(defn dijkstra 
  "Usage: (dijkstra {:1 {:2 10} :2 {}} :1 :2)  
  returns map: vertex-> [distance path]" 
  [g v-start & [v-dest]]
  (loop [queue (priority-map-by #(compare (first %1) (first %2)) 
                                v-start 
                                [0 [v-start]]) ; vertex -> [distance path]
         result {}]                            ; same, but visited
    (if-let [[curr-v [dist path]] (peek queue)]
      (if (contains? result v-dest)
        result
        (recur 
          (into (pop queue)                          ; removing curr-v 
                (for [[adj-v w] (get g curr-v)       ; check neighbours
                      :when (not (get result adj-v)) ; ignore visited
                      :let [[dist-old :as old] (get queue adj-v)]]
                  (if (and dist-old (< dist-old (+ w dist)))
                    [adj-v old] 
                    [adj-v [(+ w dist) (conj path adj-v)]])))
          (assoc result curr-v (get queue curr-v))))
      result)))

(defn shortest-path [g v-start v-dest]
  (if-let [result (get (dijkstra g v-start v-dest) v-dest)]
    (second result)
    []))

(defn add-edge-with-rand [G verticies max-weight]
  (assoc-in G verticies (rand-int max-weight)))

(defn n->key [n] (keyword (str n)))

(defn rand-v [v-count]
  (n->key (rand-int v-count)))

(defn empty-graph [v-count]
  (into {} (for [i (range v-count)] [(keyword (str i)) {}])))

(defn make-spanning-tree [v-count max-weight]
  (loop [curr-v (rand-v v-count) 
         visited #{curr-v} 
         g (empty-graph v-count)]
    (if (= (count visited) v-count)
      g
      (let [adj-v (rand-v v-count)]
        (if (visited adj-v)
          (recur adj-v visited g) 
          (recur adj-v
                 (conj visited adj-v)
                 (add-edge-with-rand g [curr-v adj-v] max-weight)))))))

(defn lazy-shuffle 
  "Get vector, return lazy shuffled sequence; O(n)"
  [v]
  (lazy-seq
    (when (seq v)
      (let [idx (rand-int (count v))]
        (cons (nth v idx)
              (lazy-shuffle (pop (assoc v idx (peek v)))))))))

(defn edges-seq [v-count]
  (for [i (range v-count)
        j (range v-count)
        :when (not= i j)]
    [(n->key i) (n->key j)]))

(defn random-edges-seq [v-count]
  (lazy-shuffle 
    (vec (edges-seq v-count))))

(defn- add-edges-with-rand [g max-value edges]
  (reduce (fn [g vs] (add-edge-with-rand g vs max-value)) g edges))

(defn make-graph 
  "v — number of verticies
   e — number of directed edges, sparseness"
  [v e & {:keys [max-value] :or {max-value 100}}]

  (assert (<= (dec v) e (* v (dec v)))
          "count of edges must be in range [v-1..v*(v-1)]; v = verticies count")

  (if (= (* (dec v) v) e)
    (add-edges-with-rand {} max-value (edges-seq v))
    (let [g (make-spanning-tree v max-value)
          e (- e (dec v))] ;; v - 1 edges in the spanning tree
      (->> (random-edges-seq v)
           (filter (fn [[v1 v2]] (not (get-in g [v1 v2]))))
           (take e)
           (add-edges-with-rand g max-value)))))

(comment 

  (make-spanning-tree 5 5)
  (def a (make-graph 111 5055))

  (dijkstra a :0 :v-dest :25)

  (def G
    {:1 {:2 5 :6 1}
     :2 {:4 16}
     :3 {:5 5}
     :4 {:1 15}
     :5 {}
     :6 {:5 1}})
  
  (def random-graph (make-graph 10 10))

  (shortest-path random-graph (first (keys random-graph)) (last (keys random-graph)))

  (time
   (shortest-path a :0 :110))
  ,)
