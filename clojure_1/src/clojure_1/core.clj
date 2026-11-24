(ns clojure-1.core)

(defn- extend-word [alphabet word]
  (map #(.concat word %)
       (remove #(.endsWith word %) alphabet)))

(defn- extend-words [alphabet words]
  (reduce into [] (map #(extend-word alphabet %) words)))

(defn words-without-repeats [alphabet n]
  (reduce (fn [words _] (extend-words alphabet words))
          [""]
          (range n)))

(defn -main [& args]
  (if (empty? args)
    (prn (words-without-repeats ["a" "b" "c"] 2))
    (prn (words-without-repeats (rest args) (parse-long (first args))))))
