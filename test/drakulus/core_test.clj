(ns drakulus.core-test
  (:require [clojure.test :refer :all]
            [drakulus.core :refer :all]))

(defn map-values [m f]
  (zipmap (keys m) (map f (vals m))))

(defn ->graph
  "return graph map from vector like [[vertex-from vertex-to weight] ...]"
  [nodes]
  (apply merge-with into (for [[v v2 w] nodes]
                           {(n->key v) {(n->key v2) w}})))

(defn test-distances [[[frst] :as g] expected]
  (let [rslt (-> g ->graph (dijkstra (n->key frst))) 
        g-rslt (map-values rslt first)]
    (= g-rslt expected)))

(deftest dijkstra-test
  ;; https://stepik.org/lesson/368577/step/1
  (testing "check distances for graphs from stepik.org"
    (is 
      (test-distances
        [[0,1,4],[1,2,1],[2,3,8],[3,4,9],[3,5,5],
         [5,4,12],[6,5,7],[0,6,10],[1,6,2],[6,2,6]]
        {:0 0 :1 4 :2 5 :3 13 :4 22 :5 13 :6 6}))
    (is 
      (test-distances
        [[0,1,10],[0,4,3],[1,2,2],[1,4,4], [2,3,9],
         [3,2,7],[4,1,1],[4,2,8],[4,3,2]]
        {:0 0 :1 4 :2 6 :3 5 :4 3}))
    (is 
      (test-distances
        [[0,6,7],[0,1,2],[1,2,3],[1,3,3],[6,3,3],
         [3,5,1],[6,5,1],[2,5,1],[0,4,5],[4,6,2]]
        {:0 0 :1 2 :4 5 :2 5 :3 5 :5 6 :6 7})))

  (testing "check path and weight for a cycle graph"
    (is (= (:4 (dijkstra {:1 {:2 1}
                          :2 {:3 1 :4 10}
                          :3 {:1 1}} :1 :4))
           [11 [:1 :2 :4]]))))
