# Drakulus

Start repl with `lein repl`.

```clojure
(def random-graph (make-graph 10 20))

(shortest-path random-graph (first (keys random-graph))
                            (last (keys random-graph)))
; => list of nodes which is the shortest path

(eccentricity random-graph (first (keys random-graph)))
; => number expressing eccentricity for `first` vertex in random-graph

(radius random-graph) ; => minimal eccentricity
(diameter random-graph) ; => maximal eccentricity

(radius random-graph ecc-distance—weight-fn) 
; => maximal eccentricity with weight calculated by distance
```

## Assumptions:

1. Graph doesn't have to be strongly connected; based on test description: 

```
(shortest-path random-graph (first (keys random-graph)) (last (keys random-graph)) ; => list of nodes which is the shortest path by edge weight between the 2 nodes, or no path if one does not exist.
```

2. `Weight <= Integer/MAX_VALUE`, or we could have long overflows. It could be
fixed by using operators that supports big integers (like `+'`, `inc'`, etc). 

3. No OOM protection is needed. OOM will be possible for graphs with more than 5k
vertices (for `Xmx` = 4096mb). Graph is optimized for no more than 512 nodes.

4. There is only one path from one vertex to another.

## Abbreviations

- G, g — graph;
- e — edges; 
- v — vertices;
- w — weight;
- adj — adjacent, adjacency.

## Graph representation:

Taking into account the `assumptions` above, graph is represented as: 

```clojure
{:1 {:2 10, :3 15}
 :2 {:3 12}}
```

Meaning there is an edge from 1 to 2 with cost 10, 1 -> 3 with cost 15, 2 -> 3 —
12. 

Using this model to query graph with `O(1)` effectively. Example:

`(contains (get G :1) :2) ; => true`

## Random connected digraph generation approach:

#### Step 1: Generate a spanning tree:

Expected time: `O(v log v)`; worst case: `O(v^3)`, where v — count of vertices
([Paper](https://www.cs.cmu.edu/~15859n/RelatedWork/Broder-GenRanSpanningTrees.pdf));

#### Step 2: add random edges 

Expected time: `O(v^2)`; `O(v)` in best case, where v — count of vertices.

#### Generation approach notes:

Random weights are generated in place for performance reason. It's also possible
to generate a graph without weights and then extend it (`O(e)`) as a third step.
Asymptotically it would be the same, but would work slower.

Another approaches is to extend one of the methods described in these papers:

- [The Random Plots Graph Generation Model for Studying Systems with Unknown Connection Structures](https://www.mdpi.com/1099-4300/24/2/297)
- [Efficient and simple generation of random simple connected graphs with
prescribed degree sequence](http://complexnetworks.fr/wp-content/uploads/2011/01/random.pdf)

## Dijkstra implementation

Using an optimized implementation without infinities in the initial queue.

[Practical optimizations and infinite
graphs](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm#cite_note-felner-9).

## Notes: 

1. You have a typo in test 
```
1. Extend the graph definition to include a weight between graph edges` 
```
It should be `between graph vertices`

2. Don't know for sure whether I should implementat dfs/bfs for my graph 
representation.
