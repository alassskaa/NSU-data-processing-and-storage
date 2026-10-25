(ns clojure-1.core-test
  (:require [clojure.test :refer [deftest is]]
            [clojure-1.core :as core]))

(deftest main-test
  (is (= "Hello, Clojure!\n" (with-out-str (core/-main)))))
