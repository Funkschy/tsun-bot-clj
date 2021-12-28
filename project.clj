(defproject tsunbot2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.suskalo/discljord "1.3.0"]
                 [org.clojure/data.json "2.4.0"]]
  :main tsunbot.core
  :aot [tsunbot.core]
  :repl-options {:init-ns tsunbot.core})
