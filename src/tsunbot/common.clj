(ns tsunbot.common)

(defn select-relevant [m ks]
  (if ks (select-keys m ks) {}))

(defn levenshtein
  "Implementation of the Wagner-Fischer algorithm to calculate the Levenshtein distance of
  2 finite sequences
  reference: https://en.wikipedia.org/wiki/Wagner%E2%80%93Fischer_algorithm"
  [a b]
  (letfn [(next-row [last-row y]
            (reduce (fn [nr [x deletion]]
                      (let [x            (dec x)
                            sub-cost     (if (= (nth a x) (nth b y)) -1 0)
                            substitution (+ (last-row x) sub-cost)
                            insertion    (peek nr)]
                        (conj nr (inc (min deletion insertion substitution)))))
                    [(inc y)]
                    (next (map-indexed vector last-row))))]
    (peek (reduce next-row
                  (into [] (range (inc (count a))))
                  (range (count b))))))
