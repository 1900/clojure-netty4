(defproject clj-netty4 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"local" ~(str (.toURI (java.io.File. "repo")))}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [cheshire "5.4.0"]
                 [io.netty/netty-all "4.0.29.Final"]]
  :main ^:skip-aot clj-netty4.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
