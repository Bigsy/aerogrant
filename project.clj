(defproject org.clojars.bigsy/aerogrant "0.1.6"
  :description "mashup of aero and integrant with some nice addons"
  :url "https://github.com/Bigsy/aerogrant"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [aero "1.1.6"]
                 [cheshire "5.11.0"]
                 [com.cognitect.aws/api "0.8.686"]
                 [com.cognitect.aws/sts "847.2.1387.0"]
                 [com.cognitect.aws/endpoints "1.1.12.626"]
                 [com.cognitect.aws/secretsmanager "847.2.1387.0"]
                 [integrant "0.8.1"]]
  :repl-options {:init-ns aerogrant.core})
