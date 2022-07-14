(ns drakulus.core
  (:gen-class))

(comment
  "
  # Assumptions:

  1. Graph doesn't have to be strongly connected because of this text from the task: 
  `=> ... or no path if one does not exist.`
  2. Weight are <= Integer/MAX_VALUE, or we could have long overflows. 


  # Abbreviations

  G, g — graph
  e — edges
  v — verticies
  w — weight
  adj — adjecent, adjecency


  # Graph representation:

  {:1 {:2 10, :3 15}
   :2 {:3 12}}

  Meaning there is an edge from 1 to 2 with cost 10, 1->3 with cost 15, 2->3 — 12. 
  Represent it this way to have O(1) effectively for cases like this:  
  (contains (:1 G) :2) => true


  # Generation appoach:

  1 step — Generate a spanning tree 
  Paper: https://www.cs.cmu.edu/~15859n/RelatedWork/Broder-GenRanSpanningTrees.pdf
  Expected time: O(n log n); worst case: O(n^3), where n = count of verticies;

  2 step — add random edges
  O(n^2); O(n) in best case.

  # Dijkstra approach

  Using an optimized implementation without inifinities in the initial queue.

  notes: 
  1. Random weights are generated in place for performance reason. 
  Don't want to overingeneer as it's not going to be supported in the future :)

  2. You have a typo in
  `1. Extend the graph definition to include a weight between graph edges`. 
  It should be `verticies`
  ")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
