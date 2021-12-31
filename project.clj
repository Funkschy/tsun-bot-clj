(defproject tsunbot2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/funkschy/tsun-bot-clj"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.suskalo/discljord "1.3.0" :exclusions [org.clojure/clojure]]
                 [org.clojure/data.json "2.4.0"]
                 [org.xerial/sqlite-jdbc "3.36.0"]]
  :main ^:skip-aot tsunbot.core
  :repl-options {:init-ns tsunbot.core}
  :profiles {:uberjar {:aot :all}})
