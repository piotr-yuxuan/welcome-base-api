(ns piotr-yuxuan.welcome-base-api
  (:require [piotr-yuxuan.closeable-map :refer [closeable-map]])
  (:gen-class))

(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))
